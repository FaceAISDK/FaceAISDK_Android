package com.faceAI.demo.SysCamera.search;

import static com.faceAI.demo.SysCamera.search.FaceSearch1NActivity.ACTION_COMPLETE_UPDATE_FACE_DATA;
import static com.faceAI.demo.SysCamera.search.FaceSearch1NActivity.ACTION_START_UPDATE_FACE_DATA;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ai.face.core.engine.FaceAISDKEngine;
import com.ai.face.faceSearch.search.FaceSearchFeature;
import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
import com.ai.face.faceSearch.search.Image2FaceFeature;
import com.airbnb.lottie.LottieAnimationView;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将 assets 根目录中的测试人脸图片转换为特征，并分批提交给 SDK。
 * 正式业务建议录入时直接提取特征，避免长期保存原始人脸图片。
 *
 * <p>导入期间暂停相机搜索；SDK 共享推理缓冲区，故同一时间只提取一张图片。
 * Bitmap 的保存、释放完成后才处理下一张，避免慢磁盘造成图片队列积压。
 *
 * <p>2026.09.29 的 insertFeatures() 内部异步写库，没有事务完成回调。
 * 本工具的完成通知表示提取、缓存图片保存及特征提交结束，不保证特征已经落库。
 * 要确认落库结果，需要 SDK 提供可等待的写入接口，不能用固定延时替代。
 */
