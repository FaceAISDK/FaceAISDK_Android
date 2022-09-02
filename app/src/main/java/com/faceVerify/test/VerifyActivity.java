package com.faceVerify.test;

import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM.ACTION_FAILED;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM.ACTION_NO_FACE_DETECT;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM.ACTION_OK;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM.ACTION_TIME_OUT;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD;

import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.BLINK;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE;
import static com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SMILE;
import static com.faceVerify.test.FaceApplication.BASE_FACE_KEY;
import static com.faceVerify.test.FaceApplication.CACHE_BASE_FACE_DIR;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.AI.FaceVerify.graphic.GraphicOverlay;
import com.AI.FaceVerify.verify.FaceDetectorUtils;
import com.AI.FaceVerify.verify.FaceProcessBuilder;
import com.AI.FaceVerify.verify.ProcessCallBack;
import com.AI.FaceVerify.utils.AiUtil;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * 检验超时周末有空添加，可以根据Demo 结合自身进行业务定制UX，请先了解人脸识别基础
 *
 * 2.0.0 版本后已经稳定
 */
public class VerifyActivity extends AppCompatActivity {

    private TextView tipsTextView;

    private FaceDetectorUtils faceDetectorUtils = new FaceDetectorUtils();


    /**
     * 资源释放
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetectorUtils.destroyProcess();
        faceDetectorUtils = null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_verify);

        tipsTextView = findViewById(R.id.tips_view);

        findViewById(R.id.back).setOnClickListener(v -> VerifyActivity.this.finish());

        String yourUniQueFaceId = getIntent().getStringExtra(BASE_FACE_KEY); //你的FaceID Key

        //要对比的人脸底片（都是1：1 比对）
        File file = new File(CACHE_BASE_FACE_DIR, yourUniQueFaceId);
        Bitmap baseBitmap = AiUtil.compressPath(VerifyActivity.this, Uri.fromFile(file));

        //初始化引擎
        initVerify(baseBitmap);

        initCameraXAnalysis();
    }


    /**
     * 初始化认证引擎
     *
     *
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名和功能简介到 anylife.zlb@gmail.com
     * @param baseBitmap 底片
     */
    private void initVerify(Bitmap baseBitmap){

        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //底片,请录入的时候保证底片质量
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
                        runOnUiThread(() -> {
                            if (isMatched) {
                                tipsTextView.setText("核验已通过，与底片为同一人！ ");
                                new AlertDialog.Builder(VerifyActivity.this)
                                        .setMessage("核验已通过，与底片为同一人！")
                                        .setPositiveButton("知道了",
                                                (dialog1, which) -> {
                                                    VerifyActivity.this.finish();
                                                })
                                        .show();


                            } else {

                                tipsTextView.setText("核验不通过，与底片不符！ ");

                                new AlertDialog.Builder(VerifyActivity.this)
                                        .setMessage("核验不通过，与底片不符！ ")
                                        .setPositiveButton("知道了",
                                                (dialog1, which) -> {
                                                    VerifyActivity.this.finish();
                                                })
                                        .show();

                            }
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
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     */
    private void showAliveDetectTips(int actionCode) {
        runOnUiThread(() -> {
            switch (actionCode) {

                case ACTION_TIME_OUT:
                    new android.app.AlertDialog.Builder(VerifyActivity.this)
                            .setMessage("检测超时了！")
                            .setPositiveButton("再来一次",
                                    (dialog1, which) -> {
                                        //Demo 只是把每种状态抛出来，用户可以自己根据需求改造

                                        faceDetectorUtils.retryVerify();
                                    })
                            .show();
                    break;

                case ACTION_NO_FACE_DETECT:
                    tipsTextView.setText("画面没有检测到人脸");
                    break;

                case ACTION_FAILED:
                    tipsTextView.setText("活体检测失败了");
                    break;
                case ACTION_OK:
                    tipsTextView.setText("已经完成活体检测");

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
     * 初始化相机,使用CameraX 结合CNN
     *
     */
    public void initCameraXAnalysis() {
        PreviewView previewView = findViewById(R.id.previewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
                        imageProxy -> {
                            if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
                                throw new IllegalArgumentException("Invalid image format");
                            }

                            faceDetectorUtils.goVerify(imageProxy);


                            imageProxy.close();
                        });


                //front camera default
                int lensFacing=cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)?
                        CameraSelector.LENS_FACING_FRONT:CameraSelector.LENS_FACING_BACK;


                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                // Attach use cases to the camera with the same lifecycle owner
                cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview, imageAnalysis);


                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

            } catch (InterruptedException | ExecutionException | CameraInfoUnavailableException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }

        }, ContextCompat.getMainExecutor(this));
    }



}
