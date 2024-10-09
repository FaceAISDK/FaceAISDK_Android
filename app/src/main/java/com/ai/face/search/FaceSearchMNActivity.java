package com.ai.face.search;

import static com.ai.face.MyFaceApplication.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.EMGINE_INITING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_DIR_EMPTY;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.MASK_DETECTION;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_LIVE_FACE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_MATCHED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCHING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;

import com.ai.face.R;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.databinding.ActivityFaceSearchMnBinding;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;

import java.util.List;


/**
 * 应多位用户要求，默认使用java 版本演示怎么快速接入SDK
 *
 * 1：N 和 M：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 */
public class FaceSearchMNActivity extends AppCompatActivity {
    private ActivityFaceSearchMnBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFaceSearchMnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class));
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);

        /**
         * 1. Camera 的初始化。第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.001f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         */
        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0));
        CameraXFragment cameraX = CameraXFragment.newInstance(cameraLens, 0.01f); //参数1，前后摄像头 2是焦距
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraX)
                .commit();


        cameraX.setOnAnalyzerListener(imageProxy -> {
            //可以加个红外检测之类的，有人靠近再启动检索服务，不然机器老化快
            if (!isDestroyed() && !isFinishing()) {
                //MN 检索，第二个参数就不要裁剪了
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });


        // 2.各种参数的初始化设置 （M：N 建议阈值放低）
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setThreshold(0.80f)            //识别成功阈值设置，范围仅限 0.8-0.95！默认0.8
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_M) //1:N 搜索
                .setImageFlipped(cameraLens == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
                .setProcessCallBack(new SearchProcessCallBack() {

                    //坐标框和对应的 搜索匹配到的图片标签
                    //人脸检测成功后画白框，此时还没有标签字段
                    //人脸搜索匹配成功后白框变绿框，并标记出对应的人脸ID Label（建议用唯一ID 命名人脸图片）
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, cameraX);

                        if(!result.isEmpty()) {
                            binding.searchTips.setText("");
                        }
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showPrecessTips(i);
                    }

                    @Override
                    public void onLog(String log) {
                        binding.tips.setText(log);
                    }

                }).create();


        //3.初始化r引擎
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);
    }


    /**
     * 显示提示
     *
     * @param code
     */
    private void showPrecessTips(int code) {
        binding.searchTips.setText("提示码：$code");

        switch (code) {

            case THRESHOLD_ERROR:
                binding.searchTips.setText("识别阈值Threshold范围为0.8-0.95");
                break;

            case MASK_DETECTION:
                binding.searchTips.setText("请摘下口罩"); //默认无口罩检测
                break;

            case NO_LIVE_FACE:
                binding.searchTips.setText("未检测到人脸");
                break;

            case EMGINE_INITING:
                binding.searchTips.setText("初始化中");
                break;

            case FACE_DIR_EMPTY:
                binding.searchTips.setText("人脸库为空");
                break;

            case NO_MATCHED:
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.searchTips.setText("没有匹配项");
                break;

            case SEARCHING: {
                binding.searchTips.setText("");
                break;
            }

        }
    }


    /**
     * 销毁，停止
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }


}