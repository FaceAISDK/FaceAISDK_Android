package com.ai.face

import android.app.Application
import android.content.res.Configuration
import java.io.File

/**
 * global param init
 *
 *
 */
class FaceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //init your custom params
        CACHE_BASE_FACE_DIR = cacheDir.path + "/faceVerify"  //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = filesDir.path + "/faceSearch"    //1:N 人脸搜索目录

        //这次重构没把这步骤放到SDK，出错添加一下先。下版本删除
        val file= File(CACHE_SEARCH_FACE_DIR)
        if (!file.exists()) file.mkdirs()

        USER_ID_KEY = "AI_BASE_FACE_NAME"
        FACE_DIR_KEY = "BASE_FACE_DIR"
    }


    override fun onTerminate() {
        super.onTerminate()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }


    override fun onLowMemory() {
        super.onLowMemory()
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }


    companion object {
        lateinit var CACHE_BASE_FACE_DIR: String   //1：1 人脸识别目录
        lateinit var USER_ID_KEY: String
        lateinit var FACE_DIR_KEY: String
        lateinit var CACHE_SEARCH_FACE_DIR: String  //1:N 人脸搜索目录
    }


}