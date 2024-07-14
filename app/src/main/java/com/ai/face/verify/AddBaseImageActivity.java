package com.ai.face.verify;

import static com.ai.face.FaceApplication.CACHE_BASE_FACE_DIR;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;

import com.ai.face.R;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.CameraXFragment;

/**
 * 修改底图,实际业务可以调用系统相机拍照后再调用API 处理
 * 准备增加人脸质量检测（VIP）
 *
 */
public class AddBaseImageActivity extends AppCompatActivity {
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private long index = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_base);
        setTitle("1:1人脸底图添加");

        tipsTextView = findViewById(R.id.tips_view);
        findViewById(R.id.back).setOnClickListener(v -> {
            this.finish();
        });


        //准备增加人脸质量检测（VIP）
        baseImageDispose = new BaseImageDispose(getBaseContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(() -> showConfirmDialog(bitmap));
            }

            @Override
            public void onProcessTips(int actionCode) {
                runOnUiThread(() -> {

                    //准备增加人脸质量检测（VIP） 不合格的给提示
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

                        //更多？
                    }
                });
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);
        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0));


        /**
         * 1. Camera 的初始化。第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.01f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         */
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens,0.09f);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            index++;
            if(index%33==0){ //让程序响应慢一点
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy,10,false));
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

        EditText editText = dialogView.findViewById(R.id.edit_text);

        editText.requestFocus();
        editText.setVisibility(View.GONE);  //face id, 1:1 写死，实际业务自行修改

        btnOK.setOnClickListener(v -> {
                String yourUniQueFaceId = "18707611416"; //face id, 1:1 写死，实际业务自行修改
                //添加新的底片，业务需要处理是新加还是修改！！自行处理
                baseImageDispose.saveBaseImage(bitmap, CACHE_BASE_FACE_DIR, yourUniQueFaceId);
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