@Deprecated
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";
    private static final int BATCH_SIZE = 100;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean IMPORT_RUNNING = new AtomicBoolean(false);

    // 仅在主线程访问，并在页面销毁或任务完成时释放。
    private static AlertDialog loadingDialog;

    public interface Callback {
        /**
         * 主线程回调。submittedCount 是 SDK 接受提交的特征数，非数据库提交成功数；
         * failureCount 是未能提取或被 SDK 拒绝提交的图片数。
         * Demo 图片缓存失败单独记录日志，不影响特征提交计数。
         */
        void onComplete(int submittedCount, int failureCount);

        /** 主线程回调：已有导入任务，或任务初始化失败；此时不再调用 onComplete。 */
        default void onError(@NonNull String message) {
        }
    }

    /** 异步入口；重复调用明确拒绝，不发送广播、不干扰当前任务的弹窗。 */
    public static void copyTestFaceImages(@NonNull Context context, @NonNull Callback callback) {
        if (!IMPORT_RUNNING.compareAndSet(false, true)) {
            runOnMainThread(() -> {
                if (isContextAlive(context)) {
                    callback.onError("已有图片导入任务正在运行，请稍后重试");
                }
            });
            return;
        }

        runOnMainThread(() -> {
            ImportTask task;
            try {
                task = new ImportTask(context, callback);
            } catch (Exception e) {
                IMPORT_RUNNING.set(false);
                Log.e(TAG, "Failed to create image import task", e);
                if (isContextAlive(context)) {
                    callback.onError("图片导入初始化失败，请查看日志");
                }
                return;
            }
            task.start(context);
        });
    }

    /** 所有图片状态和批次缓冲区都由同一个 worker 维护，不需要跨线程计数或锁。 */
    private static final class ImportTask {
        private final Context appContext;
        private final WeakReference<Activity> owner;
        private final boolean hasActivityOwner;
        private final List<String> imageFiles = new ArrayList<>();
        private final List<FaceSearchFeature> featureBuffer = new ArrayList<>(BATCH_SIZE);
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private Callback callback; // 仅主线程访问；页面销毁时清除，避免回调闭包持有 Activity。
        private LifecycleEventObserver lifecycleObserver;
        private ExecutorService worker;
        private int nextImageIndex;
        private int submittedCount;
        private int cacheFailureCount;
        private int maxImageSize;

        ImportTask(Context context, Callback callback) {
            Context application = context.getApplicationContext();
            appContext = application != null ? application : context;
            hasActivityOwner = context instanceof Activity;
            owner = new WeakReference<>(hasActivityOwner ? (Activity) context : null);
            this.callback = callback;
        }

        void start(Context uiContext) {
            try {
                if (!isContextAlive(uiContext)) {
                    callback = null;
                } else if (uiContext instanceof LifecycleOwner) {
                    lifecycleObserver = (source, event) -> {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            callback = null;
                            dismissLoadingFloat();
                            source.getLifecycle().removeObserver(lifecycleObserver);
                            lifecycleObserver = null;
                        }
                    };
                    ((LifecycleOwner) uiContext).getLifecycle().addObserver(lifecycleObserver);
                }
                showLoadingFloat(uiContext);
                sendUpdateBroadcast(appContext, ACTION_START_UPDATE_FACE_DATA);
                worker = Executors.newSingleThreadExecutor();
                worker.execute(this::prepare);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start image import", e);
                finish("图片导入初始化失败，请查看日志");
            }
        }

        private void prepare() {
            try {
                String[] assets = appContext.getAssets().list("");
                if (assets != null) {
                    for (String fileName : assets) {
                        String lowerName = fileName.toLowerCase(Locale.ROOT);
                        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                                || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
                            imageFiles.add(fileName);
                        }
                    }
                }
                maxImageSize = calcMaxImageSize();
                Log.i(TAG, "Start importing " + imageFiles.size() + " images, max size: "
                        + maxImageSize + ", SDK extraction concurrency: 1");
                processNextImage();
            } catch (Exception e) {
                Log.e(TAG, "Failed to read image assets", e);
                finish("无法读取测试图片，请查看日志");
            }
        }

        private void processNextImage() {
            if (nextImageIndex == imageFiles.size()) {
                submitFeatureBatch();
                finish(null);
                return;
            }

            String fileName = imageFiles.get(nextImageIndex++);
            Bitmap originBitmap;
            try {
                originBitmap = decodeSampledBitmapFromAsset(appContext.getAssets(), fileName,
                        maxImageSize, maxImageSize);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding image: " + fileName, e);
                worker.execute(this::processNextImage);
                return;
            }
            if (originBitmap == null) {
                Log.w(TAG, "Failed to decode image: " + fileName);
                worker.execute(this::processNextImage);
                return;
            }

            // SDK 既可能同步回调，也可能异步回调；只受理第一个结果。
            AtomicBoolean resultHandled = new AtomicBoolean(false);
            Image2FaceFeature.Callback resultCallback = new Image2FaceFeature.Callback() {
                @Override
                public void onSuccess(@NotNull Bitmap croppedBitmap, @NotNull String faceID,
                                      @NotNull String faceFeature) {
                    if (resultHandled.compareAndSet(false, true)) {
                        worker.execute(() -> handleSuccess(fileName, originBitmap, croppedBitmap,
                                faceFeature));
                    }
                }

                @Override
                public void onFailed(@NotNull String message) {
                    if (resultHandled.compareAndSet(false, true)) {
                        worker.execute(() -> handleFailure(fileName, originBitmap, message));
                    }
                }
            };
            try {
                Image2FaceFeature.getInstance(appContext).getFaceFeatureByBitmap(originBitmap,
                        fileName, resultCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error extracting feature: " + fileName, e);
                // 若 SDK 回调后又抛异常，不能再次结算或回收已经交给保存任务的 Bitmap。
                resultCallback.onFailed("SDK 特征提取异常");
            }
            // 不在 finally 中回收原图或继续下一张：异步 SDK 可能仍在读取它。
            // SDK 未提供取消接口，不能超时后直接回收图片并恢复搜索，否则可能并发访问推理资源。
        }

        private void handleSuccess(String fileName, Bitmap originBitmap, Bitmap croppedBitmap,
                                   String faceFeature) {
            try {
                // SDK 对 112×112、224×224 输入可能直接返回原图，必须保留到保存结束。
                if (originBitmap != croppedBitmap) {
                    recycleBitmap(originBitmap);
                }
                try {
                    // SDK 内部会自行记录部分 IO 异常；这里只统计能够传出的保存异常。
                    FaceAISDKEngine.getInstance(appContext).saveCroppedFaceImage(croppedBitmap,
                            FaceSDKConfig.CACHE_SEARCH_FACE_DIR, fileName);
                } catch (Exception e) {
                    cacheFailureCount++;
                    Log.e(TAG, "Failed to cache face image: " + fileName, e);
                }

                // 文件名作为稳定的 faceID，保持与 Demo 的图片管理列表一致。
                featureBuffer.add(new FaceSearchFeature(fileName, faceFeature,
                        System.currentTimeMillis()));
                if (featureBuffer.size() >= BATCH_SIZE) {
                    submitFeatureBatch();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling extracted feature: " + fileName, e);
            } finally {
                recycleBitmap(croppedBitmap);
                // 两者相同只回收一次；两者不同时原图已在上面提前释放。
                if (originBitmap != croppedBitmap) {
                    recycleBitmap(originBitmap);
                }
                worker.execute(this::processNextImage);
            }
        }

        private void handleFailure(String fileName, Bitmap originBitmap, String message) {
            Log.w(TAG, "Feature extraction failed: " + fileName + ", " + message);
            recycleBitmap(originBitmap);
            worker.execute(this::processNextImage);
        }

        private void submitFeatureBatch() {
            if (featureBuffer.isEmpty()) {
                return;
            }
            List<FaceSearchFeature> batch = new ArrayList<>(featureBuffer);
            featureBuffer.clear();
            try {
                int accepted = FaceSearchFeatureManger.getInstance(appContext).insertFeatures(batch);
                submittedCount += Math.max(0, Math.min(accepted, batch.size()));
                // 返回值仅表示通过 SDK 校验并接受提交；SDK 内部的写库异常不会传回这里。
                Log.i(TAG, "SDK accepted " + accepted + "/" + batch.size()
                        + " features; database commit is asynchronous");
            } catch (Exception e) {
                Log.e(TAG, "Failed to submit feature batch (" + batch.size() + ")", e);
            }
        }

        private void finish(String errorMessage) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (worker != null) {
                worker.shutdown();
            }
            int failures = imageFiles.size() - submittedCount;
            Log.i(TAG, "Import finished, submitted: " + submittedCount + ", failed: " + failures
                    + ", reported cache failures: " + cacheFailureCount);
            runOnMainThread(() -> {
                Activity activity = owner.get();
                if (lifecycleObserver != null && activity instanceof LifecycleOwner) {
                    ((LifecycleOwner) activity).getLifecycle().removeObserver(lifecycleObserver);
                }
                lifecycleObserver = null;
                try {
                    dismissLoadingFloat();
                    sendUpdateBroadcast(appContext, ACTION_COMPLETE_UPDATE_FACE_DATA);
                } finally {
                    IMPORT_RUNNING.set(false);
                }
                Callback completedCallback = callback;
                callback = null;
                if (completedCallback != null && (!hasActivityOwner
                        || (activity != null && isContextAlive(activity)))) {
                    if (errorMessage == null) {
                        completedCallback.onComplete(submittedCount, failures);
                    } else {
                        completedCallback.onError(errorMessage);
                    }
                }
            });
        }
    }

    /** 采用当前剩余堆预算，而非最大堆容量；仍需在目标设备校验缩放后的小人脸效果。 */
    private static int calcMaxImageSize() {
        Runtime runtime = Runtime.getRuntime();
        long availableHeapMB = (runtime.maxMemory() - runtime.totalMemory()
                + runtime.freeMemory()) / (1024 * 1024);
        if (availableHeapMB < 128) return 640;
        if (availableHeapMB < 256) return 800;
        return 1080;
    }

    private static void sendUpdateBroadcast(Context context, String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }

    private static boolean isContextAlive(Context context) {
        if (!(context instanceof Activity)) {
            return true;
        }
        Activity activity = (Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void runOnMainThread(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            MAIN_HANDLER.post(action);
        }
    }

    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static void showLoadingFloat(Context context) {
        runOnMainThread(() -> {
            if (!(context instanceof Activity) || !isContextAlive(context)) {
                return;
            }
            if (loadingDialog != null && loadingDialog.isShowing()) {
                return;
            }
            try {
                View view = LayoutInflater.from(context).inflate(R.layout.float_loading, null);
                LottieAnimationView animation = view.findViewById(R.id.entry);
                animation.setAnimation(R.raw.waiting);
                animation.setRepeatCount(-1);
                animation.playAnimation();
                loadingDialog = new AlertDialog.Builder(context, R.style.TransparentDialog)
                        .setView(view)
                        .setCancelable(false)
                        .create();
                loadingDialog.show();
                Window window = loadingDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    params.gravity = Gravity.CENTER;
                    window.setAttributes(params);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to show loading dialog", e);
                dismissLoadingFloat();
            }
        });
    }

    /** 主线程直接关闭，保证完成回调执行时弹窗已经释放。 */
    public static void dismissLoadingFloat() {
        runOnMainThread(() -> {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to dismiss loading dialog", e);
            } finally {
                loadingDialog = null;
            }
        });
    }

    /** 先采样降低解码峰值，再等比缩放，确保长宽都不超过目标尺寸。 */
    private static Bitmap decodeSampledBitmapFromAsset(AssetManager assetManager, String fileName,
                                                      int reqWidth, int reqHeight) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = assetManager.open(fileName)) {
            BitmapFactory.decodeStream(stream, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap decoded;
        try (InputStream stream = assetManager.open(fileName)) {
            decoded = BitmapFactory.decodeStream(stream, null, options);
        }
        if (decoded == null || (decoded.getWidth() <= reqWidth && decoded.getHeight() <= reqHeight)) {
            return decoded;
        }
        try {
            float scale = Math.min((float) reqWidth / decoded.getWidth(),
                    (float) reqHeight / decoded.getHeight());
            return Bitmap.createScaledBitmap(decoded, Math.max(1, Math.round(decoded.getWidth() * scale)),
                    Math.max(1, Math.round(decoded.getHeight() * scale)), true);
        } finally {
            recycleBitmap(decoded);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        // 采样后最长边约为目标的 1～2 倍；使用 OR 才能限制长条图片的解码内存。
        while (options.outWidth / sampleSize / 2 >= reqWidth
                || options.outHeight / sampleSize / 2 >= reqHeight) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}
