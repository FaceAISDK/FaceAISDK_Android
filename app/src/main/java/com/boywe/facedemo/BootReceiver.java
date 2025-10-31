package com.boywe.facedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "BootReceiverLogs";
    private static final String LOGS_KEY = "logs";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "=== BootReceiver onReceive ===");
        Log.d(TAG, "Action: " + action);
        Log.d(TAG, "Android Version: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        Log.d(TAG, "Manufacturer: " + android.os.Build.MANUFACTURER);
        Log.d(TAG, "Model: " + android.os.Build.MODEL);
        
        // 记录详细日志
        logBroadcastReceived(context, action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "Boot/Replace event detected, attempting to start MainActivity");
            
            try {
                // 启动欢迎页面Activity，保持正常的应用启动流程
                Intent welcomeIntent = new Intent(context, WelcomeActivity.class);
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(welcomeIntent);
                Log.d(TAG, "WelcomeActivity started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start WelcomeActivity", e);
                logBroadcastReceived(context, "启动失败: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Action not handled: " + action);
        }
    }
    
    private void logBroadcastReceived(Context context, String action) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String existingLogs = prefs.getString(LOGS_KEY, "");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            
            // 构建详细日志信息
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(timestamp).append(" - 广播: ").append(action).append("\n");
            logBuilder.append("  系统: Android ").append(android.os.Build.VERSION.RELEASE)
                     .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
            logBuilder.append("  设备: ").append(android.os.Build.MANUFACTURER)
                     .append(" ").append(android.os.Build.MODEL).append("\n");
            
            String newLog = logBuilder.toString();
            
            // 保持最近50条日志（因为每条日志现在更详细）
            String[] logLines = (existingLogs + newLog).split("\n");
            StringBuilder sb = new StringBuilder();
            int startIndex = Math.max(0, logLines.length - 150); // 50条日志 * 3行每条
            for (int i = startIndex; i < logLines.length; i++) {
                if (!logLines[i].trim().isEmpty()) {
                    sb.append(logLines[i]).append("\n");
                }
            }
            
            prefs.edit().putString(LOGS_KEY, sb.toString()).apply();
            Log.d(TAG, "Log saved: " + newLog.trim());
        } catch (Exception e) {
            Log.e(TAG, "Error saving log", e);
        }
    }
    
    public static String getLogs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LOGS_KEY, "暂无日志记录");
    }
    
    public static void clearLogs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(LOGS_KEY).apply();
    }
}