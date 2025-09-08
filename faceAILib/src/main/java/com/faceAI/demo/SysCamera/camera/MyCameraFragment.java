package com.faceAI.demo.SysCamera.camera;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 摄像头的管理，使用Google CameraX,用户可以根据平台特性和业务需求自行拓展
 * CameraX 说明：https://developer.android.com/codelabs/camerax-getting-started?hl=zh-cn
 *
 * 你也可以使用老的Camera2 相机等方式管理摄像头，通过预览流回调数据转为Bitmap 后持续送入SDK
 * FaceSearchEngine.Companion.getInstance().runSearchWithBitmap(bitmap); //不要在主线程调用
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class MyCameraFragment extends Fragment implements CameraXConfig.Provider{
    private static final String CAMERA_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";  //焦距缩放比例
    private static final String CAMERA_LENS_FACING = "CAMERA_LENS_FACING";  //前后配置
    private static final String CAMERA_ROTATION = "CAMERA_ROTATION";  //旋转
    private int rotation = Surface.ROTATION_0; //旋转角度
    private long lastAnalyzedTimestamp;
    private int cameraLensFacing = 0; //默认前置摄像头
    private float scaleX = 0f, scaleY = 0f;
    private float linearZoom = 0.01f; //焦距
    private float mDefaultBright;
    private ProcessCameraProvider cameraProvider;
    private onAnalyzeData analyzeDataCallBack;
    private ExecutorService executorService;
    private CameraSelector cameraSelector;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private Camera camera;
    private PreviewView previewView;


    /**
     * CameraX 会枚举和查询设备上可用摄像头的特性。由于 CameraX 需要与硬件组件通信，因此对每个摄像头执行此过程可能
     * 需要较长时间，尤其是在低端设备上。如果您的应用仅使用设备上的特定摄像头（例如默认前置摄像头）您可以将 CameraX
     * 设置为忽略其他摄像头，从而缩短应用所用摄像头的启动延迟时间。
     *
     * 注意本配置需要放置在你的Application类中，并添加到 AndroidManifest.xml 文件中
     *
     * 更多：https://developer.android.com/media/camera/camerax/configuration?hl=zh-cn
     * @return CameraXConfig
     */
    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                // 设置唯一固定摄像头，需要配置在Application中
                //.setAvailableCamerasLimiter(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setMinimumLoggingLevel(Log.ERROR)
                .build();
    }


    public MyCameraFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.analyzeDataCallBack = callback;
    }

    public interface onAnalyzeData {
        void analyze(@NonNull ImageProxy imageProxy);
    }

    public static MyCameraFragment newInstance(CameraXBuilder cameraXBuilder) {
        MyCameraFragment fragment = new MyCameraFragment();
        Bundle args = new Bundle();
        args.putInt(CAMERA_LENS_FACING, cameraXBuilder.getCameraLensFacing());
        args.putFloat(CAMERA_LINEAR_ZOOM, cameraXBuilder.getLinearZoom());
        args.putInt(CAMERA_ROTATION, cameraXBuilder.getRotation());
//        args.putSerializable(CAMERA_SIZE, cameraXBuilder.getSize()); //默认一种
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraLensFacing = getArguments().getInt(CAMERA_LENS_FACING, 0); //默认的摄像头
            linearZoom = getArguments().getFloat(CAMERA_LINEAR_ZOOM, 0.01f);
            rotation = getArguments().getInt(CAMERA_ROTATION, Surface.ROTATION_0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.my_camera_fragment, container, false);
        mDefaultBright = BrightnessUtil.getBrightness(requireActivity());
        initCameraXAnalysis(rootView);
        return rootView;
    }

    /**
     * 初始化相机,使用CameraX
     *
     */
    private void initCameraXAnalysis(View rootView) {
        executorService = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        //图像预览和摄像头原始数据回调 暴露，以便后期格式转换和处理
        //图像编码默认格式 YUV_420_888。
        cameraProviderFuture.addListener(() -> {

            // Camera provider is now guaranteed to be available
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceAI SDK", "\ncameraProviderFuture.get() 发生错误！\n" + e.toString());
            }

            preview = new Preview.Builder()
                    .setTargetRotation(rotation)
                    .build();

            previewView = rootView.findViewById(R.id.previewView);
            //高性能模式
            previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

            imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetRotation(rotation)
                    .build();

            if (cameraLensFacing == 0) {
                // Choose the camera by requiring a lens facing
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
            } else {
                // Choose the camera by requiring a lens facing
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
            }

            // Connect the preview use case to the previewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            lastAnalyzedTimestamp = System.currentTimeMillis() + 500; //延迟半秒执行
            imageAnalysis.setAnalyzer(executorService, imageProxy -> {

                    try {
                        if (scaleX == 0f || scaleY == 0f) {
                            setScaleXY(imageProxy);
                        } else {
                            //控制10帧每秒,SDK内部有背压处理，防止用户业务上其他处理耗费性能
                            if (System.currentTimeMillis() - lastAnalyzedTimestamp > 200) {
                                analyzeDataCallBack.analyze(imageProxy);
                                lastAnalyzedTimestamp = System.currentTimeMillis();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
                    } finally {
                        imageProxy.close();
                    }

            });

            try {
                cameraProvider.unbindAll();
                // Attach use cases to the camera with the same lifecycle owner
                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview, imageAnalysis);
                camera.getCameraControl().setLinearZoom(linearZoom);

            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 并不是都要使用CameraX, 开发人员也可以使用Camera1 相机管理摄像头
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //前面，cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
        //CameraX 相关都是绑定Activity生命周期的，一般不需要手动管理，特殊平台咨询平台技术供应商
    }

    @Override
    public void onResume() {
        super.onResume();
        BrightnessUtil.setBrightness(requireActivity(), 1.0f);
    }

    @Override
    public void onStop() {
        super.onStop();
        //设置为原来亮度
        BrightnessUtil.setBrightness(requireActivity(), mDefaultBright);
    }

    /**
     * 计算缩放比例
     */
    private void setScaleXY(ImageProxy imageProxy) {
        float max = imageProxy.getWidth();
        float min = imageProxy.getHeight();
        if (max < min) { //交换
            float temp = max;
            max = min;
            min = temp;
        }
        if (previewView.getWidth() > previewView.getHeight()) {
            scaleX = (float) previewView.getWidth() / max;
            scaleY = (float) previewView.getHeight() / min;
        } else {
            scaleX = (float) previewView.getWidth() / min;
            scaleY = (float) previewView.getHeight() / max;
        }
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

}