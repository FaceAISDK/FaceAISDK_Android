package com.AI.test.utils;

import android.os.Environment;

import java.io.File;

/**
 * 外部临时文件的存储路径，比如一张临时图片，apk
 * 好像名字要固定下来会比较好不要外面船
 *
 */
public class FileStorage {
    private File file;

    /**
     * 所有的App 的缓存路径都在一个目录下面，保证每个类型的临时文件只有一个
     * @param fileChildPath 要放的子目录
     */

    public FileStorage(String fileChildPath) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            file = new File(fileChildPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    /**
     * 创建一个临时文件
     * @param fileName
     * @return
     */
    public File createTempFile(String fileName) {

        return new File(file, fileName);
    }

}
