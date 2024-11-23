package com.ai.face;

import android.content.Context;

/**
 * 不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
 *
 */
public class FaceAIConfig {

    //不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
    public static String CACHE_BASE_FACE_DIR;   //1：1 人脸识别人脸图片存储目录
    public static String CACHE_SEARCH_FACE_DIR; //1：N 人脸识别人脸图片存储目录

    public static  void init(Context context) {
        //初始化人脸识别 人脸搜索存储目录，放哪里根据你的业务定,注意可能需要存储权限
        CACHE_BASE_FACE_DIR = context.getCacheDir().getPath() + "/faceVerify/";    //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = context.getFilesDir().getPath() + "/faceSearch/";  //人脸搜索人脸库目录，自行决定放在哪里
    }


}
