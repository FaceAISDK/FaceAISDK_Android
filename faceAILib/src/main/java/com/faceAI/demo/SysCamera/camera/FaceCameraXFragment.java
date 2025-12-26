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
 *  更高的兼容性改造，2025.12.26。 炫彩活体改造版本基于CameraX 1.4.2，AbsFaceCameraXFragment
 *  低配置设备要加快设备首次启动时间参考配置{@link com.faceAI.demo.FaceApplication}
 *
 *
 *  @author FaceAISDK.Service@gmail.com
 */
public class FaceCameraXFragment extends AbsFaceCameraXFragment {
    // 常量定义
    private static final String KEY_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";
    private static final String KEY_LENS_FACING = "CAMERA_LENS_FACING";
    private static final String KEY_ROTATION = "CAMERA_ROTATION";
    private static final String KEY_IS_HIGH_RES = "CAMERA_SIZE_HIGH";
    private static final String TAG = "FaceCameraXFragment";

    // 配置参数
    private int mCameraLensFacing = CameraSelector.LENS_FACING_FRONT;
    private float mLinearZoom = 0f;
    private int mRotation = Surface.ROTATION_0;
    private boolean isHighResolution = false;

    // 运行时状态
    private volatile int mImageWidth = 0;
    private volatile int mImageHeight = 0;

    // CameraX 组件
    private ProcessCameraProvider mCameraProvider;
    private CameraControl mCameraControl;
    private PreviewView mPreviewView;
    private ExecutorService mExecutorService;
    private onAnalyzeData mAnalyzeListener;

    public FaceCameraXFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.mAnalyzeListener = callback;
    }

    public interface onAnalyzeData {
        //用于SDK内部数据分析
        void analyze(@NonNull ImageProxy imageProxy);
        //回调图片帧大小，以便画框UI处理
        default void backImageSize(int imageWidth, int imageHeight) {
        }
    }

    @Override
    public @NotNull CameraControl getCameraControl() {
        return mCameraControl;
    }

    @Override
    public int getCameraLensFacing() {
        return mCameraLensFacing;
    }

    public static FaceCameraXFragment newInstance(CameraXBuilder builder) {
        FaceCameraXFragment fragment = new FaceCameraXFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_LENS_FACING, builder.getCameraLensFacing());
        args.putFloat(KEY_LINEAR_ZOOM, builder.getLinearZoom());
        args.putInt(KEY_ROTATION, builder.getRotation());
        args.putBoolean(KEY_IS_HIGH_RES, builder.getCameraSizeHigh());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mCameraLensFacing = args.getInt(KEY_LENS_FACING, CameraSelector.LENS_FACING_FRONT);
            mLinearZoom = args.getFloat(KEY_LINEAR_ZOOM, 0f);
            mRotation = args.getInt(KEY_ROTATION, Surface.ROTATION_0);
            isHighResolution = args.getBoolean(KEY_IS_HIGH_RES, false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.face_camerax_fragment, container, false);
        mPreviewView = rootView.findViewById(R.id.previewView);
        initCameraX();
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //关闭线程池，防止内存泄漏
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }

    private void initCameraX() {
        // 每次重新初始化时重置尺寸，防止横竖屏切换等导致尺寸变化未更新
        mImageWidth = 0;
        mImageHeight = 0;

        mExecutorService = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            // Fragment 可能已销毁，检查上下文安全性
            if (!isAdded() || getContext() == null) {
                return;
            }
            try {
                mCameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider init failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        // 1. 配置 ImageAnalysis
        ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(mRotation);

        if (isHighResolution) {
            // 远距离或高精度场景
            analysisBuilder.setTargetResolution(new Size(1280, 720));
        } else {
            // 默认场景，性能优先
            analysisBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
        }
        ImageAnalysis mImageAnalysis = analysisBuilder.build();

        // 2. 配置 Preview
        Preview mPreview = new Preview.Builder()
                .setTargetRotation(mRotation)
                .build();

        // 3. 配置 PreviewView
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        // 4. 构建 CameraSelector (兼容逻辑)
        CameraSelector mCameraSelector = createCompatibleCameraSelector();

        // 5. 设置分析器
        mImageAnalysis.setAnalyzer(mExecutorService, imageProxy -> {
            // 这里位于子线程
            if (mImageWidth == 0 || mImageHeight == 0) {
                mImageWidth = imageProxy.getWidth();
                mImageHeight = imageProxy.getHeight();
                if (mAnalyzeListener != null) {
                    mAnalyzeListener.backImageSize (mImageWidth, mImageHeight);
                }
            }

            if (mAnalyzeListener != null) {
                mAnalyzeListener.analyze(imageProxy);
            }
            // 必须关闭，否则不会收到下一帧
            imageProxy.close();
        });

        // 6. 绑定生命周期
        try {
            mCameraProvider.unbindAll();
            Camera mCamera = mCameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(),
                    mCameraSelector,
                    mPreview,
                    mImageAnalysis);

            mCameraControl = mCamera.getCameraControl();
            mCameraControl.setLinearZoom(mLinearZoom);
        } catch (Exception e) {
            Log.e(TAG, "Camera bind failed: " + e.getMessage());
        }
    }

    /**
     * 构建兼容的 CameraSelector
     * 针对部分 RK 设备/工控机接口定义混乱的情况进行降级处理
     */
    private CameraSelector createCompatibleCameraSelector() {
        CameraSelector.Builder builder = new CameraSelector.Builder();

        int preferredLensFacing = mCameraLensFacing;
        // 另一种可能的 LensFacing (如果首选是 Front，备选就是 Back)
        int fallbackLensFacing = (preferredLensFacing == CameraSelector.LENS_FACING_FRONT)
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;

        if (hasCamera(mCameraProvider, preferredLensFacing)) {
            // 1. 完美匹配
            builder.requireLensFacing(preferredLensFacing);
        } else if (hasCamera(mCameraProvider, fallbackLensFacing)) {
            // 2. 降级匹配：找不到指定方向，就用另一个方向
            Log.w(TAG, "Preferred camera not found, fallback to opposite facing.");
            builder.requireLensFacing(fallbackLensFacing);
        } else {
            // 3. 暴力兜底：都找不到（可能是 External USB 摄像头），不过滤，接受任意摄像头
            Log.w(TAG, "Standard cameras not found, allowing ALL cameras (External/USB).");
            builder.addCameraFilter(cameraInfos -> cameraInfos);
        }

        return builder.build();
    }

    private boolean hasCamera(ProcessCameraProvider provider, int lensFacing) {
        try {
            return provider.hasCamera(new CameraSelector.Builder().requireLensFacing(lensFacing).build());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isFrontCamera() {
        return mCameraLensFacing == CameraSelector.LENS_FACING_FRONT;
    }
}