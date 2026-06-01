package com.hiface.demo.SysCamera.search;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import com.sdk.hiface.search.FaceSearchEngine;
import com.sdk.hiface.search.SearchProcessBuilder;
import com.sdk.hiface.search.SearchProcessCallBack;
import com.sdk.hiface.search.SearchProcessTipsCode;
import com.sdk.hiface.search.FaceSearchResult;
import com.hiface.demo.BuildConfig;
import com.hiface.demo.R;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ando.file.core.FileOperator;
import ando.file.core.FileUtils;
import ando.file.selector.FileSelectCallBack;
import ando.file.selector.FileSelectOptions;
import ando.file.selector.FileSelectResult;
import ando.file.selector.FileSelector;
import ando.file.selector.FileType;

/**
 * 图片人脸搜索，以图搜人
 * 图片规范 https://i.postimg.cc/RCwNy0kV/add-Face.jpg
 *
 */
public class FaceSearchByImageActivity extends AppCompatActivity {
    private static final int REQUEST_SELECT_IMAGE = 1001;
    private static final int TARGET_BITMAP_MAX_SIDE = 1920; // 目标解码最大边长

    private ImageView ivSelectedImage;
    private TextView tvSearchResult;
    private FileSelector mFileSelector;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicInteger currentTaskVersion = new AtomicInteger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_search_by_image);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FileOperator.INSTANCE.init(getApplication(), BuildConfig.DEBUG);

        initViews();
        initFaceSearchParam();
    }

    private void initViews() {
        ivSelectedImage = findViewById(R.id.iv_selected_image);
        tvSearchResult = findViewById(R.id.tv_search_result);
        Button btnSelectImage = findViewById(R.id.btn_select_image);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnSelectImage.setOnClickListener(v -> chooseImageFromAlbum());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && mFileSelector != null) {
                        mFileSelector.obtainResult(REQUEST_SELECT_IMAGE, result.getResultCode(), result.getData());
                    }
                }
        );
    }

    private void chooseImageFromAlbum() {
        FileSelectOptions options = new FileSelectOptions();
        options.setFileType(FileType.IMAGE);
        options.setSingleFileMaxSize(10485760); // 10M
        options.setMinCount(1);
        options.setMaxCount(1);

        mFileSelector = FileSelector.Companion.with(this, imagePickerLauncher)
                .setRequestCode(REQUEST_SELECT_IMAGE)
                .applyOptions(options)
                .filter((fileType, uri) -> fileType == FileType.IMAGE && uri != null && !FileUtils.INSTANCE.isGif(uri))
                .callback(new FileSelectCallBack() {
                    @Override
                    public void onSuccess(List<FileSelectResult> results) {
                        if (results != null && !results.isEmpty()) {
                            processSelectedImage(results.get(0).getUri());
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(FaceSearchByImageActivity.this, "选择失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).choose();
    }

    /**
     * 后台纯内存下采样解码，抛弃低效的“先写文件后读文件”压缩流程
     */
    private void processSelectedImage(Uri uri) {
        if (uri == null) return;
        int taskVersion = currentTaskVersion.incrementAndGet();

        executorService.execute(() -> {
            Bitmap bitmap = decodeSampledBitmap(uri, TARGET_BITMAP_MAX_SIDE);

            // 抛弃过期任务
            if (currentTaskVersion.get() != taskVersion || isFinishing() || isDestroyed()) {
                if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
                return;
            }

            runOnUiThread(() -> {
                if (bitmap == null) {
                    tvSearchResult.setText("图片解码失败");
                    return;
                }
                ivSelectedImage.setImageBitmap(bitmap);
                FaceSearchEngine.Companion.getInstance().runSearchWithBitmap(bitmap);
            });
        });
    }

    @Nullable
    private Bitmap decodeSampledBitmap(@NonNull Uri uri, int maxSide) {
        try {
            // 1. 获取图片原始宽高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, options);
            }

            // 2. 计算采样率并解码
            options.inSampleSize = calculateInSampleSize(options, maxSide);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 进一步节省内存

            Bitmap originalBmp;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                originalBmp = BitmapFactory.decodeStream(is, null, options);
            }

            if (originalBmp == null) return null;

            // 3. 处理方向旋转
            int degree = getExifRotationDegree(uri);
            if (degree != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degree);
                Bitmap rotatedBmp = Bitmap.createBitmap(originalBmp, 0, 0, originalBmp.getWidth(), originalBmp.getHeight(), matrix, true);
                if (rotatedBmp != originalBmp && !originalBmp.isRecycled()) originalBmp.recycle();
                return rotatedBmp;
            }
            return originalBmp;
        } catch (Exception e) {
            Log.e("FaceSearch", "Decode failed", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int maxSide) {
        int max = Math.max(options.outHeight, options.outWidth);
        int inSampleSize = 1;
        while ((max / inSampleSize) > maxSide) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private void initFaceSearchParam() {
        SearchProcessBuilder builder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setSearchType(SearchProcessBuilder.SearchType.SINGLE_IMAGE)
                .setThreshold(0.82f) //默认0.85，可设置范围[0.8,0.9]
                .setProcessCallBack(new SearchProcessCallBack() {
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> results, Bitmap bitmap, float liveness) {
                        drawFaceBoxAndName(bitmap, results);
                    }
                    @Override
                    public void onProcessTips(int i) {
                        if(i== SearchProcessTipsCode.NO_LIVE_FACE){
                            tvSearchResult.setText("NO_LIVE_FACE");
                        }else if(i== SearchProcessTipsCode.LOCAL_FACE_DATABASE_EMPTY){
                            tvSearchResult.setText("LOCAL_FACE_DATABASE_EMPTY");
                        }
                    }
                }).create();

        FaceSearchEngine.Companion.getInstance().initSearchParams(builder);
    }

    private void drawFaceBoxAndName(Bitmap searchBitmap, List<FaceSearchResult> matchedResults) {
        if (searchBitmap == null || matchedResults == null || matchedResults.isEmpty()) {
            runOnUiThread(() -> tvSearchResult.setText("null error"));
            return;
        }

        Bitmap mutableBitmap = searchBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        float maxDim = Math.max(mutableBitmap.getWidth(), mutableBitmap.getHeight());

        Paint paintRect = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRect.setColor(Color.GREEN);
        paintRect.setStyle(Paint.Style.STROKE);
        paintRect.setStrokeWidth(maxDim * 0.003f);

        Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBg.setColor(Color.parseColor("#99000000"));

        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(maxDim * 0.02f);

        Paint.FontMetrics fm = paintText.getFontMetrics();
        float padding = paintText.getTextSize() * 0.3f;

        int matchedFace=0;
        for (FaceSearchResult result : matchedResults) {
            Rect rect = result.getRect();
            if (rect != null) {
                canvas.drawRect(rect, paintRect);
                if (!TextUtils.isEmpty(result.getFaceName())) {
                    matchedFace++;
                    String label = String.format(Locale.getDefault(), "%s | %.2f", result.getFaceName(), result.getFaceScore());
                    float textWidth = paintText.measureText(label);
                    float textHeight = fm.descent - fm.ascent;

                    float bgLeft = rect.left;
                    float bgTop = rect.top - textHeight - (padding * 2);
                    if (bgTop < 0) bgTop = rect.top + padding; // 保护：避免画出界外

                    float bgBottom = bgTop + textHeight + (padding * 2);
                    float bgRight = bgLeft + textWidth + (padding * 2);

                    canvas.drawRoundRect(new RectF(bgLeft, bgTop, bgRight, bgBottom), padding, padding, paintBg);
                    canvas.drawText(label, bgLeft + padding, bgBottom - padding - fm.descent, paintText);
                }
            }
        }

        int finalMatchedFace = matchedFace;
        runOnUiThread(() -> {
            ivSelectedImage.setImageBitmap(mutableBitmap);
            tvSearchResult.setText(String.format(Locale.getDefault(), "检测到%1d个人脸，搜索匹配成功%2d个", matchedResults.size(), finalMatchedFace));
        });
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
        } catch (Exception ignored) { return 0; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}