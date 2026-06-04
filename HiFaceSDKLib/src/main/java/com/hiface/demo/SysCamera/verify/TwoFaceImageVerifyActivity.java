package com.hiface.demo.SysCamera.verify;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.sdk.hiface.search.Image2FaceFeature;
import com.sdk.hiface.recognize.FaceVerifyUtils;
import com.hiface.demo.BuildConfig;
import com.hiface.demo.R;
import com.hiface.demo.SysCamera.search.ImageToast;
import com.hiface.demo.databinding.ActivityTwoFaceImageVerifyBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ando.file.core.FileOperator;
import ando.file.core.FileUtils;
import ando.file.selector.FileSelectCallBack;
import ando.file.selector.FileSelectOptions;
import ando.file.selector.FileSelectResult;
import ando.file.selector.FileSelector;
import ando.file.selector.FileType;

/**
 * 对比两张图片中人脸相似度
 * 图片规范 https://i.postimg.cc/RCwNy0kV/add-Face.jpg
 */
public class TwoFaceImageVerifyActivity extends AppCompatActivity {
    private static final int REQUEST_ADD_FACE_IMAGE = 1;
    private static final String DEFAULT_FACE_ID = "faceID";

    private ActivityTwoFaceImageVerifyBinding viewBinding;
    private FileSelector mFileSelector;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Bitmap leftBitmap;
    private Bitmap rightBitmap;

    private ImageView currentImageView;
    private TextView currentTextView;
    private boolean isSelectingLeft = true;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && mFileSelector != null) {
                    mFileSelector.obtainResult(REQUEST_ADD_FACE_IMAGE, result.getResultCode(), result.getData());
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityTwoFaceImageVerifyBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        FileOperator.INSTANCE.init(getApplication(), BuildConfig.DEBUG);

        viewBinding.back.setOnClickListener(v -> finish());

        // 修改点：监听外层容器的点击事件
        viewBinding.containerLeft.setOnClickListener(v -> {
            isSelectingLeft = true;
            currentImageView = viewBinding.imageLeft;
            currentTextView = viewBinding.textLeft;
            chooseFile();
        });

        viewBinding.containerRight.setOnClickListener(v -> {
            isSelectingLeft = false;
            currentImageView = viewBinding.imageRight;
            currentTextView = viewBinding.textRight;
            chooseFile();
        });

        viewBinding.goVerify.setOnClickListener(v -> verifyFaces());
    }

    private void chooseFile() {
        FileSelectOptions options = new FileSelectOptions();
        options.setFileType(FileType.IMAGE);
        options.setSingleFileMaxSize(9242880L);
        options.setMinCount(1);
        options.setMaxCount(1);

        mFileSelector = FileSelector.Companion
                .with(this, filePickerLauncher)
                .setRequestCode(REQUEST_ADD_FACE_IMAGE)
                .applyOptions(options)
                .callback(new FileSelectCallBack() {
                    @Override
                    public void onSuccess(@Nullable List<FileSelectResult> results) {
                        if (results != null && !results.isEmpty() && currentImageView != null && currentTextView != null) {
                            processSelectedImage(results.get(0).getUri(), currentImageView, currentTextView, isSelectingLeft);
                        }
                    }
                    @Override
                    public void onError(@Nullable Throwable e) {}
                })
                .create()
                .choose();
    }

    private void processSelectedImage(@Nullable Uri selectUri, @NonNull ImageView targetImgView, @NonNull TextView targetTxtView, boolean isLeft) {
        if (selectUri == null) return;

        String faceName = FileUtils.INSTANCE.getFileNameFromUri(selectUri);
        String finalFaceName = (faceName == null || faceName.isEmpty()) ? DEFAULT_FACE_ID : faceName;

        executorService.execute(() -> {
            try {
                Bitmap originalBitmap = loadBitmapFromUri(selectUri);
                if (originalBitmap == null) return;

                int degree = getExifRotationDegree(selectUri);
                Bitmap bitmapSelected = degree != 0 ? rotateBitmap(originalBitmap, degree) : originalBitmap;

                // 选择文件后恢复 UI 状态：显示文字（比如文件名），清空可能存在的旧图
                runOnUiThread(() -> {
                    targetTxtView.setVisibility(View.VISIBLE);
                    targetTxtView.setText(finalFaceName);
                    targetImgView.setImageBitmap(null);
                });

                Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap(bitmapSelected, finalFaceName, new Image2FaceFeature.Callback() {
                    @Override
                    public void onSuccess(@NonNull Bitmap bitmap, @NonNull String faceID, @NonNull String faceFeature) {
                        runOnUiThread(() -> {
                            if (isLeft) leftBitmap = bitmap;
                            else rightBitmap = bitmap;

                            // 修复成功：隐藏提示文字，给专用的 ImageView 设置提取好的人脸 Bitmap
                            targetTxtView.setVisibility(View.GONE);
                            targetImgView.setImageBitmap(bitmap);
                        });
                    }
                    @Override
                    public void onFailed(@NonNull String msg) {
                        runOnUiThread(() -> Toast.makeText(TwoFaceImageVerifyActivity.this, msg, Toast.LENGTH_LONG).show());
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(TwoFaceImageVerifyActivity.this, "Load image error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void verifyFaces() {
        if (leftBitmap == null || rightBitmap == null) {
            Toast.makeText(this, "Select image first", Toast.LENGTH_SHORT).show();
            return;
        }
        // 图片规范 https://i.postimg.cc/RCwNy0kV/add-Face.jpg
        executorService.execute(() -> {
            float simi = new FaceVerifyUtils().evaluateFaceSimiByBitmap(this, leftBitmap, rightBitmap);
            runOnUiThread(() -> {
                if (simi > 0.75) { //是一个经验值，实际使用中可以根据需求调整
                    new ImageToast().showBitmap(getApplication(), null, "Simi: "+simi);
                } else {
                    new ImageToast().showBitmap(getApplication(), null, "Simi: "+simi,false);
                }
            });
        });
    }

    @Nullable
    private Bitmap loadBitmapFromUri(@NonNull Uri uri) throws IOException {
        ContentResolver resolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri), (decoder, info, src) -> {
                decoder.setMutableRequired(true);
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }
        return MediaStore.Images.Media.getBitmap(resolver, uri);
    }

    private int getExifRotationDegree(@NonNull Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return 0;
            int orientation = new ExifInterface(is).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) { return 0; }
    }

    @NonNull
    private Bitmap rotateBitmap(@NonNull Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) degree);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}