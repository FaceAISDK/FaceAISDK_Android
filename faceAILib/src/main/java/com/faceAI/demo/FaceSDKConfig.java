package com.faceAI.demo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.ai.face.faceSearch.search.Image2FaceFeature;
import com.bumptech.glide.Glide;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.tencent.mmkv.MMKV;

/**
 * 不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化，检测质量等操作
 */
public class FaceSDKConfig {

    //不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
    public static String CACHE_BASE_FACE_DIR;   //1：1 人脸识别人脸图片存储目录
    public static String CACHE_SEARCH_FACE_DIR; //1：N 人脸识别搜索人脸图片存储目
    public static String CACHE_FACE_LOG_DIR;    //本地保存某次人脸校验完成后的场景图目录

    /**
     * 初始化人脸本地图片存储目录，请在Application onCreate中调用
     *
     */
    public static void init(Context context) {
        // 人脸图存储在App内部私有空间，SDK未做分区存储
        // Warming: 目前仅能存储在context.getCacheDir() 或者context.getFilesDir() 内部私有空间
        // https://developer.android.com/training/data-storage?hl=zh-cn
        CACHE_BASE_FACE_DIR = context.getFilesDir().getPath() + "/FaceAI/Verify/";    //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = context.getFilesDir().getPath() + "/FaceAI/Search/";  //人脸搜索人脸库目录
        CACHE_FACE_LOG_DIR= context.getFilesDir().getPath() + "/FaceAI/Log/";  //使用场景图目录

        //1:1人脸特征保存key为faceID,value为特征值
        MMKV.initialize(context);

        //语音提示播报,后面文字转语音吧
        VoicePlayer.getInstance().init(context);
    }

    /**
     * 清除所有的{人脸搜索识别}人脸特征值和本地缓存的图片
     */
    public static void clearAllFaceSearchData(Context context){
        Image2FaceFeature.getInstance(context).clearFaceImages(CACHE_SEARCH_FACE_DIR);
        Glide.get(context).clearMemory();
    }


    /**
     * 清除某个{人脸搜索识别}人脸特征值和本地缓存的图片
     */
    public static void deleteFaceSearchData(Context context,String faceID){
        Image2FaceFeature.getInstance(context).deleteFaceImage(CACHE_SEARCH_FACE_DIR+faceID);
    }



    /**
     * 删除1:1 人脸识别faceID 本地对应的图片和特征向量编码
     */
    public static void deleteFaceVerifyData(Context context,String faceID){
        //1:1 的人脸特征清除
        MMKV.defaultMMKV().removeValueForKey(faceID);
        //如果缓存了图片也删除
        Image2FaceFeature.getInstance(context).deleteFaceImage(CACHE_BASE_FACE_DIR+faceID);
    }




    /**
     * 检测App 是否调试模式
     *
     * @param  mContext
     * @return
     */
    public static boolean isDebugMode(Context mContext){
        //Debug 模式是打开状态
        return 0 != (mContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
    }

}
