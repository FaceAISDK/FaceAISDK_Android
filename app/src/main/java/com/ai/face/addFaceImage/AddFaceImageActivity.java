package com.ai.face.addFaceImage;

import static com.ai.face.MyFaceApplication.FACE_DIR_KEY;
import static com.ai.face.MyFaceApplication.USER_ID_KEY;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;

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
 * 添加一张规范的人脸图并裁剪为SDK需要的正方形。
 *
 * 其他系统的录入的人脸请自行保证人脸规范，否则会导致识别错误
 *
 *  -  1. 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 *  -  2. 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 *  -  3. 光线环境好，检测的人脸化浓妆或佩戴墨镜 口罩 帽子等遮盖
 *  -  4. 人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 *
 */
public class AddFaceImageActivity extends AppCompatActivity {
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String pathName, fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_image);
        setTitle("Add Face Image");

        tipsTextView = findViewById(R.id.tips_view);
        findViewById(R.id.back).setOnClickListener(v -> {
            this.finish();
        });

        fileName = getIntent().getStringExtra(USER_ID_KEY);
        pathName = getIntent().getStringExtra(FACE_DIR_KEY);

        baseImageDispose = new BaseImageDispose(true, getBaseContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(() -> showConfirmDialog(bitmap));
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(() -> {
                    switch (actionCode) {
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
                        case NOT_REAL_HUMAN:
                            tipsTextView.setText(R.string.not_real_face);
                            break;
                    }
                });
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);

        // 1. Camera 的初始化。第一个参数0/1 指定前后摄像头； 第二个参数linearZoom [0.001f,1.0f] 指定焦距，默认0.1
        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0));
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens, 0.001f);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            baseImageDispose.dispose(DataConvertUtils. imageProxy2Bitmap(imageProxy, 10, false));
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }



    /**
     * 确认是否保存底图
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
        if (TextUtils.isEmpty(fileName)) {
            editText.setVisibility(View.VISIBLE);
        } else {
            editText.setVisibility(View.GONE);
        }

        btnOK.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(fileName)) {
                baseImageDispose.saveBaseImage(bitmap, pathName, fileName, 400);
                dialog.dismiss();
                finish();
            } else if (!TextUtils.isEmpty(editText.getText().toString())) {
                dialog.dismiss();
                Intent intent = new Intent();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] bitmapByte = baos.toByteArray();
                intent.putExtra("picture_data", bitmapByte);
                intent.putExtra("picture_name", editText.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(getBaseContext(), "Input Face Name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            baseImageDispose.retry();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}

