package com.faceAI.demo.SysCamera.diyCamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.faceAI.demo.R;

/**
 * 自定义调试管理摄像头，把SDK 中的源码暴露出来放在 CustomCameraXFragment
 *
 */
public class CustomCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);
        setTitle("Custom Camera");

        findViewById(R.id.back).setOnClickListener(v -> {
            finish();
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);

        CustomCameraXFragment cameraXFragment = CustomCameraXFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }



}

