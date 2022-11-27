package com.faceVerify.test.utils;


import static com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL;
import static com.faceVerify.test.FaceApplication.FACE_DIR_KEY;
import static com.faceVerify.test.FaceApplication.BASE_FACE_PATH;
import static com.faceVerify.test.FaceApplication.USER_ID_KEY;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.faceVerify.test.R;


/**
 * 新加，修改底图
 */
public class AddBaseImageActivity extends AppCompatActivity {
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String yourUniQueFaceId;
    private String childDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_base);  //1:n 对比

        setTitle("底图添加");

        yourUniQueFaceId = getIntent().getStringExtra(USER_ID_KEY);
        childDir = getIntent().getStringExtra(FACE_DIR_KEY);

        tipsTextView = findViewById(R.id.tips_view);

        new BaseImageDispose(getBaseContext(),new BaseImageCallBack(){
            @Override
            public void onCompleted(Bitmap bitmap) {

            }

            @Override
            public void onProcessTips(int actionCode) {

            }
        });


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
                                tipsTextView.setText("未检测到人脸");
                                break;
                            case MANY_FACE:
                                tipsTextView.setText("多张人脸出现");
                                break;
                            case SMALL_FACE:
                                tipsTextView.setText("靠近一点");
                                break;
                            case AlIGN_FAILED:
                                tipsTextView.setText("图像校准失败");
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
                //什么Bitmap,保存为最终形态吧
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy,15));
            }

            @Override
            public void analyze(byte[] rgbBytes,int w,int h) {
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
        ImageView basePreView = (ImageView) dialogView.findViewById(R.id.preview);

        basePreView.setImageBitmap(bitmap);

        Button btnOK = (Button) dialogView.findViewById(R.id.btn_ok);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                baseImageDispose.saveBaseImage(bitmap, BASE_FACE_PATH
                        + childDir,yourUniQueFaceId);

                dialog.dismiss();
                finish();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

                //太快了，可以延迟一点重试
                baseImageDispose.retry();
            }
        });
        dialog.show();
    }


}
