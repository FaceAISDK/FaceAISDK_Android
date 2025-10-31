package com.boywe.facedemo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

public class DebugHelper {
    private static final String TAG = "DebugHelper";
    private Context context;

    public DebugHelper(Context context) {
        this.context = context;
    }

    /**
     * 获取系统诊断信息
     */
    public String getSystemDiagnostics() {
        StringBuilder sb = new StringBuilder();
        
        // 基本系统信息
        sb.append("=== 系统信息 ===\n");
        sb.append("Android版本: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("设备制造商: ").append(Build.MANUFACTURER).append("\n");
        sb.append("设备型号: ").append(Build.MODEL).append("\n");
        sb.append("系统版本: ").append(Build.DISPLAY).append("\n\n");
        
        // 应用信息
        sb.append("=== 应用信息 ===\n");
        sb.append("包名: ").append(context.getPackageName()).append("\n");
        sb.append("目标SDK: ").append(getTargetSdkVersion()).append("\n");
        sb.append("是否系统应用: ").append(isSystemApp() ? "是" : "否").append("\n\n");
        
        // 权限检查
        sb.append("=== 权限状态 ===\n");
        sb.append("开机广播权限: ").append(hasBootPermission() ? "已授予" : "未授予").append("\n");
        sb.append("电池优化: ").append(isBatteryOptimized() ? "已优化(需关闭)" : "已忽略").append("\n");
        sb.append("后台应用限制: ").append(isBackgroundRestricted() ? "受限制" : "无限制").append("\n\n");
        
        // 广播接收器状态
        sb.append("=== 广播接收器 ===\n");
        sb.append("BootReceiver状态: ").append(isReceiverEnabled() ? "已启用" : "已禁用").append("\n");
        sb.append("支持的广播: ").append(getSupportedBroadcasts()).append("\n\n");
        
        // 厂商特定信息
        sb.append("=== 厂商特定 ===\n");
        sb.append("可能的自启动设置: ").append(getAutoStartSettings()).append("\n");
        
        return sb.toString();
    }

    private int getTargetSdkVersion() {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0);
            return appInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private boolean isSystemApp() {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean hasBootPermission() {
        return context.checkSelfPermission(android.Manifest.permission.RECEIVE_BOOT_COMPLETED) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBatteryOptimized() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return false;
    }

    private boolean isBackgroundRestricted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            return am != null && am.isBackgroundRestricted();
        }
        return false;
    }

    private boolean isReceiverEnabled() {
        try {
            PackageManager pm = context.getPackageManager();
            int state = pm.getComponentEnabledSetting(
                    new android.content.ComponentName(context, BootReceiver.class));
            return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking receiver state", e);
            return false;
        }
    }

    private String getSupportedBroadcasts() {
        return "BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, QUICKBOOT_POWERON, MY_PACKAGE_REPLACED, PACKAGE_REPLACED";
    }

    private String getAutoStartSettings() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        switch (manufacturer) {
            case "xiaomi":
                return "设置 > 应用管理 > 自启动管理";
            case "huawei":
            case "honor":
                return "设置 > 应用 > 应用启动管理";
            case "oppo":
                return "设置 > 电池 > 应用耗电管理 > 自启动管理";
            case "vivo":
                return "设置 > 电池 > 后台应用管理";
            case "samsung":
                return "设置 > 应用程序 > 特殊访问权限 > 优化电池使用";
            case "oneplus":
                return "设置 > 电池 > 电池优化";
            default:
                return "请在系统设置中查找自启动或电池优化相关选项";
        }
    }

    /**
     * 检查是否可以接收开机广播
     */
    public boolean canReceiveBootBroadcast() {
        Intent bootIntent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(bootIntent, 0);
        
        for (ResolveInfo receiver : receivers) {
            if (context.getPackageName().equals(receiver.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成调试报告
     */
    public String generateDebugReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 自启动调试报告 ===\n");
        report.append("生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        report.append(getSystemDiagnostics());
        
        report.append("\n=== 建议解决方案 ===\n");
        if (isBatteryOptimized()) {
            report.append("1. 关闭电池优化：设置 > 电池 > 电池优化 > 选择应用 > 不优化\n");
        }
        if (isBackgroundRestricted()) {
            report.append("2. 允许后台运行：设置 > 应用 > 后台应用限制 > 允许\n");
        }
        report.append("3. 开启自启动权限：").append(getAutoStartSettings()).append("\n");
        report.append("4. 重启设备测试自启动功能\n");
        
        return report.toString();
    }
}