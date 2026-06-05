package com.hiface.demo.SysCamera.addFace;

import static android.view.View.GONE;
import static com.hiface.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.hiface.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.hiface.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import static com.sdk.hiface.base.addFace.AddFaceDispose.PERFORMANCE_MODE_ACCURATE;
import static com.sdk.hiface.base.addFace.AddFaceDispose.PERFORMANCE_MODE_FAST;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.*;
import static com.sdk.hiface.recognize.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.*;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.hiface.demo.FaceSDKConfig;
import com.hiface.demo.R;
import com.hiface.demo.SysCamera.camera.FaceCameraXFragment;
import com.hiface.demo.base.AbsBaseActivity;
import com.hiface.demo.base.view.FaceCoverView;
import com.sdk.hiface.base.addFace.AddFaceCallBack;
import com.sdk.hiface.base.addFace.AddFaceDispose;
import com.sdk.hiface.base.utils.DataConvertUtils;
import com.sdk.hiface.base.view.camera.CameraXBuilder;
import com.sdk.hiface.core.engine.HiFaceSDKEngine;
import com.sdk.hiface.search.FaceSearchFeatureManger;
import com.tencent.mmkv.MMKV;

import java.util.Objects;

/**
 * 演示使用SDK相机规范人脸录入,保存人脸特征值
 * 1:1 和1:N 人脸特征数据保存有点差异，参考代码详情
 *
 * 通过图片录入人脸特征 {@link AddFaceByImageTestDemo} 我们提供2个API（请尽量使用SDK相机录入人脸信息）
 * Image2FaceFeature.getFaceFeatureByBitmap /getFaceFeatureByBase64
 *
 * @author FaceSDK.Support@gmail.com
 */
public class AddFaceFeatureActivity extends AbsBaseActivity {
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    public static String ADD_FACE_PERFORMANCE_MODE = "ADD_FACE_PERFORMANCE_MODE";
    public static String NEED_CONFIRM_ADD_FACE = "NEED_CONFIRM_ADD_FACE"; //是否需要弹窗确认
    private boolean needConfirmAdd = true;   //是否需要弹窗给用户确认人脸信息,强烈建议需要确认
    private FaceCoverView faceCoverView;
    private AddFaceDispose addFaceDispose;
    private String faceID, addFaceType;
    private boolean isConfirmAdd = false;   //是否弹出Dialog等待确定人脸

    //PERFORMANCE_MODE_EASY | PERFORMANCE_MODE_ACCURATE |PERFORMANCE_MODE_NO_LIMIT
    private int addFacePerformanceMode = PERFORMANCE_MODE_FAST;

