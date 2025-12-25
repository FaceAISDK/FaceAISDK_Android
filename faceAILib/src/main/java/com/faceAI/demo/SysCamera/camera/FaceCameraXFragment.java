package com.faceAI.demo.SysCamera.camera;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.ai.face.base.view.camera.AbsFaceCameraXFragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.R;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 炫彩活体改造版本基于CameraX1.4.2，需 extends AbsFaceCameraXFragment(底层封装非业务相关特性)
 * 2025.12.24 提高设备兼容性
 *
 *
 * CameraX 说明：https://developer.android.com/codelabs/camerax-getting-started?hl=zh-cn
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceCameraXFragment extends AbsFaceCameraXFragment {
    private static final String CAMERA_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";  //焦距缩放比例
    private static final String CAMERA_LENS_FACING = "CAMERA_LENS_FACING";  //前后配置
    private static final String CAMERA_ROTATION = "CAMERA_ROTATION";  //旋转
    private static final String CAMERA_SIZE_HIGH = "CAMERA_SIZE_HIGH";  //是否高分辨率用于分析
    private int cameraLensFacing = 0; //默认前置摄像头，
    private int imageWidth, imageHeight;
    private float linearZoom = 0f; //焦距
    private boolean cameraSizeHigh = false;
    private int rotation = Surface.ROTATION_0; //旋转角度
    private ProcessCameraProvider cameraProvider;
    private onAnalyzeData analyzeDataCallBack;
    private ExecutorService executorService;
    private CameraSelector cameraSelector;
    private ImageAnalysis imageAnalysis;
    private CameraControl cameraControl;
    private Preview preview;
    private Camera camera;
    private PreviewView previewView;

    public FaceCameraXFragment() {}

    //底层控制,AbsFaceCameraXFragment需要
    @Override
    public @NotNull CameraControl getCameraControl() {
        return cameraControl;
    }

    //当前使用的摄像头
    @Override
    public int getCameraLensFacing() {
        return cameraLensFacing;
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.analyzeDataCallBack = callback;
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
        View rootView = inflater.inflate(R.layout.face_camerax_fragment, container, false);
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

        cameraProviderFuture.addListener(() -> {
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
                        .setTargetResolution(new Size(1280, 720)) //不应该写死，从支持的分辨率中选择最接近的一个
                        .build();
            } else {
                //系统默认匹配分辨率，炫彩活体检测使用默认的，人脸搜索可以用High
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetRotation(rotation)  //画面选择角度
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)  //首选4:3，640*480 最好
                        .build();
            }
            preview = new Preview.Builder()
                    .setTargetRotation(rotation)
                    .build();

            previewView = rootView.findViewById(R.id.previewView);
            previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

            //20251202，为了后面炫彩活体做准备改为 FIT_CENTER
            //https://developer.android.com/media/camera/camerax/preview?hl=zh-cn
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
            CameraSelector.Builder builder = new CameraSelector.Builder();

            if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
                if (hasCamera(cameraProvider, CameraSelector.LENS_FACING_FRONT)) {
                    builder.requireLensFacing(CameraSelector.LENS_FACING_FRONT);
                } else if (hasCamera(cameraProvider, CameraSelector.LENS_FACING_BACK)) {
                    builder.requireLensFacing(CameraSelector.LENS_FACING_BACK);
                    Log.w("FaceSDK", "前置未找到，回退到后置");
                } else {
                    // 都没有？可能是 USB 外接摄像头自己调试调整哈 (LENS_FACING_EXTERNAL)
                    // CameraX 默认策略可能会包含 External，这里可以不设置 requireLensFacing，让它选第一个
                    Log.w("FaceSDK", "标准摄像头未找到，尝试使用默认/外接摄像头");
                }
            } else {
                // 同理处理后置优先的情况...
                if (hasCamera(cameraProvider, CameraSelector.LENS_FACING_BACK)) {
                    builder.requireLensFacing(CameraSelector.LENS_FACING_BACK);
                } else if (hasCamera(cameraProvider, CameraSelector.LENS_FACING_FRONT)) {
                    builder.requireLensFacing(CameraSelector.LENS_FACING_FRONT);
                }
            }

            cameraSelector = builder.build();

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

                cameraControl.setLinearZoom(linearZoom);
            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }


    // 判断是否有对应的摄像头，定制设备比较混乱
    private boolean hasCamera(ProcessCameraProvider provider, int lensFacing) {
        try {
            return provider.hasCamera(new CameraSelector.Builder().requireLensFacing(lensFacing).build());
        } catch (Exception e) {
            return false;
        }
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
    @Deprecated
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