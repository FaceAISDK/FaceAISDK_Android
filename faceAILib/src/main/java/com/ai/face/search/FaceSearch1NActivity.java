package com.ai.face.search;

import static com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.*;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;

import com.ai.face.R;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.databinding.ActivityFaceSearchBinding;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;

/**
 * 1:N 人脸搜索「1:N face search」
 *
 * 怎么提高人脸搜索的精确度 ？<a href="https://github.com/AnyLifeZLB/FaceVerificationSDK/issues/42">...</a>
 * 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * 光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
 * 人脸照片要求300*300（人脸部分区域大于200*200） 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 *
 *
 * 系统相机跑久了也会性能下降，建议测试前重启系统，定时重启
 * .setNeedMultiValidate(true) //是否需要确认机制防止误识别，开启会影响低配设备的识别速度
 */
public class FaceSearch1NActivity extends AppCompatActivity {
    //如果设备没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class)
                    .putExtra("isAdd",false));
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);

        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 1));

        /*
         * 1. Camera 的初始化。
         * 第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.001f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         * 焦距拉远一点，人才会靠近屏幕，才会减轻杂乱背景的影响。定制设备的摄像头自行调教此参数
         */
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens, 0.001f);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();



        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setThreshold(0.88f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
                .setNeedMultiValidate(false) //是否需要确认机制防止误识别，低配置设备影响搜索速度
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setImageFlipped(cameraLens == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
//                .setLicenseKey("FaceAIVIPLicense")
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, cameraXFragment);
                    }

                    //人脸搜索最像的结果
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        binding.searchTips.setText(faceID);
                        Glide.with(getBaseContext())
                                .load(CACHE_SEARCH_FACE_DIR + faceID)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transform(new RoundedCorners(12))
                                .into(binding.image);
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


        //3.初始化引擎，是个耗时耗资源操作
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);


        // 4.从摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上  摄像头RGB 宽动态镜头分辨率720p以上，帧率大于30并且无拖影。
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {

            //可以加个红外检测之类的，有人靠近再启动人脸搜索检索服务，不然机器性能下降机器老化快
            if (!isDestroyed() && !isFinishing()) {
                //runSearch() 方法第二个参数是指圆形人脸框到屏幕边距，有助于加快裁剪图像
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });



        //其他方式搜索，把数据转为Bitmap 去搜索。延时1秒是为了让引擎初始化完毕
        new Handler().postDelayed(() -> {
            //5.静态人脸图搜索(对应的),延迟1秒是为了防止引擎初始化没有完成
//            Bitmap searchBmp= VerifyUtils.getBitmapFromAssert(FaceSearch1NActivity.this, "v3_0835054.jpg");
//            FaceSearchEngine.Companion.getInstance().runSearch(searchBmp);
        },1000);


    }


    /**
     * 显示提示
     *
     * @param code
     */
    private void showPrecessTips(int code) {
        binding.image.setImageResource(R.drawable.face_logo);
        switch (code) {
            default:
                binding.searchTips.setText("Tips Code：" + code);
                break;
            case FACE_TOO_SMALL:
                binding.searchTips.setText(R.string.come_closer_tips);

                break;
            case TOO_MUCH_FACE:
                Toast.makeText(this, R.string.multiple_faces_tips, Toast.LENGTH_SHORT).show();
                break;

            case THRESHOLD_ERROR:
                binding.searchTips.setText(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                binding.searchTips.setText(R.string.no_mask_please);
                break;

            case NO_LIVE_FACE:
                binding.searchTips.setText(R.string.no_face_detected_tips);
                break;

            case EMGINE_INITING:
                binding.searchTips.setText(R.string.sdk_init);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有录入照片
                binding.searchTips.setText(R.string.face_dir_empty);
                break;

            case NO_MATCHED:
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.searchTips.setText(R.string.no_matched_face);
                break;

            case SEARCHING:
                binding.searchTips.setText("");
                break;

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