    //是1:1识别还是1:N人脸搜索录入添加人脸特征信息
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_add_face_feature);
        findViewById(R.id.back)
                .setOnClickListener(v -> finishAddFace(0, "Cancel by user",""));

        faceCoverView = findViewById(R.id.face_cover);
        addFaceType = getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);

        if(FaceSDKConfig.isDebugMode(this)){
            addFacePerformanceMode=PERFORMANCE_MODE_FAST;
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(USER_FACE_ID_KEY)) {
                faceID = intent.getStringExtra(USER_FACE_ID_KEY);
            }
            if (intent.hasExtra(ADD_FACE_PERFORMANCE_MODE)) {
                addFacePerformanceMode = intent.getIntExtra(ADD_FACE_PERFORMANCE_MODE,PERFORMANCE_MODE_ACCURATE);
            }
            if (intent.hasExtra(NEED_CONFIRM_ADD_FACE)) {
                needConfirmAdd = intent.getBooleanExtra(NEED_CONFIRM_ADD_FACE,true);
            }
        }


        /* 添加人脸,实时检测相机视频流人脸角度是否符合当前模式设置，并给予提示
         *
         *  2 PERFORMANCE_MODE_ACCURATE   精确模式 人脸要正对摄像头，严格要求角度
         *  1 PERFORMANCE_MODE_FAST       快速模式 允许人脸角度可以有一定的偏差
         *  0 PERFORMANCE_MODE_EASY       简单模式 允许人脸角度可以「较大」的偏差
         * -1 PERFORMANCE_MODE_NO_LIMIT   无限制模式 基本上检测到人脸就返回了
         */
        addFaceDispose = new AddFaceDispose(this, addFacePerformanceMode,false, new AddFaceCallBack() {
            /**
             * 人脸检测裁剪完成
             * @param cropped         SDK检测裁剪矫正后的Bitmap，20260227版本统一大小为224*224
             * @param silentScore     静默活体分数(摄像头品质有关)，needLivenessCheck=true才有值
             * @param origin          640*480 原图
             */
            @Override
            public void onCompleted(Bitmap cropped, float silentScore,Bitmap origin) {
                isConfirmAdd=true;
                //提取人脸特征值,从已经经过SDK裁剪好的Bitmap中提取人脸特征值
                //如果非SDK相机录入的人脸照片提取特征值用异步方法 Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap
                String faceFeature = HiFaceSDKEngine.getInstance(getBaseContext()).croppedBitmap2Feature(cropped);

                if(!needConfirmAdd){
                    if(TextUtils.isEmpty(faceID)){
                        Toast.makeText(getBaseContext(), R.string.input_face_id_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //明确指示不需要弹窗确认，并且faceID指定了
                    if (addFaceType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                        saveFaceVerifyData(cropped,faceID,faceFeature);
                    } else {
                        saveFaceSearchData(cropped,faceID,faceFeature);
                    }
                    finishAddFace(1, "Add face success",faceFeature);
                }else{
                    confirmAddFaceDialog(cropped,faceFeature,origin);
                }
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
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.1f)  //范围[0f,1.0f]，根据应用场景，自行适当调整焦距参数（摄像头需支持变焦）
                .setRotation(degree)  //画面旋转角度0，90，180，270
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing() && !isConfirmAdd) {
                //某些设备如果一直提示检测不到人脸，可以断点调试看看转化的Bitmap 是否有问题
                addFaceDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy));
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 这样写是为了返回明确信息给UTS，RN，Flutter 等三方插件
        finishAddFace(0, "Cancel by user","");
    }

    /**
     * 识别结束返回结果, 为了给Flutter，RN，uniApp UTS等第三方插件统一的交互返回格式
     */
    private void finishAddFace(int code, String msg,String faceFeature) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", msg)
                .putExtra("faceFeature", faceFeature);

        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 经过SDK裁剪矫正处理好的bitmap 转为人脸特征值
     *
     * @param croppedBitmap 符合对应参数设置的SDK裁剪好的人脸图
     */
    private void confirmAddFaceDialog(Bitmap croppedBitmap,String faceFeature,Bitmap originBitmap) {
        ConfirmFaceDialog confirmFaceDialog=new ConfirmFaceDialog(this,originBitmap);

        confirmFaceDialog.btnConfirm.setOnClickListener(v -> {
            faceID = confirmFaceDialog.faceIDEdit.getText().toString();
            if (!TextUtils.isEmpty(faceID)) {
                if (addFaceType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                    saveFaceVerifyData(croppedBitmap,faceID,faceFeature);
                } else {
                    saveFaceSearchData(croppedBitmap,faceID,faceFeature);
                }
                finishConfirm(confirmFaceDialog.dialog, faceFeature);
            } else {
                Toast.makeText(getBaseContext(), R.string.input_face_id_tips, Toast.LENGTH_SHORT).show();
            }
        });

        confirmFaceDialog.btnCancel.setOnClickListener(v -> {
            isConfirmAdd=false;
            confirmFaceDialog.dialog.dismiss();
            addFaceDispose.retry();
        });

        confirmFaceDialog.dialog.show();
    }


    /**
     * 保存1:1 人脸识别人脸特征值
     * @param bitmap  如果有需要,bitmap 也保存一下吧
     * @param faceID
     * @param faceFeature 人脸特征值
     */
    private void saveFaceVerifyData(Bitmap bitmap,String faceID,String faceFeature){
        //保存1:1 人脸识别特征数据，直接以KEY-Value的形式保存在MMKV中
        MMKV.defaultMMKV().encode(faceID, faceFeature); //保存人脸faceID 对应的特征值,SDK 只要这个
        //如果人脸图业务上需要人脸头像进行UI展示也可以保存到本地
        HiFaceSDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_BASE_FACE_DIR, faceID);
    }


    /**
     * 保存1:N ，M：N 人脸搜索识别人脸特征值
     * @param bitmap 如果业务有需要,bitmap 也保存一下
     * @param faceID
     * @param faceFeature 人脸特征值
     */
    private void  saveFaceSearchData(Bitmap bitmap,String faceID,String faceFeature){
        //人脸搜索(1:N) 不适合存放在MMKV中,使用SDK提供的FaceSearchFeatureManger保存。
        //tag 和 group 可以用来做标记和分组。人脸搜索的时候可以作为权限场景控制以及 加快速度降低误差
        FaceSearchFeatureManger.getInstance(this)
                .insertFaceFeature(faceID, faceFeature, System.currentTimeMillis(),"tag","group");

        //可选步骤：裁剪处理好的Bitmap保存到人脸搜索目录(注意！只保存人脸图不保存人脸特征值，人脸搜索是无法工作的)
        HiFaceSDKEngine.getInstance(this).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, faceID);
    }


    private void finishConfirm(Dialog dialog, String faceFeature){
        dialog.dismiss();
        finishAddFace(1, "Add face success",faceFeature);
    }


    /**
     * 人脸录入确认弹窗
     */
    public class ConfirmFaceDialog{
        public AlertDialog dialog;
        public Button btnConfirm,btnCancel;
        public EditText faceIDEdit;
        public ConfirmFaceDialog(Context context,Bitmap bitmap){
            dialog = new AlertDialog.Builder(context).create();
            View dialogView = View.inflate(context, R.layout.dialog_confirm_add_face, null);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setView(dialogView);
            dialog.setCanceledOnTouchOutside(false);
            ImageView facePreView = dialogView.findViewById(R.id.preview);
            Glide.with(context)
                    .load(bitmap)
                    .transform(new CenterCrop(), new RoundedCorners(33))
                    .into(facePreView);
            btnConfirm = dialogView.findViewById(R.id.share_face_feature);
            btnCancel = dialogView.findViewById(R.id.btn_cancel);
            faceIDEdit = dialogView.findViewById(R.id.edit_text);
            faceIDEdit.setText(faceID);
            if (!TextUtils.isEmpty(faceID)) {
                faceIDEdit.setVisibility(GONE);
            }else {
                faceIDEdit.requestFocus();
            }
        }

        public void show(){
            dialog.show();
        }

        public void dismiss(){
            dialog.dismiss();
        }
    }

}
