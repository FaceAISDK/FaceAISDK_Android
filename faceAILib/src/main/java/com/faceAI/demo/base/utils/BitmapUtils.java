package com.faceAI.demo.base.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.ai.face.base.baseImage.FileStorage;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 优化后的图片处理工具类
 */
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    /**
     * 保存缩放后的图片，增加内存异常处理与副本回收优化
     */
    public static void saveScaledBitmap(Bitmap srcBitmap, String pathName, String fileName) {
        if (srcBitmap == null || srcBitmap.isRecycled() || TextUtils.isEmpty(pathName) || TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "Save Failed: srcBitmap is null, recycled, or invalid paths.");
            return;
        }

        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        int targetWidth = srcWidth;
        int targetHeight = srcHeight;
        float MAX_SIDE = 480f; // 目标最大边长

        // 计算等比缩放比例
        if (srcWidth > MAX_SIDE || srcHeight > MAX_SIDE) {
            float scale = Math.min(MAX_SIDE / srcWidth, MAX_SIDE / srcHeight);
            targetWidth = Math.round(srcWidth * scale);
            targetHeight = Math.round(srcHeight * scale);
        }

        File file = new FileStorage(pathName).createTempFile(fileName);
        Bitmap finalBitmap = null;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (targetWidth != srcWidth || targetHeight != srcHeight) {
                // 执行缩放，注意内存溢出风险
                finalBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true);
            } else {
                finalBitmap = srcBitmap;
            }

            // 写入文件，JPEG 90 质量平衡
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush(); // 显式冲刷流
            Log.d(TAG, "Save Success: " + targetWidth + "x" + targetHeight + " to " + file.getAbsolutePath());

        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "Save Image Failed due to OOM");
        } catch (IOException e) {
            Log.e(TAG, "Save Image Error: " + e.getMessage());
        } finally {
            // 内存管理：仅回收该方法内部创建的副本，不回收外部传入的 srcBitmap
            if (finalBitmap != null && finalBitmap != srcBitmap) {
                finalBitmap.recycle();
            }
        }
    }

    /**
     * 文件转 Base64，引入预估容量与缓冲流优化
     */
    public static String bitmapToBase64(String filepath) {
        if (TextUtils.isEmpty(filepath)) return "";

        File file = new File(filepath);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "File not found or unreadable: " + filepath);
            return "";
        }

        // Base64 编码后的长度约为原长度的 4/3，预设缓冲区大小减少扩容
        int initialCapacity = (int) (file.length() * 1.34) + 1024;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             ByteArrayOutputStream baos = new ByteArrayOutputStream(initialCapacity)) {

            byte[] buffer = new byte[16384]; // 增大到 16K 缓冲区提升读速度
            int len;
            while ((len = bis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            byte[] fileBytes = baos.toByteArray();
            return "data:image/jpg;base64," + Base64.encodeToString(fileBytes, Base64.NO_WRAP);

        } catch (IOException e) {
            Log.e(TAG, "Read file to Base64 Error: " + e.getMessage());
        }

        return "";
    }

    /**
     * Base64 转为 Bitmap，增加 decode 质量控制
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        if (TextUtils.isEmpty(base64Data)) return null;

        try {
            String clearBase64Data = base64Data;
            int commaIndex = base64Data.indexOf(",");
            if (commaIndex != -1) {
                clearBase64Data = base64Data.substring(commaIndex + 1);
            }

            byte[] bytes = Base64.decode(clearBase64Data, Base64.NO_WRAP);

            // 优化：配置 Options 减少色彩开销（如果不需要 alpha 通道，可用 RGB_565）
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "base64ToBitmap: Invalid Base64 format");
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "base64ToBitmap: OOM while decoding");
        } catch (Exception e) {
            Log.e(TAG, "base64ToBitmap Error: " + e.getMessage());
        }

        return null;
    }
}