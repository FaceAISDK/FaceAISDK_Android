package com.hiface.demo;

import static com.sdk.hiface.base.addFace.AddFaceDispose.PERFORMANCE_MODE_FAST;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.sdk.hiface.recognize.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE;
import static com.sdk.hiface.recognize.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL;
import static com.sdk.hiface.recognize.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import static com.hiface.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.hiface.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import com.sdk.hiface.base.addFace.*;
import com.sdk.hiface.base.utils.DataConvertUtils;
import com.sdk.hiface.base.view.camera.CameraXBuilder;
import com.sdk.hiface.core.engine.HiFaceSDKEngine;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.hiface.demo.SysCamera.camera.FaceCameraXFragment;
import com.hiface.demo.base.AbsBaseActivity;
import com.hiface.demo.base.view.FaceCoverView;

import java.util.Objects;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.OutputStream;
import com.tencent.mmkv.MMKV;

/**
 * 「分享导出」使用SDK相机规范人脸录入,保存人脸特征值。
 */
public class ShareFaceFeatureActivity extends AbsBaseActivity {
    private FaceCoverView faceCoverView;
    private AddFaceDispose addFaceDispose;
    private boolean isConfirmAdd = false;   //是否正在弹出Dialog确定人脸合规，确认期间停止人脸角度合规检测
    private int addFacePerformanceMode = PERFORMANCE_MODE_FAST;  //默认快速模式，要求人脸正对摄像头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_add_face_feature);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        faceCoverView = findViewById(R.id.face_cover);
        
        if(FaceSDKConfig.isDebugMode(this)){
            addFacePerformanceMode=PERFORMANCE_MODE_FAST;
        }

        addFaceDispose = new AddFaceDispose(this, addFacePerformanceMode,false,new AddFaceCallBack() {
            @Override
            public void onCompleted(Bitmap cropped, float silentScore,Bitmap origin) {
                isConfirmAdd=true;
                String faceFeature = HiFaceSDKEngine.getInstance(getBaseContext()).croppedBitmap2Feature(cropped);
                confirmAddFaceDialog(cropped,faceFeature);
            }

            @Override
            public void onProcessTips(int actionCode) {
                AddFaceTips(actionCode);
            }
        });

        MMKV mmkv = MMKV.defaultMMKV();
        int cameraLensFacing = mmkv.decodeInt(FRONT_BACK_CAMERA_FLAG, 0);
        int degree = mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing)
                .setLinearZoom(0.1f)
                .setRotation(degree)
                .setCameraSizeHigh(false)
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing() && !isConfirmAdd) {
                addFaceDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy));
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();
    }

    private void AddFaceTips(int tipsCode) {
        switch (tipsCode) {
            case NO_FACE_REPEATEDLY:
                faceCoverView.setTipsText(R.string.no_face_detected_tips);
                break;
            case FACE_TOO_SMALL:
                faceCoverView.setTipsText(R.string.come_closer_tips);
                break;
            case FACE_TOO_LARGE:
                faceCoverView.setTipsText(R.string.far_away_tips);
                break;
            case CLOSE_EYE:
                faceCoverView.setTipsText(R.string.no_close_eye_tips);
                break;
            case HEAD_CENTER:
                faceCoverView.setTipsText(R.string.keep_face_tips);
                break;
            case TILT_HEAD:
                faceCoverView.setTipsText(R.string.no_tilt_head_tips);
                break;
            case HEAD_LEFT:
                faceCoverView.setTipsText(R.string.head_turn_left_tips);
                break;
            case HEAD_RIGHT:
                faceCoverView.setTipsText(R.string.head_turn_right_tips);
                break;
            case HEAD_UP:
                faceCoverView.setTipsText(R.string.no_look_up_tips);
                break;
            case HEAD_DOWN:
                faceCoverView.setTipsText(R.string.no_look_down_tips);
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        addFaceDispose.release();
    }

    public static void shareImageUri(Context context, Uri imageUri) {
        if (imageUri == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "分享图片到"));
    }

    public Uri saveBitmapToPictures(Context context, Bitmap bitmap) {
        long timestamp = System.currentTimeMillis();
        String displayName = "IMG_" + timestamp + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri imageUri = context.getContentResolver().insert(externalUri, values);

        if (imageUri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear();
                        values.put(MediaStore.Images.Media.IS_PENDING, 0);
                        context.getContentResolver().update(imageUri, values, null, null);
                    }
                    return imageUri;
                }
            } catch (Exception e) {
                e.printStackTrace();
                context.getContentResolver().delete(imageUri, null, null);
            }
        }
        return null;
    }

    private void confirmAddFaceDialog(Bitmap bitmap,String faceFeature) {
        ConfirmFaceDialog confirmFaceDialog=new ConfirmFaceDialog(this,bitmap);
        confirmFaceDialog.btnShareFaceFeature.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, faceFeature);
            intent.setType("text/plain");
            startActivity(intent);
            confirmFaceDialog.dialog.dismiss();
            finish();
        });

        confirmFaceDialog.btnShareFaceImage.setOnClickListener(v -> {
            Uri savedUri = saveBitmapToPictures(this, bitmap);
            if (savedUri != null) {
                shareImageUri(this, savedUri);
            } else {
                Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
            }
            confirmFaceDialog.dialog.dismiss();
            finish();
        });

        confirmFaceDialog.btnRetry.setOnClickListener(v -> {
            isConfirmAdd=false;
            confirmFaceDialog.dialog.dismiss();
            addFaceDispose.retry();
        });

        confirmFaceDialog.close.setOnClickListener(v -> {
            confirmFaceDialog.dialog.dismiss();
            finish();
        });

        confirmFaceDialog.dialog.show();
    }

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
            Glide.with(context).load(bitmap).transform(new RoundedCorners(22)).into(basePreView);
            btnShareFaceFeature = dialogView.findViewById(R.id.share_face_feature);
            btnShareFaceImage = dialogView.findViewById(R.id.share_face_image);
            btnRetry = dialogView.findViewById(R.id.retry);
            close = dialogView.findViewById(R.id.close);
        }
        public void show(){ dialog.show(); }
    }
}
