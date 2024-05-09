package com.ai.face

import android.app.Application
import android.content.res.Configuration
import com.tencent.bugly.crashreport.CrashReport
import java.io.File

/**
 * global param init
 * 1：N（M：N） 人脸识别检索可以独立引入 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 *
 *
 */
class FaceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        //init your custom params
        CACHE_BASE_FACE_DIR = cacheDir.path + "/faceVerify"      //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = filesDir.path + "/faceSearch"    //1:N（M：N） 人脸搜索目录

        //初始化创建1:N（M：N） 人脸搜索目录，人脸图集放在这里
        //不要直接文件操作把人脸图放到这个目录，这样不能搜索，要通过SDK 的API 进行人脸的增删改查
        val file = File(CACHE_SEARCH_FACE_DIR)
        if (!file.exists()) file.mkdirs()


        //Crash 收集
        CrashReport.initCrashReport(this, "36fade54d8", true)

    }




    companion object {
        lateinit var CACHE_BASE_FACE_DIR: String   //1：1 人脸识别目录
        lateinit var CACHE_SEARCH_FACE_DIR: String  //1:N 人脸搜索目录

        const val USER_ID_KEY: String = "AI_BASE_FACE_NAME"
        const val FACE_DIR_KEY: String = "BASE_FACE_DIR"
    }


}