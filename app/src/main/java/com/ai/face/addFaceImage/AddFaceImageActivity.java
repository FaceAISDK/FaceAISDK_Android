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
 * 1.人脸角度提示
 * 2.人脸完整度提示 （待开发）
 * 3.闭眼提示
 * 4.特征点遮挡提示 （待开发）
 * 5.活体检测
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
                            tipsTextView.setText("保持正脸不要晃动"); //2秒后确认图像
                            break;

                        case TILT_HEAD:
                            tipsTextView.setText("请勿歪头");
                            break;

                        case HEAD_LEFT:
                            tipsTextView.setText("脸偏左");
                            break;
                        case HEAD_RIGHT:
                            tipsTextView.setText("脸偏右");
                            break;
                        case HEAD_UP:
                            tipsTextView.setText("请勿抬头");
                            break;
                        case HEAD_DOWN:
                            tipsTextView.setText("请勿低头");
                            break;

                        case NO_FACE:
                            showTempTips("未检测到人脸");
                            break;
                        case MANY_FACE:
                            showTempTips("多张人脸出现");
                            break;
                        case SMALL_FACE:
                            showTempTips("请靠近一点");
                            break;
                        case AlIGN_FAILED:
                            showTempTips("图像校准失败");
                            break;
                        case NOT_REAL_HUMAN:
                            showTempTips("非真正人脸"); //对着照片录入质量不高
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
            baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy, 10, false));
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }


    private void showTempTips(String tips) {
        tipsTextView.setText(tips);
    }


    /**
     * 确认是否保存底图
     *
     * @param bitmap
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
                Toast.makeText(getBaseContext(), "请输入人脸名称", Toast.LENGTH_SHORT).show();
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

