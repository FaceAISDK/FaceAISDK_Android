package com.ai.face.UVCCamera.verify;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ai.face.R;

/**
 * 演示USB 双目摄像头1:1人脸识别，活体检测
 * **更多外接USB外接UVC摄像头**的操作参考这个大神的库：https://github.com/shiyinghan/UVCAndroid
 * 项目中的libs/libuvccamera-release.aar 就是根据此调整部分
 */
public class FaceVerify_UVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binocular_camera_face_aiactivity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        FaceVerify_UVCCameraFragment binocularUVCCameraFragment = new FaceVerify_UVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, binocularUVCCameraFragment);

        fragmentTransaction.commit();
    }


}