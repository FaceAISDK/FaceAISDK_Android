package com.faceAI.demo.SysCamera.verify;

import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_FACE_LOG_DIR;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.MyCameraXFragment;
import com.faceAI.demo.SysCamera.search.ImageToast;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.base.view.DemoFaceCoverView;

/**
 * 活体检测 SDK 接入演示Demo 代码.
 * 使用系统相机怎么活体检测，包含动作活体，静默活体（静默需要摄像头成像清晰，宽动态大于105Db）
 *
 * 摄像头管理源码开放了 {@link MyCameraXFragment}
 * @author FaceAISDK.Service@gmail.com
 */
public class LivenessDetectActivity extends AbsBaseActivity {
    private TextView tipsTextView, secondTipsTextView, scoreText;
    private DemoFaceCoverView faceCoverView;
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();

    private MyCameraXFragment cameraXFragment;

    public static final String SILENT_THRESHOLD_KEY = "SILENT_THRESHOLD_KEY";   //RGB 静默活体KEY
    public static final String FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE";   //活体检测的类型
    public static final String MOTION_STEP_SIZE = "MOTION_STEP_SIZE";   //动作活体的步骤数
    public static final String MOTION_TIMEOUT = "MOTION_TIMEOUT";   //动作活体超时数据

    private FaceLivenessType faceLivenessType = FaceLivenessType.SILENT_MOTION;//活体检测类型
    private float silentLivenessThreshold = 0.85f; //静默活体分数通过的阈值,摄像头成像能力弱的自行调低
    private int motionStepSize = 1; //动作活体的个数
    private int motionTimeOut = 10; //动作超时秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveness_detection);//建议背景白色可以补充光照不足
        setTitle(R.string.liveness_detection);
        scoreText = findViewById(R.id.silent_Score);
        tipsTextView = findViewById(R.id.tips_view);
        secondTipsTextView = findViewById(R.id.second_tips_view);
        faceCoverView = findViewById(R.id.face_cover);
        findViewById(R.id.back).setOnClickListener(v -> finishFaceVerify(0,"用户取消"));

