package com.faceAI.demo.SysCamera.verify;

import static com.faceAI.demo.FaceSDKConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;
import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.face.base.baseImage.FaceEmbedding;
import com.chad.library.adapter.base.listener.OnItemLongClickListener;
import com.faceAI.demo.UVCCamera.verify.FaceVerify_UVCCameraActivity;
import com.faceAI.demo.UVCCamera.addFace.AddFace_UVCCameraActivity;
import com.faceAI.demo.UVCCamera.addFace.AddFace_UVCCameraFragment;
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity;
import com.faceAI.demo.SysCamera.search.ImageBean;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.faceAI.demo.R;
import com.faceAI.demo.base.utils.BitmapUtils;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 1:1 人脸识别引导说明页面,支持传统的HAL 接口的Android Camera API 摄像头 和 UVC 协议摄像头
 *
 * 包含怎么添加人脸照片，人脸活体检测，人脸识别
 */
public class FaceVerifyWelcomeActivity extends AbsAddFaceFromAlbumActivity {
    public static final String FACE_VERIFY_DATA_SOURCE_TYPE = "FACE_VERIFY_DATA_SOURCE_TYPE";
    private final List<ImageBean> faceImageList = new ArrayList<>();
    private FaceImageListAdapter faceImageListAdapter;
    private DataSourceType dataSourceType = DataSourceType.Android_HAL;

    // Android_HAL 摄像头： 采用标准的 Android Camera2 API 和摄像头 HAL 接口。FaceAI SDK 底层使用CameraX 管理
    public enum DataSourceType {
        UVC, Android_HAL;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verify_welcome);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getIntent().getExtras();

        //1.判定摄像头种类
        if(bundle!=null){
            dataSourceType = (DataSourceType) bundle.getSerializable(FACE_VERIFY_DATA_SOURCE_TYPE);
            if (DataSourceType.Android_HAL.equals(dataSourceType)) {
                ((TextView) findViewById(R.id.camera_mode)).setText("系统相机模式");
            }
        }

        LinearLayout addFaceView = findViewById(R.id.add_face_from_camera);
        addFaceView.setOnClickListener(view -> {
                    if (dataSourceType.equals(DataSourceType.Android_HAL)) {
                        startActivity(
                                new Intent(getBaseContext(), AddFaceImageActivity.class)
                                        .putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_VERIFY.name()));
                    } else {
                        startActivity(
                                new Intent(getBaseContext(), AddFace_UVCCameraActivity.class)
                                        .putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFace_UVCCameraFragment.AddFaceImageTypeEnum.FACE_VERIFY.name()));
                    }
                }
        );

        LinearLayout addFaceFromPhoto= findViewById(R.id.add_face_from_photo);
        addFaceFromPhoto.setOnClickListener(view -> chooseFaceImage());

        // 2 横向滑动列表初始化
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);//设置为横向滑动
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(layoutManager);

        faceImageListAdapter = new FaceImageListAdapter(faceImageList);
        mRecyclerView.setAdapter(faceImageListAdapter);
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            ImageBean imageBean = faceImageList.get(i);
            new AlertDialog.Builder(this).setTitle("确定要删除"
                    + imageBean.name).setMessage("删除后对应的人将无法被识别").setPositiveButton("确定", (dialog, which) -> {
                File file = new File(imageBean.path);
                if (file.delete()) {
                    updateFaceList();
                } else {
                    Toast.makeText(getApplication(), "Delete failed", Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton("取消", null).show();
            return false;
        });

        //32 位CPU测试
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            startActivity(
                    new Intent(getBaseContext(), FaceVerification32CPUTestActivity.class)
                            .putExtra(USER_FACE_ID_KEY, faceImageList.get(i).name));
            return false;
        });

        faceImageListAdapter.setOnItemClickListener((adapter, view, i) -> {
                    // 根据摄像头种类启动不同的模式
                    if (dataSourceType.equals(DataSourceType.Android_HAL)) {
                        startActivity(
                                new Intent(getBaseContext(), FaceVerificationActivity.class)
                                        .putExtra(USER_FACE_ID_KEY, faceImageList.get(i).name));
                    } else {
                        //USB UVC协议摄像头，双目
                        startActivity(
                                new Intent(getBaseContext(), FaceVerify_UVCCameraActivity.class)
                                        .putExtra(USER_FACE_ID_KEY, faceImageList.get(i).name));
                    }
                }
        );

        faceImageListAdapter.setEmptyView(R.layout.verify_empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> addFaceView.performClick());
    }


    /**
     * 相册选择的照片,裁剪等处理好数据后返回了
     *
     */
    @Override
    public void disposeSelectImage(@NotNull String faceID, @NotNull Bitmap disposedBitmap, @NonNull float[] faceEmbedding) {
        //1:1 人脸识别保存人脸底图
        BitmapUtils.saveDisposedBitmap(disposedBitmap,CACHE_BASE_FACE_DIR,faceID);
        //保存在App 的私有目录，
        FaceEmbedding.saveEmbedding(getBaseContext(),faceID,faceEmbedding);
        updateFaceList();
    }

    /**
     * 加载人脸文件夹CACHE_BASE_FACE_DIR 里面的人脸照片，根据修改时间排序
     */
    private void loadImageList() {
        faceImageList.clear();
        File file = new File(CACHE_BASE_FACE_DIR);
        File[] subFaceFiles = file.listFiles();
        if (subFaceFiles != null) {
            Arrays.sort(subFaceFiles, new Comparator<>() {
                public int compare(File f1, File f2) {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0) return -1;
                    else if (diff == 0) return 0;
                    else return 1;
                }
                public boolean equals(Object obj) {
                    return true;
                }
            });

            for (File fileItem : subFaceFiles) {
                if (!fileItem.isDirectory()) {
                    String fileName = fileItem.getName();
                    String filePath = fileItem.getPath();
                    faceImageList.add(new ImageBean(filePath, fileName));
                }
            }
        }
    }


    /**
     * 加载已经录入的人脸账户列表
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateFaceList();
    }

    private void updateFaceList(){
        loadImageList();
        faceImageListAdapter.notifyDataSetChanged();
    }

    /**
     * 人脸横向列表适配器,
     */
    public class FaceImageListAdapter extends BaseQuickAdapter<ImageBean, BaseViewHolder> {
        public FaceImageListAdapter(List<ImageBean> data) {
            super(R.layout.adapter_face_verify_list_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, ImageBean imageBean) {
            Glide.with(getBaseContext()).load(imageBean.path).skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(new CenterCrop(), new RoundedCorners(15))
                    .into((ImageView) helper.getView(R.id.face_image));
            TextView faceName = helper.getView(R.id.face_name);
            faceName.setText(imageBean.name);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); //关闭页面
        }
        return super.onOptionsItemSelected(item);
    }

}
