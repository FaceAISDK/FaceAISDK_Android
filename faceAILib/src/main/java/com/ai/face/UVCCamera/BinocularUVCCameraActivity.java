package com.ai.face.UVCCamera;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ai.face.R;

/**
 * 演示USB 双目摄像头人脸识别，活体检测
 *
 */
public class BinocularUVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binocular_camera_face_aiactivity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        BinocularUVCCameraFragment binocularUVCCameraFragment = new BinocularUVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, binocularUVCCameraFragment);

        fragmentTransaction.commit();
    }


}