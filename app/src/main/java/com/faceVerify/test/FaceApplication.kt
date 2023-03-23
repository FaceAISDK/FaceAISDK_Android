package com.faceVerify.test

import android.app.Application
import android.content.res.Configuration
import android.os.Environment
import com.tencent.bugly.crashreport.CrashReport

/**
 * global param init
 *
 */
class FaceApplication : Application() {


//    /**
//     *  限制摄像头可以加速启动
//     *
//     */
//    override fun getCameraXConfig(): CameraXConfig {
//        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
//            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_FRONT_CAMERA)
//            .build()
//    }



    override fun onCreate() {
        super.onCreate()
        BASE_FACE_PATH = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()

        DIR_11_VALUE = "/11";
        DIR_1N_VALUE = "/1n";
        FACE_DIR_KEY="FACE_DIR_KEY"

        USER_ID_KEY="USER_ID_KEY"


        //SDK 所有的能力都是离线识别，不需要网络，Bugly 收集错误信息用了网络而已！！！
        CrashReport.initCrashReport(this, "36fade54d8", false)  //Bugly Crash 收集
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

    //可以精简一些了
    public companion object {
        lateinit var  BASE_FACE_PATH: String

        lateinit var  DIR_11_VALUE: String
        lateinit var  DIR_1N_VALUE: String
        lateinit var  FACE_DIR_KEY: String

        lateinit var  USER_ID_KEY: String

    }

}