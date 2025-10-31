package com.faceAI.demo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ai.face.base.baseImage.FaceEmbedding;
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.faceAI.demo.base.utils.VoicePlayer;

import java.io.File;

/**
 * 不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化，检测质量等操作
 */
public class FaceSDKConfig {

    //不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
    public static String CACHE_BASE_FACE_DIR;   //1：1 人脸识别人脸图片存储目录
    public static String CACHE_SEARCH_FACE_DIR; //1：N 人脸识别搜索人脸图片存储目
    public static String CACHE_FACE_LOG_DIR;    //本地保存某次人脸校验完成后的场景图目录

    /**
     * 初始化人脸本地图片存储目录，也可以不存图片转化为人脸特征向量保存
     *
     */
    public static void init(Context context) {
        // 人脸图存储在App内部私有空间，SDK未做分区存储
        // Warming: 目前仅能存储在context.getCacheDir() 或者context.getFilesDir() 内部私有空间
        // https://developer.android.com/training/data-storage?hl=zh-cn
        CACHE_BASE_FACE_DIR = context.getFilesDir().getPath() + "/FaceAI/Verify/";    //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = context.getFilesDir().getPath() + "/FaceAI/Search/";  //人脸搜索人脸库目录
        CACHE_FACE_LOG_DIR= context.getFilesDir().getPath() + "/FaceAI/Log/";  //使用场景图目录

        //语音提示播报,后面文字转语音吧
        VoicePlayer.getInstance().init(context);
    }

    /**
     * 清除1:N 所有的人脸搜索识别库人脸图片数据
     */
    public static void clearAllFaceSearchLib(Context context){
        FaceSearchImagesManger.Companion.getInstance((Application) context).clearFaceImages(CACHE_SEARCH_FACE_DIR);
    }

    /**
     * 删除1:1 人脸识别faceID 本地对应的图片和特征向量编码
     * @param filePath 人脸图片路径
     * @param faceID 人脸ID
     */
    public static boolean deleteFace(Context mContext, @NonNull String filePath,@NonNull String faceID){
        FaceEmbedding.deleteEmbedding(mContext,faceID);
        File file=new File(filePath);
        if (file.exists()){
            return file.delete();
        }else {
            return false;
        }
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
