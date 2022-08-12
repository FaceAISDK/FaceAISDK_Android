package com.faceVerify.test.utils

import android.os.Environment
import java.io.File

/**
 * 外部临时文件的存储路径，比如一张临时图片，apk
 * 好像名字要固定下来会比较好不要外面传
 *
 *
 */
class FileStorage(fileChildPath: String?) {
    private var file: File? = null

    /**
     * 创建一个临时文件
     * @param fileName
     * @return
     */
    fun createTempFile(fileName: String): File {
        return File(file, fileName)
    }

    /**
     * 所有的App 的缓存路径都在一个目录下面，保证每个类型的临时文件只有一个
     * @param fileChildPath 要放的子目录
     */
    init {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            file = File(fileChildPath)
            if (!file!!.exists()) {
                file!!.mkdirs()
            }
        }
    }
}