package com.boywe.facedemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import com.boywe.facedemo.utils.IconFontHelper;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.faceAI.demo.FaceAINaviActivity;

public class MainActivity extends AppCompatActivity {

    private Animation pulseAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置状态栏和导航栏
        setupStatusBar();
        
        setContentView(R.layout.activity_main);
        
        // 设置Toolbar
        setupToolbar();
        
        // 初始化字体图标
        initIconFont();
        
        // 初始化动画
        initAnimations();
        
        // 开始动画
        startAnimations();
    }
    
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_blue));
            
            // 设置状态栏文字为白色（移除LIGHT_STATUS_BAR标志）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("");
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            
            // 手动设置居中的标题
            TextView titleView = toolbar.findViewById(R.id.toolbar_title);
            if (titleView != null) {
                titleView.setText("中国海关人脸识别");
            }
        }
    }
    
    private void initIconFont() {
        // 初始化字体图标工具类
        IconFontHelper.init(this);
        
        // 设置设置按钮的字体图标
        TextView settingsIcon = findViewById(R.id.tv_settings_icon);
        IconFontHelper.setIconFont(settingsIcon);
    }
    
    private void initAnimations() {
        // 获取人脸头像视图引用
        ImageView faceAvatar = findViewById(R.id.iv_face_avatar);
        
        // 加载动画
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        
        // 为人脸头像设置动画
        if (faceAvatar != null && pulseAnimation != null) {
            faceAvatar.startAnimation(pulseAnimation);
        }
    }
    
    private void startAnimations() {
        // 获取人脸头像视图引用并开始动画
        ImageView faceAvatar = findViewById(R.id.iv_face_avatar);
        if (faceAvatar != null && pulseAnimation != null) {
            faceAvatar.startAnimation(pulseAnimation);
        }
    }
    
    private void stopAnimations() {
        // 停止人脸头像动画
        ImageView faceAvatar = findViewById(R.id.iv_face_avatar);
        if (faceAvatar != null) {
            faceAvatar.clearAnimation();
        }
    }
    
    public void onSettingsClick(View view) {
        // 跳转到SettingsActivity
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    public void onStartFaceRecognition(View view) {
        // 开始人脸识别时重新启动动画
        startAnimations();
        
        startFaceRecognition();
    }
    
    private void startFaceRecognition() {
        // 跳转到人脸搜索识别页面
        Intent intent = new Intent(this, com.faceAI.demo.SysCamera.search.FaceSearch1NActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时停止动画以节省资源
        stopAnimations();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复时重新开始动画
        startAnimations();
    }
}