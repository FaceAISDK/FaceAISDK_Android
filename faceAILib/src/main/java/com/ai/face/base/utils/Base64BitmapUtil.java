package com.ai.face.base.utils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Function:Base64和Bitmap相互转换类
 */
public class Base64BitmapUtil {

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
            result = "data:image/jpg;base64,"+Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
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