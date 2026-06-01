package com.hiface.demo.SysCamera.search;

import static com.hiface.demo.SysCamera.search.FaceSearch1NActivity.ACTION_COMPLETE_UPDATE_FACE_DATA;
import static com.hiface.demo.SysCamera.search.FaceSearch1NActivity.ACTION_START_UPDATE_FACE_DATA;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sdk.hiface.core.engine.FaceAISDKEngine;
import com.sdk.hiface.search.FaceSearchFeature;
import com.sdk.hiface.search.FaceSearchFeatureManger;
import com.sdk.hiface.search.Image2FaceFeature;
import com.airbnb.lottie.LottieAnimationView;
import com.hiface.demo.FaceSDKConfig;
import com.hiface.demo.R;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步人脸原始图转为人脸特征值保存到SDK（直接采集照片不但面临数据合规问题，人脸角度大小没经过SDK校验精度也会降低）
 * 不建议收集人脸图片提取人脸特征的方式,人脸录入的时候就直接提取特征值
 * 详细见：https://mp.weixin.qq.com/s/aGPwYUYxnr6ZDRxwAQd8vg
 *
 * 注意：人脸数据更新时候请暂停人脸搜索识别，新起一个页面提示数据更新维护中
 */
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";

    /**
     * 根据设备可用堆内存和 CPU 核心数动态计算并发数，低配设备自动降级
     * - 可用堆 < 128MB → 并发 1
     * - 可用堆 < 256MB → 并发 2
     * - 否则取 CPU 核心数，上限 4（避免过高并发导致 Bitmap 内存峰值过大）
     */
    private static int calcConcurrency() {
        long maxHeapMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int cpuCores = Runtime.getRuntime().availableProcessors();
        if (maxHeapMB < 128) return 1;
        if (maxHeapMB < 256) return 2;
        return Math.min(cpuCores, 4);
    }

    /**
     * 根据可用堆内存动态选择解码目标尺寸，低配设备使用更小分辨率加速解码并减少内存占用
     */
    private static int calcMaxImageSize() {
        long maxHeapMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (maxHeapMB < 128) return 640;
        if (maxHeapMB < 256) return 800;
        return 1080;
    }

    // 分批入库阈值，满100就更新插入数据库防止数据丢失
    private static final int BATCH_SIZE = 100;

    public interface Callback {
        void onComplete(int successCount, int failureCount);
    }

    /**
     * 异步执行图片导入（入口）
     */
    public static void copyTestFaceImages(@NonNull Context context, @NonNull Callback callBack) {
        showLoadingFloat(context);

        //发送广播停止人脸搜索
        Intent intent = new Intent(ACTION_START_UPDATE_FACE_DATA);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        ExecutorService initExecutor = Executors.newSingleThreadExecutor();
        initExecutor.execute(() -> {
            try {
                prepareAndStart(context, callBack);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during init", e);
                finalizeProcess(context,callBack, 0, 0);
            } finally {
                initExecutor.shutdown();
            }
        });
    }

    private static void prepareAndStart(@NonNull Context context, @NonNull Callback callBack) {
        AssetManager assetManager = context.getAssets();
        String[] allFiles;
        try {
            allFiles = assetManager.list("");
        } catch (IOException e) {
            Log.e(TAG, "Error accessing assets", e);
            finalizeProcess(context,callBack, 0, 0);
            return;
        }

        List<String> imageFiles = new ArrayList<>();
        if (allFiles != null) {
            for (String fileName : allFiles) {
                String lowerName = fileName.toLowerCase();
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") ||
                        lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")) {
                    imageFiles.add(fileName);
                }
            }
        }

        if (imageFiles.isEmpty()) {
            Log.w(TAG, "No image files found.");
            finalizeProcess(context,callBack, 0, 0);
            return;
        }

        Log.e(TAG, "\nStart processing " + imageFiles.size() + " images concurrently...\n");
        processImagesConcurrently(context, imageFiles, callBack);
    }

    /**
     * 核心优化：根据设备性能动态调整并发数，及时回收 Bitmap，线程池全生命周期管理
     */
    private static void processImagesConcurrently(Context context, List<String> imageFiles, Callback callBack) {
        int totalImages = imageFiles.size();
        int concurrency = calcConcurrency();
        int maxImageSize = calcMaxImageSize();

        Log.i(TAG, "设备并发数: " + concurrency + ", 解码目标尺寸: " + maxImageSize
                + ", 最大堆: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // 用于缓存当前批次的特征对象
        List<FaceSearchFeature> featureBuffer = new ArrayList<>();

        // 图像解码与特征提取的并发线程池（动态并发数）
        ExecutorService extractExecutor = Executors.newFixedThreadPool(concurrency);
        // IO 线程池：低配设备 1 个线程即可，避免磁盘 IO 争抢
        ExecutorService ioExecutor = Executors.newFixedThreadPool(concurrency <= 2 ? 1 : 2);
        // SQLite 串行写入
        ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

        Semaphore semaphore = new Semaphore(concurrency);
        AssetManager assetManager = context.getAssets();

        for (int i = 0; i < totalImages; i++) {
            final String fileName = imageFiles.get(i);
            final int currentIndex = i;

            extractExecutor.execute(() -> {
                try {
                    semaphore.acquire();

                    Bitmap originBitmap = decodeSampledBitmapFromAsset(assetManager, fileName, maxImageSize, maxImageSize);

                    if (originBitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap: " + fileName);
                        handleImageComplete(false, totalImages, successCount, failureCount, processedCount,
                                semaphore, featureBuffer, dbExecutor, ioExecutor, context, callBack);
                        return;
                    }

                    Image2FaceFeature.getInstance(context).getFaceFeatureByBitmap(originBitmap, fileName, new Image2FaceFeature.Callback() {
                        @Override
                        public void onSuccess(@NotNull Bitmap croppedBitmap, @NotNull String faceID, @NotNull String faceFeature) {
                            // 原始大图已不再需要，立即回收释放内存
                            recycleBitmap(originBitmap);

                            // 异步保存裁剪图，仅仅是Demo演示需要保存这种临时图，实际根据你的业务处理
                            ioExecutor.execute(() -> {
                                try {
                                    FaceAISDKEngine.getInstance(context).saveCroppedFaceImage(croppedBitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, fileName);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving image for: " + fileName, e);
                                } finally {
                                    recycleBitmap(croppedBitmap);
                                }
                            });

                            FaceSearchFeature featureEntity = new FaceSearchFeature(fileName, faceFeature, System.currentTimeMillis());
                            List<FaceSearchFeature> batchToInsert = null;

                            synchronized (featureBuffer) {
                                featureBuffer.add(featureEntity);
                                if (featureBuffer.size() >= BATCH_SIZE) {
                                    batchToInsert = new ArrayList<>(featureBuffer);
                                    featureBuffer.clear();
                                }
                            }

                            if (batchToInsert != null) {
                                final List<FaceSearchFeature> finalBatch = batchToInsert;
                                dbExecutor.execute(() -> {
                                    try {
                                        FaceSearchFeatureManger.getInstance(context).insertFeatures(finalBatch);
                                        Log.i(TAG, "=> 分批入库成功，插入了 " + finalBatch.size() + " 条记录");
                                    } catch (Exception e) {
                                        Log.e(TAG, "分批入库发生异常", e);
                                    }
                                });
                            }

                            Log.d(TAG, "Processed [" + (currentIndex + 1) + "/" + totalImages + "]: " + fileName + " (Success)");
                            handleImageComplete(true, totalImages, successCount, failureCount, processedCount,
                                    semaphore, featureBuffer, dbExecutor, ioExecutor, context, callBack);
                        }

                        @Override
                        public void onFailed(@NotNull String msg) {
                            // 提取失败也要回收原始 Bitmap
                            recycleBitmap(originBitmap);
                            Log.e(TAG, "Image2FaceFeature Failed [" + (currentIndex + 1) + "/" + totalImages + "]: " + fileName + ", Msg: " + msg);
                            handleImageComplete(false, totalImages, successCount, failureCount, processedCount,
                                    semaphore, featureBuffer, dbExecutor, ioExecutor, context, callBack);
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleImageComplete(false, totalImages, successCount, failureCount, processedCount,
                            semaphore, featureBuffer, dbExecutor, ioExecutor, context, callBack);
                }
            });
        }

        extractExecutor.shutdown();
    }

    /** 安全回收 Bitmap，防止重复回收异常 */
    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * 统一处理状态计算，并在全盘处理结束后触发最后一次残留数据的入库
     */
    private static void handleImageComplete(boolean isSuccess, int totalImages,
                                            AtomicInteger successCount, AtomicInteger failureCount, AtomicInteger processedCount,
                                            Semaphore semaphore, List<FaceSearchFeature> featureBuffer,
                                            ExecutorService dbExecutor, ExecutorService ioExecutor,
                                            Context context, Callback callBack) {
        if (isSuccess) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }

        // 释放一个信号量，允许处理下一张图片
        semaphore.release();

        // 核心：当全部图片处理完毕时，检查是否还有未满 BATCH_SIZE 的残留数据
        if (processedCount.incrementAndGet() == totalImages) {
            Log.e(TAG, "-------- 所有图片特征提取计算完毕，处理尾部数据 ------- ");

            List<FaceSearchFeature> finalBatchToInsert = null;
            synchronized (featureBuffer) {
                if (!featureBuffer.isEmpty()) {
                    finalBatchToInsert = new ArrayList<>(featureBuffer);
                    featureBuffer.clear();
                }
            }

            if (finalBatchToInsert != null) {
                final List<FaceSearchFeature> finalBatch = finalBatchToInsert;
                dbExecutor.execute(() -> {
                    try {
                        FaceSearchFeatureManger.getInstance(context).insertFeatures(finalBatch);
                        Log.e(TAG, "=> 尾部数据入库完成，插入了 " + finalBatch.size() + " 条记录");
                    } catch (Exception e) {
                        Log.e(TAG, "尾部数据入库异常", e);
                    }
                });
            }

            // 确保在所有数据库操作执行完毕后，再通知 UI 和关闭线程池
            dbExecutor.execute(() -> {
                finalizeProcess(context,callBack, successCount.get(), failureCount.get());
                ioExecutor.shutdown();  // 关闭 IO 线程池
                dbExecutor.shutdown();  // 关闭数据库专属线程池
            });
        }
    }

    private static void finalizeProcess(@NonNull Context context,Callback callback, int success, int failed) {
        new Handler(Looper.getMainLooper()).post(() -> {

            //发送广播可以重新人脸搜索
            Intent intent = new Intent(ACTION_COMPLETE_UPDATE_FACE_DATA);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            dismissLoadingFloat(); // 关闭 Loading 动画
            if (callback != null) {
                callback.onComplete(success, failed);
            }
        });
    }

    // --- Loading UI 方法 ---
    private static AlertDialog loadingDialog;

    public static void showLoadingFloat(Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context must be an Activity to show a dialog");
            return;
        }
        Activity activity = (Activity) context;
        if (activity.isFinishing()) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            if (activity.isFinishing()) {
                return;
            }

            if (loadingDialog != null && loadingDialog.isShowing()) return;

            try {
                View view = LayoutInflater.from(activity).inflate(R.layout.float_loading, null);
                LottieAnimationView entry = view.findViewById(R.id.entry);
                entry.setAnimation(R.raw.waiting);
                entry.loop(true);
                entry.playAnimation();

                loadingDialog = new AlertDialog.Builder(activity, R.style.TransparentDialog)
                        .setView(view)
                        .setCancelable(false)
                        .create();

                Window window = loadingDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    // 解决 Dialog 宽度不全屏的问题（可选）
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.gravity = Gravity.CENTER;
                    window.setAttributes(lp);
                }
                loadingDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to show loading dialog", e);
            }
        });
    }

    public static void dismissLoadingFloat() {
        new Handler(Looper.getMainLooper()).post(() -> {
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

    // --- 图像解码压缩方法保持不变 ---
    private static Bitmap decodeSampledBitmapFromAsset(AssetManager assetManager, String strName, int reqWidth, int reqHeight) {
        try (InputStream instr = assetManager.open(strName)) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(instr, null, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            // 使用 RGB_565 降低一半的内存占用
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream instrActual = assetManager.open(strName)) {
                return BitmapFactory.decodeStream(instrActual, null, options);
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot open asset: " + strName, e);
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}