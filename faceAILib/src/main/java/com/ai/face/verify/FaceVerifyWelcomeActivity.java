package com.ai.face.verify;

import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.ai.face.addFaceImage.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;
import static com.ai.face.verify.FaceVerificationActivity.BASE_FACE_DIR_KEY;
import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.face.R;
import com.ai.face.addFaceImage.AddFaceImageActivity;
import com.ai.face.search.ImageBean;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 提示怎么样使用人脸识别，参考其他App
 */
public class FaceVerifyWelcomeActivity extends AppCompatActivity {
    private List<ImageBean> faceImageList = new ArrayList();
    private FaceImageListAdapter faceImageListAdapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verify_welcome);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        LinearLayout addFaceView = findViewById(R.id.add_faceid_layout);
        addFaceView.setOnClickListener(view -> startActivity(
                new Intent(FaceVerifyWelcomeActivity.this, AddFaceImageActivity.class)
                        .putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_VERIFY.name())
                        .putExtra(BASE_FACE_DIR_KEY, CACHE_BASE_FACE_DIR)));

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
                //删除FaceID
                File file = new File(imageBean.path);
                if (file.delete()) {
                    loadImageList();
                    faceImageListAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplication(), "Delete failed", Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton("取消", null).show();
            return false;
        });


        faceImageListAdapter.setOnItemClickListener((adapter, view, i) -> startActivity(
                new Intent(getBaseContext(), FaceVerificationActivity.class)
                        .putExtra(USER_FACE_ID_KEY, faceImageList.get(i).name) //1:1 底片人脸ID
                        .putExtra(BASE_FACE_DIR_KEY, CACHE_BASE_FACE_DIR) //保存目录
        ));


        faceImageListAdapter.setEmptyView(R.layout.empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> addFaceView.performClick());

    }


    /**
     * 加载已经录入的人脸账户列表
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadImageList();
        faceImageListAdapter.notifyDataSetChanged();
    }

    /**
     * 加载人脸文件夹CACHE_BASE_FACE_DIR 里面的人脸照片
     * 1:1 人脸识别
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
//                    if (filename.trim().toLowerCase().endsWith(".jpg")) {
//                        faceImageList.add(new ImageBean(filePath,filename));
//                    }
                }
            }
        }
    }


    /**
     * 简单的适配器写法
     */
    public class FaceImageListAdapter extends BaseQuickAdapter<ImageBean, BaseViewHolder> {
        public FaceImageListAdapter(List<ImageBean> data) {
            super(R.layout.adapter_face_verify_list_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, ImageBean imageBean) {
            Glide.with(getBaseContext()).load(imageBean.path).skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(new CenterCrop(),new RoundedCorners(15))
                    .into((ImageView) helper.getView(R.id.face_image));

            TextView faceName = helper.getView(R.id.face_name);
            faceName.setText(imageBean.name);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
