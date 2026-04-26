package com.example.demo;
import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraXConfig;
import androidx.camera.camera2.Camera2Config;
import com.faceAI.demo.FaceSDKConfig;

/**
 * FaceSDKConfig.init(this); in your Application onCreate()
 * FaceSDK API Demo in module 「FaceSDKLib」
 *
 */
public class FaceApplication extends Application implements CameraXConfig.Provider {

    /**
     * How to speed up camera startup
     * 如何加快摄像头启动
     * 카메라 시작 속도 향상 방법
     * カメラの起動を高速化する方法
     *
     * More：https://developer.android.com/media/camera/camerax/configuration?hl=zh-cn
     * @return CameraXConfig
     */
    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
//                .setAvailableCamerasLimiter(CameraSelector.DEFAULT_FRONT_CAMERA) //记住上一次选择加快启动
                .setMinimumLoggingLevel(Log.ERROR)
                .build();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // init Face SDK
        FaceSDKConfig.init(this);
    }



}