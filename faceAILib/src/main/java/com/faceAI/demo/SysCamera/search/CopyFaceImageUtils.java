package com.faceAI.demo.SysCamera.search;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import androidx.annotation.NonNull;

import com.ai.face.core.engine.FaceAISDKEngine;
import com.ai.face.faceSearch.search.FaceSearchFeature;
import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
import com.ai.face.faceSearch.search.Image2FaceFeature;
import com.airbnb.lottie.LottieAnimationView;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.lzf.easyfloat.EasyFloat;

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
 * 模拟同步大量图片人脸转为1024长度人脸特征值到SDK
 * 【已优化】：并发降维解码 + 并发特征提取 + 线程池复用文件存盘 + 单线程分批次安全入库
 */
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";

    // 限制最大并发数，防止同时加载过多 Bitmap 导致 OOM
    private static final int MAX_CONCURRENT_TASKS = Math.max(2, Runtime.getRuntime().availableProcessors());

    // 目标图片最大长宽，用于缩放优化解码速度
    private static final int MAX_IMAGE_SIZE = 1080;

    // 【新增】分批入库阈值：每满 50 条就立刻执行一次数据库插入，防止中途中断导致数据全部丢失
    private static final int BATCH_SIZE = 50;

    public interface Callback {
        void onComplete(int successCount, int failureCount);
    }

    /**
     * 异步执行图片导入（入口）
     */
    public static void copyTestFaceImages(@NonNull Context context, @NonNull Callback callBack) {
        showLoadingFloat(context);

        ExecutorService initExecutor = Executors.newSingleThreadExecutor();
        initExecutor.execute(() -> {
            try {
                prepareAndStart(context, callBack);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during init", e);
                finalizeProcess(callBack, 0, 0);
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
            finalizeProcess(callBack, 0, 0);
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
            finalizeProcess(callBack, 0, 0);
            return;
        }

        Log.e(TAG, "\nStart processing " + imageFiles.size() + " images concurrently...\n");
        processImagesConcurrently(context, imageFiles, callBack);
    }

    /**
     * 核心优化：并发处理图片，分批次持久化入库
     */
    private static void processImagesConcurrently(Context context, List<String> imageFiles, Callback callBack) {
        int totalImages = imageFiles.size();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // 用于缓存当前批次的特征对象
        List<FaceSearchFeature> featureBuffer = new ArrayList<>();

        // 图像解码与特征提取的并发线程池
        ExecutorService extractExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS);
        // 【新增】专门用于保存图片的 IO 线程池（复用线程，防止原代码频繁 new 导致的内存泄漏）
        ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
        // 【新增】专门用于 SQLite 数据库串行写入的单线程池（防止数据库并发写入锁冲突）
        ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_TASKS);
        AssetManager assetManager = context.getAssets();

        for (int i = 0; i < totalImages; i++) {
            final String fileName = imageFiles.get(i);
            final int currentIndex = i;

            extractExecutor.execute(() -> {
                try {
                    // 获取许可，控制并发防止 OOM
                    semaphore.acquire();

                    // 优化解码：使用采样压缩读取 Bitmap
                    Bitmap originBitmap = decodeSampledBitmapFromAsset(assetManager, fileName, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);

                    if (originBitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap: " + fileName);
                        handleImageComplete(false, totalImages, successCount, failureCount, processedCount, semaphore, featureBuffer, dbExecutor, context, callBack);
                        return;
                    }

                    // 调用 SDK 提取特征
                    Image2FaceFeature.getInstance(context).getFaceFeatureByBitmap(originBitmap, fileName, new Image2FaceFeature.Callback() {
                        @Override
                        public void onSuccess(@NotNull Bitmap croppedBitmap, @NotNull String faceID, @NotNull String faceFeature) {

                            // 1. 异步保存裁剪图到本地
                            ioExecutor.execute(() -> {
                                try {
                                    FaceAISDKEngine.getInstance(context).saveCroppedFaceImage(croppedBitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, fileName);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving image for: " + fileName, e);
                                }
                            });

                            FaceSearchFeature featureEntity = new FaceSearchFeature(fileName, faceFeature, System.currentTimeMillis());
                            List<FaceSearchFeature> batchToInsert = null;

                            // 2. 线程安全地控制特征缓存区
                            synchronized (featureBuffer) {
                                featureBuffer.add(featureEntity);
                                // 如果缓存区达到批量阈值，则拷贝一份用于异步入库，并清空当前缓存区
                                if (featureBuffer.size() >= BATCH_SIZE) {
                                    batchToInsert = new ArrayList<>(featureBuffer);
                                    featureBuffer.clear();
                                }
                            }

                            // 3. 异步执行批量入库（不阻塞特征提取流程）
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
                            handleImageComplete(true, totalImages, successCount, failureCount, processedCount, semaphore, featureBuffer, dbExecutor, context, callBack);
                        }

                        @Override
                        public void onFailed(@NotNull String msg) {
                            Log.e(TAG, "Image2FaceFeature Failed [" + (currentIndex + 1) + "/" + totalImages + "]: " + fileName + ", Msg: " + msg);
                            handleImageComplete(false, totalImages, successCount, failureCount, processedCount, semaphore, featureBuffer, dbExecutor, context, callBack);
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleImageComplete(false, totalImages, successCount, failureCount, processedCount, semaphore, featureBuffer, dbExecutor, context, callBack);
                }
            });
        }

        // 提交完所有特征提取任务后，平滑关闭提取线程池
        extractExecutor.shutdown();
    }

    /**
     * 统一处理状态计算，并在全盘处理结束后触发最后一次残留数据的入库
     */
    private static void handleImageComplete(boolean isSuccess, int totalImages,
                                            AtomicInteger successCount, AtomicInteger failureCount, AtomicInteger processedCount,
                                            Semaphore semaphore, List<FaceSearchFeature> featureBuffer,
                                            ExecutorService dbExecutor, Context context, Callback callBack) {
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
                finalizeProcess(callBack, successCount.get(), failureCount.get());
                dbExecutor.shutdown(); // 关闭数据库专属线程池
            });
        }
    }

    private static void finalizeProcess(Callback callback, int success, int failed) {
        new Handler(Looper.getMainLooper()).post(() -> {
            dismissLoadingFloat(); // 关闭 Loading 动画
            if (callback != null) {
                callback.onComplete(success, failed);
            }
        });
    }

    // --- Loading UI 方法保持不变 ---
    public static void showLoadingFloat(Context context) {
        EasyFloat.with(context.getApplicationContext())
                .setTag("loading_float")
                .setGravity(Gravity.CENTER, 0, 0)
                .setDragEnable(false)
                .setLayout(R.layout.float_loading, view -> {
                    LottieAnimationView entry = view.findViewById(R.id.entry);
                    entry.setAnimation(R.raw.waiting);
                    entry.loop(true);
                    entry.playAnimation();
                })
                .show();
    }

    public static void dismissLoadingFloat() {
        EasyFloat.dismiss("loading_float");
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