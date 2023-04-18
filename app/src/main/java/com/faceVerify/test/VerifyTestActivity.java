package com.faceVerify.test;

import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.BLINK;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SMILE;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_MATCH;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_OK;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT;
import static com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL;
import static com.faceVerify.test.FaceApplication.BASE_FACE_PATH;
import static com.faceVerify.test.FaceApplication.DIR_1N_VALUE;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import com.AI.FaceVerify.utils.AiUtil;
import com.AI.FaceVerify.verify.FaceProcessBuilder;
import com.AI.FaceVerify.verify.FaceVerifyUtils;
import com.AI.FaceVerify.verify.ProcessCallBack;
import com.AI.FaceVerify.view.CameraXAnalyzeFragment;
import com.AI.FaceVerify.view.FaceCoverView;
import java.io.File;

/**
 * Demo 接入演示。提高识别精确度和速度
 *
 *
 */
@Deprecated
public class VerifyTestActivity extends AppCompatActivity {
    private TextView tipsTextView;
    private FaceCoverView faceCoverView;
    private FaceVerifyUtils faceDetectorUtils = new FaceVerifyUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_11);  //1:n 对比

        setTitle("Demo 接入演示");

        tipsTextView = findViewById(R.id.tips_view);
        faceCoverView = findViewById(R.id.face_cover);

        //要对比的人脸底片，业务方自行处理
        File file = new File(
                FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_11_VALUE,
                "yourUniQueFaceId");

        Bitmap baseBitmap = AiUtil.compressPath(getBaseContext(), Uri.fromFile(file));

        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //1:1 识别的单张底片
                .setFaceLibFolder(BASE_FACE_PATH + DIR_1N_VALUE) //1:N 的底片库
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考 ReadMe
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
                        runOnUiThread(() -> {
                            if (isMatched) {
                                tipsTextView.setText("是本人，核对已通过！ ");
                            } else {
                                tipsTextView.setText("非本人，或未对正脸！ ");
                            }
                            finish();
                        });
                    }

                    @Override
                    public void onFailed(int code) {

                    }

                    @Override
                    public void onProcessTips(int actionCode) {
                        showAliveDetectTips(actionCode);
                    }

                })
                .create();

        faceDetectorUtils.setDetectorParams(faceProcessBuilder);

        CameraXAnalyzeFragment cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL);
        cameraXFragment.setOnAnalyzerListener(new CameraXAnalyzeFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {

                if (faceDetectorUtils != null&&!VerifyTestActivity.this.isFinishing())
                    faceDetectorUtils.goVerify(imageProxy, faceCoverView.getMargin());

            }

            @Override
            public void analyze(byte[] rgbBytes,int w,int h) {
                Log.d("1NN1", "length" + rgbBytes.length);
            }

        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }


    /**
     * 根据业务和UI交互修改完善你的UI
     *
     */
    private void showAliveDetectTips(int actionCode) {

        if(VerifyTestActivity.this.isDestroyed()||VerifyTestActivity.this.isFinishing()) return;

        runOnUiThread(() -> {
            switch (actionCode) {

                case ACTION_NO_MATCH:
                    tipsTextView.setText("人脸库中无匹配");

                    break;

                case ACTION_TIME_OUT:
                    new AlertDialog.Builder(VerifyTestActivity.this)
                            .setMessage("检测超时！")
                            .setPositiveButton("再来一次",
                                    (dialog1, which) -> {
                                        //Demo 只是把每种状态抛出来，用户可以自己根据需求改造

                                        faceDetectorUtils.retryVerify();
                                    })
                            .show();
                    break;

                case ACTION_FAILED: //2次超时就是失败，只能退出本页面
                    tipsTextView.setText("活体检测失败了");
                    new AlertDialog.Builder(VerifyTestActivity.this)
                            .setMessage("检测失败！")
                            .setPositiveButton("知道了",
                                    (dialog1, which) -> {
                                        //Demo 只是把每种状态抛出来，用户可以自己根据需求改造

                                    })
                            .show();

                    break;


                case ACTION_PROCESS:
                    tipsTextView.setText("匹配中...");
                    break;

                case ACTION_OK:
                    tipsTextView.setText("已经完成活体检测");
                    break;

                case ACTION_NO_FACE:
                    tipsTextView.setText("画面没有检测到人脸");
                    break;

                case OPEN_MOUSE:
                    tipsTextView.setText("请张嘴");
                    break;

                case SMILE:
                    tipsTextView.setText("请微笑");
                    break;

                case BLINK:
                    tipsTextView.setText("请轻眨眼");
                    break;

                case SHAKE_HEAD:
                    tipsTextView.setText("请缓慢左右摇头");
                    break;

                case NOD_HEAD:
                    tipsTextView.setText("请缓慢上下点头");
                    break;

            }
        });
    }


    /**
     * 资源释放
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetectorUtils.destroyProcess();
    }



}
