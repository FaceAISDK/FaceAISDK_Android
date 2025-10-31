package com.boywe.facedemo.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * 字体图标工具类
 * 用于统一管理和设置字体图标
 */
public class IconFontHelper {
    
    private static Typeface iconFont;
    
    /**
     * 初始化字体图标
     * @param context 上下文
     */
    public static void init(Context context) {
        try {
            iconFont = Typeface.createFromAsset(context.getAssets(), "iconfont.ttf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 为TextView设置字体图标
     * @param textView 目标TextView
     */
    public static void setIconFont(TextView textView) {
        if (iconFont != null && textView != null) {
            textView.setTypeface(iconFont);
        }
    }
    
    /**
     * 为TextView设置字体图标和文本
     * @param textView 目标TextView
     * @param iconText 图标文本
     */
    public static void setIconFont(TextView textView, String iconText) {
        if (iconFont != null && textView != null) {
            textView.setTypeface(iconFont);
            textView.setText(iconText);
        }
    }
    
    /**
     * 获取字体图标Typeface
     * @return 字体图标Typeface
     */
    public static Typeface getIconFont() {
        return iconFont;
    }
    
    /**
     * 检查字体图标是否已初始化
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return iconFont != null;
    }
}