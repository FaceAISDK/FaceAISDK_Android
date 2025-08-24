package com.faceAI.demo.base.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.ai.face.base.baseImage.FileStorage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Function:Base64和Bitmap相互转换类
 */
public class BitmapUtils {


    /**
     * 保存裁剪处理好后的Bitmap
     *
     * @param disposedBitmap 经过SDK裁剪处理好后的Bitmap
     * @param pathName       路径目录
     * @param fileName       文件名
     */
    public static void saveDisposedBitmap(Bitmap disposedBitmap, String pathName, String fileName) {
        saveBitmap(disposedBitmap, pathName, fileName);
    }


    /**
     * 保存裁剪处理好后的Bitmap
     *
     * @param disposedBitmap 经过SDK裁剪处理好后的Bitmap
     * @param pathName       路径目录
     * @param fileName       文件名
     */
    @Deprecated
    public static void saveBitmap(Bitmap disposedBitmap, String pathName, String fileName) {
        File file = new FileStorage(pathName).createTempFile(fileName);
        if (null != disposedBitmap) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                disposedBitmap = Bitmap.createScaledBitmap(disposedBitmap, 280, 280, true);
                disposedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                Log.e("Save Base Image", "Save Error： " + e.toString());
            }
        }
    }


    /**
     * 从Assert 获取Bitmap
     */
    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }


    /**
     * bitmap转为base64
     *
     * @param filepath
     * @return
     */
    public static String bitmapToBase64(String filepath) {

        Bitmap bitmap = BitmapFactory.decodeFile(filepath);
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = "data:image/jpg;base64," + Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * base64转为bitmap
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        Bitmap bitmap = null;
        try {
            String clearBase64Data = base64Data.substring(base64Data.indexOf(";base64,") + 8);
            if (TextUtils.isEmpty(clearBase64Data)) return null;

            byte[] bytes = Base64.decode(clearBase64Data, Base64.NO_WRAP);
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}