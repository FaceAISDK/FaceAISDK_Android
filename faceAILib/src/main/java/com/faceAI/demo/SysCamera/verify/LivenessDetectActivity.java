package com.faceAI.demo.SysCamera.verify;

import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_FACE_LOG_DIR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM;
import com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.SysCamera.search.ImageToast;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.base.view.FaceVerifyCoverView;

/**
 * 活体检测 SDK 接入演示代码.
 * <p>
 * 摄像头管理源码开放了 {@link FaceCameraXFragment}
 * More：<a href="https://github.com/FaceAISDK/FaceAISDK_Android">人脸识别FaceAISDK</a>
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class LivenessDetectActivity extends AbsBaseActivity {
    private TextView tipsTextView, secondTipsTextView;
    private FaceVerifyCoverView faceCoverView;
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private FaceCameraXFragment cameraXFragment;
    public static final String FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE";   //活体检测的类型
    public static final String MOTION_STEP_SIZE = "MOTION_STEP_SIZE";   //动作活体的步骤数
    public static final String MOTION_TIMEOUT = "MOTION_TIMEOUT";   //动作活体超时数据
    public static final String MOTION_LIVENESS_TYPES = "MOTION_LIVENESS_TYPES"; //动作活体种类
    private int retryTime = 0; //记录失败尝试的次数
    private FaceLivenessType faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION; //活体检测类型
    private int motionStepSize = 2; //动作活体的个数
    private int motionTimeOut = 7;  //动作超时秒
    private String motionLivenessTypes = "1,2,3,4,5"; //【配置动作活体类型】1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();//炫彩活体全屏显示各种颜色
        setContentView(R.layout.activity_liveness_detection);
        tipsTextView = findViewById(R.id.tips_view);
        secondTipsTextView = findViewById(R.id.second_tips_view);
        faceCoverView = findViewById(R.id.face_cover);
        findViewById(R.id.back).setOnClickListener(v -> finishFaceVerify(0, R.string.face_verify_result_cancel));

        getIntentParams();    //接收三方插件的参数 数据

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0f)    //焦距范围[0f,1.0f]，炫彩请设为0；根据应用场景适当调整焦距参数（摄像头需支持变焦）
                .setRotation(degree)  //画面旋转方向
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

        initLivenessParam();
    }

    /**
     * 初始化认证引擎
     */
    private void initLivenessParam() {
        //建议老的低配设备减少活体检测步骤
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setLivenessOnly(true)
                .setLivenessType(faceLivenessType)  //活体检测可以炫彩&动作活体组合，炫彩活体不能在强光下使用
                .setMotionLivenessStepSize(motionStepSize)             //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)               //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
                .setLivenessDetectionMode(MotionLivenessMode.ACCURACY) //硬件配置低用FAST动作活体模式，否则用精确模式
                .setMotionLivenessTypes(motionLivenessTypes)           //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
                .setStopVerifyNoFaceRealTime(true)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
                .setProcessCallBack(new ProcessCallBack() {

                    /**
                     * 动作活体+炫彩活体都 检测完成，返回活体分数
                     *
                     * @param livenessValue 活体分数(不同设备的情况可能不一样，建议大于0.75为真人)
                     * @param bitmap 活体检测快照，可以用于log记录
                     */
                    @Override
                    public void onLivenessDetected(float livenessValue, Bitmap bitmap) {
                        BitmapUtils.saveScaledBitmap(bitmap, CACHE_FACE_LOG_DIR, "liveBitmap"); //保存给插件用，原生开发忽略
                        VoicePlayer.getInstance().addPayList(R.raw.verify_success);
                        new ImageToast().show(getApplicationContext(), bitmap, getString(R.string.face_verify_success)); //判断一下
                        finishFaceVerify(10, R.string.liveness_detection_done, livenessValue);
                    }

                    /**
                     * 控制屏幕闪烁哪种颜色的光线，不能在室外强光环境使用
                     */
                    @Override
                    public void onColorFlash(int color) {
                        faceCoverView.setFlashColor(color);
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showFaceVerifyTips(i);
                    }

                    @Override
                    public void onTimeCountDown(float percent) {
                        faceCoverView.setProgress(percent); //动作活体倒计时
                    }

                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed错误!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //防止在识别过程中关闭页面导致Crash
            if (!isDestroyed() && !isFinishing()) {
                faceVerifyUtils.goVerifyWithImageProxy(imageProxy);
                //自定义管理相机可以使用 goVerifyWithBitmap
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishFaceVerify(0, R.string.face_verify_result_cancel);
    }

    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     * 制作自定义声音：https://www.minimax.io/audio/text-to-speech
     */
    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            switch (actionCode) {
                //炫彩活体检测需要人脸更加靠近屏幕摄像头才能通过检测
                case VERIFY_DETECT_TIPS_ENUM.COLOR_FLASH_NEED_CLOSER_CAMERA:
                    setSecondTips(R.string.color_flash_need_closer_camera);
                    break;

                //炫彩活体通过✅
                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_SUCCESS:
                    setMainTips(R.string.keep_face_visible);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_FAILED:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.color_flash_liveness_failed)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(7, R.string.color_flash_liveness_failed);
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
                                    finishFaceVerify(9, R.string.color_flash_light_high);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;

                // 动作活体检测完成了
                case ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_SUCCESS:
                    setMainTips(R.string.keep_face_visible);
                    //如果还配置了炫彩活体，最好语音提前提示靠近屏幕，以便彩色光达到脸上
                    VoicePlayer.getInstance().play(R.raw.closer_to_screen);
                    break;

                // 动作活体检测超时
                case ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_TIMEOUT:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.motion_liveness_detection_time_out)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(3, R.string.face_verify_result_timeout);
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
                    VoicePlayer.getInstance().play(R.raw.open_mouse);
                    setMainTips(R.string.repeat_open_close_mouse);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.SMILE:
                    setMainTips(R.string.motion_smile);
                    VoicePlayer.getInstance().play(R.raw.smile);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.BLINK:
                    VoicePlayer.getInstance().play(R.raw.blink);
                    setMainTips(R.string.motion_blink_eye);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                    VoicePlayer.getInstance().play(R.raw.shake_head);
                    setMainTips(R.string.motion_shake_head);
                    break;

                case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                    VoicePlayer.getInstance().play(R.raw.nod_head);
                    setMainTips(R.string.motion_node_head);
                    break;

                // 人脸识别活体检测过程切换到后台防止作弊
                case VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.face_verify_pause)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(6, R.string.face_verify_result_pause);
                            }).show();
                    break;

                //多次没有人脸，想作弊啊🤔️
                case VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY:
                    setMainTips(R.string.no_face_or_repeat_switch_screen);
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.stop_verify_tips)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(5, R.string.face_verify_result_no_face_multi_time);
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
            }
        }
    }


    /**
     * 主要提示
     */
    private void setMainTips(int resId) {
        tipsTextView.setText(resId);
    }

    /**
     * 第二行提示
     */
    private void setSecondTips(int resId) {
        if (resId == 0) {
            secondTipsTextView.setText("");
            secondTipsTextView.setVisibility(View.INVISIBLE);
        } else {
            secondTipsTextView.setVisibility(View.VISIBLE);
            secondTipsTextView.setText(resId);
        }
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


    // ************************** 下面代码是为了兼容三方插件，原生开放可以忽略   ***********************************

    /**
     * 获取UNI,RN,Flutter三方插件传递的参数,以便在原生代码中生效
     */
    private void getIntentParams() {
        Intent intent = getIntent(); // 获取发送过来的Intent对象
        if (intent != null) {

            if (intent.hasExtra(FACE_LIVENESS_TYPE)) {
                int type = intent.getIntExtra(FACE_LIVENESS_TYPE, 1);
                // 1.动作活体  2.动作+炫彩活体 3.炫彩活体(不能强光环境使用)
                switch (type) {
                    case 0:
                        faceLivenessType = FaceLivenessType.NONE;
                        break;
                    case 1:
                        faceLivenessType = FaceLivenessType.MOTION;
                        break;
                    case 2:
                        faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION;
                        break;
                    case 3:
                        faceLivenessType = FaceLivenessType.COLOR_FLASH;
                        break;
                    default:
                        faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION;
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
        finishFaceVerify(code, msgStrRes, 0f);
    }

    private void finishFaceVerify(int code, int msgStrRes, float livenessValue) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("livenessValue",livenessValue)
                .putExtra("msg", getString(msgStrRes));

        setResult(RESULT_OK, intent);
        finish();
    }

}

