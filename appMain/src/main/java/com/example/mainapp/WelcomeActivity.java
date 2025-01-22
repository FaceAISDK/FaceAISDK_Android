package com.example.mainapp;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.airbnb.lottie.LottieAnimationView;
import com.tencent.bugly.crashreport.CrashReport;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.FaceAINaviActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 演示快速集成到你的主工程，人脸识别相关放到 FaceAILIb 里面
 * 先以子module 的形式配置到你的主工程跑起来后，再根据你的业务调整
 *
 * 1.整体拷贝faceAILib 代码到你的主程一级目录
 * 2.settings.gradle 中 include ':faceAILib'
 * 3.调整工程一级目录root级build.gradle 的
 *
 */
public class WelcomeActivity extends AppCompatActivity {

    /**
     * 获取签名密钥 SHA1值
     */
    public  String getSHA1(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (byte b : publicKey) {
                String appendString = Integer.toHexString(0xFF & b)
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length() - 1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.goFaceAILib).setOnClickListener(view ->
                startActivity(new Intent(WelcomeActivity.this, FaceAINaviActivity.class)));

        LottieAnimationView startFaceAILib= findViewById(R.id.goFaceAILib);
        startFaceAILib.setAnimation(com.ai.face.R.raw.loading);
        startFaceAILib.loop(false);
        startFaceAILib.setSpeed(4f);
        startFaceAILib.playAnimation();

        //主工程调用Face AI Lib 中人脸识别功能
        startFaceAILib.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                startActivity(new Intent(WelcomeActivity.this, FaceAINaviActivity.class));
                WelcomeActivity.this.finish();
            }
            @Override
            public void onAnimationStart(@NonNull Animator animation) {}
            @Override
            public void onAnimationCancel(@NonNull Animator animation) {}
            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {}
        });


        // 收集闪退运行日志
        if(!BuildConfig.DEBUG){
            CrashReport.initCrashReport(this, "36fade54d8", true);
        }


        String test=getSHA1(this);
        int a=1;
    }
}