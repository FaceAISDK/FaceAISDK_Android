package com.boywe.facedemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.faceAI.demo.SysCamera.search.FaceSearchImageMangerActivity;

public class SettingsActivity extends AppCompatActivity {

    // 摄像头相关常量
    private static final String FRONT_BACK_CAMERA_FLAG = "cameraFlag";
    private static final String SYSTEM_CAMERA_DEGREE = "cameraDegree";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 显示返回按钮，禁用默认标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // 初始化显示当前设置值
        updateSettingsDisplay();
    }
    
    /**
     * 更新设置显示值
     */
    private void updateSettingsDisplay() {
        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        
        // 更新摄像头切换显示
        TextView cameraSwitchValue = findViewById(R.id.camera_switch_value);
        int cameraFlag = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 1);
        if (cameraFlag == 2) {
            cameraSwitchValue.setText("后置摄像头");
        } else {
            cameraSwitchValue.setText("前置摄像头");
        }
        
        // 更新摄像头方向显示
        TextView cameraOrientationValue = findViewById(R.id.camera_orientation_value);
        int degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, 3) % 4;
        String degreeStr;
        switch (degree) {
            case 0:
                degreeStr = "0°";
                break;
            case 1:
                degreeStr = "90°";
                break;
            case 2:
                degreeStr = "180°";
                break;
            case 3:
                degreeStr = "270°";
                break;
            default:
                degreeStr = "0°";
                break;
        }
        cameraOrientationValue.setText(degreeStr);
    }

    /**
     * 人脸库管理点击事件
     */
    public void onFaceLibManagementClick(View view) {
        Intent intent = new Intent(this, FaceSearchImageMangerActivity.class);
        intent.putExtra("isAdd", false);
        startActivity(intent);
    }

    /**
     * 切换前后摄像头点击事件
     */
    public void onCameraSwitchClick(View view) {
        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        if (sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 1) == 1) {
            sharedPref.edit().putInt(FRONT_BACK_CAMERA_FLAG, 0).commit();
            Toast.makeText(
                getBaseContext(),
                "Front camera now",
                Toast.LENGTH_SHORT
            ).show();
        } else {
            sharedPref.edit().putInt(FRONT_BACK_CAMERA_FLAG, 1).commit();
            Toast.makeText(
                getBaseContext(),
                "Back/USB Camera",
                Toast.LENGTH_SHORT
            ).show();
        }
        // 更新显示值
        updateSettingsDisplay();
    }

    /**
     * 摄像头方向设置点击事件
     */
    public void onCameraOrientationClick(View view) {
        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int degreeSys = (sharedPref.getInt(SYSTEM_CAMERA_DEGREE, 3) + 1) % 4;
        sharedPref.edit().putInt(SYSTEM_CAMERA_DEGREE, degreeSys).commit();
        
        String degreeStrSys;
        switch (degreeSys) {
            case 0:
                degreeStrSys = "0°";
                break;
            case 1:
                degreeStrSys = "90°";
                break;
            case 2:
                degreeStrSys = "180°";
                break;
            case 3:
                degreeStrSys = "270°";
                break;
            default:
                degreeStrSys = "0°";
                break;
        }
        
        Toast.makeText(
            getBaseContext(),
            "摄像头方向: " + degreeStrSys,
            Toast.LENGTH_SHORT
        ).show();
        
        // 更新显示值
        updateSettingsDisplay();
    }

    /**
     * 启动日志点击事件
     */
    public void onLogClick(View view) {
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
    }
}