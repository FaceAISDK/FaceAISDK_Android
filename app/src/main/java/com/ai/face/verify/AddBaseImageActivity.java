package com.ai.face.verify;

import static com.ai.face.FaceApplication.CACHE_BASE_FACE_DIR;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.R;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.faceVerify.baseImage.BaseImageCallBack;
import com.ai.face.faceVerify.baseImage.BaseImageDispose;


/**
 * 修改底图,实际业务可以调用系统相机拍照后再调用API 处理
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

        setTitle("底图添加");

        tipsTextView = findViewById(R.id.tips_view);
        baseImageDispose = new BaseImageDispose(getBaseContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showConfirmDialog(bitmap);
                    }
                });
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

        CameraXFragment cameraXFragment = CameraXFragment.newInstance(getSharedPreferences("faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag",0));

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            index++;
            //什么Bitmap,保存为最终形态吧
            if(index%33==0){
                baseImageDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy,1));
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
        editText.setVisibility(View.GONE); //face id, 1:1 写死，实际业务自行修改

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
