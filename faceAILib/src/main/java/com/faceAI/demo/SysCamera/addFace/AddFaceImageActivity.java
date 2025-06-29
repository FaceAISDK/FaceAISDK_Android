package com.faceAI.demo.SysCamera.addFace;

import static android.view.View.GONE;
import static com.faceAI.demo.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.base.baseImage.BaseImageCallBack.AlIGN_FAILED;
import static com.ai.face.base.baseImage.BaseImageCallBack.MANY_FACE;
import static com.ai.face.base.baseImage.BaseImageCallBack.NOT_REAL_HUMAN;
import static com.ai.face.base.baseImage.BaseImageCallBack.NO_FACE;
import static com.ai.face.base.baseImage.BaseImageCallBack.SMALL_FACE;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.faceAI.demo.R;

import org.jetbrains.annotations.NotNull;
/**
 * 使用系统相机 添加一张规范的人脸图并裁剪调整为符合SDK规范，1:1 和1:N 公共的添加人脸图
 * 注意保存的方式有点差异。
 * <p>
 * 其他系统的录入的人脸请自行保证人脸规范，否则会导致识别错误
 * <p>
 * -  1. 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * -  2. 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * -  3. 光线环境好，检测的人脸化浓妆或佩戴墨镜 口罩 帽子等遮盖
 * -  4. 人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 */
public class AddFaceImageActivity extends AppCompatActivity {
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView, secondTips;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;

    //如果启用活体检测，根据自身情况完善业务逻辑
    private boolean isRealFace = true;

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_image);
        findViewById(R.id.back)
                .setOnClickListener(v -> finishFaceVerify(0,"用户取消"));

        tipsTextView = findViewById(R.id.tips_view);
        secondTips = findViewById(R.id.second_tips_view);
        addFaceImageType = getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);
        faceID = getIntent().getStringExtra(USER_FACE_ID_KEY);

        /*
         * context 需要是Activity context
         * 2 PERFORMANCE_MODE_ACCURATE 精确模式 人脸要正对摄像头，严格要求
         * 1 PERFORMANCE_MODE_FAST 快速模式 允许人脸方位可以有一定的偏移
         * 0 PERFORMANCE_MODE_EASY 简单模式 允许人脸方位可以「较大」的偏移
         *
         */
        baseImageDispose = new BaseImageDispose(this, 2, new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap, float silentLiveValue) {
                runOnUiThread(() -> showConfirmDialog(bitmap, silentLiveValue));
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(() -> {
                    AddFaceTips(actionCode);
                });
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.001f) //焦距范围[0.001f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)   //画面旋转方向
                .setSize(CameraXFragment.SIZE.DEFAULT) //相机的分辨率大小。分辨率越大画面中人像很小也能检测但是会更消耗CPU
                .create();

        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy, 10, false));
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();
    }

    /**
     * 添加人脸过程中的提示
     *
     * @param tipsCode
     */
    private void AddFaceTips(int tipsCode) {
        switch (tipsCode) {
            case NOT_REAL_HUMAN:
                Toast.makeText(getBaseContext(), R.string.not_real_face, Toast.LENGTH_LONG).show();
                secondTips.setText(R.string.not_real_face);
                //公版Demo 为了方便调试不处理人脸活体，实际业务中请根据自身情况完善业务逻辑
                isRealFace = false;
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
            case NO_FACE:
                tipsTextView.setText(R.string.no_face_detected_tips);
                break;
            case MANY_FACE:
                tipsTextView.setText(R.string.multiple_faces_tips);
                break;
            case SMALL_FACE:
                tipsTextView.setText(R.string.come_closer_tips);
                break;
            case AlIGN_FAILED:
                tipsTextView.setText(R.string.align_face_error_tips);
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
        // 这样写是为了明确给UTS 插件信息
        finishFaceVerify(0,"用户取消") ;
    }


    /**
     * 识别结束返回结果, 为了给uniApp UTS插件统一的交互返回格式
     *
     * @param code
     * @param msg
     */
    private void finishFaceVerify(int code,String msg) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", msg);
        setResult(RESULT_OK, intent);
        finish();
    }


    /**
     * 确认是否保存人脸底图
     *
     */
    private void showConfirmDialog(Bitmap bitmap, float silentLiveValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.dialog_confirm_base, null);

        //设置对话框布局
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(false);
        ImageView basePreView = dialogView.findViewById(R.id.preview);
        TextView realManTips = dialogView.findViewById(R.id.realManTips);

        if (!isRealFace) {
            realManTips.setVisibility(View.VISIBLE);
            realManTips.setText(getString(R.string.not_real_face_for_debug) + silentLiveValue );
        } else {
            realManTips.setVisibility(GONE);
        }
        basePreView.setImageBitmap(bitmap);

        Button btnOK = dialogView.findViewById(R.id.btn_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.requestFocus();
        editText.setText(faceID);
        if (addFaceImageType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())
                &&!TextUtils.isEmpty(faceID)) {
            editText.setVisibility(GONE);
        }
        btnOK.setOnClickListener(v -> {
            faceID = editText.getText().toString();

            if (!TextUtils.isEmpty(faceID)) {
                if (addFaceImageType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                    Toast.makeText(getBaseContext(), "录入成功", Toast.LENGTH_SHORT).show();
                    //1:1 人脸识别保存人脸底图
                    baseImageDispose.saveBaseImage(bitmap, CACHE_BASE_FACE_DIR, faceID, 300);
                    dialog.dismiss();
                    //这样写是为了明确给UTS 插件信息
                    finishFaceVerify(1,"人脸添加成功");
                } else {
                    //1:N ，M：N 人脸搜索保存人脸
                    String faceName = editText.getText().toString() + ".jpg";
                    String filePathName = CACHE_SEARCH_FACE_DIR + faceName;
                    // 一定要用SDK API 进行添加删除，不要直接File 接口文件添加删除，不然无法同步人脸SDK中特征值的更新
                    FaceSearchImagesManger.Companion.getInstance(getApplication()).insertOrUpdateFaceImage(bitmap, filePathName, new FaceSearchImagesManger.Callback() {
                        @Override
                        public void onSuccess() {
                            finishFaceVerify(1,"人脸添加成功");
                        }
                        @Override
                        public void onFailed(@NotNull String msg) {
                            Toast.makeText(getBaseContext(), "人脸图入库失败：：" + msg, Toast.LENGTH_SHORT).show();
                            finishFaceVerify(-1,"人脸添加失败");
                        }
                    });
                }
            } else {
                Toast.makeText(getBaseContext(), "Input FaceID Name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            baseImageDispose.retry();
            isRealFace = true;
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}

