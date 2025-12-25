package com.faceAI.demo.SysCamera.search;

import static com.ai.face.faceSearch.search.SearchProcessTipsCode.EMGINE_INITING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_DIR_EMPTY;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_SIZE_FIT;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_LARGE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_SMALL;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.MASK_DETECTION;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_LIVE_FACE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_MATCHED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCHING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCH_PREPARED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.TOO_MUCH_FACE;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;

import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.base.view.FaceVerifyCoverView;
import com.faceAI.demo.databinding.ActivityFaceSearchBinding;

import java.util.List;

/**
 * RGB摄像头动作活体检测+1:N 人脸搜索识别。
 *
 * 摄像头管理源码开放在 {@link FaceCameraXFragment}
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceSearch1NWithMotionLivenessActivity extends AbsBaseActivity {
    //如果设备在弱光环境没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchBinding binding;
    private FaceCameraXFragment cameraXFragment; //可以使用开放的摄像头管理源码MyCameraFragment，自行管理摄像头
    private boolean pauseSearch =false; //控制是否送数据到SDK进行搜索
    private int cameraLensFacing;

    //================活体检测--------------
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private FaceLivenessType faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION; //活体检测类型
    private int motionStepSize = 1; //动作活体的个数
    private int motionTimeOut = 4; //动作超时秒
    private String motionLivenessTypes ="1,2,3,4,5" ; //1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头
    private boolean isLivenessPass=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();
        binding.close.setOnClickListener(v -> finish());

        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class)
                    .putExtra("isAdd", false));
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT); //默认前置
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //1. 摄像头相关参数配置
        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0f)     //焦距范围[0f,1.0f]，参考 {@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)      //画面旋转方向
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降
                .create();

        //可以不用SDK 内部相机管理，自定义摄像头参考 FaceCameraXFragment，源码开放自由修改
        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        initLivenessParam();

        // 4.从标准默认的HAL CameraX 摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上,  摄像头RGB 宽动态(大于105Db)高清成像，光线不足设备加补光灯
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
            if (!isDestroyed() && !isFinishing()&&!pauseSearch) {
                if(isLivenessPass){
                    FaceSearchEngine.Companion.getInstance().runSearchWithImageProxy(imageProxy, 0);
                }else{
                    faceVerifyUtils.goVerifyWithImageProxy(imageProxy);
                }
            }
        });

    }


    /**
     * 初始化活体检测引擎
     */
    private void initLivenessParam() {
        //建议老的低配设备减少活体检测步骤
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setLivenessOnly(true)
                .setLivenessType(faceLivenessType) //活体检测可以炫彩&动作活体组合
                .setSilentLivenessThreshold(0.7f)   //已经废弃，2025.12.19 改为炫彩活体检测
                .setMotionLivenessStepSize(motionStepSize)             //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)               //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
                .setLivenessDetectionMode(MotionLivenessMode.ACCURACY) //硬件配置低用FAST动作活体模式，否则用精确模式
                .setMotionLivenessTypes(motionLivenessTypes)           //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
                .setStopVerifyNoFaceRealTime(false)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
                .setProcessCallBack(new ProcessCallBack() {

                    /**
                     * 动作活体检测完成
                     *
                     * @param score  炫彩分
                     * @param bitmap 活体检测快照，可以用于log记录
                     */
                    @Override
                    public void onLivenessDetected(float score, Bitmap bitmap) {
                            initFaceSearchParam();
                            isLivenessPass=true;
                    }

                    @Override
                    public void onColorFlash(int color) {
                       binding.faceCover.setFlashColor(color); //更新炫彩屏幕颜色，不能在室外强光下使用
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showFaceVerifyTips(i);
                    }

                    /**
                     * 动作活体倒计时
                     * @param percent
                     */
                    @Override
                    public void onTimeCountDown(float percent) {
                        binding.faceCover.setProgress(percent);
                    }

                    //发送严重错误，会中断业务流程
                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed错误!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);

    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    int retryTime = 0;
    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
                switch (actionCode) {
                    //炫彩活体检测需要人脸更加靠近屏幕摄像头才能通过检测
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.COLOR_FLASH_NEED_CLOSER_CAMERA:
                        setSecondTips(R.string.color_flash_need_closer_camera);
                        break;

                    //炫彩活体通过✅
                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_SUCCESS:
                        setMainTips(R.string.keep_face_visible);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_FAILED:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.color_flash_liveness_failed)
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                    retryTime++;
                                    if (retryTime > 1) {
                                        finish();
                                    } else {
                                        faceVerifyUtils.retryVerify();
                                    }
                                }).show();
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIGHT_HIGH:
                        LayoutInflater inflater = LayoutInflater.from(this);
                        View dialogView = inflater.inflate(R.layout.dialog_light_warning, null);
                        new AlertDialog.Builder(this)
                                .setView(dialogView) // 【关键】设置自定义的 View
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                    retryTime++;
                                    if (retryTime > 1) {
                                        finish();
                                    } else {
                                        faceVerifyUtils.retryVerify();
                                    }
                                }).show();
                        break;

                    // 动作活体检测完成了
                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_SUCCESS:
                        VoicePlayer.getInstance().play(R.raw.verify_success);
                        setMainTips(R.string.keep_face_visible); //2秒后抓取一张正脸图
                        setSecondTips(0);
                        break;

                    // 动作活体检测超时
                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_TIMEOUT:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.motion_liveness_detection_time_out)
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                    retryTime++;
                                    if (retryTime > 1) {
                                        finish();
                                    } else {
                                        faceVerifyUtils.retryVerify();
                                    }
                                }).show();
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        setMainTips(R.string.face_verifying);
                        break;


                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        setMainTips(R.string.repeat_open_close_mouse);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        setMainTips(R.string.motion_smile);
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        setMainTips(R.string.motion_blink_eye);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        setMainTips(R.string.motion_shake_head);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        setMainTips(R.string.motion_node_head);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.face_verify_pause)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    finish();
                                })
                                .show();
                        break;


                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY:
                        setMainTips(R.string.no_face_or_repeat_switch_screen);
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.stop_verify_tips)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    finish();
                                })
                                .show();
                        break;

                    // 单独使用一个textview 提示，防止上一个提示被覆盖。
                    // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                        setSecondTips(R.string.far_away_tips);
                        break;

                    //人脸太小了，靠近一点摄像头
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL:
                        setSecondTips(R.string.come_closer_tips);
                        break;

                    //检测到正常的人脸，尺寸大小OK
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT:
                        setSecondTips(0);
                        break;
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        setSecondTips(R.string.no_face_detected_tips);
                        break;
                }
        }
    }


    /**
     * 初始化人脸搜索参数
     */
    private void initFaceSearchParam() {
        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
                .setThreshold(0.85f) //阈值范围限[0.85 , 0.95] 搜索识别可信度，阈值高摄像头成像品质宽动态值以及人脸底片质量也要高
                .setCallBackAllMatch(true) //默认是false,是否返回所有的大于设置阈值的搜索结果
                .setSearchIntervalTime(1900) //默认2000，范围[1500,3000]毫秒。搜索成功后的继续下一次搜索的间隔时间，不然会一直搜索一直回调结果
                .setMirror(cameraLensFacing == CameraSelector.LENS_FACING_FRONT) //后面版本去除此参数
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * 搜索出来的所有大于设置阈值的人脸matchedResults
                     * onMostSimilar回调方法 是返回搜索到最相似的人脸
                     * 业务上可以添加容错处理，onFaceMatched会返回所有大于设置阈值的结果并排序好
                     *
                     * 强烈建议使用支持宽动态的高品质摄像头，录入高品质人脸
                     * SearchProcessBuilder setCallBackAllMatch(true) onFaceMatched才会回调
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
                        //已经按照降序排列，可以弹出一个列表框
                        Log.d("onFaceMatched","符合设定阈值的结果: "+matchedResults.toString());
                    }

                    /**
                     * 最相似的人脸搜索识别结果
                     *
                     * @param faceID  人脸ID
                     * @param score   相似度值
                     * @param bitmap  场景图，可以用来做使用记录log
                     */
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        Bitmap mostSimilarBmp = BitmapFactory.decodeFile(CACHE_SEARCH_FACE_DIR + faceID);
                        new ImageToast().show(getApplicationContext(), mostSimilarBmp, faceID.replace(".jpg"," ")+score);
                        VoicePlayer.getInstance().play(R.raw.success);
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
                        binding.tips.setText(log);
                    }

                }).create();


        //3.根据参数初始化引擎
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);

    }


    /**
     * 显示人脸搜索识别提示，根据Code码显示对应的提示,用户根据自己业务处理细节
     *
     * @param code 提示Code码
     */
    private void showFaceSearchPrecessTips(int code) {
        switch (code) {
            case NO_MATCHED:
                //本次没有搜索匹配到结果.没有结果会持续尝试1秒之内没有结果会返回NO_MATCHED code
                setSecondTips(R.string.no_matched_face);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，使用SDK API插入人脸
                setMainTips(R.string.face_dir_empty);
                Toast.makeText(this, R.string.face_dir_empty, Toast.LENGTH_LONG).show();
                break;

            case EMGINE_INITING:
//                setMainTips(R.string.sdk_init);
                break;

            case SEARCH_PREPARED:
                //initSearchParams 后引擎需要加载人脸库等初始化，完成会回调
                setMainTips(R.string.keep_face_tips);
                break;

            case  SEARCHING:
                //后期将废除本状态
                setMainTips(R.string.keep_face_tips);
                break;

            case NO_LIVE_FACE:
                Toast.makeText(this, R.string.no_face_detected_tips, Toast.LENGTH_LONG).show();

                finish();

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

            case TOO_MUCH_FACE:
                Toast.makeText(this, R.string.multiple_faces_tips, Toast.LENGTH_SHORT).show();
                break;

            case THRESHOLD_ERROR:
                setMainTips(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                setMainTips(R.string.no_mask_please);
                break;

            default:
                binding.searchTips.setText("搜索提示：" + code);
                break;
        }
    }

    private void setMainTips(int resId) {
        binding.searchTips.setText(resId);
    }

    /**
     * 第二行提示
     * @param resId
     */
    private void setSecondTips(int resId){
        if(resId==0){
            binding.secondSearchTips.setText("");
            binding.secondSearchTips.setVisibility(View.INVISIBLE);
        }else {
            binding.secondSearchTips.setText(resId);
            binding.secondSearchTips.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 停止人脸搜索，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pauseSearch=false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseSearch=true;
    }
}