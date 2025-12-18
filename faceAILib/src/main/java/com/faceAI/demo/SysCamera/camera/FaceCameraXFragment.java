package com.faceAI.demo.SysCamera.camera;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.ai.face.base.view.camera.AbsFlashColorCameraFragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 炫彩活体改造版本基于CameraX1.4.2，需 extends AbsFlashColorCameraFragment
 *
 * CameraX 说明：https://developer.android.com/codelabs/camerax-getting-started?hl=zh-cn
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceCameraXFragment extends AbsFlashColorCameraFragment {
    private static final String CAMERA_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";  //焦距缩放比例
    private static final String CAMERA_LENS_FACING = "CAMERA_LENS_FACING";  //前后配置
    private static final String CAMERA_ROTATION = "CAMERA_ROTATION";  //旋转
    private static final String CAMERA_SIZE_HIGH = "CAMERA_SIZE_HIGH";  //是否高分辨率用于分析
    private int rotation = Surface.ROTATION_0; //旋转角度
    private int cameraLensFacing = 0; //默认前置摄像头
    private int imageWidth, imageHeight;
    private float linearZoom = 0f; //焦距
    private boolean cameraSizeHigh = false;
    private ProcessCameraProvider cameraProvider;
    private onAnalyzeData analyzeDataCallBack;
    private ExecutorService executorService;
    private CameraSelector cameraSelector;
    private ImageAnalysis imageAnalysis;
    private CameraControl cameraControl;
    private Preview preview;
    private Camera camera;
    private PreviewView previewView;

    public FaceCameraXFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.analyzeDataCallBack = callback;
    }

    /**
     * 底层控制逻辑需要使用
     *
     * @return
     */
    @Override
    public @NotNull CameraControl getCameraControl() {
        return cameraControl;
    }


    public interface onAnalyzeData {
        void analyze(@NonNull ImageProxy imageProxy);

        default void backImageSize(int imageWidth, int imageHeight) {
        }
    }

    public static FaceCameraXFragment newInstance(CameraXBuilder cameraXBuilder) {
        FaceCameraXFragment fragment = new FaceCameraXFragment();
        Bundle args = new Bundle();
        args.putInt(CAMERA_LENS_FACING, cameraXBuilder.getCameraLensFacing());
        args.putFloat(CAMERA_LINEAR_ZOOM, cameraXBuilder.getLinearZoom());
        args.putInt(CAMERA_ROTATION, cameraXBuilder.getRotation());
        args.putBoolean(CAMERA_SIZE_HIGH, cameraXBuilder.getCameraSizeHigh());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraLensFacing = getArguments().getInt(CAMERA_LENS_FACING, 0); //默认的摄像头
            linearZoom = getArguments().getFloat(CAMERA_LINEAR_ZOOM, 0f);
            rotation = getArguments().getInt(CAMERA_ROTATION, Surface.ROTATION_0);
            cameraSizeHigh = getArguments().getBoolean(CAMERA_SIZE_HIGH, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.face_camerax_fragment, container, false);
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
                Log.e("FaceAI SDK", "\ncameraProviderFuture.get() 发生错误！\n" + e.getMessage());
            }

            if (cameraSizeHigh) {
                //送入人脸识别FaceAISDK画面分析设置。根据你的场景，摄像头特性和硬件配置设置合理的参数
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetRotation(rotation) //画面选择角度
                        .setTargetResolution(new Size(1280, 720)) //从支持的分辨率中选择最接近的一个
                        .build();
            } else {
                //系统默认匹配分辨率
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetRotation(rotation)  //画面选择角度
                        .build();
            }
            preview = new Preview.Builder()
                    .setTargetRotation(rotation)
                    .build();

            previewView = rootView.findViewById(R.id.previewView);

            //预览画面渲染模式：高性能模式
            previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

            //20251102，为了后面炫彩活体做准备（默认的FILL_CENTER会把人脸区域放很大）
            //https://developer.android.com/media/camera/camerax/preview?hl=zh-cn
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

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
                if (imageWidth == 0f || imageHeight == 0f) {
                    imageWidth = imageProxy.getWidth();
                    imageHeight = imageProxy.getHeight();
                    if (analyzeDataCallBack != null) {
                        analyzeDataCallBack.backImageSize(imageWidth, imageHeight);
                    }
                } else {
                    if (analyzeDataCallBack != null) {
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
                cameraControl = camera.getCameraControl();

                //并非所有的相机支持焦距控制
                cameraControl.setLinearZoom(linearZoom);
            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 摄像头硬件等级判断，不稳定的等级Toast 提示
     *
     */
    private void getCameraLevel() {
        try {
            //判断当前摄像头等级 ,Android 9以上才支持判断
            CameraManager cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = Integer.toString(cameraLensFacing);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level != null && level != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
                    && level != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                if (FaceSDKConfig.isDebugMode(requireContext())) {
                    Toast.makeText(requireContext(), "Camera level low !", Toast.LENGTH_LONG).show();
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

    }

    /**
     * 有些定制设备把后置摄像头接口画面当前置用的自己改一下逻辑
     *
     * @return 是否前置摄像头
     */
    public boolean isFrontCamera() {
        return cameraLensFacing == CameraSelector.LENS_FACING_FRONT;
    }

    /**
     * 手动释放所有资源（不同硬件平台处理方式不一样），一般资源释放会和页面销毁自动联动
     */
    public void releaseCamera() {
        if (executorService != null && !executorService.isTerminated()) {
            executorService.shutdownNow();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll(); //解绑所有用例
            cameraProvider = null;
        }
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer(); //清除图像分析
        }
        if (previewView != null) {
            preview.setSurfaceProvider(null);
        }
        camera = null;
    }

}