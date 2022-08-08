package com.faceVerify.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import com.AI.FaceVerify.utils.AiUtil;
import com.AI.FaceVerify.utils.FaceFileProviderUtils;
import com.faceVerify.test.utils.FileStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 更换照片底片
 *
 */
public class UpdateBaseFaceActivity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE = 2;      //拍照
    private static final int REQUEST_PICTURE_CUT = 3;  //剪裁图片

    private String cacheMediasDir =
            Environment.getExternalStorageDirectory().toString() + "/cameraX/face/";

    private String baseImgName;
    private Uri imageUri;        //拍照的原图保存地址
    private Uri cropImgUri;      //裁剪过的图片地址

    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_base_face);

        imageView = findViewById(R.id.baseImg);
        textView = findViewById(R.id.update);
        baseImgName = getIntent().getStringExtra("baseImgName");

        //可以自己录一张人脸底片 baseImgName
        File file = new File(cacheMediasDir + "testBaseImgName.jpg");
        imageView.setImageBitmap(AiUtil.compressPath(UpdateBaseFaceActivity.this, Uri.fromFile(file)));

        textView.setOnClickListener(v -> {
            openCamera();
        });

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CAPTURE://拍照
                if (resultCode == RESULT_OK) {
                    cropPhoto();
                }
                break;

            case REQUEST_PICTURE_CUT://裁剪完成
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(cropImgUri));

                    imageView.setImageBitmap(bitmap);

                    //save Bitmap
                    File file = new File(cacheMediasDir.concat(baseImgName).concat(".jpg"));
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    /**
     * 打开系统相机
     */
    private void openCamera() {
        File file = new FileStorage(cacheMediasDir).createTempFile("tempTake.jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(UpdateBaseFaceActivity.this,
                    FaceFileProviderUtils.getAuthority(this), file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);          //设置Action为拍照
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);         //将拍取的照片保存到指定URI

        intent.putExtra("android.intent.extras.CAMERA_FACING", 1); // 调用前置摄像头
        intent.putExtra("autofocus", true); // 自动对焦
        intent.putExtra("fullScreen", false); // 全屏

        startActivityForResult(intent, REQUEST_CAPTURE);
    }


    /**
     * 裁剪相片
     */
    private void cropPhoto() {
        File file = new FileStorage(cacheMediasDir).createTempFile("temp.jpg");
        cropImgUri = Uri.fromFile(file);
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 250);
        intent.putExtra("outputY", 250);

        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropImgUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, REQUEST_PICTURE_CUT);
    }

}