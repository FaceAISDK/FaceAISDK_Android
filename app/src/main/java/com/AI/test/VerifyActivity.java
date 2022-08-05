package com.AI.test;

import static com.AI.FaceVerify.verify.FaceDetectorUtils.ACTION_FAILED;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ACTION_OK;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.cacheMediasDir;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ALIVE_DETECT_TYPE_ENUM.BLINK;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE;
import static com.AI.FaceVerify.verify.FaceDetectorUtils.ALIVE_DETECT_TYPE_ENUM.SMILE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
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
import android.widget.TextView;
import com.AI.FaceVerify.verify.FaceDetectorUtils;
import com.AI.FaceVerify.graphic.GraphicOverlay;
import com.AI.FaceVerify.verify.VerifyStatusCallBack;
import com.AI.FaceVerify.utils.AiUtil;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * 检验超时也要写出来
 * <p>
 * 百度：https://ai.baidu.com/ai-doc/FACE/Bkp6nusr3
 */
public class VerifyActivity extends AppCompatActivity {

    private TextView resultTextView, tipsTextView;
    private GraphicOverlay mGraphicOverlay; //遮罩层，仅仅用于调试用

    private boolean isPass = false;
    private Bitmap baseBitmap; //底片Bitmap
    private FaceDetectorUtils faceDetectorUtils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        tipsTextView = findViewById(R.id.tips_view);
        resultTextView = findViewById(R.id.result_text_view);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        //可以自己录一张人脸底片
        File file = new File(cacheMediasDir + "testBaseImgName.jpg");
        baseBitmap = AiUtil.compressPath(VerifyActivity.this, Uri.fromFile(file));

        //Builder 模式吧。

        //第二个参数表示是否需要活体检测
        //mGraphicOverlay可以不传，仅仅是辅助开发调试
        faceDetectorUtils = new FaceDetectorUtils(VerifyActivity.this, true, mGraphicOverlay, baseBitmap,
                new VerifyStatusCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
                        runOnUiThread(() -> {
                            if (isMatched) {
                                isPass = true;
                                resultTextView.setText("是本人，核对已通过！ ");
                                resultTextView.setBackgroundColor(getResources()
                                        .getColor(R.color.colorAccent));
                            } else {
                                isPass = false;
                                resultTextView.setText("非本人，或未对正脸！ ");
                                resultTextView.setBackgroundColor(getResources()
                                        .getColor(R.color.black_overlay));
                            }
                        });
                    }

                    @Override
                    public void onFailed(int code) {
                        //错误Code 见错误文档编码

                    }

                    @Override
                    public void onProcessTips(int actionCode) {
                        showAliveDetectTips(actionCode);
                    }

                });

        initCameraXAnalysis();
    }


    /**
     * 根据业务和UI交互修改你的 UI
     *
     */
    private void showAliveDetectTips(int actionCode){
        runOnUiThread(() -> {
            switch (actionCode) {
                case ACTION_FAILED:
                    tipsTextView.setText("活体检测超时了");
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
     */
    public void initCameraXAnalysis() {
        PreviewView previewView = findViewById(R.id.previewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        //图像预览和摄像头原始数据回调 暴露，以便后期格式转换和人工智障处理
        //图像编码默认格式 YUV_420_888。
        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //CameraX 可通过 setOutputImageFormat(int) 支持 YUV_420_888
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
                        new ImageAnalysis.Analyzer() {
                            @SuppressLint("UnsafeOptInUsageError")
                            @Override
                            public void analyze(@NonNull ImageProxy imageProxy) {
                                if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
                                    throw new IllegalArgumentException("Invalid image format");
                                }

                                if (!isPass) {
                                    faceDetectorUtils.goVerify(imageProxy);
                                }

                                imageProxy.close();
                            }
                        });


                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview, imageAnalysis);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }

        }, ContextCompat.getMainExecutor(this));
    }


    /**
     * 资源释放
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetectorUtils = null;
    }


}
