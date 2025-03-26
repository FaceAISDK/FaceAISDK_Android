package com.ai.face.search;

import static com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
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
 * USB UVC协议双目摄像头参考{@link com.ai.face.UVCCamera.BinocularUVCCameraActivity} 改造一下
 *
 * 怎么提高人脸搜索的精确度 ？<a href="https://github.com/AnyLifeZLB/FaceVerificationSDK/issues/42">...</a>
 * 尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * 录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * 光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
 * 人脸照片要求300*300（人脸部分区域大于200*200） 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 * <p>
 * <p>
 * 系统相机跑久了也会性能下降，建议测试前重启系统，定时重启
 */
public class FaceSearch1NActivity extends AppCompatActivity {
    //如果设备没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchBinding binding;
    private CameraXFragment cameraXFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class)
                    .putExtra("isAdd", false));
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt("cameraFlag", 0);
        int degree=sharedPref.getInt("cameraDegree", 0);

        /*
         * 1. Camera 的初始化。
         * 第一个参数0/1 指定前后摄像头；
         * 第二个参数linearZoom [0.001f,1.0f] 指定焦距，参考{@link CameraControl#setLinearZoom(float)}
         * 焦距拉远一点，人才会靠近屏幕，才会减轻杂乱背景的影响。定制设备的摄像头自行调教此参数
         *
         * 第三个参数是摄像头旋转角度 {@link Surface.ROTATION_0}
         */
        cameraXFragment = CameraXFragment.newInstance(cameraLensFacing, 0.001f,degree);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        initFaceSearchParam();

    }


    /**
     *
     */
    private void initFaceSearchParam(){

        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setThreshold(0.88f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setImageFlipped(cameraXFragment.getCameraLensFacing() == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, cameraXFragment);
                    }

                    //得分最高的搜索结果
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        Glide.with(getBaseContext())
                                .load(CACHE_SEARCH_FACE_DIR + faceID)
                                .skipMemoryCache(false)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transform(new RoundedCorners(12))
                                .into(binding.searchResult);
                        binding.searchTips.setText(faceID);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showFaceSearchPrecessTips(i);
                    }


                    @Override
                    public void onLog(String log) {
                        binding.tips.setText(log);
                    }

                }).create();


        //3.初始化引擎，是个耗时耗资源操作
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);

        // 4.从标准默认的HAL CameraX 摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上  摄像头RGB 宽动态镜头分辨率720p以上，帧率大于30并且无拖影。
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //可以加个红外检测之类的，有人靠近再启动人脸搜索检索服务，不然机器性能下降机器老化快
            if (!isDestroyed() && !isFinishing()) {
                //runSearch() 方法第二个参数是指圆形人脸框到屏幕边距，有助于加快裁剪图像
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });



    }


    /**
     * 显示提示
     *
     * @param code
     */
    private void showFaceSearchPrecessTips(int code) {
        binding.secondSearchTips.setText("");
        switch (code) {
            default:
                binding.searchTips.setText("Tips Code：" + code);
                break;

            case FACE_TOO_SMALL:
                binding.secondSearchTips.setText(R.string.come_closer_tips);
                break;

            // 单独使用一个textview 提示，防止上一个提示被覆盖。
            // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
            case FACE_TOO_LARGE:
                binding.secondSearchTips.setText(R.string.far_away_tips);
                break;

            //检测到正常的人脸，尺寸大小OK
            case FACE_SIZE_FIT:
                binding.secondSearchTips.setText("");
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
                binding.searchResult.setImageResource(R.drawable.face_logo);
                break;

            case EMGINE_INITING:
                binding.searchTips.setText(R.string.sdk_init);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，没有使用SDK API录入人脸
                binding.searchTips.setText(R.string.face_dir_empty);
                break;

            case NO_MATCHED:
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.searchTips.setText(R.string.no_matched_face);
                binding.searchResult.setImageResource(R.drawable.face_logo);
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