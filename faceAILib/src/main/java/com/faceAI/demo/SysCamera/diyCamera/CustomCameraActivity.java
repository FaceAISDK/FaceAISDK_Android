package com.faceAI.demo.SysCamera.diyCamera;

import android.os.Bundle;
import com.faceAI.demo.R;
import com.faceAI.demo.base.BaseActivity;

/**
 * 自定义调试管理摄像头，把SDK 中的源码暴露出来放在 CustomCameraXFragment
 *
 */
public class CustomCameraActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);
        setTitle("Custom Camera");

        findViewById(R.id.back).setOnClickListener(v -> {
            finish();
        });


        CustomCameraXFragment cameraXFragment = CustomCameraXFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }



}

