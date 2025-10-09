package com.faceAI.demo.UVCCamera.liveness;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.faceAI.demo.R;

/**
 * UVC协议USB摄像头活体检测 Liveness Detection with UVC USB Camera
 * 更多外接USB外接UVC摄像头**的操作参考这个大神的库：https://github.com/shiyinghan/UVCAndroid
 * @author FaceAISDK.Service@gmail.com
 */
public class Liveness_UVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc_camera_faceai_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Liveness_UVCCameraFragment myUVCCameraFragment = new Liveness_UVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, myUVCCameraFragment);

        fragmentTransaction.commit();
    }


}