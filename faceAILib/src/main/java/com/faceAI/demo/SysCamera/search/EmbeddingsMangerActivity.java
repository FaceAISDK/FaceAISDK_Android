package com.faceAI.demo.SysCamera.search;

import static com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity;
import com.faceAI.demo.base.AbsBaseActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 人脸搜索特征向量管理，增删改查 演示
 *
 * 以图片的方式管理，更新都比较麻烦，部分场合设备要去不能保存人脸图片用特征向量管理更合规
 * @author FaceAISDK.Service@gmail.com
 */
public class EmbeddingsMangerActivity extends AbsBaseActivity {
    private final List<ImageBean> faceImageList = new ArrayList<>();
    private FaceImageListAdapter faceImageListAdapter;
    public static final int REQUEST_ADD_FACE_IMAGE = 10086;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_image_manger);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        faceImageListAdapter = new FaceImageListAdapter(faceImageList);
        mRecyclerView.setAdapter(faceImageListAdapter);

        //删除本地的人脸照片和对应的特征值，删除后对应的人将无法被程序识别
        faceImageListAdapter.setOnItemLongClickListener((adapter, view, i) -> {
            ImageBean imageBean = faceImageList.get(i);
            new AlertDialog.Builder(this)
                    .setTitle("确定要删除" + imageBean.name)
                    .setMessage("删除后对应的人将无法被程序识别")
                    .setPositiveButton("确定", (dialog, which) -> {


                        loadImageList();
                        faceImageListAdapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("取消", null).show();
            return false;
        });

        faceImageListAdapter.setEmptyView(R.layout.empty_layout);
        faceImageListAdapter.getEmptyLayout().setOnClickListener(v -> copyFaceTestImage());

        TextView tips = findViewById(R.id.tips);
        tips.setOnLongClickListener(v -> {
            new AlertDialog.Builder(EmbeddingsMangerActivity.this)
                    .setTitle("确定要删除所有人脸数据？")
                    .setMessage("删除后设备本地所有人脸数据将被清除，请谨慎操作")
                    .setPositiveButton("确定", (dialog, which) -> {


                        loadImageList();
                        faceImageListAdapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("取消", null).show();
            return false;
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
        loadImageList();
        faceImageListAdapter.notifyDataSetChanged();
    }

    /**
     * 查询本地数据库中的人脸特征向量数据
     */
    private void loadImageList() {

    }


    /**
     * 快速复制工程目录 ./app/src/main/assert目录下200+张 人脸图入库，用于测试验证
     * 人脸图规范要求 大于 300*300的光线充足无遮挡的正面人脸如（./images/face_example.jpg)
     */
    private void copyFaceTestImage() {

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
            Glide.with(getBaseContext()).load(imageBean.path).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into((ImageView) helper.getView(R.id.face_image));
            TextView faceName = helper.getView(R.id.face_name);
            faceName.setText(imageBean.name);
        }
    }


}
