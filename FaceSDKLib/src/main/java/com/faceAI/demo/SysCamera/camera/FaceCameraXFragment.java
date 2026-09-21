package com.faceAI.demo.SysCamera.camera;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 * 自定义FaceSDK相机
 * 低配置设备要加快相机首次启动时间参考配置{@link com.faceAI.demo.FaceApplication}
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceCameraXFragment extends AbsFaceCameraXFragment {
    // 常量定义
    private static final String KEY_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";
    private static final String KEY_LENS_FACING = "CAMERA_LENS_FACING";
    private static final String KEY_ROTATION = "CAMERA_ROTATION";
    private static final String KEY_IS_HIGH_RES = "CAMERA_SIZE_HIGH";
    private static final String TAG = "FaceCameraXFragment";

    // 配置参数
    private volatile int mCameraLensFacing = CameraSelector.LENS_FACING_FRONT;
    private float mLinearZoom = 0f;
    private int mRotation = Surface.ROTATION_0;
    private boolean isHighResolution = false;

    // 运行时状态
    private volatile int mImageWidth = 0;
    private volatile int mImageHeight = 0;
    private volatile long mAnalyzerGeneration = 0;
    private int mCameraSessionGeneration = 0;
    private boolean mPendingSwitchNotification = false;

    // CameraX 组件
    private ProcessCameraProvider mCameraProvider;
    private volatile CameraControl mCameraControl;
    private PreviewView mPreviewView;
    private Preview mPreview;
    private ImageAnalysis mImageAnalysis;
    private CameraSelector mCameraSelector;
    private ExecutorService mExecutorService;
    private volatile onAnalyzeData mAnalyzeListener;
    private volatile OnCameraSwitchListener mCameraSwitchListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public FaceCameraXFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.mAnalyzeListener = callback;
    }

    /**
     * 相机切换结果回调，始终在主线程触发。
     */
    public interface OnCameraSwitchListener {
        void onCameraSwitched(@CameraSelector.LensFacing int lensFacing);

        default void onCameraSwitchFailed(@CameraSelector.LensFacing int requestedLensFacing) {
        }
    }

    public void setOnCameraSwitchListener(@Nullable OnCameraSwitchListener listener) {
        mCameraSwitchListener = listener;
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

    /**
     * 在 Fragment 运行期间切换前/后摄像头。可在任意线程调用。
     */
    public void switchCamera() {
        runOnMainThread(() -> {
            int targetLensFacing = mCameraLensFacing == CameraSelector.LENS_FACING_FRONT
                    ? CameraSelector.LENS_FACING_BACK
                    : CameraSelector.LENS_FACING_FRONT;
            switchCameraOnMainThread(targetLensFacing);
        });
    }

    /**
     * 在 Fragment 运行期间切换到指定的前/后摄像头。可在任意线程调用。
     */
    public void switchCamera(@CameraSelector.LensFacing int lensFacing) {
        if (!isSupportedLensFacing(lensFacing)) {
            throw new IllegalArgumentException("Only front and back cameras can be selected");
        }
        runOnMainThread(() -> switchCameraOnMainThread(lensFacing));
    }

    /**
     * CameraProvider 准备完成且设备存在另一颗前/后摄像头时返回 true。
     */
    public boolean canSwitchCamera() {
        ProcessCameraProvider provider = mCameraProvider;
        if (provider == null) {
            return false;
        }
        int targetLensFacing = mCameraLensFacing == CameraSelector.LENS_FACING_FRONT
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
        return hasCamera(provider, targetLensFacing);
    }

    public static FaceCameraXFragment newInstance(CameraXBuilder builder) {
        FaceCameraXFragment fragment = new FaceCameraXFragment();
        fragment.mCameraLensFacing = builder.getCameraLensFacing();
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
        if (savedInstanceState != null) {
            mCameraLensFacing = savedInstanceState.getInt(KEY_LENS_FACING, mCameraLensFacing);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_LENS_FACING, mCameraLensFacing);
        super.onSaveInstanceState(outState);
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
    public void onDestroyView() {
        mCameraSessionGeneration++;
        mAnalyzerGeneration++;

        if (mImageAnalysis != null) {
            mImageAnalysis.clearAnalyzer();
        }
        unbindOwnedUseCases();

        // 关闭与当前 View 对应的分析线程，避免重建 View 时泄漏旧线程池。
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
        mExecutorService = null;
        mCameraControl = null;
        mCameraProvider = null;
        mCameraSelector = null;
        mImageAnalysis = null;
        mPreview = null;
        mPreviewView = null;
        super.onDestroyView();
    }

    private void initCameraX() {
        mImageWidth = 0;
        mImageHeight = 0;

        mExecutorService = Executors.newSingleThreadExecutor();
        final int sessionGeneration = ++mCameraSessionGeneration;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            if (sessionGeneration != mCameraSessionGeneration
                    || !isAdded()
                    || getContext() == null
                    || mPreviewView == null) {
                return;
            }
            try {
                mCameraProvider = cameraProviderFuture.get();
                boolean notifySwitch = mPendingSwitchNotification;
                mPendingSwitchNotification = false;
                // Provider 尚未就绪时收到切换请求，也要保留初始化阶段的兼容降级能力，
                // 避免目标镜头缺失时整个预览无法启动。
                bindCameraUseCases(mCameraLensFacing, true, notifySwitch);
            } catch (ExecutionException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Log.e(TAG, "CameraProvider init failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private boolean bindCameraUseCases(@CameraSelector.LensFacing int requestedLensFacing,
                                       boolean allowFallback,
                                       boolean notifySwitch) {
        ProcessCameraProvider cameraProvider = mCameraProvider;
        PreviewView previewView = mPreviewView;
        ExecutorService executorService = mExecutorService;
        if (cameraProvider == null || previewView == null || executorService == null
                || executorService.isShutdown()) {
            return false;
        }

        CameraSelection selection = createCompatibleCameraSelector(requestedLensFacing, allowFallback);
        if (selection == null) {
            Log.w(TAG, "Requested camera is not available. lensFacing=" + requestedLensFacing);
            notifyCameraSwitchFailed(requestedLensFacing, notifySwitch);
            return false;
        }

        // 1. 配置 ImageAnalysis
        ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(mRotation);

        if (isHighResolution) {
            // 远距离识别，但是性能会下降，定制设备需要配置摄像头支持的分辨率。请开发工程师切换后调试效果！
            analysisBuilder.setTargetResolution(new Size(1280, 720));
        } else {
            // 默认场景，性能优先
            analysisBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
        }
        ImageAnalysis newImageAnalysis = analysisBuilder.build();

        // 2. 配置 Preview
        Preview newPreview = new Preview.Builder()
                .setTargetRotation(mRotation)
                .build();

        // 3. 配置 PreviewView
//        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);//兼容模式可以选择相机角度
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        newPreview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 4. 设置分析器。generation 用于丢弃切换后仍排队的旧相机帧。
        final long analyzerGeneration = mAnalyzerGeneration + 1;
        newImageAnalysis.setAnalyzer(executorService, imageProxy -> {
            try {
                if (analyzerGeneration != mAnalyzerGeneration) {
                    return;
                }

                if (mImageWidth == 0 || mImageHeight == 0) {
                    mImageWidth = imageProxy.getWidth();
                    mImageHeight = imageProxy.getHeight();
                    onAnalyzeData listener = mAnalyzeListener;
                    if (listener != null) {
                        listener.backImageSize(mImageWidth, mImageHeight);
                    }
                }

                onAnalyzeData listener = mAnalyzeListener;
                if (listener != null) {
                    listener.analyze(imageProxy);
                }
            } catch (Exception e) {
                Log.e(TAG, "Camera frame analysis failed", e);
            } finally {
                // 必须关闭，否则不会收到下一帧。
                imageProxy.close();
            }
        });

        Preview previousPreview = mPreview;
        ImageAnalysis previousImageAnalysis = mImageAnalysis;
        CameraSelector previousCameraSelector = mCameraSelector;

        // 5. 绑定生命周期。先验证 selector，再解绑，避免目标摄像头不存在时造成黑屏。
        Camera camera;
        try {
            unbindOwnedUseCases();
            camera = cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(),
                    selection.cameraSelector,
                    newPreview,
                    newImageAnalysis);
        } catch (Exception e) {
            newImageAnalysis.clearAnalyzer();
            Log.e(TAG, "Camera bind failed", e);
            restorePreviousUseCases(previousCameraSelector, previousPreview, previousImageAnalysis);
            notifyCameraSwitchFailed(requestedLensFacing, notifySwitch);
            return false;
        }

        int actualLensFacing = selection.lensFacing;
        try {
            int cameraLensFacing = camera.getCameraInfo().getLensFacing();
            if (isSupportedLensFacing(cameraLensFacing)) {
                actualLensFacing = cameraLensFacing;
            }
        } catch (Exception cameraInfoError) {
            Log.w(TAG, "Unable to resolve actual lens facing; using selected facing", cameraInfoError);
        }

        mPreview = newPreview;
        mImageAnalysis = newImageAnalysis;
        mCameraSelector = selection.cameraSelector;
        mCameraControl = camera.getCameraControl();
        mCameraLensFacing = actualLensFacing;
        persistLensFacing(actualLensFacing);
        invalidateCameraCharacteristicsCache();

        mImageWidth = 0;
        mImageHeight = 0;
        mAnalyzerGeneration = analyzerGeneration;

        if (previousImageAnalysis != null) {
            previousImageAnalysis.clearAnalyzer();
        }
        try {
            mCameraControl.setLinearZoom(mLinearZoom);
        } catch (Exception zoomError) {
            Log.w(TAG, "Unable to restore linear zoom on the selected camera", zoomError);
        }

        if (notifySwitch && actualLensFacing != requestedLensFacing) {
            notifyCameraSwitchFailed(requestedLensFacing, true);
        } else {
            notifyCameraSwitched(actualLensFacing, notifySwitch);
        }
        return true;
    }

    /**
     * 构建兼容的 CameraSelector
     * 针对部分 RK 设备/工控机接口定义混乱的情况进行降级处理
     */
    @Nullable
    private CameraSelection createCompatibleCameraSelector(
            @CameraSelector.LensFacing int preferredLensFacing,
            boolean allowFallback) {
        // 另一种可能的 LensFacing (如果首选是 Front，备选就是 Back)
        int fallbackLensFacing = (preferredLensFacing == CameraSelector.LENS_FACING_FRONT)
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;

        if (hasCamera(mCameraProvider, preferredLensFacing)) {
            // 1. 完美匹配
            return new CameraSelection(cameraSelector(preferredLensFacing), preferredLensFacing);
        } else if (!allowFallback) {
            return null;
        } else if (hasCamera(mCameraProvider, fallbackLensFacing)) {
            // 2. 降级匹配：找不到指定方向，就用另一个方向
            Log.w(TAG, "Preferred camera not found, fallback to opposite facing.");
            return new CameraSelection(cameraSelector(fallbackLensFacing), fallbackLensFacing);
        } else {
            // 3. 暴力兜底：都找不到（可能是 External USB 摄像头），不过滤，接受任意摄像头
            Log.w(TAG, "Standard cameras not found, allowing ALL cameras (External/USB).");
            CameraSelector selector = new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> cameraInfos)
                    .build();
            return new CameraSelection(selector, preferredLensFacing);
        }
    }

    private boolean hasCamera(ProcessCameraProvider provider, int lensFacing) {
        if (provider == null) {
            return false;
        }
        try {
            return provider.hasCamera(cameraSelector(lensFacing));
        } catch (Exception e) {
            return false;
        }
    }

    private CameraSelector cameraSelector(@CameraSelector.LensFacing int lensFacing) {
        return new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    }

    private void switchCameraOnMainThread(@CameraSelector.LensFacing int lensFacing) {
        if (lensFacing == mCameraLensFacing && mCameraControl != null) {
            return;
        }

        if (mCameraProvider == null || mPreviewView == null) {
            mCameraLensFacing = lensFacing;
            persistLensFacing(lensFacing);
            mPendingSwitchNotification = true;
            return;
        }

        bindCameraUseCases(lensFacing, false, true);
    }

    private void runOnMainThread(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mMainHandler.post(action);
        }
    }

    private void persistLensFacing(@CameraSelector.LensFacing int lensFacing) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            arguments.putInt(KEY_LENS_FACING, lensFacing);
        }
    }

    private void unbindOwnedUseCases() {
        if (mCameraProvider == null) {
            return;
        }
        if (mPreview != null) {
            mCameraProvider.unbind(mPreview);
        }
        if (mImageAnalysis != null) {
            mCameraProvider.unbind(mImageAnalysis);
        }
    }

    private void restorePreviousUseCases(@Nullable CameraSelector selector,
                                         @Nullable Preview preview,
                                         @Nullable ImageAnalysis imageAnalysis) {
        if (mCameraProvider == null || selector == null || preview == null || imageAnalysis == null
                || mPreviewView == null) {
            return;
        }
        try {
            Camera camera = mCameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(), selector, preview, imageAnalysis);
            mCameraControl = camera.getCameraControl();
        } catch (Exception restoreError) {
            Log.e(TAG, "Failed to restore previous camera after switch failure", restoreError);
            mCameraControl = null;
        }
    }

    private void notifyCameraSwitched(@CameraSelector.LensFacing int lensFacing, boolean notify) {
        OnCameraSwitchListener listener = mCameraSwitchListener;
        if (notify && listener != null) {
            listener.onCameraSwitched(lensFacing);
        }
    }

    private void notifyCameraSwitchFailed(@CameraSelector.LensFacing int lensFacing, boolean notify) {
        OnCameraSwitchListener listener = mCameraSwitchListener;
        if (notify && listener != null) {
            listener.onCameraSwitchFailed(lensFacing);
        }
    }

    private boolean isSupportedLensFacing(int lensFacing) {
        return lensFacing == CameraSelector.LENS_FACING_FRONT
                || lensFacing == CameraSelector.LENS_FACING_BACK;
    }

    private static final class CameraSelection {
        private final CameraSelector cameraSelector;
        @CameraSelector.LensFacing
        private final int lensFacing;

        private CameraSelection(CameraSelector cameraSelector,
                                @CameraSelector.LensFacing int lensFacing) {
            this.cameraSelector = cameraSelector;
            this.lensFacing = lensFacing;
        }
    }

    public boolean isFrontCamera() {
        return mCameraLensFacing == CameraSelector.LENS_FACING_FRONT;
    }


}
