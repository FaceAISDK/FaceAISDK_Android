package com.faceVerify.test

import android.app.Application
import android.content.res.Configuration
import android.os.Environment

/**
 * global param init
 *
 */
class FaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BASE_FACE_PATH = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()

        DIR_11_VALUE = "/11";
        DIR_1N_VALUE = "/1n";
        FACE_DIR_KEY="FACE_DIR_KEY"


        USER_ID_KEY="USER_ID_KEY"


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