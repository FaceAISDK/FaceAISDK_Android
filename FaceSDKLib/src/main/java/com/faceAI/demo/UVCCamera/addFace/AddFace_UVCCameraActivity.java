package com.faceAI.demo.UVCCamera.addFace;

import static android.view.View.GONE;
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
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_SELECT;
import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import static com.faceAI.demo.UVCCamera.manger.UVCCameraManager.RGB_KEY_DEFAULT;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.core.engine.FaceAISDKEngine;
import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.UVCCamera.manger.CameraBuilder;
import com.faceAI.demo.UVCCamera.manger.UVCCameraManager;
import com.faceAI.demo.databinding.ActivityUvcCameraAddFaceBinding;
import com.tencent.mmkv.MMKV;

import java.util.Objects;

/**
 * 使用UVC RGB摄像头录入人脸
 * 更多UVC 摄像头参数设置 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class AddFace_UVCCameraActivity extends AppCompatActivity {

    public ActivityUvcCameraAddFaceBinding binding;
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;
    private UVCCameraManager rgbCameraManager; //添加人脸只用到 RBG camera
    private boolean isConfirmAdd = false; //确认期间停止人脸检测

    //是1:1 还是1:N 人脸搜索添加人脸，他们的数据处理方式有点不同
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUvcCameraAddFaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRGBCamara();
        addFaceInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rgbCameraManager != null) {
            rgbCameraManager.releaseCameraHelper();//释放 RGB camera 资源
        }
    }

    private void initRGBCamara() {
        SharedPreferences sp = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);

        String s = sp.getString(RGB_UVC_CAMERA_SELECT, RGB_KEY_DEFAULT);
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("UVC RGB Camera")
                .setCameraKey(s)
                .setCameraView(binding.rgbCameraView)
                .setContext(this)
                .setDegree(sp.getInt(RGB_UVC_CAMERA_DEGREE, 0))
                .setHorizontalMirror(sp.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager = new UVCCameraManager(cameraBuilder);

        rgbCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                if (!isConfirmAdd) {
                    baseImageDispose.dispose(bitmap);
                }
            }
        });
    }

    private void addFaceInit() {
        tipsTextView = binding.tipsView;
        binding.back.setOnClickListener(v -> finish());
        addFaceImageType = getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);
        faceID = getIntent().getStringExtra(USER_FACE_ID_KEY);

        /**
         * context 需要是
         *
         * 2 PERFORMANCE_MODE_ACCURATE 精确模式 人脸要正对摄像头，严格要求
         * 1 PERFORMANCE_MODE_FAST 快速模式 允许人脸方位可以有一定的偏移
         * 0 PERFORMANCE_MODE_EASY 简单模式 允许人脸方位可以「较大」的偏移
         */
        baseImageDispose = new BaseImageDispose(this, PERFORMANCE_MODE_FAST, new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap, float silentLiveValue, float faceBrightness) {
                isConfirmAdd = true;
                confirmAddFaceDialog(bitmap, silentLiveValue);
            }

            @Override
            public void onProcessTips(int actionCode) {
                AddFaceTips(actionCode);
            }
        });
    }

    /**
     * 确认人脸图
     *
     * @param bitmap 符合对应参数设置的SDK裁剪好的人脸图
     * @param score  1
     */
    private void confirmAddFaceDialog(Bitmap bitmap, float score) {
        ConfirmFaceDialog confirmFaceDialog = new ConfirmFaceDialog(this, bitmap, score);

        confirmFaceDialog.btnConfirm.setOnClickListener(v -> {
            //提取人脸特征值,从已经经过SDK裁剪好的Bitmap中提取人脸特征值
            //如果非SDK录入的人脸照片提取特征值用 Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap
            String faceFeature = FaceAISDKEngine.getInstance(this).croppedBitmap2Feature(bitmap);

            String faceIDName = confirmFaceDialog.faceIDEdit.getText().toString();
            if (!TextUtils.isEmpty(faceIDName)) {
                if (AddFaceImageTypeEnum.FACE_VERIFY.name().equals(addFaceImageType)) {
                    //保存1:1 人脸识别特征数据，直接以KEY-Value的形式保存在MMKV中
                    MMKV.defaultMMKV().encode(faceIDName, faceFeature); //保存人脸faceID 对应的特征值,SDK 只要这个

                    //如果人脸图业务上需要人脸头像进行UI展示也可以保存到本地
                    FaceAISDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_BASE_FACE_DIR, faceIDName);
                    finish();
                } else {
                    //人脸搜索(1:N) 不适合存放在MMKV中。
                    //tag 和 group 可以用来做标记和分组。人脸搜索的时候可以加快速度降低误差
                    FaceSearchFeatureManger.getInstance(this)
                            .insertFaceFeature(faceIDName, faceFeature, System.currentTimeMillis(), "tag", "group");

                    //保存到人脸搜索目录；
                    FaceAISDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, faceIDName);
                    finish();
                }
            } else {
                Toast.makeText(this, R.string.input_face_id_tips, Toast.LENGTH_SHORT).show();
            }
        });

        confirmFaceDialog.btnCancel.setOnClickListener(v -> {
            confirmFaceDialog.dialog.dismiss();
            baseImageDispose.retry();
            isConfirmAdd = false;
        });

        confirmFaceDialog.dialog.show();
    }

    /**
     * 人脸确认框View 管理
     */
    public class ConfirmFaceDialog {
        public AlertDialog dialog;
        public Button btnConfirm, btnCancel;
        public EditText faceIDEdit;

        public ConfirmFaceDialog(Context context, Bitmap bitmap, float silentLiveValue) {
            dialog = new AlertDialog.Builder(context).create();
            View dialogView = View.inflate(context, R.layout.dialog_confirm_base, null);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setView(dialogView);
            dialog.setCanceledOnTouchOutside(false);
            ImageView basePreView = dialogView.findViewById(R.id.preview);
            Glide.with(context)
                    .load(bitmap)
                    .transform(new RoundedCorners(22))
                    .into(basePreView);
            btnConfirm = dialogView.findViewById(R.id.share_face_feature);
            btnCancel = dialogView.findViewById(R.id.btn_cancel);
            faceIDEdit = dialogView.findViewById(R.id.edit_text);
            faceIDEdit.setText(faceID);
            if (AddFaceImageTypeEnum.FACE_VERIFY.name().equals(addFaceImageType) && !TextUtils.isEmpty(faceID)) {
                faceIDEdit.setVisibility(GONE); //制作UTS等插件传过来的FaceID,用户不能再二次编辑
            } else {
                faceIDEdit.requestFocus();
            }
            TextView livenessScore = dialogView.findViewById(R.id.liveness_score);
            livenessScore.setText("Liveness Score: " + silentLiveValue);
        }

        public void show() {
            dialog.show();
        }

        public void dismiss() {
            dialog.dismiss();
        }
    }

    private void AddFaceTips(int actionCode) {
        switch (actionCode) {
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
                tipsTextView.setText(R.string.keep_face_tips); //2秒后确认图像
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
}
