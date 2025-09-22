package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.faceAI.demo.FaceAINaviActivity;
import com.faceAI.demo.FaceSDKConfig;
import com.tencent.bugly.library.Bugly;
import com.tencent.bugly.library.BuglyBuilder;

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
        FaceSDKConfig.init(this);

        initBugly(getApplicationContext());

        new Handler().postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, FaceAINaviActivity.class));
            finish();
        }, 1600);

    }


    /**
     * Bugly 初始化
     * @param context
     */
    public static void initBugly(Context context) {
        // 1. 初始化参数预构建，必需设置初始化参数
        String appID = "d7ea07bb1d"; // 【必需设置】在Bugly 专业版 注册产品的appID
        String appKey = "3e69dca1-f91d-43a3-80fc-8e94270e3e0d"; // 【必需设置】在Bugly 专业版 注册产品的appKey
        BuglyBuilder builder = new BuglyBuilder(appID, appKey);
        // 专业版：BuglyBuilder.ServerHostTypeBuglyPro
        // 海外版：BuglyBuilder.ServerHostTypeBuglyOversea
        builder.setServerHostType(BuglyBuilder.ServerHostTypeBuglyPro);
        // 2. 基本初始化参数，推荐设置初始化参数
        builder.deviceModel = Build.MODEL; // 【推荐设置】设置设备类型，设置机型后，Bugly SDK不再读取系统的机型
        // 3. 更多初始化参数，按需设置初始化参数
        builder.debugMode = false; // 设置debug模式，可在调试阶段开启
        builder.initAppState = BuglyBuilder.APP_STATE_FOREGROUND; // 自4.4.3.7版本起支持。该参数为非必选项，可在初始化 Bugly SDK 时指定应用的前后台状态。若未指定，SDK将在初始化时通过 getRunningAppProcesses 判断应用的前后台状态；若已指定，SDK将直接采用指定状态，不再调用 getRunningAppProcesses 进行判断。

        // 5. 初始化，必需调用
        Bugly.init(context, builder);
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