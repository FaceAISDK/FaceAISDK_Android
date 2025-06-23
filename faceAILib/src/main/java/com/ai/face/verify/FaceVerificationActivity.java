package com.ai.face.verify;

import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.R;
import com.ai.face.base.baseImage.FaceAIUtils;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus.*;
import com.ai.face.faceVerify.verify.VerifyUtils;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessType;
import com.ai.face.utils.VoicePlayer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import org.jetbrains.annotations.NotNull;

/**
 * 1：1 的人脸识别 + 动作活体检测 SDK 接入演示Demo 代码. 正式接入集成需要你根据你的业务完善
 * <p>
 * 移动考勤签到、App免密登录、刷脸授权、刷脸解锁。请熟悉Demo主流程后根据你的业务情况再改造
 */
public class FaceVerificationActivity extends AppCompatActivity {
    public static final String USER_FACE_ID_KEY = "USER_FACE_ID_KEY";   //1:1 face verify ID KEY
    private TextView tipsTextView, secondTipsTextView, scoreText;
    private FaceCoverView faceCoverView;
    private ImageView baseFaceImageView;
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private CameraXFragment cameraXFragment;
    private final float silentLivenessPassScore = 0.85f; //静默活体分数通过的阈值
    private String faceID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);//建议背景白色可以补充光照不足
        setTitle("1:1 face verify");
        scoreText = findViewById(R.id.silent_Score);
        tipsTextView = findViewById(R.id.tips_view);
        secondTipsTextView = findViewById(R.id.second_tips_view);
        faceCoverView = findViewById(R.id.face_cover);
        baseFaceImageView = findViewById(R.id.base_face);

        findViewById(R.id.back).setOnClickListener(v -> {
            finishFaceVerify(0,"用户取消");
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt("cameraFlag", 0);
        int degree = sharedPref.getInt("cameraDegree", getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.001f) //焦距范围[0.001f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)   //画面旋转方向
                .setSize(CameraXFragment.SIZE.DEFAULT) //相机的分辨率大小。分辨率越大画面中人像很小也能检测但是会更消耗CPU
                .create();

        cameraXFragment = CameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

        initFaceVerifyBaseBitmap();
    }

    /**
     * 初始化人脸识别底图
     */
    private void initFaceVerifyBaseBitmap() {
        //1:1 人脸对比，摄像头实时采集的人脸和预留的人脸底片对比。（动作活体人脸检测完成后开始1:1比对）
        faceID = getIntent().getStringExtra(USER_FACE_ID_KEY);
        //2.先去Path 路径读取有没有faceID 对应的处理好的人脸，如果没有从网络其他地方同步图片过来并进行合规处理
        String faceFilePath = CACHE_BASE_FACE_DIR + faceID;
        Bitmap baseBitmap = BitmapFactory.decodeFile(faceFilePath);

        //如果本地已经有了合规处理好的人脸图
        if (baseBitmap != null) {
            //3.初始化引擎，各种参数配置
            initFaceVerificationParam(baseBitmap);
        } else {
            Toast.makeText(getBaseContext(), R.string.add_a_face_image, Toast.LENGTH_LONG).show();
            //为了演示方便放这里，实际应该在启动人脸识别前处理好
            if(VerifyUtils.isDebugMode(getBaseContext())){
                //模拟从你的业务服务器获取对应的人脸图，Demo简化从Asset目录读取。请熟悉Demo主流程后根据你的业务情况再改造
                Bitmap remoteBitmap = VerifyUtils.getBitmapFromAssert(this, "0a_模拟证件照.jpeg");
                if (remoteBitmap == null) {
                    Toast.makeText(getBaseContext(), R.string.add_a_face_image, Toast.LENGTH_LONG).show();
                    tipsTextView.setText(R.string.add_a_face_image);
                    return;
                }

                //其他地方同步过来的人脸可能是不规范的没有经过校准的人脸图（证件照，多人脸，过小等）。disposeBaseFaceImage处理
                FaceAIUtils.Companion.getInstance(getApplication())
                        .disposeBaseFaceImage(remoteBitmap, faceFilePath, new FaceAIUtils.Callback() {
                            //处理优化人脸成功完成去初始化引擎
                            @Override
                            public void onSuccess(@NonNull Bitmap disposedBitmap) {
                                initFaceVerificationParam(disposedBitmap);
                            }

                            //底片处理异常的信息回调
                            @Override
                            public void onFailed(@NotNull String msg, int errorCode) {
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }


    /**
     * 初始化认证引擎
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名 签名SHA1和功能简介到 FaceAISDK.Service@gmail.com
     *
     * @param baseBitmap 1:1 人脸识别对比的底片，如果仅仅需要活体检测，可以把App logo Bitmap 当参数传入并忽略对比结果
     */
    private void initFaceVerificationParam(Bitmap baseBitmap) {
        //建议老的低配设备减少活体检测步骤，加长活体检测 人脸对比时间。
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.85f)                    //阈值设置，范围限 [0.8,0.95] 识别可信度，也是识别灵敏度
                .setBaseBitmap(baseBitmap)              //1:1 人脸识别对比的底片，仅仅需要SDK活体检测可以忽略比对结果
                .setCompareDurationTime(3000)           //人脸识别对比时间[3000,5000] 毫秒。相似度很低会持续设置的时间
                .setLivenessType(MotionLivenessType.SILENT)  //活体检测可以有静默活体，动作活体或者组合也可以不需要活体NONE
                .setSilentLivenessThreshold(silentLivenessPassScore)  //静默活体阈值 [0.88,0.98]
                .setLivenessDetectionMode(MotionLivenessMode.FAST) //硬件配置低用FAST动作活体模式，否则用精确模式
                .setMotionLivenessStepSize(2)           //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(12)  //动作活体检测，支持设置超时时间 [9,22] 秒 。API 名字0410 修改
                //.setExceptMotionLivelessType(ALIVE_DETECT_TYPE_ENUM.SMILE) //动作活体去除微笑 或其他某一种
                .setProcessCallBack(new ProcessCallBack() {
                    /**
                     * 1:1 人脸识别 活体检测 对比结束
                     *
                     * @param isMatched   true匹配成功（大于setThreshold）； false 与底片不是同一人
                     * @param similarity  与底片匹配的相似度值
                     * @param vipBitmap   识别完成的时候人脸实时图，金融级别可以再次和自己的服务器二次校验
                     */
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float silentLivenessScore, Bitmap vipBitmap) {
                        showVerifyResult(isMatched, similarity, silentLivenessScore);
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

        Glide.with(getBaseContext())
                .load(baseBitmap)
                .transform(new RoundedCorners(12))
                .into(baseFaceImageView);
    }

    /**
     * 检测1:1 人脸识别是否通过
     * <p>
     * 动作活体要有动作配合，必须先动作匹配通过再1：1 匹配
     * 静默活体不需要人配合，如果不需要静默活体检测，分数直接会被赋值 1.0
     */
    private void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore) {
        runOnUiThread(() -> {
            scoreText.setText("SilentLive:" + silentLivenessScore);
            //1.静默活体分数判断
            if (silentLivenessScore < silentLivenessPassScore) {
                tipsTextView.setText(R.string.silent_anti_spoofing_error);
                new AlertDialog.Builder(FaceVerificationActivity.this)
                        .setMessage(R.string.silent_anti_spoofing_error)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            finishFaceVerify(2,"活体分数过低，请重试");
                        })
                        .show();
            } else if (isVerifyMatched) {
                //2.和底片同一人
                tipsTextView.setText("Success:  " + similarity);
                VoicePlayer.getInstance().addPayList(R.raw.verify_success);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    finishFaceVerify(1,"人脸识别成功");
                }, 2000);
            } else {
                //3.和底片不是同一个人
                tipsTextView.setText("Failed: " + similarity);
                VoicePlayer.getInstance().addPayList(R.raw.verify_failed);
                new AlertDialog.Builder(FaceVerificationActivity.this)
                        .setTitle("识别失败，相似度 " + similarity)
                        .setMessage(R.string.face_verify_failed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            finishFaceVerify(4,"人脸识别相似度低于阈值");
                        })
                        .setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify())
                        .show();
            }
        });
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
     * 识别结束返回结果, 为了给uniApp UTS插件统一的交互返回格式
     *
     * @param code
     * @param msg
     */
    private void finishFaceVerify(int code,String msg) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", msg);
        setResult(RESULT_OK, intent);
        finish();
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
    protected void onPause() {
        super.onPause();
        faceVerifyUtils.pauseProcess();
    }
}

