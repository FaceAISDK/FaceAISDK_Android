package com.faceAI.demo.SysCamera.addFace;

import static android.view.View.GONE;
import static com.ai.face.base.baseImage.BaseImageDispose.PERFORMANCE_MODE_ACCURATE;
import static com.ai.face.base.baseImage.BaseImageDispose.PERFORMANCE_MODE_FAST;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import static com.faceAI.demo.FaceSDKConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;

import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.baseImage.FaceEmbedding;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.core.engine.FaceAISDKEngine;
import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import com.tencent.mmkv.MMKV;
import java.util.Objects;

/**
 * 使用SDK相机规范人脸录入,保存人脸特征值
 *
 * 1:1 和1:N 人脸特征数据保存有点差异，参考代码详情
 * <p>
 * 其他系统的录入的人脸请自行保证人脸规范，否则会导致识别错误
 * -  1. 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * -  2. 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * -  3. 光线环境好，检测的人脸化浓妆或佩戴墨镜 口罩 帽子等遮盖
 * -  4. 人脸照片要求300*300 裁剪好的仅含人脸的正方形照片
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class AddFaceImageActivity extends AbsBaseActivity {
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    public static String ADD_FACE_PERFORMANCE_MODE = "ADD_FACE_PERFORMANCE_MODE";

    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceType;
    private boolean isConfirmAdd = false;   //是否正在弹出Dialog确定人脸合规，确认期间停止人脸检测
    private int addFacePerformanceMode = PERFORMANCE_MODE_FAST;  //默认快速模式，要求人脸正对摄像头

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_image);
        findViewById(R.id.back)
                .setOnClickListener(v -> finishFaceVerify(0, "Cancel by user"));

        tipsTextView = findViewById(R.id.tips_view);
        addFaceType = getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);

        if(FaceSDKConfig.isDebugMode(this)){
            addFacePerformanceMode=PERFORMANCE_MODE_FAST;
        }

        Intent intent = getIntent(); // 获取发送过来的Intent对象
        if (intent != null) {
            if (intent.hasExtra(USER_FACE_ID_KEY)) {
                faceID = intent.getStringExtra(USER_FACE_ID_KEY);
            }
            if (intent.hasExtra(ADD_FACE_PERFORMANCE_MODE)) {
                addFacePerformanceMode = intent.getIntExtra(ADD_FACE_PERFORMANCE_MODE,PERFORMANCE_MODE_ACCURATE);
            }
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
             * @param bitmap           检测裁剪后的Bitmap
             * @param silentLiveValue  静默活体分数（不再使用，后面静默会改为炫彩）
             * @param faceBrightness   人脸周围环境光线亮度
             */
            @Override
            public void onCompleted(Bitmap bitmap, float silentLiveValue,float faceBrightness) {
                isConfirmAdd=true;
                confirmAddFaceDialog(bitmap, silentLiveValue);
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
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing() && !isConfirmAdd) {
                //某些设备如果一直提示检测不到人脸，可以断点调试看看转化的Bitmap 是否有问题
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy, 10, false));
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 这样写是为了返回明确信息给UTS，RN，Flutter 等三方插件
        finishFaceVerify(0, "Cancel by user");
    }

    /**
     * 识别结束返回结果, 为了给uniApp UTS等插件统一的交互返回格式
     */
    private void finishFaceVerify(int code, String msg) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", msg);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 经过SDK裁剪矫正处理好的bitmap 转为人脸特征值
     *
     * @param bitmap 符合对应参数设置的SDK裁剪好的人脸图
     * @param silentLiveValue 静默活体分数，和摄像头有关，自行根据业务需求处理
     */
    private void confirmAddFaceDialog(Bitmap bitmap, float silentLiveValue) {
        ConfirmFaceDialog confirmFaceDialog=new ConfirmFaceDialog(this,bitmap,silentLiveValue);
        confirmFaceDialog.btnConfirm.setOnClickListener(v -> {
            //提取人脸特征值,从已经经过SDK裁剪好的Bitmap中提取人脸特征值
            //如果非SDK相机录入的人脸照片提取特征值用异步方法 Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap
            String faceFeature = FaceAISDKEngine.getInstance(this).croppedBitmap2Feature(bitmap);

            faceID = confirmFaceDialog.faceIDEdit.getText().toString();
            if (!TextUtils.isEmpty(faceID)) {
                if (addFaceType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                    //保存1:1 人脸识别特征数据，直接以KEY-Value的形式保存在MMKV中
                    MMKV.defaultMMKV().encode(faceID, faceFeature); //保存人脸faceID 对应的特征值,SDK 只要这个

                    //如果人脸图业务上需要人脸头像进行UI展示也可以保存到本地
                    FaceAISDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_BASE_FACE_DIR, faceID);
                    finishConfirm(confirmFaceDialog.dialog, 1, "Add face success");
                } else {
                    //人脸搜索(1:N) 不适合存放在MMKV中,使用SDK提供的FaceSearchFeatureManger保存。
                    String faceIDName = confirmFaceDialog.faceIDEdit.getText().toString();
                    //tag 和 group 可以用来做标记和分组。人脸搜索的时候可以加快速度降低误差
                    FaceSearchFeatureManger.getInstance(this)
                            .insertFaceFeature(faceIDName, faceFeature, System.currentTimeMillis(),"tag","group");

                    //可选步骤：裁剪处理好的Bitmap保存到人脸搜索目录(注意！只保存人脸图不保存人脸特征值，人脸搜索是无法工作的)
                    FaceAISDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, faceIDName);

                    finishConfirm(confirmFaceDialog.dialog, 1, "Add face success");
                }
            } else {
                Toast.makeText(getBaseContext(), R.string.input_face_id_tips, Toast.LENGTH_SHORT).show();
            }
        });

        confirmFaceDialog.btnCancel.setOnClickListener(v -> {
            isConfirmAdd=false;
            confirmFaceDialog.dialog.dismiss();
            baseImageDispose.retry();
        });

        confirmFaceDialog.dialog.show();
    }


    /**
     * 人脸录入确认弹窗
     */
    public class ConfirmFaceDialog{
        public AlertDialog dialog;
        public Button btnConfirm,btnCancel;
        public EditText faceIDEdit;
        public ConfirmFaceDialog(Context context,Bitmap bitmap,float silentLiveValue){
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
            btnConfirm = dialogView.findViewById(R.id.btn_ok);
            btnCancel = dialogView.findViewById(R.id.btn_cancel);
            faceIDEdit = dialogView.findViewById(R.id.edit_text);
            faceIDEdit.setText(faceID);
            if (addFaceType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name()) && !TextUtils.isEmpty(faceID)) {
                faceIDEdit.setVisibility(GONE); //制作UTS等插件传过来的FaceID,用户不能再二次编辑
            }else {
                faceIDEdit.requestFocus();
            }
            TextView livenessScore = dialogView.findViewById(R.id.liveness_score);
            livenessScore.setText("Liveness Score: "+ silentLiveValue);
        }

        public void show(){
            dialog.show();
        }

        public void dismiss(){
            dialog.dismiss();
        }
    }


    private void finishConfirm(Dialog dialog,int code,String msg){
        dialog.dismiss();
        finishFaceVerify(code, msg);
        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
    }


}

