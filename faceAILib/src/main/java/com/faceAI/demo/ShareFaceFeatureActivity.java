package com.faceAI.demo;

import static com.ai.face.base.baseImage.BaseImageDispose.PERFORMANCE_MODE_FAST;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.core.engine.FaceAISDKEngine;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import java.util.Objects;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.OutputStream;

/**
 * 使用SDK相机规范人脸录入,保存人脸特征值
 *
 * 其他系统的录入的人脸请自行保证人脸规范，否则会导致识别错误
 * -  1. 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * -  2. 录入高质量的正脸图，人脸清晰，背景简单纯色
 * -  3. 光线环境好，人脸不能化浓妆或佩戴墨镜 口罩 帽子等遮盖
 * -  4. 人脸照片要求300*300 裁剪好的仅含人脸的正方形照片
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class ShareFaceFeatureActivity extends AbsBaseActivity {
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private boolean isConfirmAdd = false;   //是否正在弹出Dialog确定人脸合规，确认期间停止人脸角度合规检测
    private int addFacePerformanceMode = PERFORMANCE_MODE_FAST;  //默认快速模式，要求人脸正对摄像头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_add_face_image);
        findViewById(R.id.back)
                .setOnClickListener(v -> finish());

        tipsTextView = findViewById(R.id.tips_view);
        if(FaceSDKConfig.isDebugMode(this)){
            addFacePerformanceMode=PERFORMANCE_MODE_FAST;
        }

        /* 添加人脸,实时检测相机视频流人脸角度是否符合当前模式设置，并给予提示
         *
         *  2 PERFORMANCE_MODE_ACCURATE   精确模式 人脸要正对摄像头，严格要求角度
         *  1 PERFORMANCE_MODE_FAST       快速模式 允许人脸角度可以有一定的偏差
         *  0 PERFORMANCE_MODE_EASY       简单模式 允许人脸角度可以「较大」的偏差
         * -1 PERFORMANCE_MODE_NO_LIMIT   无限制模式 基本上检测到人脸就返回了
         */
        baseImageDispose = new BaseImageDispose(this, addFacePerformanceMode, new BaseImageCallBack() {
            /**
             * 人脸检测裁剪完成
             * @param bitmap           SDK检测裁剪矫正后的Bitmap，20260227版本统一大小为224*224
             * @param silentScore      静默活体分数
             * @param faceBrightness   人脸周围环境光线亮度
             */
            @Override
            public void onCompleted(Bitmap bitmap, float silentScore,float faceBrightness) {
                isConfirmAdd=true;
                //提取人脸特征值,从已经经过SDK裁剪好的Bitmap中提取人脸特征值
                //如果非SDK相机录入的人脸照片提取特征值用异步方法 Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap
                String faceFeature = FaceAISDKEngine.getInstance(getBaseContext()).croppedBitmap2Feature(bitmap);

                confirmAddFaceDialog(bitmap,faceFeature);

            }

            @Override
            public void onProcessTips(int actionCode) {
                AddFaceTips(actionCode);
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.1f)  //范围[0f,1.0f]，根据应用场景，自行适当调整焦距参数（摄像头需支持变焦）
                .setRotation(degree)  //画面旋转角度0，90，180，270
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing() && !isConfirmAdd) {
                //某些设备如果一直提示检测不到人脸，可以断点调试看看转化的Bitmap 是否有问题
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy));
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();
    }

    /**
     * 添加人脸过程中的提示
     *
     */
    private void AddFaceTips(int tipsCode) {
        switch (tipsCode) {
            case NO_FACE_REPEATEDLY:
                tipsTextView.setText(R.string.no_face_detected_tips);
                break;

            case FACE_TOO_SMALL:
                tipsTextView.setText(R.string.come_closer_tips);
                break;
            case FACE_TOO_LARGE:
                tipsTextView.setText(R.string.far_away_tips);
                break;
            case CLOSE_EYE:
                tipsTextView.setText(R.string.no_close_eye_tips);
                break;
            case HEAD_CENTER:
                tipsTextView.setText(R.string.keep_face_tips); //英文翻译不太友善
                break;
            case TILT_HEAD:
                tipsTextView.setText(R.string.no_tilt_head_tips);
                break;
            case HEAD_LEFT:
                tipsTextView.setText(R.string.head_turn_left_tips);
                break;
            case HEAD_RIGHT:
                tipsTextView.setText(R.string.head_turn_right_tips);
                break;
            case HEAD_UP:
                tipsTextView.setText(R.string.no_look_up_tips);
                break;
            case HEAD_DOWN:
                tipsTextView.setText(R.string.no_look_down_tips);
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        baseImageDispose.release();
    }

    public static void shareImageUri(Context context, Uri imageUri) {
        if (imageUri == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        // 授予临时读取权限，防止第三方 App 报错
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(shareIntent, "分享图片到"));
    }

    public  Uri saveBitmapToPictures(Context context, Bitmap bitmap) {
        long timestamp = System.currentTimeMillis();
        String displayName = "IMG_" + timestamp + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        // Android 10 (API 29) 及以上处理隔离存储
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri imageUri = context.getContentResolver().insert(externalUri, values);

        if (imageUri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                if (out != null) {
                    // 压缩并写入
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                    // 写入完成后，解除 PENDING 状态
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear();
                        values.put(MediaStore.Images.Media.IS_PENDING, 0);
                        context.getContentResolver().update(imageUri, values, null, null);
                    }
                    return imageUri;
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 失败时删除记录
                context.getContentResolver().delete(imageUri, null, null);
            }
        }
        return null;
    }

    /**
     * 经过SDK裁剪矫正处理好的bitmap 转为人脸特征值
     *
     * @param bitmap 符合对应参数设置的SDK裁剪好的人脸图
     */
    private void confirmAddFaceDialog(Bitmap bitmap,String faceFeature) {
        ConfirmFaceDialog confirmFaceDialog=new ConfirmFaceDialog(this,bitmap);

        confirmFaceDialog.btnShareFaceFeature.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, faceFeature);
            intent.setType("text/plain");
            startActivity(intent);

            confirmFaceDialog.dialog.dismiss();
            ShareFaceFeatureActivity.this.finish();
        });

        confirmFaceDialog.btnShareFaceImage.setOnClickListener(v -> {
            Uri savedUri = saveBitmapToPictures(this, bitmap);
            if (savedUri != null) {
                shareImageUri(this, savedUri);
            } else {
                Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
            }
            confirmFaceDialog.dialog.dismiss();
            ShareFaceFeatureActivity.this.finish();
        });

        confirmFaceDialog.btnRetry.setOnClickListener(v -> {
            isConfirmAdd=false;
            confirmFaceDialog.dialog.dismiss();
            baseImageDispose.retry();
        });

        confirmFaceDialog.close.setOnClickListener(v -> {
            confirmFaceDialog.dialog.dismiss();
            ShareFaceFeatureActivity.this.finish();
        });

        confirmFaceDialog.dialog.show();
    }

    /**
     * 人脸录入确认弹窗
     */
    public static class ConfirmFaceDialog{
        public AlertDialog dialog;
        public ImageView close;
        public Button btnShareFaceFeature, btnShareFaceImage ,btnRetry;
        public ConfirmFaceDialog(Context context,Bitmap bitmap){
            dialog = new AlertDialog.Builder(context).create();
            View dialogView = View.inflate(context, R.layout.dialog_share_face, null);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setView(dialogView);
            dialog.setCanceledOnTouchOutside(false);
            ImageView basePreView = dialogView.findViewById(R.id.preview);
            Glide.with(context)
                    .load(bitmap)
                    .transform(new RoundedCorners(22))
                    .into(basePreView);
            btnShareFaceFeature = dialogView.findViewById(R.id.share_face_feature);
            btnShareFaceImage = dialogView.findViewById(R.id.share_face_image);
            btnRetry = dialogView.findViewById(R.id.retry);
            close = dialogView.findViewById(R.id.close);
        }

        public void show(){
            dialog.show();
        }

        public void dismiss(){
            dialog.dismiss();
        }
    }


}

