package com.example.demo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import com.faceAI.demo.FaceImageConfig;
import androidx.appcompat.app.AppCompatActivity;
import com.faceAI.demo.FaceAINaviActivity;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * 演示快速集成到你的主工程，人脸识别相关放到 FaceAILIb 里面
 * 先以子module 的形式配置到你的主工程跑起来后，再根据你的业务调整
 * <p>
 * 1.整体拷贝faceAILib 代码到你的主程一级目录
 * 2.settings.gradle 中 include ':faceAILib'
 * 3.调整工程一级目录root级build.gradle 的
 * <p>
 * <a href="https://github.com/FaceAISDK/FaceAISDK_Android">...</a>
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        TextView sdkVersion=findViewById(R.id.sdk_version);
        sdkVersion.setText("SDK 版本： v"+getVersionName());

        //人脸图保存路径等初始化配置
        FaceImageConfig.init(this);

        // 收集Crash,ANR 运行日志
        if(!FaceImageConfig.isDebugMode(getBaseContext())){
            CrashReport.initCrashReport(getApplicationContext(), "36fade54d8", true);
        }

        new Handler().postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, FaceAINaviActivity.class));
            finish();
        }, 1600);

    }


    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e){
            return "FaceAISDK";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}