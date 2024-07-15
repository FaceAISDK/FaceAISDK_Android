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
import android.widget.Toast;

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
    private int indexPeriod;

    private boolean isAliveCheck=false; //是否要真人，还是拍个照片也行

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_base);
        setTitle("1:1人脸底图添加");

        tipsTextView = findViewById(R.id.tips_view);
        findViewById(R.id.back).setOnClickListener(v -> {
            this.finish();
        });

        if(isAliveCheck){
            indexPeriod=5;
        } else {
            indexPeriod = 33;
        }

        //第一个参数是否开启静默活体检测
        baseImageDispose = new BaseImageDispose(isAliveCheck,getBaseContext(), new BaseImageCallBack() {
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

                        case REAL_HUMAN:
                            tipsTextView.setText("活体检验通过");
                            break;

                        case NOT_REAL_HUMAN: //仅仅开启了活体检测的有
                            tipsTextView.setText("非真正人脸");

                            //业务逻辑自己处理
                            Toast.makeText(getBaseContext(),"请真人录制人脸",Toast.LENGTH_LONG).show();
                            AddBaseImageActivity.this.finish();
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
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens,0.01f);



        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            index++;
            if(index%indexPeriod==0){ // %的值得大可以让流程更慢
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

                //你可以用你自己的保存方法
                baseImageDispose.saveBaseImage(bitmap, CACHE_BASE_FACE_DIR, yourUniQueFaceId,444);
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
