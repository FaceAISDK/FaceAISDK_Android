package com.example.demo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import com.faceAI.demo.FaceAINaviActivity;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.base.AbsBaseActivity;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * FaceSDK API Demo in module 「FaceSDKLib」
 *
 * @author  FaceAISDK.Service@gmail.com
 * @website https://faceaisdk.github.io/index
 */
public class WelcomeActivity extends AbsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_welcome);
        TextView sdkVersion=findViewById(R.id.sdk_version);
        sdkVersion.setText("FaceAISDK "+getVersionName());

        new Handler().postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, FaceAINaviActivity.class));
            finish();
        }, 2222);

        //test delete face data
        FaceSDKConfig.deleteFaceSearchData(this,"1696");

        if (!BuildConfig.DEBUG) {
            CrashReport.initCrashReport(getApplicationContext(), "36fade54d8", true);
        }
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e){
            return "FaceAISDK";
        }
    }


}