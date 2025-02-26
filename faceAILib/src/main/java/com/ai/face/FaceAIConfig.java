package com.ai.face;

import android.content.Context;

/**
 * 不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化，检测质量等操作
 *
 */
public class FaceAIConfig {

    //不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
    public static String CACHE_BASE_FACE_DIR;   //1：1 人脸识别人脸图片存储目录
    public static String CACHE_SEARCH_FACE_DIR; //1：N 人脸识别人脸图片存储目录

    public static  void init(Context context) {

        //初始化人脸识别 人脸搜索存储目录，VIP 用户支持自定义存储目录
        //内部私有空间，其他应用不可以访问，卸载应用人脸图一起卸载了
        // https://developer.android.com/training/data-storage?hl=zh-cn
        // Warming: 目前仅能存储在context.getCacheDir() 或者context.getFilesDir()
        // 否则会提示无法找到人脸，VIP 可解除限制
        CACHE_BASE_FACE_DIR = context.getCacheDir().getPath() + "/faceVerify/";    //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = context.getFilesDir().getPath() + "/faceAISearch/";  //人脸搜索人脸库目录

    }

}
