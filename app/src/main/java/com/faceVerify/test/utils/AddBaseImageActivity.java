package com.faceVerify.test.utils;

import static com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL;
import static com.faceVerify.test.FaceApplication.FACE_DIR_KEY;
import static com.faceVerify.test.FaceApplication.BASE_FACE_PATH;
import static com.faceVerify.test.FaceApplication.USER_ID_KEY;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;

import com.AI.FaceVerify.baseImage.BaseImageCallBack;
import com.AI.FaceVerify.baseImage.BaseImageDispose;
import com.AI.FaceVerify.convert.DataConvertUtils;
import com.AI.FaceVerify.view.CameraXAnalyzeFragment;
import com.AI.FaceVerify.view.FaceCoverView;
import com.faceVerify.test.R;

import java.io.File;


/**
 * 仅仅供参考使用，底图处理建议调用系统相机自拍然后裁剪+程序识别以获取高清正常的底片
 * <p>
 * 底片的质量是 影响识别的准确率的直接因素之一
 * <p>
 * <p>
 * 部分业务可以添加审核环节，本Demo 仅供参考
 * <p>
 * 古木阴中系短篷，杖藜扶我过桥东。沾衣欲湿杏花雨，吹面不寒杨柳风
 */
public class AddBaseImageActivity extends AppCompatActivity {
    private BaseImageDispose baseImageDispose;
    private String yourUniQueFaceId;
    private String childDir;
    private long index = 1;

    private FaceCoverView face_cover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_base);

        setTitle("底图编辑");

        findViewById(R.id.back).setOnClickListener(v -> {
            finish();
        });

        //业务方根据自己业务修改Face ID 管理
        yourUniQueFaceId = getIntent().getStringExtra(USER_ID_KEY);
        childDir = getIntent().getStringExtra(FACE_DIR_KEY);

        face_cover = findViewById(R.id.face_cover);
        face_cover.setTipText("请保持正脸在识别框中");

        baseImageDispose = new BaseImageDispose(getBaseContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(() -> showConfirmDialog(bitmap));
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (actionCode) {
                            case NO_FACE:
                                face_cover.setTipText("未检测到人脸");
                                break;
                            case MANY_FACE:
                                face_cover.setTipText("多张人脸出现");
                                break;
                            case SMALL_FACE:
                                face_cover.setTipText("靠近一点");
                                break;
                            case AlIGN_FAILED:
                                face_cover.setTipText("图像校准失败");
                                break;
                        }
                    }
                });
            }
        });

        CameraXAnalyzeFragment cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL);
        cameraXFragment.setOnAnalyzerListener(new CameraXAnalyzeFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                index++;

                // 可以改为一个按钮来给用户控制，点击后再抓图。Demo 聚焦SDK接入不完善可能的业务需求
                if (index % 18 == 0) {
                    baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy, 15));
                }

            }

            @Override
            public void analyze(byte[] rgbBytes, int w, int h) {
                Log.d("1NN1", "length" + rgbBytes.length);
            }

        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

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

        EditText editText = dialogView.findViewById(R.id.edit_text);  //face id


        editText.requestFocus();

        btnOK.setOnClickListener(v -> {

            if (!TextUtils.isEmpty(editText.getText().toString())) {
                yourUniQueFaceId = editText.getText().toString();
            }

            //添加新的底片，业务需要处理是新加还是修改！！自行处理
            baseImageDispose.saveBaseImage(bitmap, BASE_FACE_PATH
                    + childDir, yourUniQueFaceId);
            dialog.dismiss();
            finish();
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
