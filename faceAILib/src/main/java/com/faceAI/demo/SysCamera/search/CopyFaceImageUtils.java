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

import com.ai.face.core.mfn.FaceAISDKEngine;
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

/**
 * 模拟同步大量图片人脸转为人脸特征值到SDK,强烈建议只维护人脸特征
 * 为了演示简单，当前人脸图放在工程本地Assert目录。网络人脸图：https://postimg.cc/gallery/cYBKVYP
 *
 * 2025年11月23日优化：
 * 1. 改为串行递归处理，彻底解决多图并发导致的 OOM 问题。
 */
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";

    public interface Callback {
        void onComplete(int successCount, int failureCount);
    }

    /**
     * 异步执行图片导入（入口）
     */
    public static void copyTestFaceImages(@NonNull Context context, @NonNull Callback callBack) {
        // 1. 先显示 Loading
        showLoadingFloat(context);

        // 2. 在后台线程准备文件列表，避免阻塞 UI
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                prepareAndStart(context, callBack);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during init", e);
                finalizeProcess(callBack, 0, 0);
            } finally {
                executor.shutdown();
            }
        });
    }

    /**
     * 准备文件列表并开始处理第一张
     */
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

        if (allFiles == null || allFiles.length == 0) {
            Log.w(TAG, "Assets directory is empty.");
            finalizeProcess(callBack, 0, 0);
            return;
        }

        // 过滤图片。
        List<String> imageFiles = new ArrayList<>();
        for (String fileName : allFiles) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") ||
                    lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")) {
                imageFiles.add(fileName);
            }
        }

        if (imageFiles.isEmpty()) {
            Log.w(TAG, "No image files found.");
            finalizeProcess(callBack, 0, 0);
            return;
        }

        Log.e(TAG, "\nStart processing " + imageFiles.size() + " images sequentially...\n");

        // 3. 开始递归处理第 0 张图片
        // 注意：这里切回主线程或者继续在子线程取决于 SDK 的要求。
        // 通常建议后续逻辑在主线程发起，SDK 内部会处理耗时操作，或者继续保持子线程调用。
        // 这里为了安全起见，我们直接调用，SDK 内部的回调通常是异步的。
        processNextImage(context, imageFiles, 0, 0, 0, callBack);
    }

    /**
     * 核心方法：递归串行处理每一张图片
     *
     * @param index        当前处理的图片索引
     * @param successCount 当前成功总数
     * @param failureCount 当前失败总数
     */
    private static void processNextImage(Context context, List<String> imageFiles,
                                         int index, int successCount, int failureCount,
                                         Callback callBack) {

        // 1. 终止条件：所有图片处理完毕
        if (index >= imageFiles.size()) {
            Log.e(TAG, "-------- 完成处理 ------- ");
            finalizeProcess(callBack, successCount, failureCount);
            return;
        }

        String fileName = imageFiles.get(index);
        AssetManager assetManager = context.getAssets();

        // 2. 加载 Bitmap (即使在这里 OOM，也只会因为一张图，而不是50张并发)
        Bitmap originBitmap = getBitmapFromAsset(assetManager, fileName);

        if (originBitmap == null) {
            Log.e(TAG, "Failed to decode bitmap: " + fileName);
            // 失败，直接处理下一张 (index + 1)
            processNextImage(context, imageFiles, index + 1, successCount, failureCount + 1, callBack);
            return;
        }

        // 3. 调用 SDK 提取特征
        Image2FaceFeature.getInstance(context).getFaceFeatureByBitmap(originBitmap, fileName, new Image2FaceFeature.Callback() {
            @Override
            public void onSuccess(@NotNull Bitmap croppedBitmap, @NotNull String faceID, @NotNull String faceFeature) {
                try {
                    // 插入特征库
                    FaceSearchFeatureManger.getInstance(context)
                            .insertFaceFeature(fileName, faceFeature, System.currentTimeMillis(), "", "");

                    // 保存人脸小图
                    FaceAISDKEngine.getInstance(context).saveCroppedFaceImage(croppedBitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, fileName);

                    Log.d(TAG, "Processed [" + (index + 1) + "/" + imageFiles.size() + "]: " + fileName + " (Success)");

                    // 递归调用下一张：成功数 + 1
                    processNextImage(context, imageFiles, index + 1, successCount + 1, failureCount, callBack);

                } catch (Exception e) {
                    Log.e(TAG, "Error saving data for: " + fileName, e);
                    // 即使保存出错，也视为处理完成（算失败），继续下一张
                    processNextImage(context, imageFiles, index + 1, successCount, failureCount + 1, callBack);
                } finally {

                }
            }

            @Override
            public void onFailed(@NotNull String msg) {
                Log.e(TAG, "SDK Failed [" + (index + 1) + "/" + imageFiles.size() + "]: " + fileName + ", Msg: " + msg);
                // 递归调用下一张：失败数 + 1
                processNextImage(context, imageFiles, index + 1, successCount, failureCount + 1, callBack);
            }
        });
    }

    /**
     * 流程结束，统一出口
     */
    private static void finalizeProcess(Callback callback, int success, int failed) {
        // 确保在主线程执行 UI 操作和回调
        new Handler(Looper.getMainLooper()).post(() -> {
            callback.onComplete(success, failed);
        });
    }

    /**
     * 显示 Loading (主线程)
     */
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

    /**
     * 关闭 Loading
     */
    public static void dismissLoadingFloat() {
        EasyFloat.dismiss("loading_float");
    }


    /**
     * 从 Assets 读取图片
     * 建议：如果图片特别大，可以在这里加入 BitmapFactory.Options 进行采样压缩
     */
    private static Bitmap getBitmapFromAsset(AssetManager assetManager, String strName) {
        try (InputStream instr = assetManager.open(strName)) {
            return BitmapFactory.decodeStream(instr);
        } catch (IOException e) {
            Log.e(TAG, "Cannot open asset: " + strName, e);
            return null;
        }
    }
}