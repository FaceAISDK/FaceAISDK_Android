package com.ai.face.UVCCamera.addFace;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ai.face.R;

/**
 * 使用UVC RGB摄像头录入人脸
 *
 */
public class AddFaceUVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binocular_camera_face_aiactivity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        AddFaceUVCCameraFragment binocularUVCCameraFragment = new AddFaceUVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, binocularUVCCameraFragment);

        fragmentTransaction.commit();
    }


}