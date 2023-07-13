package com.ai.face.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.FaceApplication;
import com.ai.face.R;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.faceVerify.graphic.FaceTipsOverlay;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus.*;
import com.ai.face.utils.VoicePlayer;
import java.io.File;


/**
 * 1：1 的人脸识别 + 动作活体检测 SDK JAVA  Demo*
 * <p>
 * 1：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 */
public class Verify_11_javaActivity extends AppCompatActivity {
    private TextView tipsTextView, scoreText;
    private FaceTipsOverlay faceTipsOverlay;
    private FaceCoverView faceCoverView;
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_verify_11);  //1:n 对比

        setTitle("1:1 人脸识别");

        String yourUniQueFaceId = getIntent().getStringExtra(FaceApplication.USER_ID_KEY);

        scoreText = findViewById(R.id.silent_Score);
        tipsTextView = findViewById(R.id.tips_view);
        faceCoverView = findViewById(R.id.face_cover);
        faceTipsOverlay = findViewById(R.id.faceTips);

        // 0 ,前置摄像头       1，后置摄像头    部分外接USB摄像头支持可能是1
        int cameraLensFacing = getSharedPreferences("faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag", 0);

        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLensFacing);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();


        //要对比的人脸底片，业务方自行处理
        File file = new File(FaceApplication.CACHE_BASE_FACE_DIR, yourUniQueFaceId);

        //预留的底图，缓存在本地私有目录
        Bitmap baseBitmap = BitmapFactory.decodeFile(file.getPath());

        //1.初始化引擎，各种参数配置
        initFaceVerify(baseBitmap);


        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (faceVerifyUtils != null)
                faceVerifyUtils.goVerify(imageProxy, faceCoverView.getMargin());
        });
    }


    /**
     * 初始化认证引擎
     * <p>
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名和功能简介到 anylife.zlb@gmail.com
     *
     * @param baseBitmap 底片
     */
    private void initFaceVerify(Bitmap baseBitmap) {

        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.81f)       //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap) //底片,请录入的时候保证底片质量
                .setLiveCheck(true)        //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setVerifyTimeOut(16)      //活体检测支持设置超时时间 9-16 秒
                .setMotionStepSize(1)      //随机动作验证活体的步骤个数，支持1-2个步骤
                .setGraphicOverlay(faceTipsOverlay)//正式环境请去除设置
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onFailed(int i) {

                    }

                    //静默活体检测得分大于0.85 可以认为是真人
                    @Override
                    public void onSilentAntiSpoofing(float scoreValue) {
                        runOnUiThread(() -> {
                            scoreText.setText("静默活体可靠系数：" + scoreValue);
                        });
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showAliveDetectTips(i);
                    }

                    @Override
                    public void onTimeOutStart(float time) {
                        faceCoverView.startCountDown(time);
                    }

                    @Override
                    public void onCompleted(boolean isMatched) {
                        runOnUiThread(() -> {
                            if (isMatched) {
                                //各种形式的提示，根据业务需求选择
                                tipsTextView.setText("核验已通过，与底片为同一人！ ");

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Verify_11_javaActivity.this.finish();
                                    }
                                }, 1000);

                                VoicePlayer.getInstance().play(R.raw.verify_success);

                            } else {
                                tipsTextView.setText("核验不通过，与底片不符！ ");

                                new AlertDialog.Builder(Verify_11_javaActivity.this)
                                        .setMessage("核验不通过，与底片不符！ ")
                                        .setCancelable(false)
                                        .setPositiveButton("知道了", (dialogInterface, i) -> finish())
                                        .show();

                                VoicePlayer.getInstance().play(R.raw.verify_failed);
                            }
                        });
                    }

                }).create();


        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    private void showAliveDetectTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(() -> {
                switch (actionCode) {
                    case VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT: {
                        new AlertDialog.Builder(this)
                                .setMessage("检测超时了")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> faceVerifyUtils.retryVerify()
                                ).show();
                    }

                    break;

                    case VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY: {
                        tipsTextView.setText("多次切换画面或无人脸");
//                        faceCoverView.setTipText("多次切换画面或无人脸");

                        new AlertDialog.Builder(this)
                                .setMessage("多次切换画面或无人脸，停止识别。\n识别过程请保持人脸在画面中")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> finish())
                                .show();
                    }
                    break;

                    //5次相比阈值太低就判断为非同一人
                    case VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        tipsTextView.setText("人脸比对中...");
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        tipsTextView.setText("画面没有检测到人脸");
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        tipsTextView.setText(" 活体检测失败了");
                        break;

                    case VERIFY_DETECT_TIPS_ENUM.ACTION_OK: {
                        VoicePlayer.getInstance().play(R.raw.face_camera);
                        tipsTextView.setText("请保持正对屏幕");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE: {
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        tipsTextView.setText("请张嘴");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        tipsTextView.setText("请微笑");
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        tipsTextView.setText("请轻眨眼");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD: {
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        tipsTextView.setText("请缓慢左右摇头");
                    }
                    break;

                    case ALIVE_DETECT_TYPE_ENUM.NOD_HEAD: {
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        tipsTextView.setText("请缓慢上下点头");
                    }
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

}

