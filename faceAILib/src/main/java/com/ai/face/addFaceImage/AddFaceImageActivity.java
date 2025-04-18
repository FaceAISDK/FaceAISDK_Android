package com.ai.face.addFaceImage;

import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

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

import com.ai.face.R;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.CameraXFragment;

import java.io.ByteArrayOutputStream;

/**
 * 添加一张规范的人脸图并裁剪为SDK需要的正方形，1:1 和1:N 公共的添加人脸图
 * 注意保存的方式有点差异
 *
 *  其他系统的录入的人脸请自行保证人脸规范，否则会导致识别错误
 *
 *  -  1. 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 *  -  2. 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 *  -  3. 光线环境好，检测的人脸化浓妆或佩戴墨镜 口罩 帽子等遮盖
 *  -  4. 人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 *
 */
public  class AddFaceImageActivity extends AppCompatActivity {
    public static String ADD_FACE_IMAGE_TYPE_KEY="ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView,secondTips;

    private BaseImageDispose baseImageDispose;
    private String faceID,addFaceImageType;

    //如果启用活体检测，根据自身情况完善业务逻辑
    private boolean isRealFace=true;

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum
    {
        FACE_VERIFY,FACE_SEARCH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_image);

        tipsTextView = findViewById(R.id.tips_view);
        secondTips=findViewById(R.id.second_tips_view);

        findViewById(R.id.back).setOnClickListener(v -> this.finish());

        addFaceImageType= getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);
        faceID = getIntent().getStringExtra(USER_FACE_ID_KEY);

        /*
         * BaseImageDispose
         * 第一个参数是否启用活体检测，部分定制SDK 会需要
         * context 需要是Activity context
         */
        baseImageDispose = new BaseImageDispose(true, this, new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //公版Demo 为了方便调试不处理人脸活体，实际业务中请根据自身情况完善业务逻辑
                        if(isRealFace){
                            showConfirmDialog(bitmap);
                        }else {
                            Toast.makeText(getBaseContext(),R.string.not_real_face,Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(() -> {
                    switch (actionCode) {

                        case NOT_REAL_HUMAN:
                            Toast.makeText(getBaseContext(),R.string.not_real_face,Toast.LENGTH_LONG).show();
                            secondTips.setText(R.string.not_real_face);
                            //公版Demo 为了方便调试不处理人脸活体，实际业务中请根据自身情况完善业务逻辑
//                            isRealFace=false;
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
                });
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);

        int cameraLensFacing = sharedPref.getInt("cameraFlag", 0);
        int degree=sharedPref.getInt("cameraDegree", getWindowManager().getDefaultDisplay().getRotation());

        /*
         * 1. Camera 的初始化。
         * 第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.001f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         * 焦距拉远一点，人才会靠近屏幕，才会减轻杂乱背景的影响。定制设备的摄像头自行调教此参数
         *
         * 第三个参数是摄像头旋转角度 {@Link Surface.ROTATION_0}
         * 共5个值，默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
         */
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLensFacing, 0.001f,degree);



        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            baseImageDispose.dispose(DataConvertUtils. imageProxy2Bitmap(imageProxy, 10, false));
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }



    /**
     * 确认是否保存人脸底图
     *
     */
    private void showConfirmDialog(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.dialog_confirm_base, null);

        //设置对话框布局
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(false);
        ImageView basePreView = dialogView.findViewById(R.id.preview);
        basePreView.setImageBitmap(bitmap);

        Button btnOK = dialogView.findViewById(R.id.btn_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.requestFocus();
        editText.setText(faceID);
        btnOK.setOnClickListener(v -> {
           faceID=editText.getText().toString();

             if (!TextUtils.isEmpty(faceID)) {
                 if (addFaceImageType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                     Toast.makeText(getBaseContext(), "Add 1:1 Face Image Finish", Toast.LENGTH_SHORT).show();
                     //1:1 人脸识别保存人脸底图
                     baseImageDispose.saveBaseImage(bitmap, CACHE_BASE_FACE_DIR, faceID, 300);
                     dialog.dismiss();
                     finish();
                 } else{
                     //1:N ，M：N 人脸搜索保存人脸
                     dialog.dismiss();
                     Intent intent = new Intent();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                     byte[] bitmapByte = baos.toByteArray();
                     intent.putExtra("picture_data", bitmapByte);
                     intent.putExtra("picture_name", editText.getText().toString());
                     setResult(RESULT_OK, intent);
                     finish();
                 }
            } else {
                Toast.makeText(getBaseContext(), "Input FaceID Name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            baseImageDispose.retry();
            isRealFace=true;
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}

