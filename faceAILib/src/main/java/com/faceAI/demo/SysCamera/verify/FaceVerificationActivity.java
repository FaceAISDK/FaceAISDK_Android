package com.faceAI.demo.SysCamera.verify;

import static com.faceAI.demo.FaceSDKConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_FACE_LOG_DIR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;

import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.baseImage.FaceAIUtils;
import com.ai.face.base.baseImage.FaceEmbedding;
import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.search.ImageToast;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.SysCamera.camera.MyCameraXFragment;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.view.DemoFaceCoverView;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus.*;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

/**
 * 1：1 的人脸识别 + 动作活体检测 接入演示D代码。正式接入集成需要你根据你的业务完善
 * 仅仅需要活体检测参考{@link LivenessDetectActivity}
 * <p>
 * 移动考勤签到、App免密登录、刷脸授权、刷脸解锁。请熟悉Demo主流程后根据你的业务情况再改造
 * 摄像头管理源码开放了 {@link MyCameraXFragment}
 * More：<a href="https://github.com/FaceAISDK/FaceAISDK_Android">人脸识别FaceAISDK</a>
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceVerificationActivity extends AbsBaseActivity {
    public static final String USER_FACE_ID_KEY = "USER_FACE_ID_KEY";   //1:1 face verify ID KEY
    public static final String THRESHOLD_KEY = "THRESHOLD_KEY";           //人脸识别通过的阈值
    public static final String SILENT_THRESHOLD_KEY = "SILENT_THRESHOLD_KEY";   //RGB 静默活体KEY
    public static final String FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE";   //活体检测的类型
    public static final String MOTION_STEP_SIZE = "MOTION_STEP_SIZE";   //动作活体的步骤数
    public static final String MOTION_TIMEOUT = "MOTION_TIMEOUT";   //动作活体超时数据
    public static final String MOTION_LIVENESS_TYPES = "MOTION_LIVENESS_TYPES"; //动作活体种类

    private String faceID; //你的业务系统中可以唯一定义一个账户的ID，手机号/身份证号等
    private float verifyThreshold = 0.85f; //1:1 人脸识别对比通过的阈值
    private float silentLivenessThreshold = 0.85f; //静默活体分数通过的阈值,摄像头成像能力弱的自行调低
    private int motionStepSize = 2; //动作活体的个数
    private int motionTimeOut = 7; //动作超时秒
    private String motionLivenessTypes ="1,2,3,4,5" ; //动作活体种类用英文","隔开。 1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头
    private FaceLivenessType faceLivenessType = FaceLivenessType.SILENT_MOTION;//活体检测类型
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private TextView tipsTextView, secondTipsTextView;
    private DemoFaceCoverView faceCoverView;
    private MyCameraXFragment cameraXFragment;  //摄像头管理源码，可自行管理摄像头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification); //建议背景白色可以补充光照不足
        tipsTextView = findViewById(R.id.tips_view);
        secondTipsTextView = findViewById(R.id.second_tips_view); //次要提示
        faceCoverView = findViewById(R.id.face_cover);
        findViewById(R.id.back).setOnClickListener(v -> finishFaceVerify(0, R.string.face_verify_result_cancel));

        getIntentParams(); //接收三方插件传递的参数，原生开发可以忽略裁剪掉

        initCameraX();
        initFaceVerifyEmbedding();
    }

    /**
     * 初始化摄像头
     */
    private void initCameraX() {
        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT);
        int degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.001f)    //焦距范围[0f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)       //画面旋转角度
                .create();

        cameraXFragment = MyCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment).commit();
    }


    /**
     * 初始化人脸识别底图 人脸特征向量
     */
    private void initFaceVerifyEmbedding() {
        //1:1 人脸对比，摄像头实时采集的人脸和预留的人脸底片对比
        float[] faceEmbedding = FaceEmbedding.loadEmbedding(getBaseContext(), faceID);
        // 去Path 路径读取有没有faceID 对应的处理好的人脸Bitmap
        String faceFilePath = CACHE_BASE_FACE_DIR + faceID;
        Bitmap baseBitmap = BitmapFactory.decodeFile(faceFilePath);

        //本地没有faceID对应的人脸特征向量 （ 本段代码仅供演示，你需要根据自身业务完善流程）
        if (faceEmbedding.length == 0) {
            disposeBaseBitmapGetEmbedding(baseBitmap);
        } else {
            initFaceVerificationParam(faceEmbedding);
        }

        Glide.with(getBaseContext()).load(baseBitmap)
                .transform(new RoundedCorners(44))
                .into((ImageView) findViewById(R.id.base_face));
    }


    /**
     * 初始化认证引擎，仅仅需要活体检测参考{@link LivenessDetectActivity}
     *
     * @param faceEmbedding 1:1 人脸识别对比的底片特征向量
     */
    private void initFaceVerificationParam(float[] faceEmbedding) {
        //建议老的低配设备减少活体检测步骤，加长活体检测 人脸对比时间。
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(verifyThreshold)          //阈值设置，范围限 [0.75,0.95] ,低配摄像头可适量放低，默认0.85
                .setFaceEmbedding(faceEmbedding)        //1:1 人脸识别对比的底片人脸特征向量，以前是传bitmap
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)  //相机类型，目前分为3种
                .setCompareDurationTime(3500)           //人脸识别对比时间[3000,5000] 毫秒。相似度低会持续识别比对的时间
                .setLivenessType(faceLivenessType)      //活体检测可以静默&动作活体组合，静默活体效果和摄像头成像能力有关(宽动态>105Db)
                .setSilentLivenessThreshold(silentLivenessThreshold)  //静默活体阈值 [0.66,0.98]
                .setLivenessDetectionMode(MotionLivenessMode.FAST)    //硬件配置低或不需太严格用FAST快速模式，否则用精确模式
                .setMotionLivenessStepSize(motionStepSize)            //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)              //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
                .setMotionLivenessTypes(motionLivenessTypes)                  //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
                .setStopVerifyNoFaceRealTime(true)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
                .setProcessCallBack(new ProcessCallBack() {
                    /**
                     * 1:1 人脸识别 活体检测 对比结束
                     *
                     * @param isMatched   true匹配成功（大于setThreshold）； false 与底片不是同一人
                     * @param similarity  与底片匹配的相似度值
                     * @param silentLivenessScore  静默活体分数，根据你的摄像头设定合理值,需要摄像头成像清晰支持宽动态
                     * @param bitmap      识别完成的时候人脸实时图，金融级别应用可以再次和自己的服务器二次校验
                     */
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float silentLivenessScore, Bitmap bitmap) {
                        showVerifyResult(isMatched, similarity, silentLivenessScore, bitmap);
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showFaceVerifyTips(i);
                    }

                    @Override
                    public void onTimeCountDown(float percent) {
                        faceCoverView.startCountDown(percent);
                    }

                    //发送严重错误，会中断业务流程
                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed error!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //防止在识别过程中关闭页面导致Crash
            if (!isDestroyed() && !isFinishing()) {
                //2.第二个参数是指圆形人脸框到屏幕边距，可加快裁剪图像和指定识别区域，设太大会裁剪掉人脸区域
                faceVerifyUtils.goVerifyWithImageProxy(imageProxy, faceCoverView.getMargin());
                //false 代表不左右翻转图
                //faceVerifyUtils.goVerifyWithBitmap(DataConvertUtils.imageProxy2Bitmap(imageProxy,faceCoverView.getMargin(),false));
            }
        });
    }

    /**
     * 1:1 人脸识别是否通过
     * <p>
     * 动作活体要有动作配合，必须先动作匹配通过再1：1 匹配
     * 静默活体不需要人配合，如果不需要静默活体检测，分数直接会被赋值 1.0
     */
    private int retryTime = 0;

    private void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore, Bitmap bitmap) {
        //切换到主线程操作UI
        runOnUiThread(() -> {
            BitmapUtils.saveBitmap(bitmap, CACHE_FACE_LOG_DIR, "verifyBitmap");//保存场景图给三方插件使用

            //1.RGB静默活体分数判断，根据你的摄像头设定合理值,需要摄像头成像清晰支持宽动态
            if (silentLivenessScore < silentLivenessThreshold) {
                tipsTextView.setText(R.string.silent_anti_spoofing_error);
                new AlertDialog.Builder(FaceVerificationActivity.this).setMessage(R.string.silent_anti_spoofing_error).setCancelable(false).setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                    finishFaceVerify(2, R.string.face_verify_result_silent_failed, silentLivenessScore, similarity);
                }).show();
            } else if (isVerifyMatched) {
                //2.和底片同一人
                VoicePlayer.getInstance().addPayList(R.raw.verify_success);
                new ImageToast().show(getApplicationContext(), bitmap, "Success " + similarity);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    finishFaceVerify(1, R.string.face_verify_result_success, silentLivenessScore, similarity);
                }, 1500);
            } else {
                //3.和底片不是同一个人
                VoicePlayer.getInstance().addPayList(R.raw.verify_failed);
                new AlertDialog.Builder(FaceVerificationActivity.this).setTitle(R.string.face_verify_failed_title).setMessage(R.string.face_verify_failed).setCancelable(false).setPositiveButton(R.string.know, (dialogInterface, i) -> {
                    finishFaceVerify(4, R.string.face_verify_result_failed, silentLivenessScore, similarity);
                }).setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify()).show();
            }
        });
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(() -> {
                switch (actionCode) {
                    // 动作活体检测完成了
                    case ALIVE_DETECT_TYPE_ENUM.ALIVE_CHECK_DONE:
                        VoicePlayer.getInstance().play(R.raw.face_camera);
                        setMainTips(R.string.keep_face_visible);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        setMainTips(R.string.face_verifying);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        setMainTips(R.string.motion_liveness_detection_failed);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        setMainTips(R.string.repeat_open_close_mouse);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        setMainTips(R.string.motion_smile);
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        setMainTips(R.string.motion_blink_eye);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        setMainTips(R.string.motion_shake_head);
                        break;

                    case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        setMainTips(R.string.motion_node_head);
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT:
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
                    case VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY:
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.face_verify_pause)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    finishFaceVerify(6, R.string.face_verify_result_pause);
                                }).show();
                        break;

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

                    //人脸太小了，靠近一点摄像头
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
            });
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
     * 退出页面，释放资源
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishFaceVerify(0, R.string.face_verify_result_cancel);
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

    /**
     * 处理人脸底片，获取人脸特征向量
     *
     * @param baseBitmap
     */
    private void disposeBaseBitmapGetEmbedding(Bitmap baseBitmap) {
        //需要你把以前版本的人脸bitmap 转变为faceEmbedding
        if (baseBitmap != null) {
            //如果是经过FaceAISDK 裁剪处理过的人脸处理更简单
            float[] embedding = new BaseImageDispose().saveBaseImageGetEmbedding(baseBitmap, CACHE_BASE_FACE_DIR, faceID);
            FaceEmbedding.saveEmbedding(getBaseContext(), faceID, embedding);  //本地保存起来
            initFaceVerificationParam(embedding);

            //非FaceAI SDK录入处理的人脸可能不规范的没有经过矫正裁剪需要合规化处理
            FaceAIUtils.Companion.getInstance(getApplication()).disposeBaseFaceImage(getBaseContext(), baseBitmap, new FaceAIUtils.Callback() {
                @Override
                public void onSuccess(@NonNull Bitmap disposedBitmap, @NonNull float[] faceEmbedding) {
                    FaceEmbedding.saveEmbedding(getBaseContext(), faceID, faceEmbedding);  //本地保存起来
                    initFaceVerificationParam(faceEmbedding);
                }

                //底片处理异常的信息回调
                @Override
                public void onFailed(@NonNull String msg, int errorCode) {
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                }
            });

        } else {
            //你的业务代码，从你的服务器拿到对应的人脸特征向量，或提示录入人脸并同步数据到你的服务器，SDK不存储敏感数据
            Toast.makeText(getBaseContext(), R.string.no_face_image_found, Toast.LENGTH_LONG).show();
        }
    }


    // ************************** 下面代码是为了兼容三方插件，原生开放可以忽略   ***********************************

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
            if (intent.hasExtra(MOTION_LIVENESS_TYPES)) {
                motionLivenessTypes = intent.getStringExtra(MOTION_LIVENESS_TYPES);
            }
        }
    }


    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private void finishFaceVerify(int code, int msgStrRes) {
        finishFaceVerify(code, msgStrRes, 0f, 0f);
    }


    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private void finishFaceVerify(int code, int msgStrRes, float silentLivenessScore, float similarity) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", getString(msgStrRes))
                .putExtra("similarity", similarity)
                .putExtra("silentLivenessScore", silentLivenessScore);
        setResult(RESULT_OK, intent);
        finish();
    }

}

