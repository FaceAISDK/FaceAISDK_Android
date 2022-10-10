package com.faceVerify.test.verify1N;

import static com.faceVerify.test.FaceApplication.BASE_FACE_DIR_1N;
import static com.faceVerify.test.FaceApplication.CACHE_BASE_FACE_DIR;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.AI.FaceVerify.utils.FaceFileProviderUtils;
import com.AI.FaceVerify.verify.BaseImageDispose;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.faceVerify.test.R;
import com.faceVerify.test.utils.FileStorage;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 添加1：N 的人脸识别底照
 */
public class Add1NBaseFaceActivity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE = 2;      //拍照
    private static final int REQUEST_PICTURE_CUT = 3;  //剪裁图片

    private String yourUniQueFaceId;
    private Uri imageUri;         // 拍照的原图保存地址
    private Uri cropImgUri;       // 裁剪过的图片地址

    private List<String> BaseImageList = new ArrayList<>();

    private ImageView imageView;
    private TextView textView;
    private RecyclerView recyclerView;

    private Bitmap baseBitmap;

    private BaseImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_base_face_1n);

        imageView = findViewById(R.id.baseImg);
        textView = findViewById(R.id.update);
        recyclerView = findViewById(R.id.recyclerView);


        textView.setOnClickListener(v -> {
            if (null != baseBitmap) {
                yourUniQueFaceId = "base-face-" + SystemClock.currentThreadTimeMillis();
                File file = new File(CACHE_BASE_FACE_DIR + BASE_FACE_DIR_1N, yourUniQueFaceId);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    baseBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    fos.flush();
                    fos.close();

                    Toast.makeText(getBaseContext(), "保存成功", Toast.LENGTH_LONG).show();
                    baseBitmap = null;

                    BaseImageList.clear();
                    BaseImageList.addAll(getFilesAllName(CACHE_BASE_FACE_DIR + BASE_FACE_DIR_1N));
                    adapter.notifyDataSetChanged();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                openCamera();
            }
        });


        FlexboxLayoutManager flm = new FlexboxLayoutManager(this);
        flm.setFlexWrap(FlexWrap.WRAP);
        flm.setFlexDirection(FlexDirection.COLUMN);
        flm.setAlignItems(AlignItems.CENTER);
        flm.setJustifyContent(JustifyContent.FLEX_START);

        BaseImageList.addAll(getFilesAllName(CACHE_BASE_FACE_DIR + BASE_FACE_DIR_1N));

        adapter = new BaseImageAdapter(BaseImageList);
        recyclerView.setLayoutManager(flm);
        recyclerView.setAdapter(adapter);
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
                try {
                    baseBitmap = BaseImageDispose.dispose(getBaseContext(),
                            BitmapFactory.decodeStream(getContentResolver().openInputStream(cropImgUri)));

                    if (null != baseBitmap) {
                        imageView.setImageBitmap(baseBitmap);
                    } else {
                        Add1NBaseFaceActivity.this.finish();
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
        File file = new FileStorage(CACHE_BASE_FACE_DIR + BASE_FACE_DIR_1N).createTempFile("tempTake.jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(Add1NBaseFaceActivity.this,
                    FaceFileProviderUtils.getAuthority(this), file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        File file = new FileStorage(CACHE_BASE_FACE_DIR + BASE_FACE_DIR_1N).createTempFile("tempTake.jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cropImgUri = FileProvider.getUriForFile(Add1NBaseFaceActivity.this,
                    FaceFileProviderUtils.getAuthority(this), file);
        } else {
            cropImgUri = Uri.fromFile(file);
        }

        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
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

    /**
     * 获取底片文件夹下的所有文件
     *
     * @param path
     * @return
     */
    public List<String> getFilesAllName(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        List<String> fileNames = new ArrayList<>();

        if (files == null) {
            Log.e("error", "空目录");
            return fileNames;
        }
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getAbsolutePath().contains("tempTake")) {
                fileNames.add(files[i].getAbsolutePath());
            }
        }
        return fileNames;
    }


    public class BaseImageAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        /**
         * 构造方法，此示例中，在实例化Adapter时就传入了一个List。
         * 如果后期设置数据，不需要传入初始List，直接调用 super(layoutResId); 即可
         */
        public BaseImageAdapter(List<String> list) {
            super(R.layout.layout_1n_base_image, list);
        }

        /**
         * 在此方法中设置item数据
         */
        @Override
        protected void convert(@NotNull BaseViewHolder helper, @NotNull String item) {
            ImageView view = helper.getView(R.id.baseImg);

            Glide.with(getBaseContext()).load(item)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(view);


        }
    }

}