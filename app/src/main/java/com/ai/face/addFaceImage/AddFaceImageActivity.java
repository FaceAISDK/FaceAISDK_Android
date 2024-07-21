package com.ai.face.addFaceImage;


import static com.ai.face.FaceApplication.FACE_DIR_KEY;
import static com.ai.face.FaceApplication.USER_ID_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;

import com.ai.face.R;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.CameraXFragment;

import java.io.ByteArrayOutputStream;

/**
 * 修改底图,实际业务可以调用系统相机拍照后再调用API 处理
 * 人脸照片返回高清人脸图，同时返回原图（VIP）
 * <p>
 * <p>
 * OpenCV
 * 1.人脸角度提示
 * 2.人脸完整度提示
 * 3.闭眼提示
 * 4.特征点遮挡提示（待开发）
 * 5.高清人脸图和原图输出（Beta 试点中）
 */
public class AddFaceImageActivity extends AppCompatActivity {
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private long index = 1;
    private int indexPeriod;

    private boolean isAliveCheck = false; //是否要真人来录制人脸，还是别人代拍一张照片也行？防止作弊

    private String pathName, fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_image);
        setTitle("1:1人脸底图添加");

        tipsTextView = findViewById(R.id.tips_view);
        findViewById(R.id.back).setOnClickListener(v -> {
            this.finish();
        });

        fileName = getIntent().getStringExtra(USER_ID_KEY);
        pathName = getIntent().getStringExtra(FACE_DIR_KEY);

        // 根据自己业务需求指定丢帧参数
        if (isAliveCheck) {
            indexPeriod = 5;
        } else {
            indexPeriod = 33;
        }

        //第一个参数是否开启静默活体检测
        baseImageDispose = new BaseImageDispose(isAliveCheck, getBaseContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(() -> showConfirmDialog(bitmap));
            }

            //人脸照片返回高清人脸图，同时返回原图（VIP），15 beta 3 公测中
            @Override
            public void onCompletedVIP(Bitmap bitmap, Bitmap bitmap1) {
                //可以断点看看清晰度
                super.onCompletedVIP(bitmap, bitmap1);
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(() -> {

                    //准备增加人脸质量检测（VIP） 不合格的给提示
                    switch (actionCode) {
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

                        case REAL_HUMAN:
                            showTempTips("活体检验通过");
                            break;

                        case NOT_REAL_HUMAN: //仅仅开启了活体检测的有
                            showTempTips("非真正人脸");
                            break;
                    }
                });
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);
        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0));


        /**
         * 1. Camera 的初始化。第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.001f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         */
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens, 0.1f);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            index++;
            if (index % indexPeriod == 0) { // %的值得大可以让流程更慢
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy, 10, false));
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }


    /**
     * 准备加语音提示
     *
     * @param tips 提示语
     */
    private void showTempTips(String tips) {
        countDownTimer.cancel();
        tipsTextView.setText(tips);
        tipsTextView.setVisibility(View.VISIBLE);
        countDownTimer.start();
    }


    /**
     * 封装成一个Lib ，倒计时显示库
     */
    CountDownTimer countDownTimer = new CountDownTimer(1000L * 2, 1000L) {
        @Override
        public void onTick(long millisUntilFinished) {
            Log.e("countDownTimer", "onTick()" + millisUntilFinished);
        }

        @Override
        public void onFinish() {
            runOnUiThread(() -> {
                tipsTextView.setText("");
                tipsTextView.setVisibility(View.INVISIBLE);
            });
        }
    };


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
                baseImageDispose.saveBaseImage(bitmap, pathName, fileName, 456);
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
            index = 1;
            dialog.dismiss();
            //太快了，可以延迟一点重试
            baseImageDispose.retry();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}
