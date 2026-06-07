package com.hiface.demo.SysCamera.search;

import static com.hiface.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.hiface.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.hiface.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;
import static com.sdk.hiface.search.SearchProcessTipsCode.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageProxy;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.sdk.hiface.base.view.camera.CameraXBuilder;
import com.sdk.hiface.core.utils.FaceAICameraType;
import com.sdk.hiface.search.FaceSearchEngine;
import com.sdk.hiface.search.FaceSearchResult;
import com.sdk.hiface.search.SearchProcessBuilder;
import com.sdk.hiface.search.SearchProcessCallBack;
import com.tencent.mmkv.MMKV;

import com.hiface.demo.R;
import com.hiface.demo.SysCamera.camera.FaceCameraXFragment;
import com.hiface.demo.base.AbsBaseActivity;
import com.hiface.demo.base.utils.TTSPlayer;
import com.hiface.demo.base.utils.VoicePlayer;
import com.hiface.demo.databinding.ActivityFaceSearchBinding;

import java.util.List;

/**
 * RGB摄像头动作活体检测+1:N 人脸搜索识别。当前人脸库默认最大5000，未成年搜索精度待提升
 * 数据合规建议不要再收集人脸原始图片数据 https://mp.weixin.qq.com/s/aGPwYUYxnr6ZDRxwAQd8vg
 * 摄像头管理源码开放在 {@link FaceCameraXFragment}
 * 内置1W测试人脸APK特制版本（点击导入 Asset 目录人脸）：https://www.pgyer.com/4c1e43a4e28bc50885ab942b41b1b85d
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceSearch1NActivity extends AbsBaseActivity {
    public static final String THRESHOLD_KEY = "THRESHOLD_KEY";    //人脸搜索阈值
    public static final String NEED_FACE_LIVE = "NEED_FACE_LIVE";   //是否开启活体检测
    public static final String SEARCH_ONE_TIME = "SEARCH_ONE_TIME";   //是否仅搜索一次就关闭搜索页
    public static final String IS_CAMERA_SIZE_HIGH = "IS_CAMERA_SIZE_HIGH";   //高分辨率远距离也可以工作，但是性能速度会下降
    private float searchThreshold = 0.8f;  //搜索阈值,场景越严格阈值应该设的越高(同时要求录入人脸使用SDK相机校验，不要随便拍一张照片)
    private boolean searchOneTime = false;  //是否仅搜索一次就关闭搜索页
    private boolean needFaceLive = true;   //是否开启活体检测
    private boolean isCameraSizeHigh = false; //是否高分辨率
    private int cameraLensFacing;  //摄像头前置，后置，外接
    private ActivityFaceSearchBinding binding;
    private FaceCameraXFragment cameraXFragment; //摄像头请自行管理，源码全部开放
    private boolean pauseSearch = false; //控制是否送数据到SDK进行搜索


    // 如果你在后台通过人脸图检测人脸提取人脸特征（非常不建议收集人脸原始图片），需要暂停前台的通过相机检测人脸
    // 直接更新人脸特征数据不涉及到检测人脸提取特征不需要暂停人脸搜索
    // https://mp.weixin.qq.com/s/aGPwYUYxnr6ZDRxwAQd8vg
    private  class FaceUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null){
                if (ACTION_START_UPDATE_FACE_DATA.equals(intent.getAction())) {
                    pauseSearch=true;
                    setSearchTips(R.string.face_data_update);
                }else if (ACTION_COMPLETE_UPDATE_FACE_DATA.equals(intent.getAction())) {
                    pauseSearch=false;
                    setParamsThenFaceSearch();//重新设置参数开始人脸搜索
                }
            }
        }
    }
    public static final String ACTION_START_UPDATE_FACE_DATA = "FACESDK.ACTION_START_UPDATE_FACE_DATA";
    public static final String ACTION_COMPLETE_UPDATE_FACE_DATA = "FACESDK.ACTION_COMPLETE_UPDATE_FACE_DATA";
    private LocalBroadcastManager FaceUpdateBroadcastManager;
    private FaceUpdateReceiver faceUpdateReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.close.setOnClickListener(v -> finish());

        getIntentParams(); //接收三方插件传递的参数，原生开发可以忽略裁剪掉

        MMKV mmkv = MMKV.defaultMMKV();
        cameraLensFacing = mmkv.decodeInt(FRONT_BACK_CAMERA_FLAG, 0); //默认前置
        int degree = mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //1. 摄像头相关参数配置
        /**摄像头管理源码开放在 {@link FaceCameraXFragment} **/
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.01f)      //焦距范围[0f,1.0f]，根据应用场景自行适当调整焦距（摄像头需支持变焦）
                .setRotation(degree)      //画面旋转方向
                .setCameraSizeHigh(isCameraSizeHigh) //高分辨率远距离也可以工作，但是性能速度会下降
                .create();

        //可以不用SDK 内部相机管理，自定义摄像头参考MyCameraFragment，源码开放自由修改
        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        setParamsThenFaceSearch(); //设置参数然后开始人脸搜索识别

        // 2. 获取 LocalBroadcastManager 实例
        FaceUpdateBroadcastManager = LocalBroadcastManager.getInstance(this);
    }


    /**
     * 初始化人脸搜索参数,开始人脸搜索
     */
    private void setParamsThenFaceSearch() {
        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
//                .setFaceGroup() //根据分组来搜索，比如小区不同楼栋可以设置从1A，1B，2C等分组不但能管理权限又能加快速度
//                .setFaceTag()   //根据标记来搜索，比如有些场所只有VIP才能权限进入
                .setNeedFaceLiveness(needFaceLive)//是否需要活体检测，只有1:N 搜索 有活体（选配，默认无）
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_1) //1:N 搜索
                .setThreshold(searchThreshold) //阈值范围限 [0.75 , 0.85] 识别可信度，阈值高摄像头成像品质宽动态值以及人脸底片质量也要高
                .setCallBackAllMatch(true) //默认是false,是否返回所有的大于设置阈值的搜索结果
                .setSearchIntervalTime(1800) //默认2000，范围[0,9000]毫秒。搜索成功后的继续下一次搜索的间隔时间，不然会一直搜索一直回调结果
                .setSearchTimeOut(4000)    //搜索超时时间，超时后会提示无结果,默认3000，范围[3000,6000]毫秒
                .setMirror(cameraLensFacing == CameraSelector.LENS_FACING_FRONT) //后面版本去除次参数
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * onMostSimilar 是返回搜索到最相似的人脸，有可能光线弱，人脸底片不合规导致错误匹配
                     * 业务上可以添加容错处理，onFaceMatched会返回所有大于设置阈值的结果并排序好
                     *
                     * 强烈建议使用支持宽动态的高品质摄像头，录入高品质人脸
                     * SearchProcessBuilder setCallBackAllMatch(true) onFaceMatched才会回调
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap,float livenessValue) {
                        //已经按照降序排列，可以弹出一个列表框。传给RN，Flutter,uniapp 插件使用
                        String json = new Gson().toJson(matchedResults);
                        Log.d("onFaceMatched", "符合设定阈值的结果: " + json);
                    }

                    /**
                     * 最相似的人脸搜索识别结果，得分最高
                     * @param faceID  最相似的人脸ID
                     * @param score   相似度值分数（大于searchThreshold）
                     * @param bitmap  当前场景图bitmap，可以用来做使用记录log
                     * @param livenessValue 静默活体分数(RGB摄像头静默活体表现和不同设备的相机有关)
                     */
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap, float livenessValue) {
                        Bitmap faceBitmap = BitmapFactory.decodeFile(CACHE_SEARCH_FACE_DIR + faceID);//传给插件，其他可以忽略
                        String tips=faceID + "," + score + "," + livenessValue;

                        if (livenessValue > 0.85) { //根据你的摄像头和使用场景 自定义管理活体分数业务逻辑
                            VoicePlayer.getInstance().play(R.raw.ding_success);
                            TTSPlayer.getInstance().playTTS(faceID); //检测为活体才语音提示
                            new ImageToast().showBitmap(getApplication(), faceBitmap, tips);
                        } else {
                            VoicePlayer.getInstance().play(R.raw.ding_failed);
                            new ImageToast().showBitmap(getApplication(), faceBitmap, tips,false);
                        }
                    }

                    /**
                     * 检测到人脸的位置信息，画框用
                     */
                    @Override
                    public void onFaceDetected(List<FaceSearchResult> result) {
                        //画框UI代码完全开放，用户可以根据情况自行改造
                        binding.graphicOverlay.drawRect(result);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showFaceSearchPrecessTips(i);
                    }

                    @Override
                    public void onLog(String log) {

                    }

                }).create();


        //3.根据参数初始化引擎
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);

        // 4.从标准默认的HAL CameraX 摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上,  摄像头RGB 宽动态(大于105Db)高清成像，光线不足设备加补光灯
        cameraXFragment.setOnAnalyzerListener(new FaceCameraXFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
                if (!isDestroyed() && !isFinishing() && !pauseSearch) {
                    FaceSearchEngine.Companion.getInstance().runSearchWithImageProxy(imageProxy, 0);
                }
            }

            //后台用于人脸搜索分析的图片宽高，画人脸检测框需要
            @Override
            public void backImageSize(int imageWidth, int imageHeight) {
                //如果发现人脸框坐标左右镜像了，第三个参数置反一下就可以了
                binding.graphicOverlay.setCameraInfo(imageWidth, imageHeight, cameraXFragment.isFrontCamera());
            }
        });
    }

    /**
     * 显示人脸搜索识别提示，根据Code码显示对应的提示,用户根据自己业务处理细节
     *
     * @param code 提示Code码
     */
    private void showFaceSearchPrecessTips(int code) {
        switch (code) {
            case NO_MATCHED: //setSearchTimeOut 超时没有搜索成功的提示
                //setSearchTimeOut超时没有搜索匹配到结果.
                Toast.makeText(this, R.string.no_matched_face, Toast.LENGTH_SHORT).show();
                break;

            case LOCAL_FACE_DATABASE_EMPTY:
                //人脸库没有人脸照片，使用SDK API插入人脸
                setSearchTips(R.string.local_face_database_empty);
                break;

            case SEARCH_INITIALIZING:
                setSecondTips(R.string.sdk_init);
                break;

            case SEARCH_PREPARED: //搜索准备就绪
            case SEARCHING: //人脸角度等条件OK，人脸搜索中
                setSearchTips(R.string.keep_face_tips);
                break;

            case FACE_ANGLE_NOT_FIT:
                setSearchTips(R.string.face_angle_not_fit);
                break;

            case NO_LIVE_FACE:
                Log.d("NO_LIVE_FACE", "没有检测到人脸" );
                setSearchTips(R.string.no_face_detected_tips);
                break;

            case FACE_TOO_SMALL:
                setSecondTips(R.string.come_closer_tips);
                break;

            // 单独使用一个textview 提示，防止上一个提示被覆盖。
            // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
            case FACE_TOO_LARGE:
                setSecondTips(R.string.far_away_tips);
                break;

            //检测到正常的人脸，尺寸大小OK
            case FACE_SIZE_FIT:
                setSecondTips(0);
                break;

            case THRESHOLD_ERROR:
                setSearchTips(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                setSearchTips(R.string.no_mask_please);
                break;

            default: //其他tips code
                binding.faceCover.setTipsText("Tips Code：" + code);
                break;
        }
    }

    private void setSearchTips(int resId) {
        binding.faceCover.setTipsText(resId);
    }

    /**
     * 第二行的提示
     *
     * @param resId
     */
    private void setSecondTips(int resId) {
        binding.faceCover.setSecondTipsText(resId);
    }

    /**
     * 停止人脸搜索，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();

        //防止内存泄漏
        if (FaceUpdateBroadcastManager != null && faceUpdateReceiver != null) {
            FaceUpdateBroadcastManager.unregisterReceiver(faceUpdateReceiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 动态注册广播接收器,当正在后台通过人脸图更新人脸数据时候(不建议)，需要暂停前台的通过相机检测人脸，避免冲突
        faceUpdateReceiver = new FaceUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_START_UPDATE_FACE_DATA);
        filter.addAction(ACTION_COMPLETE_UPDATE_FACE_DATA);
        FaceUpdateBroadcastManager.registerReceiver(faceUpdateReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pauseSearch = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseSearch = true;
    }



    /**
     * 获取UNI,RN,Flutter三方插件传递的参数,以便在原生代码中生效
     */
    private void getIntentParams() {
        Intent intent = getIntent(); // 获取发送过来的Intent对象
        if (intent != null) {
            if (intent.hasExtra(THRESHOLD_KEY)) {
                searchThreshold = intent.getFloatExtra(THRESHOLD_KEY, 0.85f);
            }
            if (intent.hasExtra(SEARCH_ONE_TIME)) {
                searchOneTime = intent.getBooleanExtra(SEARCH_ONE_TIME, false);
            }
            if (intent.hasExtra(IS_CAMERA_SIZE_HIGH)) {
                isCameraSizeHigh = intent.getBooleanExtra(IS_CAMERA_SIZE_HIGH, false);
            }
            if (intent.hasExtra(NEED_FACE_LIVE)) {
                needFaceLive = intent.getBooleanExtra(NEED_FACE_LIVE, false);
            }
        }
    }

}
