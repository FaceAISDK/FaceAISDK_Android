package com.ai.face.search;

import static com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.addFaceImage.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.face.R;
import com.ai.face.addFaceImage.AddFaceImageActivity;
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

/**
 * 人脸库管理,增删 查，批量添加测试数据
 */
public class FaceSearchImageMangerActivity extends AppCompatActivity {
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
        int ori = getResources().getConfiguration().orientation; //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 5;
        }
        LinearLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        loadImageList();

        faceImageListAdapter = new FaceImageListAdapter(faceImageList);
        mRecyclerView.setAdapter(faceImageListAdapter);
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            ImageBean imageBean = faceImageList.get(i);

            new AlertDialog.Builder(this).setTitle("确定要删除"
                    + imageBean.name).setMessage("删除后对应的人将无法被程序识别").setPositiveButton("确定", (dialog, which) -> {

                //删除一张照片
                FaceSearchImagesManger.Companion.getInstance(getApplication()).deleteFaceImage(imageBean.path);

                loadImageList();
                faceImageListAdapter.notifyDataSetChanged();
            }).setNegativeButton("取消", null).show();

            return false;
        });

        faceImageListAdapter.setEmptyView(R.layout.empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> copyFaceTestImage());

        //添加单张人脸照片
        if (getIntent().getExtras().getBoolean("isAdd")) {
            Intent addFaceIntent = new Intent(getBaseContext(), AddFaceImageActivity.class);
            addFaceIntent.putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_SEARCH.name());
            startActivityForResult(addFaceIntent, REQUEST_ADD_FACE_IMAGE);
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

            String filePathName = CACHE_SEARCH_FACE_DIR + faceName;

            /*
             *  人脸图要求：
             *  1.尽量使用较高配置设备和摄像头，光线不好带上补光灯
             *  2.录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
             *  3.光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
             *  4.人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
             *
             */
            FaceSearchImagesManger.Companion.getInstance(getApplication()).insertOrUpdateFaceImage(bitmap, filePathName, new FaceSearchImagesManger.Callback() {
                @Override
                public void onSuccess() {
                    loadImageList();
                    faceImageListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFailed(@NotNull String msg) {
                    Toast.makeText(getBaseContext(), "人脸图入库失败：：" + msg, Toast.LENGTH_SHORT).show();
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
                    if (diff > 0) return -1;
                    else if (diff == 0) return 0;
                    else return 1;
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
                        faceImageList.add(new ImageBean(filePath, filename));
                    } else if (filename.trim().toUpperCase().endsWith(".png")) {
                        faceImageList.add(new ImageBean(filePath, filename));
                    } else if (filename.trim().toUpperCase().endsWith(".jpeg")) {
                        faceImageList.add(new ImageBean(filePath, filename));
                    }
                }
            }

            Toast.makeText(getBaseContext(), "人脸库容量：" + faceImageList.size(), Toast.LENGTH_SHORT).show();

        }
    }


    /**
     * 快速复制工程目录 ./app/src/main/assert目录下200+张 人脸图入库
     * 人脸图规范要求 大于 300*300的光线充足无遮挡的正面人脸如（./images/face_example.jpg)
     */
    private void copyFaceTestImage() {
        Toast.makeText(getBaseContext(), "复制验证图...", Toast.LENGTH_LONG).show();
        CopyFaceImageUtils.Companion.showAppFloat(getBaseContext());

        CopyFaceImageUtils.Companion.copyTestFaceImage(getApplication(), new CopyFaceImageUtils.Companion.Callback() {
            @Override
            public void onSuccess() {
                EasyFloat.hide("speed");
                loadImageList();
                faceImageListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(@NotNull String msg) {
                Toast.makeText(getBaseContext(), "人脸图入库失败：：" + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();//添加一张
        if (itemId == R.id.action_add) {
            Intent addFaceIntent = new Intent(getBaseContext(), AddFaceImageActivity.class);
            addFaceIntent.putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_SEARCH.name());
            startActivityForResult(addFaceIntent, REQUEST_ADD_FACE_IMAGE);
        } else if (itemId == R.id.action_add_many) {//批量添加很多张测试验证人脸图
            copyFaceTestImage();
        } else if (itemId == android.R.id.home) {
            finish();
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
    public class FaceImageListAdapter extends BaseQuickAdapter<ImageBean, BaseViewHolder> {
        public FaceImageListAdapter(List<ImageBean> data) {
            super(R.layout.adapter_face_image_list_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, ImageBean imageBean) {
            Glide.with(getBaseContext()).load(imageBean.path).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into((ImageView) helper.getView(R.id.face_image));

            TextView faceName = (TextView) helper.getView(R.id.face_name);
            faceName.setText(imageBean.name);
        }
    }


}
