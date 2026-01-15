package com.faceAI.demo.UVCCamera.addFace;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.faceAI.demo.R;


/**
 * 使用UVC RGB摄像头录入人脸
 *
 */
public class AddFace_UVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc_camera_faceai_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        AddFace_UVCCameraFragment myUVCCameraFragment = new AddFace_UVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, myUVCCameraFragment);

        fragmentTransaction.commit();
    }


}