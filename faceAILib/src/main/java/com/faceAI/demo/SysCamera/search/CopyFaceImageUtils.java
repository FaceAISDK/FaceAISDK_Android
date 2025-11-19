package com.faceAI.demo.SysCamera.search;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.airbnb.lottie.LottieAnimationView;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.lzf.easyfloat.EasyFloat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java版本：拷贝 Assets 目录下的人脸图到人脸搜索库
 *
 * 2025年11月20日修改：
 */
public class CopyFaceImageUtils {
    private static final String TAG = "CopyFaceImageUtils";

    public interface Callback {
        /**
         * 当所有图片都成功处理时回调
         */
        void onComplete();

        /**
         * 当有部分或全部图片处理失败时回调
         * @param successCount 成功数量
         * @param failureCount 失败数量
         */
        void onFailed(int successCount, int failureCount);
    }

    /**
     * 快速复制工程目录 ./app/src/main/assets 目录下的人脸图入库，以便验证人脸搜索功能。
     * 此方法是异步的，处理完成后会通过Callback通知结果。
     *
     * @param context  Application Context
     * @param callBack 结果回调
     */
    public static void copyTestFaceImages(@NonNull Application context, @NonNull Callback callBack) {
        showLoadingFloat(context); // 显示悬浮窗

        new Thread(() -> {
            int successCount = 0;
            int failureCount = 0;

            try {
                // 1. 获取需要处理的图片文件列表
                AssetManager assetManager = context.getAssets();
                String[] allFiles = assetManager.list("");
                if (allFiles == null || allFiles.length == 0) {
                    Log.w(TAG, "Assets directory is empty or cannot be accessed.");
                    // 没有文件，直接回调成功
                    postSuccess(callBack);
                    return;
                }

                // 过滤出图片文件
                List<String> imageFiles = new ArrayList<>();
                for (String fileName : allFiles) {
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg") || fileName.endsWith(".webp")) {
                        imageFiles.add(fileName);
                    }
                }

                if (imageFiles.isEmpty()) {
                    Log.w(TAG, "No image files found in assets directory.");
                    postSuccess(callBack);
                    return;
                }

                // 2. 初始化计数器和SDK管理器
                final AtomicInteger tasksRemaining = new AtomicInteger(imageFiles.size());
                final AtomicInteger failedCounter = new AtomicInteger(0);
                FaceSearchImagesManger manager = FaceSearchImagesManger.getInstance(context);
                String cacheDir = FaceSDKConfig.CACHE_SEARCH_FACE_DIR;

                // 3. 遍历图片并进行入库处理
                for (String fileName : imageFiles) {
                    Bitmap originBitmap = getBitmapFromAsset(assetManager, fileName);
                    if (originBitmap != null) {
                        String savedPath = cacheDir + fileName;
                        manager.insertOrUpdateFaceImage(
                                originBitmap,
                                savedPath,
                                new FaceSearchImagesManger.Callback() {
                                    @Override
                                    public void onSuccess(@NonNull Bitmap bitmap, @NonNull float[] faceEmbedding) {
                                        Log.d(TAG, "Successfully processed: " + fileName);
                                        // 任务完成，计数器减一
                                        if (tasksRemaining.decrementAndGet() == 0) {
                                            // 所有任务都已完成
                                            finalizeProcess(callBack, imageFiles.size(), failedCounter.get());
                                        }
                                    }

                                    @Override
                                    public void onFailed(@NonNull String msg) {
                                        Log.e(TAG, "Failed to process: " + fileName + ", Reason: " + msg);
                                        failedCounter.incrementAndGet(); // 失败计数器加一
                                        if (tasksRemaining.decrementAndGet() == 0) {
                                            // 所有任务都已完成
                                            finalizeProcess(callBack, imageFiles.size(), failedCounter.get());
                                        }
                                    }
                                }
                        );
                    } else {
                        // 如果Bitmap加载失败，也算一个失败任务
                        Log.e(TAG, "Failed to decode bitmap from asset: " + fileName);
                        failedCounter.incrementAndGet();
                        if (tasksRemaining.decrementAndGet() == 0) {
                            finalizeProcess(callBack, imageFiles.size(), failedCounter.get());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error accessing assets", e);
                // 发生严重IO异常，直接回调失败
                postFailure(callBack, 0, 0); // 或者传递一个特殊值表示IO错误
            } finally {
                // 确保悬浮窗在后台线程结束后被关闭
                // 注意：这里需要确保回调执行后再关闭，所以将关闭操作移到主线程回调中
            }
        }).start();
    }

    /**
     * 所有任务处理完毕后，决定最终回调
     */
    private static void finalizeProcess(Callback callback, int total, int failed) {
        int success = total - failed;
        if (failed == 0) {
            postSuccess(callback);
        } else {
            postFailure(callback, success, failed);
        }
    }

    private static void postSuccess(Callback callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            dismissLoadingFloat();
            callback.onComplete();
        });
    }

    private static void postFailure(Callback callback, int successCount, int failureCount) {
        new Handler(Looper.getMainLooper()).post(() -> {
            dismissLoadingFloat();
            callback.onFailed(successCount, failureCount);
        });
    }

    /**
     * 显示等待悬浮窗
     */
    private static void showLoadingFloat(Context context) {
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
     * 关闭等待悬浮窗
     */
    private static void dismissLoadingFloat() {
        if (EasyFloat.getFloatView("loading_float") != null) {
            EasyFloat.dismiss("loading_float");
        }
    }


    /**
     * 从 Assets 安全地读取 Bitmap
     */
    private static Bitmap getBitmapFromAsset(AssetManager assetManager, String strName) {
        try (InputStream istr = assetManager.open(strName)) {
            return BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            Log.e(TAG, "Cannot open asset: " + strName, e);
            return null;
        }
    }
}
