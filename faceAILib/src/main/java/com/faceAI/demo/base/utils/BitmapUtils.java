package com.faceAI.demo.base.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;
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
     * bitmapToBase64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return "";

        // 1. 修复一致性：JPEG 压缩应对应 image/jpeg 的前缀
        // 如果确定要 webp，请修改 compress 格式为 Bitmap.CompressFormat.WEBP
        String prefix = "data:image/jpeg;base64,";
        ByteArrayOutputStream baos = null;

        try {
            // 2. 内存预分配合理化
            // Base64 编码后的长度约为原字节数组长度的 1.33 倍
            // 预分配缓冲区大小：原位图分配大小的 1/4（考虑到 JPEG 压缩率）
            int initialCapacity = bitmap.getAllocationByteCount() / 4;
            baos = new ByteArrayOutputStream(initialCapacity > 0 ? initialCapacity : 1024);

            // 3. 压缩策略
            // 质量 80-90 是平衡点，若用于人脸比对，建议不要低于 80
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] bitmapBytes = baos.toByteArray();

            // 4. Base64 编码优化
            // 使用 NO_WRAP 避免换行符污染数据流
            String base64Content = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);

            // 使用 StringBuilder 拼接，减少临时字符串对象产生的内存碎片
            StringBuilder sb = new StringBuilder(prefix.length() + base64Content.length());
            sb.append(prefix);
            sb.append(base64Content);

            return sb.toString();

        } catch (Exception e) {
            // 5. 修复逻辑：日志必须在 return 之前
            Log.e("BitmapUtil", "BitmapToBase64 Error: " + e.getMessage());
            return "";
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ignored) {}
            }
            // 注意：外部传入的 bitmap 通常不应在工具类方法内 recycle，
            // 除非该方法明确定义了会消耗（Consume）该位图。
        }
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