package com.ai.face;

import android.app.Application;
import com.tencent.bugly.crashreport.CrashReport;
import java.io.File;

/**
 * 不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
 *
 */
public class MyFaceApplication extends Application {

    public static final String USER_ID_KEY="AI_BASE_FACE_NAME";
    public static final String FACE_DIR_KEY="BASE_FACE_DIR";

    //不要直接使用File Api 直接往文件目录插入图片，要使用SDK 提供的APi写入数据，图片还需要向量化
    public static  String CACHE_BASE_FACE_DIR;           //1：1 人脸识别人脸图片存储目录
    public static  String CACHE_SEARCH_FACE_DIR;         //1：N 人脸识别人脸图片存储目录

    @Override
    public void onCreate() {
        super.onCreate();


        //初始化人脸识别人脸图片存储目录
        CACHE_BASE_FACE_DIR = getCacheDir().getPath()+ "/faceVerify";
        CACHE_SEARCH_FACE_DIR = getFilesDir().getPath() + "/faceSearch";

        //初始化创建1:N（M：N） 人脸搜索目录，人脸图集放在这里
        //不要直接文件操作把人脸图放到这个目录，这样不能搜索，要通过SDK 的API 进行人脸的增删改查
        File file = new File(CACHE_SEARCH_FACE_DIR);
        if (!file.exists()) file.mkdirs();



        //Crash 收集，仅仅是Demo 需要
        CrashReport.initCrashReport(this, "36fade54d8", true);
    }




}
