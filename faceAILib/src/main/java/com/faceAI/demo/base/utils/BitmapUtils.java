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
     * 将 Bitmap 转换为 Base64 字符串（JPG 格式）
     * 优化点：流式编码减少内存拷贝、预分配缓冲区、自动资源管理
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        // 1. 修改前缀为 image/jpeg
        String prefix = "data:image/jpeg;base64,";

        // 2. 预估大小：JPG 压缩比通常比 WebP 低，建议设为 1/3 左右
        // 公式：(原始字节 / 压缩比) * Base64膨胀系数
        int estimatedSize = (bitmap.getAllocationByteCount() / 3) * 4 / 3;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(estimatedSize)) {

            // 3. 核心优化：使用 Base64OutputStream 装饰原始流
            // 这样 bitmap.compress 压出的数据会直接经过 Base64 编码存入 baos，无需中间 byte[] 转换
            try (Base64OutputStream b64os = new Base64OutputStream(baos, Base64.NO_WRAP)) {

                // 4. 使用 JPEG 格式，quality 80-90 是性价比最高的区间
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, b64os);

                // 必须手动 flush 或靠 try-with-resources 自动 close
                // 确保 Base64 的结束符（Padding）被写入 baos
            }

            // 5. 使用 StringBuilder 拼接，并指定 US-ASCII 编码（Base64 字符集的标准编码）
            return new StringBuilder(prefix.length() + baos.size())
                    .append(prefix)
                    .append(baos.toString("US-ASCII"))
                    .toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
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