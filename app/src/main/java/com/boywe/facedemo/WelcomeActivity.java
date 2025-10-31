package com.boywe.facedemo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.boywe.facedemo.MainActivity;
import com.faceAI.demo.FaceSDKConfig;
import com.tencent.bugly.crashreport.CrashReport;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //人脸图保存路径等初始化配置
        FaceSDKConfig.init(this);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        }, 1600);

        //获取你的应用签名，Demo SHA1: B5:CB:44:5A:18:7D:73:F4:A0:A3:E4:53:45:64:9D:D3:F1:74:10:58
        String sha1=AppSigning.getSha1(getBaseContext());

        // 收集Crash,ANR 运行日志
        if (!BuildConfig.DEBUG) {
            CrashReport.initCrashReport(this, "36fade54d8", true);
        }

    }

}