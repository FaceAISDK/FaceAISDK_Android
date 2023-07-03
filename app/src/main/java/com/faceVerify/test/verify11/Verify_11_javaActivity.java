package com.faceVerify.test.verify11;

import static com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL;
import com.AI.FaceVerify.verify.VerifyStatus.*;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import com.AI.FaceVerify.graphic.FaceTipsOverlay;
import com.AI.FaceVerify.utils.AiUtil;
import com.AI.FaceVerify.verify.FaceProcessBuilder;
import com.AI.FaceVerify.verify.FaceVerifyUtils;
import com.AI.FaceVerify.verify.ProcessCallBack;
import com.AI.FaceVerify.verify.VerifyStatus;
import com.AI.FaceVerify.view.CameraXAnalyzeFragment;
import com.AI.FaceVerify.view.FaceCoverView;
import com.faceVerify.test.FaceApplication;
import com.faceVerify.test.R;
import com.faceVerify.test.utils.VoicePlayer;

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

    private FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_verify_11);  //1:n 对比

        setTitle("1:1 人脸识别");

        String yourUniQueFaceId = getIntent().getStringExtra(FaceApplication.USER_ID_KEY);


        scoreText = findViewById(R.id.silent_Score);
        tipsTextView = findViewById(R.id.tips_view);
//        result = findViewById(R.id.result);
        faceCoverView = findViewById(R.id.face_cover);
        faceTipsOverlay = findViewById(R.id.faceTips);

        // 0 ,前置摄像头       1，后置摄像头    部分外接USB摄像头支持可能是1
        int cameraLensFacing = getSharedPreferences("faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag", 0);

        CameraXAnalyzeFragment cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL, cameraLensFacing);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();


        //要对比的人脸底片，业务方自行处理
        File file = new File(
                FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_11_VALUE,
                yourUniQueFaceId
        );

        //预留的底图，缓存在本地私有目录
        Bitmap baseBitmap = AiUtil.compressPath(getBaseContext(), Uri.fromFile(file));

        //1.初始化引擎，各种参数配置
        initFaceVerify(baseBitmap);


        cameraXFragment.setOnAnalyzerListener(new CameraXAnalyzeFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //CAMERA
                if (faceVerifyUtils != null)
                    faceVerifyUtils.goVerify(imageProxy, faceCoverView.getMargin());
            }

            @Override
            public void analyze(byte[] rgbBytes, int w, int h) {
                Log.d("1NN1", "length" + rgbBytes.length);
            }

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
        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错

        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getApplication())
                .setThreshold(0.81f)       //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap) //底片,请录入的时候保证底片质量
                .setLiveCheck(true)        //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setVerifyTimeOut(15)      //活体检测支持设置超时时间 9-16 秒
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
                    public void onTimeOutStart(float v) {
                        faceCoverView.startCountDown(v);
                    }

                    @Override
                    public void onCompleted(boolean isMatched) {
                        runOnUiThread(() -> {
                            if (isMatched) {
                                //各种形式的提示，根据业务需求选择
                                tipsTextView.setText("核验已通过，与底片为同一人！ ");
//                                faceCoverView.setTipText("核验已通过，与底片为同一人！");

                                Toast.makeText(getBaseContext(),
                                        "验证通过", Toast.LENGTH_LONG).show();


                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Verify_11_javaActivity.this.finish();
                                    }
                                }, 1000);

                                VoicePlayer.getInstance().play(R.raw.verify_success);

                            } else {
                                tipsTextView.setText("核验不通过，与底片不符！ ");
//                                faceCoverView.setTipText("核验不通过，与底片不符！ ");

                                new android.app.AlertDialog.Builder(Verify_11_javaActivity.this)
                                        .setMessage("核验不通过，与底片不符！ ")
                                        .setCancelable(false)
                                        .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                            }
                                        });

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
                        new android.app.AlertDialog.Builder(getBaseContext())
                                .setMessage("检测超时了")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> faceVerifyUtils.retryVerify()
                                );
                    }

                    break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY: {
                        tipsTextView.setText("多次切换画面或无人脸");
//                        faceCoverView.setTipText("多次切换画面或无人脸");

                        new android.app.AlertDialog.Builder(getBaseContext())
                                .setMessage("多次切换画面或无人脸，停止识别。\n识别过程请保持人脸在画面中")
                                .setCancelable(false)
                                .setPositiveButton("知道了", (dialogInterface, i) -> finish());
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

