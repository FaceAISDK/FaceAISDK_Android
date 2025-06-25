package com.ai.face.UVCCamera.search;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ai.face.R;

/**
 * 演示UVC协议USB双目摄像头人脸搜索，
 * 使用宽动态（人脸搜索须大于105DB）抗逆光摄像头；保持镜头干净（用纯棉布擦拭油污）
 */
public class FaceSearch_UVCCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binocular_camera_face_aiactivity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        FaceSearch_UVCCameraFragment binocularUVCCameraFragment = new FaceSearch_UVCCameraFragment();
        fragmentTransaction.replace(R.id.fragment_container, binocularUVCCameraFragment);

        fragmentTransaction.commit();
    }


}