package com.faceAI.demo.UVCCamera.liveness;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.faceAI.demo.R;

/**
 * 演示USB 双目摄像头1:1人脸识别，活体检测
 * **更多外接USB外接UVC摄像头**的操作参考这个大神的库：https://github.com/shiyinghan/UVCAndroid
 * 项目中的libs/libuvccamera-release.aar 就是根据此调整部分
 *
 * 怎么提高人脸搜索识别系统的准确度？https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
 */
public class Liveness_UVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc_camera_faceai_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Liveness_UVCCameraFragment binocularUVCCameraFragment = new Liveness_UVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, binocularUVCCameraFragment);

        fragmentTransaction.commit();
    }


}