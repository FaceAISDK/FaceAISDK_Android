package com.faceAI.demo.SysCamera.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.base.utils.BrightnessUtil;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
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
public class MyCameraXFragment extends Fragment {
    private static final String CAMERA_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";  //焦距缩放比例
    private static final String CAMERA_LENS_FACING = "CAMERA_LENS_FACING";  //前后配置
    private static final String CAMERA_ROTATION = "CAMERA_ROTATION";  //旋转
    private int rotation = Surface.ROTATION_0; //旋转角度
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


    public MyCameraXFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.analyzeDataCallBack = callback;
    }

    public interface onAnalyzeData {
        void analyze(@NonNull ImageProxy imageProxy);
    }

    public static MyCameraXFragment newInstance(CameraXBuilder cameraXBuilder) {
        MyCameraXFragment fragment = new MyCameraXFragment();
        Bundle args = new Bundle();
        args.putInt(CAMERA_LENS_FACING, cameraXBuilder.getCameraLensFacing());
        args.putFloat(CAMERA_LINEAR_ZOOM, cameraXBuilder.getLinearZoom());
        args.putInt(CAMERA_ROTATION, cameraXBuilder.getRotation());
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

//        getCameraLevel();

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

            //imageAnalysis 人脸识别分析设置。默认分辨率640*480。根据你的场景，摄像头特性和硬件配置设置合理的参数
            imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetRotation(rotation)
                    .build();

            //摄像头画面预览默认分辨率也是640*480。
            preview = new Preview.Builder()
                    .setTargetRotation(rotation)
                    .build();

            previewView = rootView.findViewById(R.id.previewView);
            //预览画面渲染模式：高性能模式
            previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

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
            imageAnalysis.setAnalyzer(executorService, imageProxy -> {
                if (scaleX == 0f || scaleY == 0f) {
                    setScaleXY(imageProxy);
                } else {
                    if(analyzeDataCallBack!=null){
                        analyzeDataCallBack.analyze(imageProxy);
                    }
                }
                imageProxy.close();
            });

            try {
                cameraProvider.unbindAll();
                // Attach use cases to the camera with the same lifecycle owner
                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview, imageAnalysis);

                //并非所有的相机支持焦距控制
                camera.getCameraControl().setLinearZoom(linearZoom);

            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 摄像头硬件等级判断，不稳定的等级Toast 提示
     *
     */
    private void getCameraLevel(){
        try {
            //判断当前摄像头等级 ,Android 9以上才支持判断
            CameraManager cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
            String cameraId =Integer.toString(cameraLensFacing);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer level=characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if(level!=null&& level !=CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
                    && level !=CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL){
                if(FaceSDKConfig.isDebugMode(requireContext())){
                    Toast.makeText(requireContext(),"Camera level low !",Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("getCameraLevel", Objects.requireNonNull(e.getMessage()));
        }
    }


    /**
     * 切换摄像头立即生效自行处理
     */
    public void switchCamera() {
        //
    }


    /**
     * 并不是都要使用CameraX, 开发人员也可以使用Camera1 相机管理摄像头
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        releaseCamera();  //一般不需手动处理
    }


    /**
     * 手动释放所有资源（不同硬件平台处理方式不一样），一般资源释放会和页面销毁自动联动
     */
    public void releaseCamera() {
        if(executorService!=null&&!executorService.isTerminated()){
            executorService.shutdownNow();
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll(); // 解绑所有用例
            cameraProvider = null;
        }

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer(); // 清除图像分析
        }
        if (previewView != null) {
            preview.setSurfaceProvider(null);
        }
        camera = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        BrightnessUtil.setBrightness(requireActivity(), 0.9f);
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