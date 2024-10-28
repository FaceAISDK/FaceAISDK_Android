package com.ai.face.search;


import static com.ai.face.MyFaceApplication.CACHE_SEARCH_FACE_DIR;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.face.R;
import com.ai.face.addFaceImage.AddFaceImageActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.lzf.easyfloat.EasyFloat;

import org.jetbrains.annotations.NotNull;

import com.ai.face.faceSearch.search.FaceSearchImagesManger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 人脸库管理,增删 查，批量添加测试数据
 */
public class FaceSearchImageMangerActivity extends AppCompatActivity {
    private List<String> faceImageList = new ArrayList();
    private FaceImageListAdapter faceImageListAdapter;

    public static final int REQUEST_ADD_FACE_IMAGE = 10086;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_image_manger);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);

        int spanCount = 3;
        int ori = getResources().getConfiguration().orientation; //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 5;
        }
        LinearLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        loadImageList();

        faceImageListAdapter = new FaceImageListAdapter(faceImageList);
        mRecyclerView.setAdapter(faceImageListAdapter);
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            new AlertDialog.Builder(this)
                    .setTitle("确定要删除" + new File(faceImageList.get(i)).getName())
                    .setMessage("删除后对应的人将无法被程序识别")
                    .setPositiveButton("确定", (dialog, which) -> {
                        //删除一张照片
                        FaceSearchImagesManger.IL1Iii.getInstance(getApplication())
                                .deleteFaceImage(faceImageList.get(i));

                        loadImageList();
                        faceImageListAdapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("取消", null).show();

            return false;
        });

        faceImageListAdapter.setEmptyView(R.layout.empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> copyFaceTestImage());

        //添加单张人脸照片
        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("isAdd")) {
            startActivityForResult(new Intent(getBaseContext(), AddFaceImageActivity.class), REQUEST_ADD_FACE_IMAGE);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //去添加一张人脸照片。
        if (requestCode == REQUEST_ADD_FACE_IMAGE && resultCode == RESULT_OK) {
            byte[] bis = data.getByteArrayExtra("picture_data");
            String faceName = data.getStringExtra("picture_name") + ".jpg";
            Bitmap bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);

            Toast.makeText(getBaseContext(), "添加照片", Toast.LENGTH_SHORT).show();
            FaceSearchImagesManger.IL1Iii.getInstance(getApplication()).insertOrUpdateFaceImage(bitmap, CACHE_SEARCH_FACE_DIR + File.separatorChar + faceName);

            String filePathName = CACHE_SEARCH_FACE_DIR + File.separatorChar + faceName;
            FaceSearchImagesManger.IL1Iii.getInstance(getApplication())
                    .insertOrUpdateFaceImage(bitmap, filePathName, new FaceSearchImagesManger.Callback() {
                        @Override
                        public void onSuccess() {
                            loadImageList();
                            faceImageListAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailed(@NotNull String msg) {
                        }

                    });
        }

    }


    /**
     * 加载人脸文件夹CACHE_SEARCH_FACE_DIR 里面的人脸照片
     */
    private void loadImageList() {
        faceImageList.clear();
        File file = new File(CACHE_SEARCH_FACE_DIR);
        File[] subFaceFiles = file.listFiles();
        if (subFaceFiles != null) {
            Arrays.sort(subFaceFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0)
                        return -1;
                    else if (diff == 0)
                        return 0;
                    else
                        return 1;
                }

                public boolean equals(Object obj) {
                    return true;
                }
            });


            for (File value : subFaceFiles) {
                if (!value.isDirectory()) {
                    String filename = value.getName();
                    String filePath = value.getPath();
                    if (filename.trim().toLowerCase().endsWith(".jpg")) {
                        faceImageList.add(filePath);
                    } else if (filename.trim().toLowerCase().endsWith(".png")) {
                        faceImageList.add(filePath);
                    } else if (filename.trim().toLowerCase().endsWith(".jpeg")) {
                        faceImageList.add(filePath);
                    }
                }
            }
        }
    }


    /**
     * 快速拷贝一些测试图，模拟人脸库有多张人脸
     */
    private void copyFaceTestImage() {
        Toast.makeText(getBaseContext(), "复制验证图...", Toast.LENGTH_LONG).show();
        CopyFaceImageUtils.Companion.showAppFloat(getBaseContext());

        CopyFaceImageUtils.Companion.copyTestImage(getApplication(),
                new CopyFaceImageUtils.Companion.Callback() {
                    @Override
                    public void onSuccess() {
                        EasyFloat.hide("speed");
                        loadImageList();
                        faceImageListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailed(@NotNull String msg) {
                    }
                });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            //添加一张
            case R.id.action_add:
                startActivityForResult(new Intent(getBaseContext(), AddFaceImageActivity.class)
                        , REQUEST_ADD_FACE_IMAGE);
                break;

            case R.id.action_add_many:
                //批量添加很多张测试验证人脸图
                copyFaceTestImage();
                break;

            case android.R.id.home:
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 简单的适配器写法
     */
    public class FaceImageListAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
        public FaceImageListAdapter(List<String> data) {
            super(R.layout.adapter_face_image_list_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String faceImagePath) {
            Glide.with(getBaseContext()).load(faceImagePath)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into((ImageView) helper.getView(R.id.face_image));
        }
    }


}
