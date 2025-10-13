package com.faceAI.demo.SysCamera.search;

import static com.faceAI.demo.FaceAISettingsActivity.UVC_CAMERA_TYPE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;
import static com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.face.core.utils.FaceAICameraType;
import com.faceAI.demo.SysCamera.verify.AbsAddFaceFromAlbumActivity;
import com.faceAI.demo.UVCCamera.addFace.AddFace_UVCCameraActivity;
import com.faceAI.demo.UVCCamera.addFace.AddFace_UVCCameraFragment;
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity;
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.lzf.easyfloat.EasyFloat;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.faceAI.demo.R;

/**
 * 人脸库管理,增删 查，批量添加测试数据
 * 一定要用SDK API 进行添加删除，不要直接File 接口文件添加删除，不然无法同步人脸SDK中特征值的更新
 * <p>
 * 本页面仅仅为演示API功能，Demo并无性能优化等处理
 * 网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
 *
 * @author FaceAISDK.service@gmail.com
 */
public class FaceSearchImageMangerActivity extends AbsAddFaceFromAlbumActivity {
    private final List<ImageBean> faceImageList = new ArrayList<>();
    private FaceImageListAdapter faceImageListAdapter;
    public static final int REQUEST_ADD_FACE_IMAGE = 10086;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_image_manger);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        int spanCount = 3;
        int ori = getResources().getConfiguration().orientation;
        //横屏每行显示5张图，竖屏每行3张
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 5;
        }
        LinearLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        faceImageListAdapter = new FaceImageListAdapter(faceImageList);
        mRecyclerView.setAdapter(faceImageListAdapter);

        //删除本地的人脸照片和对应的特征值，删除后对应的人将无法被程序识别
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            ImageBean imageBean = faceImageList.get(i);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.sure_delete_face_title) + imageBean.name+"?")
                    .setMessage(R.string.sure_delete_face_tips)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        FaceSearchImagesManger.Companion.getInstance(getApplication()).deleteFaceImage(imageBean.path);
                        updateFaceList();
                    })
                    .setNegativeButton(R.string.cancel, null).show();
            return false;
        });

        faceImageListAdapter.setEmptyView(R.layout.empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> copyFaceTestImage());

        TextView tips = findViewById(R.id.tips);
        tips.setOnLongClickListener(v -> {
            new AlertDialog.Builder(FaceSearchImageMangerActivity.this)
                    .setTitle("Delete All Face Images？")
                    .setMessage("Are you sure to delete all face images? dangerous operation!")
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        FaceSearchImagesManger.Companion.getInstance(getApplication()).clearFaceImages(CACHE_SEARCH_FACE_DIR);
                        updateFaceList();
                    })
                    .setNegativeButton(R.string.cancel, null).show();
            return false;
        });

        //添加人脸照片，UVC协议摄像头添加还是普通的系统相机
        if (getIntent().getExtras().getBoolean("isAdd")) {
            SharedPreferences sharedPref =getSharedPreferences("FaceAISDK_SP", MODE_PRIVATE);
            int cameraType = sharedPref.getInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA);

            Intent addFaceIntent;
            if (cameraType==FaceAICameraType.SYSTEM_CAMERA) {
                addFaceIntent = new Intent(getBaseContext(), AddFaceImageActivity.class);
                addFaceIntent.putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_SEARCH.name());
            } else {
                addFaceIntent = new Intent(getBaseContext(), AddFace_UVCCameraActivity.class);
                addFaceIntent.putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFace_UVCCameraFragment.AddFaceImageTypeEnum.FACE_SEARCH.name());
            }
            startActivityForResult(addFaceIntent, REQUEST_ADD_FACE_IMAGE);
        }
    }

    /**
     * 相册选择的照片,裁剪等处理好数据后返回了
     */
    @Override
    public void disposeSelectImage(@NotNull String faceID, @NotNull Bitmap disposedBitmap, @NonNull float[] faceEmbedding) {
        //人脸搜索(1:N ，M：N )保存人脸
        String filePathName = CACHE_SEARCH_FACE_DIR + faceID;
        //一定要用SDK API 进行添加删除，不要直接File 接口文件添加删除，不然无法同步人脸SDK中特征值的更新
        FaceSearchImagesManger.Companion.getInstance(getApplication())
                .insertOrUpdateFaceImage(disposedBitmap, filePathName, new FaceSearchImagesManger.Callback() {
                    @Override
                    public void onSuccess(@NonNull Bitmap bitmap, @NonNull float[] faceEmbedding) {
                        updateFaceList();
                        Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(@NotNull String msg) {
                        Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
                    }
                });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 刷新人脸照片列表
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateFaceList();
    }

    /**
     * 重新刷新加载人脸图，Demo 为了演示代码少就不做加载性能优化了
     */
    private void updateFaceList() {
        loadImageList();
        faceImageListAdapter.notifyDataSetChanged();
    }

    /**
     * 加载人脸文件夹CACHE_SEARCH_FACE_DIR 里面的人脸照片
     */
    private void loadImageList() {
        faceImageList.clear();
        File file = new File(CACHE_SEARCH_FACE_DIR);
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

            //最好判断一下文件类型
            for (File value : subFaceFiles) {
                if (!value.isDirectory()) {
                    String filename = value.getName();
                    String filePath = value.getPath();
                    faceImageList.add(new ImageBean(filePath, filename));
                }
            }
            Toast.makeText(getBaseContext(), "Face size：" + faceImageList.size(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 快速复制工程目录 ./app/src/main/assert目录下200+张 人脸图入库，用于测试验证
     * 人脸图规范要求 大于 300*300的光线充足无遮挡的正面人脸如（./images/face_example.jpg)
     */
    private void copyFaceTestImage() {
        Toast.makeText(getBaseContext(), "Copying Test Faces", Toast.LENGTH_LONG).show();
        CopyFaceImageUtils.Companion.showAppFloat(getBaseContext());

        CopyFaceImageUtils.Companion.copyTestFaceImage(getApplication(), new CopyFaceImageUtils.Companion.Callback() {
            @Override
            public void onSuccess() {
                EasyFloat.hide("speed");
                updateFaceList();
            }

            @Override
            public void onFailed(@NotNull String msg) {
                Toast.makeText(getBaseContext(), "Failed：" + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 右上角加三种方式添加人脸
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();//添加一张
        if (itemId == R.id.camera_add) {
            Intent addFaceIntent = new Intent(getBaseContext(), AddFaceImageActivity.class);
            addFaceIntent.putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_SEARCH.name());
            startActivityForResult(addFaceIntent, REQUEST_ADD_FACE_IMAGE);
        } else if (itemId == R.id.assert_add) {//批量添加很多张测试验证人脸图
            copyFaceTestImage();
        } else if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.photo_add) {
            chooseFaceImage();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 简单的图片列表适配器写法
     */
    public class FaceImageListAdapter extends BaseQuickAdapter<ImageBean, BaseViewHolder> {
        public FaceImageListAdapter(List<ImageBean> data) {
            super(R.layout.adapter_face_image_list_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, ImageBean imageBean) {
            Glide.with(getBaseContext()).load(imageBean.path)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into((ImageView) helper.getView(R.id.face_image));
            TextView faceName = helper.getView(R.id.face_name);
            faceName.setText(imageBean.name);
        }
    }


}
