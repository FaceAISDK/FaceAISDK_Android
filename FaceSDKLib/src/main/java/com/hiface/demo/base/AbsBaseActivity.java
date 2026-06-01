package com.hiface.demo.base;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.hiface.demo.R;
import com.hiface.demo.base.view.FaceCoverView;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * AbsBaseActivity for Face SDK API Demo
 */
public  class AbsBaseActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private boolean shouldHideSystemUI = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkNeededPermission();
        adjustCameraMargin();
    }

    /**
     * 适配横竖屏，动态调整相机预览区域的边距为屏幕短边的 1/MARGIN_SIZE
     * 注意和 FaceCoverView 内部设置保持一致
     */
    public void adjustCameraMargin() {
        View fragmentCamera = findViewById(R.id.fragment_camerax);
        if (fragmentCamera != null && fragmentCamera.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int shorterSide = Math.min(width, height);
            int margin = shorterSide / FaceCoverView.MARGIN_SIZE;

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fragmentCamera.getLayoutParams();
            lp.setMargins(margin, margin, margin, margin);
            fragmentCamera.setLayoutParams(lp);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldHideSystemUI) {
            applyImmersiveMode();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && shouldHideSystemUI) {
            applyImmersiveMode();
        }
    }

    /**
     * 全屏隐藏系统 UI
     */
    public void hideSystemUI() {
        shouldHideSystemUI = true;
        applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 兼容旧版本
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    /**
     * 统一全局的拦截权限请求
     */
    private void checkNeededPermission() {
        String[] perms = {Manifest.permission.CAMERA};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(
                    this, getString(R.string.facesdk_camera_permission), 11, perms
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // 权限被授予时的逻辑
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.facesdk_camera_permission)
                .setPositiveButton(R.string.go_setting, (dialogInterface, i) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .show();
    }
}
