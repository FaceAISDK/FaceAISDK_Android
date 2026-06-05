package com.hiface.demo.SysCamera.verify;

import static com.hiface.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.hiface.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.hiface.demo.FaceSDKConfig.CACHE_FACE_LOG_DIR;

import android.content.Context;
import android.content.Intent;
import com.tencent.mmkv.MMKV;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;

import com.sdk.hiface.core.utils.FaceAICameraType;
import com.sdk.hiface.recognize.liveness.FaceLivenessType;
import com.hiface.demo.R;
import com.hiface.demo.SysCamera.search.ImageToast;
import com.hiface.demo.base.AbsBaseActivity;
import com.hiface.demo.SysCamera.camera.FaceCameraXFragment;
import com.hiface.demo.base.utils.BitmapUtils;
import com.sdk.hiface.base.view.camera.CameraXBuilder;
import com.sdk.hiface.recognize.FaceProcessBuilder;
import com.sdk.hiface.recognize.FaceVerifyUtils;
import com.sdk.hiface.recognize.ProcessCallBack;
import com.sdk.hiface.recognize.VerifyStatus.*;
import com.hiface.demo.base.utils.TTSPlayer;
import com.hiface.demo.base.view.FaceCoverView;
import com.tencent.mmkv.MMKV;

/**
 * 1：1 的人脸识别 + 动作活体检测 接入演示代码
 * <p>
 * 移动考勤签到、App免密登录、刷脸授权、刷脸解锁。请熟悉Demo主流程后根据你的业务情况再改造
 * 摄像头管理源码开放了 {@link FaceCameraXFragment}
 * 仅仅需要活体检测参考 {@link LivenessDetectActivity}
 *
 * GitHub  <a href="https://github.com/FaceSDKPro/HiFace_Android">Android Face recognize</a>
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceVerificationActivity extends AbsBaseActivity {
    public static final String USER_FACE_ID_KEY = "USER_FACE_ID_KEY";   //1:1 face verify ID KEY
    public static final String THRESHOLD_KEY = "THRESHOLD_KEY";         //人脸识别通过的阈值
    public static final String FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE";  //活体检测的类型
    public static final String MOTION_STEP_SIZE = "MOTION_STEP_SIZE";   //动作活体的步骤数
    public static final String MOTION_TIMEOUT = "MOTION_TIMEOUT";   //动作活体超时数据
    public static final String MOTION_LIVENESS_TYPES = "MOTION_LIVENESS_TYPES"; //动作活体种类
    public static final String ALLOW_MULTI_FACES = "ALLOW_MULTI_FACES"; //是否允许有多人出现在镜头Key
    private  boolean allowMultiFaces = true; //是否允许有多人出现在镜头
    private String faceID; //你的业务系统中可以唯一定义一个账户的ID，手机号/身份证号等
    private float verifyThreshold = 0.8f; //1:1人脸识别对比通过的阈值，根据使用场景自行调整

    //NONE表示无活体，MOTION表示动作活体，COLOR_FLASH表示炫彩活体（其他种类默认都会包含静默活体，如果仅仅需静默可指定SILENT_LIVE）
    //静默活体效果和摄像头成像有关，炫彩活体不能在强光下使用
    private FaceLivenessType faceLivenessType = FaceLivenessType.MOTION;  //活体检测类型建议MOTION或COLOR_FLASH_MOTION
    private int motionStepSize = 2; //动作活体的个数
    private int motionTimeOut = motionStepSize*3;  //动作超时秒
    private String motionLivenessTypes = "1,2,3,4,5"; //动作活体种类用英文","隔开； 1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头

    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private FaceCoverView faceCoverView;
    private FaceCameraXFragment cameraXFragment;  //Camera Manager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI(); //full screen
        setContentView(R.layout.activity_face_verification);

        faceCoverView = findViewById(R.id.face_cover);
        findViewById(R.id.back).setOnClickListener(v -> finishFaceVerify(VerifyStatue.DEFAULT, R.string.face_verify_result_cancel));

        getIntentParams(); //接收三方插件传递的参数，原生开发可以忽略裁剪掉

        initCameraX();
        initFaceVerifyFeature();
    }

    /**
     * 初始化摄像头
     */
    private void initCameraX() {
        MMKV mmkv = MMKV.defaultMMKV();
        int cameraLensFacing = mmkv.decodeInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT);
        int degree = mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0f)          //焦距范围[0f,1.0f]，根据应用场景自行适当调整焦距（摄像头需支持变焦）炫彩活体请设置为0f
                .setRotation(degree)        //画面旋转角度
                .setCameraSizeHigh(false)   //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment).commit();
    }


    /**
     * 初始化1:1人脸识别人脸特征值信息
     */
    private void initFaceVerifyFeature() {

        String faceFeature = MMKV.defaultMMKV().decodeString(faceID);
        if (!TextUtils.isEmpty(faceFeature)) {
            initFaceVerificationParam(faceFeature);
        }  else {
            //根据你的业务进行提示去录入人脸特征信息，或从你的服务器提前同步到本地
            TTSPlayer.getInstance().playTTS(R.string.no_face_feature);
            Toast.makeText(getBaseContext(), R.string.no_face_feature, Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finishFaceVerify(VerifyStatue.NO_BASE_FACE_FEATURE, R.string.no_face_feature, 0,0);
            }, 1111);
        }

    }


    /**
     * 初始化认证引擎，仅仅需要活体检测参考{@link LivenessDetectActivity}
     *
     * @param faceFeature 1:1 人脸识别对比的底片特征
     */
    private void initFaceVerificationParam(String faceFeature) {
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(verifyThreshold)          //阈值设置，范围限 [0.75,0.85]
                .setFaceFeature(faceFeature)            //1:1 人脸识别对比的底片人脸特征值
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)  //相机类型，目前分为3种
                .setCompareDurationTime(3000)           //人脸识别超时时间[3000,6000] 毫秒
                .setLivenessType(faceLivenessType)      //活体检测类型。
                .setMotionLivenessStepSize(motionStepSize)            //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)              //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
                .setMotionLivenessTypes(motionLivenessTypes)          //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
                .setStopVerifyNoFaceRealTime(true)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
                .setProcessCallBack(new ProcessCallBack() {
                    /**
                     * 1:1 人脸识别 活体检测 对比结束
                     *
                     * @param isMatched     true匹配成功（大于setThreshold）； false 与底片不是同一人
                     * @param similarity    与底片匹配的相似度值
                     * @param livenessValue 静默&炫彩活体分数，仅动作活体可以忽略判断(不同设备的情况可能不一样，建议大于0.8为真人)
                     * @param bitmap        识别完成的时候人脸实时图，可以用于保存日志，后期抽查
                     */
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float livenessValue, Bitmap bitmap) {
                        showVerifyResult(isMatched, similarity, livenessValue, bitmap);
                    }

                    @Override
                    public void onColorFlash(int color) {
                        faceCoverView.setFlashColor(color); //设置炫彩颜色，不能在室外强光环境使用
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int code) {
                        showFaceVerifyTips(code);
                    }

                    /**
                     * 动作活体超时倒计时百分比，注意适配低端机反应慢要多点时间
                     * @param percent
                     */
                    @Override
                    public void onTimeCountDown(float percent) {
                        faceCoverView.setProgress(percent);
                    }

                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed error!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing()) {
                //默认演示CameraX的 imageProxy 传入SDK，也支持NV21，Bitmap 类型，你也可以自己管理相机
                faceVerifyUtils.goVerifyWithImageProxy(imageProxy);
            }
        });
    }

    private int retryTime = 0;

    /**
     * dispose face verify and liveness. 人脸识别活体检测是否通过
     *
     * @param isVerifyMatched is similarity >threshold ，相似度是否大于阈值
     * @param similarity      similarity score 。 相似度得分
     * @param livenessValue   liveness score 静默活体分数不同设备可能有差异
     * @param bitmap 快照可用于log记录
     */
    private void showVerifyResult(boolean isVerifyMatched, float similarity,float livenessValue, Bitmap bitmap) {
        BitmapUtils.saveCompressBitmap(bitmap, CACHE_FACE_LOG_DIR, "verifyBitmap");  //保存场景图给三方插件使用

        if (isVerifyMatched&&(livenessValue>0.8||faceLivenessType.equals(FaceLivenessType.NONE))) {
            //2. 相似度>verifyThreshold，并且livenessValue>0.8
            TTSPlayer.getInstance().playTTS(R.string.face_verify_success);
            new ImageToast().show(getApplicationContext(), getString(R.string.face_verify_success));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finishFaceVerify(VerifyStatue.VERIFY_SUCCESS, R.string.face_verify_result_success, similarity,livenessValue);
            }, 500);
        } else {
            // 如果匹配成功但活体检测失败(code=11)，否则为匹配失败(code=2)
            int code = isVerifyMatched ? VerifyStatue.SILENT_LIVENESS_FAILED : VerifyStatue.VERIFY_FAILED;
            TTSPlayer.getInstance().playTTS(R.string.face_verify_failed);

            new AlertDialog.Builder(FaceVerificationActivity.this).setTitle(R.string.face_verify_failed_title).setMessage(R.string.face_verify_failed).setCancelable(false).setPositiveButton(R.string.know, (dialogInterface, i) -> {
                finishFaceVerify(code, R.string.face_verify_result_failed, similarity,livenessValue);
            }).setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify()).show();
        }
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     */
    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            switch (actionCode) {
                //检测到多人脸
                case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_MANY:
                    //防止一真一假人脸作弊,每帧画面检测
                    if(!allowMultiFaces){
                        Toast.makeText(this,R.string.multiple_faces_tips,Toast.LENGTH_LONG).show();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            finishFaceVerify(VerifyStatue.NOT_ALLOW_MULTI_FACES, R.string.multiple_faces_tips);
                        }, 999);
                    }
                    break;

                // 动作活体检测完成了
                case ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_SUCCESS:
                    setMainTips(R.string.keep_face_visible);
                    break;

                // 动作活体检测超时
                case ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_TIMEOUT:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.motion_liveness_detection_time_out)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(VerifyStatue.MOTION_LIVENESS_TIMEOUT, R.string.face_verify_result_timeout);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;

                // 人脸识别处理中
                case VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                    setMainTips(R.string.face_verifying);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                    TTSPlayer.getInstance().playTTS(R.string.repeat_open_close_mouse);
                    setMainTips(R.string.repeat_open_close_mouse);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.SMILE:
                    setMainTips(R.string.motion_smile);
                    TTSPlayer.getInstance().playTTS(R.string.motion_smile);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.BLINK:
                    TTSPlayer.getInstance().playTTS(R.string.motion_blink_eye);
                    setMainTips(R.string.motion_blink_eye);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                    TTSPlayer.getInstance().playTTS(R.string.motion_shake_head);
                    setMainTips(R.string.motion_shake_head);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                    TTSPlayer.getInstance().playTTS(R.string.motion_node_head);
                    setMainTips(R.string.motion_node_head);
                    break;

                // 人脸识别活体检测过程切换到后台防止作弊
                case VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.face_verify_pause)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(VerifyStatue.NO_FACE_MULTI, R.string.face_verify_result_no_face_multi_time);
                            }).show();
                    break;

                //多次没有人脸，想作弊啊🤔️
                case VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY:
                    setMainTips(R.string.no_face_or_repeat_switch_screen);
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.stop_verify_tips)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(VerifyStatue.NO_FACE_MULTI, R.string.face_verify_result_no_face_multi_time);
                            }).show();
                    break;

                // ------------   以下是setSecondTips    -----------------
                case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                    setSecondTips(R.string.far_away_tips);
                    break;

                //人脸太小靠近一点摄像头。炫彩活体检测强制要求靠近屏幕才能把光线打在脸上
                case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL:
                    setSecondTips(R.string.come_closer_tips);
                    break;

                //检测到正常的人脸，尺寸大小OK
                case VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT:
                    setSecondTips(0);
                    break;

                case VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                    setSecondTips(R.string.no_face_detected_tips);
                    break;

                //炫彩活体检测需要人脸更加靠近屏幕摄像头才能通过检测
                case VERIFY_DETECT_TIPS_ENUM.COLOR_FLASH_NEED_CLOSER_CAMERA:
                    setSecondTips(R.string.color_flash_need_closer_camera);
                    TTSPlayer.getInstance().playTTS(R.string.color_flash_need_closer_camera,TTSPlayer.PlayMode.DROP_IF_BUSY);
                    break;

                //炫彩活体通过
                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_SUCCESS:
                    TTSPlayer.getInstance().playTTS(R.string.keep_face_visible);
                    setMainTips(R.string.keep_face_visible);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_FAILED:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.color_flash_liveness_failed)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(VerifyStatue.COLOR_LIVENESS_FAILED, R.string.color_flash_liveness_failed);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;

                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIGHT_HIGH:
                    LayoutInflater inflater = LayoutInflater.from(this);
                    View dialogView = inflater.inflate(R.layout.dialog_light_warning, null);
                    new AlertDialog.Builder(this)
                            .setView(dialogView) // 【关键】设置自定义的 View
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(VerifyStatue.COLOR_LIVENESS_LIGHT_TOO_HIGH, R.string.color_flash_light_high);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;

                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_START:

                    break;
            }
        }
    }


    /**
     * 主要提示
     */
    private void setMainTips(int resId) {
        faceCoverView.setTipsText(resId);
    }

    /**
     * 第二行提示
     */
    private void setSecondTips(int resId) {
        faceCoverView.setSecondTipsText(resId);
    }

    /**
     * 退出页面，释放资源
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishFaceVerify(VerifyStatue.DEFAULT, R.string.face_verify_result_cancel);
    }


    /**
     * 资源释放
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceVerifyUtils.destroyProcess();
    }

    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     */
    protected void onStop() {
        super.onStop();
        faceVerifyUtils.pauseProcess();
    }


    // ************************** 下面代码是为了兼容三方插件，原生开发可以忽略   ***********************************

    /**
     * 获取UNI,RN,Flutter三方插件传递的参数,以便在原生代码中生效
     */
    private void getIntentParams() {
        Intent intent = getIntent(); // 获取发送过来的Intent对象
        if (intent != null) {
            if (intent.hasExtra(USER_FACE_ID_KEY)) {
                faceID = intent.getStringExtra(USER_FACE_ID_KEY);
            } else {
                Toast.makeText(this, R.string.input_face_id_tips, Toast.LENGTH_LONG).show();
            }

            if (intent.hasExtra(THRESHOLD_KEY)) {
                verifyThreshold = intent.getFloatExtra(THRESHOLD_KEY, 0.85f);
            }

            if (intent.hasExtra(ALLOW_MULTI_FACES)) {
                allowMultiFaces = intent.getBooleanExtra(ALLOW_MULTI_FACES, true);
            }

            if (intent.hasExtra(FACE_LIVENESS_TYPE)) {
                int type = intent.getIntExtra(FACE_LIVENESS_TYPE, 1);
                // 1.动作活体  2.动作+炫彩活体 3.炫彩活体(不能强光环境使用) 4.仅仅静默活体检测
                // 1，2，3 类型都包含静默活体；如果仅需静默活体请设置为4（表现和设备摄像头有关）
                switch (type) {
                    case 1:
                        faceLivenessType = FaceLivenessType.MOTION;
                        break;
                    case 2:
                        faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION;
                        break;
                    case 3:
                        faceLivenessType = FaceLivenessType.COLOR_FLASH;
                        break;
                    case 4:
                        faceLivenessType = FaceLivenessType.SILENT_LIVE; //仅仅静默活体
                        break;
                    default:
                        faceLivenessType = FaceLivenessType.NONE;
                }
            }

            if (intent.hasExtra(MOTION_STEP_SIZE)) {
                motionStepSize = intent.getIntExtra(MOTION_STEP_SIZE, 2);
            }
            if (intent.hasExtra(MOTION_TIMEOUT)) {
                motionTimeOut = intent.getIntExtra(MOTION_TIMEOUT, 9);
            }
            if (intent.hasExtra(MOTION_LIVENESS_TYPES)) {
                motionLivenessTypes = intent.getStringExtra(MOTION_LIVENESS_TYPES);
            }
        }
    }

    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private void finishFaceVerify(int code, int msgStrRes) {
        finishFaceVerify(code, msgStrRes, 0f,0f);
    }


    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private void finishFaceVerify(int code, int msgStrRes, float similarity,float livenessValue) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", getString(msgStrRes))
                .putExtra("livenessValue",livenessValue)
                .putExtra("similarity", similarity);
        setResult(RESULT_OK, intent);
        finish();
    }

}

