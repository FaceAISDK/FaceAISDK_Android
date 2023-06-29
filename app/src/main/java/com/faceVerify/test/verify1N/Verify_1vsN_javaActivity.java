package com.faceVerify.test.verify1N;


import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_MATCH;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_OK;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS;
import static com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import static com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;

import com.AI.FaceVerify.verify.FaceProcessBuilder;
import com.AI.FaceVerify.verify.FaceVerifyUtils;
import com.AI.FaceVerify.verify.ProcessCallBack;
import com.AI.FaceVerify.view.CameraXAnalyzeFragment;
import com.AI.FaceVerify.view.FaceCoverView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.faceVerify.test.FaceApplication;
import com.faceVerify.test.R;


/**
 * 1:N 人脸识别比对，这是java 的版本，仅供java开发参考，后期不实时维护
 * 请根据kotlin 版补充齐全
 *
 * 新版本后期迁移到 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 *
 */
public class Verify_1vsN_javaActivity extends AppCompatActivity {
    private TextView tipsTextView, scoreText;
    private ImageView result;
    private FaceCoverView faceCoverView;

    private FaceVerifyUtils faceDetectorUtils = new FaceVerifyUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_verify_n);  //1:n 对比

        setTitle("1:N 检测");
        scoreText = findViewById(R.id.silent_score);
        tipsTextView = findViewById(R.id.tips_view);
        result = findViewById(R.id.result);
        faceCoverView = findViewById(R.id.face_cover);

        getSystemData();

        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setFaceLibFolder(FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_1N_VALUE) //N 底片库文件夹路径
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考 ReadMe
                .setProcessCallBack(new ProcessCallBack() {

                    @Override
                    public void onSilentAntiSpoofing(float score) {
                        //静默活体分值，防止作弊

                        scoreText.setText("活体得分：" + score);
                    }

                    @Override
                    public void onMostSimilar(String similar) {
                        runOnUiThread(() -> {
                            tipsTextView.setText("本次匹配完成");

                            if (TextUtils.isEmpty(similar)) {
//                                Toast.makeText(Verify_1vsNActivity.this, "人脸库中无匹配：" + similar, Toast.LENGTH_SHORT).show();
                            } else {
//                                Toast.makeText(Verify_1vsNActivity.this, "最相识：" + similar, Toast.LENGTH_SHORT).show();
                                Glide.with(getBaseContext()).load(similar)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .into(result);
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

        CameraXAnalyzeFragment cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL,
                getSharedPreferences(
                        "faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag",0));

        cameraXFragment.setOnAnalyzerListener(new CameraXAnalyzeFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //CAMERA
                if (faceDetectorUtils != null)
                    faceDetectorUtils.goVerify(imageProxy, faceCoverView.getMargin());


//                drawTestRect(DataConvertUtils.imageProxy2Bitmap(imageProxy, 0),
//                        DataConvertUtils.getRect(imageProxy, faceCoverView.getMargin()));
            }

            @Override
            public void analyze(byte[] rgbBytes, int w, int h) {
                Log.d("1NN1", "length" + rgbBytes.length);
            }

        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }


    /**
     * 验证
     */
    private void drawTestRect(Bitmap tempBitmap, Rect rect) {
        Canvas canvas = new Canvas(tempBitmap);

        //图像上画矩形
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);//不填充
        paint.setStrokeWidth(6);  //线的宽度
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.setImageBitmap(tempBitmap);
            }
        });

    }


    /**
     * 根据业务和UI交互修改你的UI
     */
    private void showAliveDetectTips(int actionCode) {

        if (Verify_1vsN_javaActivity.this.isDestroyed() || Verify_1vsN_javaActivity.this.isFinishing())
            return;

        runOnUiThread(() -> {
            switch (actionCode) {

                case ACTION_NO_MATCH:
                    tipsTextView.setText("人脸库中无匹配");
                    break;

                case ACTION_PROCESS:
                    tipsTextView.setText("匹配中...");
                    break;

                case ACTION_OK:
                    tipsTextView.setText("已经完成活体检测");
                    break;

                case NO_FACE_REPEATEDLY:
                    tipsTextView.setText("画面没有检测到人脸");
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
//        faceDetectorUtils.destroyProcess();
//        faceDetectorUtils = null;
    }


    /**
     * 三星手机 maxMemory 512 MB
     *
     */
    private void getSystemData() {
        ActivityManager am = (ActivityManager) getApplication().getSystemService(Context.ACTIVITY_SERVICE);
        int limitMemorySize = am.getMemoryClass();
        int largeMemorySize = am.getLargeMemoryClass();

        long maxMemory = Runtime.getRuntime().maxMemory();//根据是否largeHeap，等于limitMemory或largeMemory
        Log.i("maxMemory:",Long.toString(maxMemory/(1024*1024)));

    }


}
