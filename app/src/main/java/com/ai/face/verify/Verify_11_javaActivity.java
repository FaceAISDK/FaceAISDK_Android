
package com.ai.face.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ai.face.FaceApplication;
import com.ai.face.R;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.faceVerify.graphic.FaceTipsOverlay;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus.*;
import com.ai.face.faceVerify.verify.alive.LivenessDetection;
import com.ai.face.utils.VoicePlayer;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;


/**
 * 1：1 的人脸识别 + 动作活体检测 SDK 接入演示Demo 代码，用户根据自己业务情况参考
 * <p>
 * <p>
 * 1：N & M：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 * 体积更小，反应速度更快
 */
public class Verify_11_javaActivity extends AppCompatActivity {
    private ConstraintLayout rootView;
    private TextView tipsTextView, scoreText;
    private FaceTipsOverlay faceTipsOverlay;
    private FaceCoverView faceCoverView;

    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();

    //RGB 镜头 1080p， 固定 30 帧，无拖影，RGB 镜头建议是宽动态
    private static final float silentPassScore = 0.92f; //静默活体分数通过的阈值
    private float silentScoreValue = 0f; //静默活体的分数
    //字段拆分出来，照顾Free 用户
    private final boolean livenessCheck =true; //是否需要活体检测，不需要的话最终识别结果不用考虑静默活体分数
    private Boolean isVerifyMatched = null; //先取一个中性的null, 真实只有true 和 false


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_11);
        setTitle("1:1人脸识别with 活体检测");
        rootView=findViewById(R.id.rootView);
        scoreText = findViewById(R.id.silent_Score);
        tipsTextView = findViewById(R.id.tips_view);
        faceCoverView = findViewById(R.id.face_cover);
        faceTipsOverlay = findViewById(R.id.faceTips);
        findViewById(R.id.back).setOnClickListener(v -> {
            Verify_11_javaActivity.this.finish();
        });


        int cameraLensFacing = getSharedPreferences("faceVerify", Context.MODE_PRIVATE)
                .getInt("cameraFlag", 0);

        // 1. Camera 的初始化。第一个参数0/1 指定前后摄像头；
        // 第二个参数linearZoom [0.1f,1.0f] 指定焦距，默认0.1。根据你的设备和场景选择合适的值
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLensFacing, 0.09f);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();


        //1:1 人脸对比，摄像头和预留的人脸底片对比。（动作活体人脸检测完成后开始1:1比对）
        //如果仅仅需要活体检测，可以把App logo Bitmap 当参数传入并忽略对比结果
        //人脸底图要经过BaseImageDispose saveBaseImage处理，不是随便一张图能当底图！！！
        String yourUniQueFaceId = getIntent().getStringExtra(FaceApplication.USER_ID_KEY);

        File file = new File(FaceApplication.CACHE_BASE_FACE_DIR, yourUniQueFaceId);
        Bitmap baseBitmap = BitmapFactory.decodeFile(file.getPath());

        //1.初始化引擎，各种参数配置
        initFaceVerify(baseBitmap);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //防止在识别过程中关闭页面导致Crash
            if (!isDestroyed() && !isFinishing() && faceVerifyUtils != null) {
                //第二个参数是指圆形人脸框到屏幕边距，可加快裁剪图像和指定识别区域，设太大会裁剪点人脸区域
                faceVerifyUtils.goVerify(imageProxy, faceCoverView.getMargin());
            }
        });
    }


    /**
     * 初始化认证引擎
     * <p>
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名和功能简介到 anylife.zlb@gmail.com
     *
     * @param baseBitmap 1:1 人脸识别对比的底片，如果仅仅需要活体检测，可以把App logo Bitmap 当参数传入并忽略对比结果
     */
    private void initFaceVerify(Bitmap baseBitmap) {

        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.88f)                  //阈值设置，范围限 [0.8 , 0.95] 识别可信度，也是识别灵敏度
                .setBaseBitmap(baseBitmap)            //1:1 人脸识别对比的底片，仅仅需要SDK活体检测可以忽略比对结果
                .setLivenessDetection(livenessCheck)  //是否需要活体检测（包含动作和静默）,开通需要发送邮件，参考ReadMe
                .setLivenessStepSize(1)               //随机动作验证活体的步骤个数[0-2]，0个没有动作活体只有静默活体
                .setLivenessDetectionMode(LivenessDetection.FAST)  //硬件配置低用FAST动作活体模式，否则用精确模式
                .setSilentLivenessThreshold(0.91f)    //静默活体阈值 [0.88,0.99]
                .setVerifyTimeOut(12)                 //活体检测支持设置超时时间 [9,16] 秒
                .setGraphicOverlay(faceTipsOverlay)//正式环境请去除设置
                .setProcessCallBack(new ProcessCallBack() {
                    //静默活体检测得分大于0.9 可以认为是真人，可结合动作活体一起使用
                    @Override
                    public void onSilentAntiSpoofing(float scoreValue) {
                        runOnUiThread(() -> {
                            scoreText.setText("静默活体可靠得分：" + scoreValue);
                        });

                        silentScoreValue = scoreValue;
                        playVerifyResult();
                    }


                    //onCompleted --- rename --->  onVerifyMatched
                    @Override
                    public void onVerifyMatched(boolean isMatched) {
                        isVerifyMatched = isMatched;
                        playVerifyResult();
                    }


                    @Override
                    public void onFailed(int i) {
                        //预留防护
                    }

                    //人脸识别活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showAliveDetectTips(i);
                    }

                    //动作活体检测时间限制倒计时
                    @Override
                    public void onTimeOutStart(float time) {
                        faceCoverView.startCountDown(time);
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
    }

    /**
     * 检测1:1 人脸识别是否通过
     * 动作活体要有人配合必须，必须先动作再1：1 匹配
     * 静默活体不需要人配合可以和1:1 同时进行但要注意不确定谁先返回的问题
     */
    private void playVerifyResult() {

        //不需要活体检测，忽略分数,下版本放到SDK 内部处理
        if(!livenessCheck){
            silentScoreValue=1.0f;
        }

        //1:1 人脸识别对比的结果，是同一个人还是非同一个人
        runOnUiThread(() -> {

            if (isVerifyMatched == null || silentScoreValue == 0f) {
                //必须要两个值都有才能判断
                Log.d("playVerifyResult", "等待状态 D silentScoreValue=" + silentScoreValue + " isVerifyMatched=" + isVerifyMatched);
            } else {

                if (silentScoreValue > silentPassScore && isVerifyMatched) {
                    tipsTextView.setText("核验已通过，与底片为同一人！ ");
                    VoicePlayer.getInstance().addPayList(R.raw.verify_success);

                    //关闭页面时间业务自己根据实际情况定
                    new Handler(Looper.getMainLooper()).postDelayed(Verify_11_javaActivity.this::finish, 1500);
                } else {
                    if (!isVerifyMatched) {
                        tipsTextView.setText("核验不通过，与底片不符！ ");
                        VoicePlayer.getInstance().addPayList(R.raw.verify_failed);

                        new AlertDialog.Builder(Verify_11_javaActivity.this)
                                .setMessage("1:1 人脸识别不通过，与底片不符！ ")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> finish())
                                .setNegativeButton("重试", (dialog, which) -> faceVerifyUtils.retryVerify())
                                .show();
                    } else {
                        tipsTextView.setText("静默活体得分过低");
                        new AlertDialog.Builder(Verify_11_javaActivity.this)
                                .setMessage("静默活体得分过低！ ")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> finish())
                                .show();
                    }
                }
            }
        });
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    boolean isSnakeBarShow=false;
    private void showAliveDetectTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(() -> {
                switch (actionCode) {


                    //5次相比阈值太低就判断为非同一人
                    case VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        tipsTextView.setText("人脸识别中...");
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        tipsTextView.setText("画面没有检测到人脸");
                        break;


                    case VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        tipsTextView.setText("动作活体检测失败了");
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_OK: {
                        VoicePlayer.getInstance().addPayList(R.raw.face_camera);
                        tipsTextView.setText("请保持正对屏幕");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE: {
                        VoicePlayer.getInstance().addPayList(R.raw.open_mouse);
                        tipsTextView.setText("请张嘴");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        tipsTextView.setText("请微笑");
                        VoicePlayer.getInstance().addPayList(R.raw.smile);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().addPayList(R.raw.blink);
                        tipsTextView.setText("请轻眨眼");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD: {
                        VoicePlayer.getInstance().addPayList(R.raw.shake_head);
                        tipsTextView.setText("请缓慢左右摇头");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD: {
                        VoicePlayer.getInstance().addPayList(R.raw.nod_head);
                        tipsTextView.setText("请缓慢上下点头");
                    }
                    break;


                    case VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT: {
                        new AlertDialog.Builder(this)
                                .setMessage("活体检测超时了")
                                .setCancelable(false)
                                .setPositiveButton("重试一次", (dialogInterface, i) -> faceVerifyUtils.retryVerify()
                                ).show();
                    }

                    break;

                    case VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY: {
                        tipsTextView.setText("多次切换画面或无人脸");
                        new AlertDialog.Builder(this)
                                .setMessage("多次切换画面或无人脸，停止识别。\n识别过程请保持人脸在画面中")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> finish())
                                .show();
                    }
                    break;

                    //请远一点,交互业务自行实现仅供参考
                    case VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                        if(isSnakeBarShow) return;
                        Snackbar.make(rootView, "请离屏幕远一点!", Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback(){
                            @Override
                            public void onShown(Snackbar sb) {
                                super.onShown(sb);
                                isSnakeBarShow=true;
                            }

                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);
                                isSnakeBarShow=false;
                            }
                        }).show();

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
        faceCoverView.destroyView();
    }


    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     */
    protected void onPause() {
        super.onPause();
        faceVerifyUtils.pauseProcess();
    }


}

