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
import com.ai.face.faceSearch.search.FaceSearchFeature; // 导入实体类
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟同步大量图片人脸转为1024长度人脸特征值到SDK
 * 并发降维解码 + 并发特征提取 + 并发图存盘 + 统一批量入库
 *
 *
 */
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";

    // 限制最大并发数，防止同时加载过多 Bitmap 导致 OOM
    // 建议设置为 CPU 核心数，或者保守设置为 3-4
    private static final int MAX_CONCURRENT_TASKS = Math.max(2, Runtime.getRuntime().availableProcessors());

    // 目标图片最大长宽，用于缩放优化解码速度
    private static final int MAX_IMAGE_SIZE = 1080;

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
     * 核心优化：并发处理图片，并将特征收集到 List 中准备批量插入
     */
    private static void processImagesConcurrently(Context context, List<String> imageFiles, Callback callBack) {
        int totalImages = imageFiles.size();

        // 1. 线程安全的计数器
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // 2. 线程安全的 List，用于收集所有成功提取的特征对象
        List<FaceSearchFeature> featureList = Collections.synchronizedList(new ArrayList<>());

        // 3. 线程池与背压信号量
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS);
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_TASKS);

        AssetManager assetManager = context.getAssets();

        for (int i = 0; i < totalImages; i++) {
            final String fileName = imageFiles.get(i);
            final int currentIndex = i;

            executorService.execute(() -> {
                try {
                    // 获取许可，控制并发防止 OOM
                    semaphore.acquire();

                    // 优化解码：使用采样压缩读取 Bitmap
                    Bitmap originBitmap = decodeSampledBitmapFromAsset(assetManager, fileName, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);

                    if (originBitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap: " + fileName);
                        handleImageResult(false, totalImages, successCount, failureCount, processedCount, semaphore, callBack, featureList, context);
                        return;
                    }

                    // 调用 SDK 提取特征
                    Image2FaceFeature.getInstance(context).getFaceFeatureByBitmap(originBitmap, fileName, new Image2FaceFeature.Callback() {
                        @Override
                        public void onSuccess(@NotNull Bitmap croppedBitmap, @NotNull String faceID, @NotNull String faceFeature) {
                            // 开启独立后台线程进行图存盘和特征对象组装，避免阻塞 SDK 回调线程
                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    // 1. 并发保存裁剪好的的人脸小图（是否需要保存根据你的实际业务进行）
                                    FaceAISDKEngine.getInstance(context).saveCroppedFaceImage(croppedBitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, fileName);

                                    FaceSearchFeature featureEntity = new FaceSearchFeature(fileName, faceFeature, System.currentTimeMillis());

                                    // 2. 将对象安全地添加到集合中
                                    featureList.add(featureEntity);

                                    Log.d(TAG, "Processed [" + (currentIndex + 1) + "/" + totalImages + "]: " + fileName + " (Success)");
                                    handleImageResult(true, totalImages, successCount, failureCount, processedCount, semaphore, callBack, featureList, context);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving data for: " + fileName, e);
                                    handleImageResult(false, totalImages, successCount, failureCount, processedCount, semaphore, callBack, featureList, context);
                                }
                            });
                        }

                        @Override
                        public void onFailed(@NotNull String msg) {
                            Log.e(TAG, "Image2FaceFeature Failed [" + (currentIndex + 1) + "/" + totalImages + "]: " + fileName + ", Msg: " + msg);
                            handleImageResult(false, totalImages, successCount, failureCount, processedCount, semaphore, callBack, featureList, context);
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleImageResult(false, totalImages, successCount, failureCount, processedCount, semaphore, callBack, featureList, context);
                }
            });
        }

        // 提交完所有任务后，平滑关闭线程池
        executorService.shutdown();
    }

    /**
     * 统一处理状态计算，并在全盘处理结束后触发批量入库
     */
    private static void handleImageResult(boolean isSuccess, int totalImages,
                                          AtomicInteger successCount, AtomicInteger failureCount, AtomicInteger processedCount,
                                          Semaphore semaphore, Callback callBack,
                                          List<FaceSearchFeature> featureList, Context context) {
        if (isSuccess) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }

        // 释放一个信号量，允许处理下一张图片
        semaphore.release();

        // 核心：当全部图片处理完毕时，触发批量插入
        if (processedCount.incrementAndGet() == totalImages) {
            Log.e(TAG, "-------- 图片特征提取完毕，准备批量插入 " + featureList.size() + " 条记录 ------- ");

            // 开启一个新的单线程来专门处理批量入库，防止阻塞
            Executors.newSingleThreadExecutor().execute(() -> {
                if (!featureList.isEmpty()) {
                    long startTime = System.currentTimeMillis();
                    try {
                        // 执行 SDK 提供的批量入库方法
                        FaceSearchFeatureManger.getInstance(context).insertFeatures(featureList);
                        Log.e(TAG, "-------- 批量入库完成！耗时: " + (System.currentTimeMillis() - startTime) + "ms ------- ");
                    } catch (Exception e) {
                        Log.e(TAG, "批量入库发生异常", e);
                    }
                }

                // 最终通知 UI 层任务彻底完成
                finalizeProcess(callBack, successCount.get(), failureCount.get());
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