package com.ai.face.utils.fileUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;


import java.io.File;

public class MyFileUtils {

    public static class FileMetaData {
        public String displayName;
        public long size;
        public String mimeType;
        public String path;

        @Override
        public String toString() {
            return "name : " + displayName + " ; size : " + size + " ; path : " + path + " ; mime : " + mimeType;
        }
    }


    public static FileMetaData getFileMetaData(Context context, Uri uri) {
        FileMetaData fileMetaData = new FileMetaData();

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            File file = new File(uri.getPath());
            fileMetaData.displayName = file.getName();
            fileMetaData.size = file.length();
            fileMetaData.path = file.getPath();

            return fileMetaData;
        } else {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            fileMetaData.mimeType = contentResolver.getType(uri);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    fileMetaData.displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                    if (!cursor.isNull(sizeIndex))
                        fileMetaData.size = cursor.getLong(sizeIndex);
                    else
                        fileMetaData.size = -1;

                    try {
                        fileMetaData.path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    } catch (Exception e) {
                        // DO NOTHING, _data does not exist
                    }

                    return fileMetaData;
                }
            } catch (Exception e) {
                Log.e("FileUtils", e.toString());
            } finally {
                if (cursor != null)
                    cursor.close();
            }

            return null;
        }
    }

}