        getIntentParams(); //接收三方插件的参数 数据

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.0001f)    //焦距范围[0f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)      //画面旋转方向
                .create();

        cameraXFragment = MyCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

        initFaceVerificationParam();
    }


    /**
     * 初始化认证引擎
     */
    private void initFaceVerificationParam() {
        //建议老的低配设备减少活体检测步骤
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(LivenessDetectActivity.this)
                .setLivenessOnly(true)
                .setLivenessType(faceLivenessType) //活体检测可以静默&动作活体组合，静默活体效果和摄像头成像能力有关(宽动态>105Db)
                .setSilentLivenessThreshold(silentLivenessThreshold)  //静默活体阈值 [0.88,0.98]
                .setMotionLivenessStepSize(motionStepSize)           //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)           //动作活体检测，支持设置超时时间 [9,22] 秒 。API 名字0410 修改
                .setLivenessDetectionMode(MotionLivenessMode.FAST) //硬件配置低用FAST动作活体模式，否则用精确模式
//                .setExceptMotionLivenessType(ALIVE_DETECT_TYPE_ENUM.SMILE) //动作活体去除微笑 或其他某一种
                .setProcessCallBack(new ProcessCallBack() {

                    /**
                     * 活体检测完成，动作活体没有超时（如有），静默活体设置了需要（不需要返回00）
                     *
                     * @param silentLivenessValue
                     * @param bitmap
                     */
                    @Override
                    public void onLivenessDetected(float silentLivenessValue, Bitmap bitmap) {
                        BitmapUtils.saveBitmap(bitmap,CACHE_FACE_LOG_DIR,"liveBitmap"); //给插件用
                        if(FaceSDKConfig.isDebugMode(getBaseContext())){
                            runOnUiThread(() -> {
                                scoreText.setText("RGB Live:"+silentLivenessValue);
                                new ImageToast().show(getApplicationContext(), bitmap, "活体检测完成");
                                new AlertDialog.Builder(LivenessDetectActivity.this)
                                        .setTitle("Debug模式提示")
                                        .setMessage("活体检测完成，其中RGB Live分数="+silentLivenessValue)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                            finishFaceVerify(9,"活体检测完成",silentLivenessValue);
                                        })
                                        .setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify())
                                        .show();
                            });
                        }else{
                            finishFaceVerify(9,"活体检测完成",silentLivenessValue);
                        }

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
                        faceCoverView.startCountDown(percent);
                    }

                    //发送严重错误，会中断业务流程
                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed错误!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //防止在识别过程中关闭页面导致Crash
            if (!isDestroyed() && !isFinishing()) {
                //2.第二个参数是指圆形人脸框到屏幕边距，可加快裁剪图像和指定识别区域，设太大会裁剪掉人脸区域
                faceVerifyUtils.goVerifyWithImageProxy(imageProxy, faceCoverView.getMargin());
                //自定义管理相机可以使用 goVerifyWithBitmap
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishFaceVerify(0,"用户取消");
    }

    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    int retryTime = 0;

    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(() -> {
                switch (actionCode) {
                    // 动作活体检测完成了
                    case ALIVE_DETECT_TYPE_ENUM.ALIVE_CHECK_DONE:
                        VoicePlayer.getInstance().play(R.raw.face_camera);
                        tipsTextView.setText(R.string.keep_face_visible);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        tipsTextView.setText(R.string.face_verifying);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        tipsTextView.setText(R.string.motion_liveness_detection_failed);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        tipsTextView.setText(R.string.repeat_open_close_mouse);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        tipsTextView.setText(R.string.motion_smile);
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        tipsTextView.setText(R.string.motion_blink_eye);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        tipsTextView.setText(R.string.motion_shake_head);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        tipsTextView.setText(R.string.motion_node_head);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.face_verify_pause)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    finishFaceVerify(6,"活体检测中断");
                                })
                                .show();
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.motion_liveness_detection_time_out)
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                            retryTime++;
                                            if (retryTime > 1) {
                                                finishFaceVerify(3,"活体检测超时");
                                            } else {
                                                faceVerifyUtils.retryVerify();
                                            }
                                        }
                                ).show();
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY:
                        tipsTextView.setText(R.string.no_face_or_repeat_switch_screen);
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.stop_verify_tips)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    finishFaceVerify(5,"多次检测无人脸");
                                })
                                .show();
                        break;

                    // 单独使用一个textview 提示，防止上一个提示被覆盖。
                    // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
                    case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                        secondTipsTextView.setText(R.string.far_away_tips);
                        break;

                    //人脸太小了，靠近一点摄像头
                    case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL:
                        secondTipsTextView.setText(R.string.come_closer_tips);
                        break;

                    //检测到正常的人脸，尺寸大小OK
                    case VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT:
                        secondTipsTextView.setText("");
                        break;
                }
            });
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
            if (intent.hasExtra(SILENT_THRESHOLD_KEY)) {
                silentLivenessThreshold = intent.getFloatExtra(SILENT_THRESHOLD_KEY, 0.85f);
            }
            if (intent.hasExtra(FACE_LIVENESS_TYPE)) {
                int type = intent.getIntExtra(FACE_LIVENESS_TYPE, 3);
                switch (type) {
                    case 0:
                        faceLivenessType = FaceLivenessType.NONE;
                        break;
                    case 1:
                        faceLivenessType = FaceLivenessType.SILENT;
                        break;
                    case 2:
                        faceLivenessType = FaceLivenessType.MOTION;
                        break;
                    default:
                        faceLivenessType = FaceLivenessType.SILENT_MOTION;
                }
            }

            if (intent.hasExtra(MOTION_STEP_SIZE)) {
                motionStepSize = intent.getIntExtra(MOTION_STEP_SIZE, 2);
            }
            if (intent.hasExtra(SILENT_THRESHOLD_KEY)) {
                motionTimeOut = intent.getIntExtra(MOTION_TIMEOUT, 10);
            }
        } else {
            // 数据不存在，执行其他操作
        }
    }


    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     *
     * @param code
     * @param msg
     */
    private void finishFaceVerify(int code, String msg) {
        finishFaceVerify(code,msg,0f);
    }

    private void finishFaceVerify(int code, String msg,float silentLivenessScore) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("msg", msg)
                .putExtra("silentLivenessScore", silentLivenessScore);
        setResult(RESULT_OK, intent);
        finish();
    }
}

