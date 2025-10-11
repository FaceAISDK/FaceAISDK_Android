package com.faceAI.demo.base.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.Window;
import android.view.WindowManager;

public class BrightnessUtil {


    /**
     * 设置屏幕亮度
     * 0-1f
     *
     */
    public static void setBrightness(Activity activity, float param) {
        Window localWindow = activity.getWindow();
        WindowManager.LayoutParams attributes = localWindow.getAttributes();
        attributes.screenBrightness = param;
        localWindow.setAttributes(attributes);
    }


    /**
     * 获取屏幕亮度
     */
    public static float getBrightness(Activity activity) {
        Window localWindow = activity.getWindow();
        WindowManager.LayoutParams attributes = localWindow.getAttributes();
        return attributes.screenBrightness;
    }


}
