package com.faceAI.demo.SysCamera.search;

import static com.faceAI.demo.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.EMGINE_INITING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_DIR_EMPTY;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_SMALL;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.MASK_DETECTION;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_LIVE_FACE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_MATCHED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCHING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.TOO_MUCH_FACE;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.faceAI.demo.databinding.ActivityFaceSearchMnBinding;
import com.faceAI.demo.R;
import java.util.List;

/**
 * 应用场景比较少，暂时不显示到Demo App ，可自行打开
 *
 * <p>
 * M：N 人脸搜索「M：N Face Search」，建议优先试用1:N，整个业务流程稳定后再考虑升级到M：N
 * 系统相机跑久了也会性能下降，建议测试前重启系统，并定时重启
 * <p>
 * 本功能要求设备硬件配置高，摄像头品质好。可以拿当前的各品牌手机旗舰机测试验证
 */
@Deprecated
public class FaceSearchMNActivity extends AppCompatActivity {
    //如果设备没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchMnBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFaceSearchMnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class)
                    .putExtra("isAdd", false));
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);

        int cameraLensFacing = sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.001f) //焦距范围[0.001f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)   //画面旋转方向
                .setSize(CameraXFragment.SIZE.DEFAULT) //相机的分辨率大小。分辨率越大画面中人像很小也能检测但是会更消耗CPU
                .create();

        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraXBuilder);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        //建议设备配置 CPU为八核64位2.4GHz以上  摄像头RGB 宽动态镜头分辨率720p以上，帧率大于30并且无拖影。
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //可以加个红外检测之类的，有人靠近再启动人脸搜索检索服务，不然机器性能下降机器老化快
            if (!isDestroyed() && !isFinishing()) {
                //MN 人脸检索，第二个参数0 画面识别区域就不裁剪了
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });

        // 2.各种参数的初始化设置 （M：N 建议阈值放低）
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(FaceSearchMNActivity.this)
                .setLifecycleOwner(this)
                .setThreshold(0.85f)            //识别成功阈值设置，范围仅限 0.85-0.95！默认0.85
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_M) //1:N 搜索
                .setImageFlipped(cameraLensFacing == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
                .setProcessCallBack(new SearchProcessCallBack() {

                    //坐标框和对应的 搜索匹配到的图片标签
                    //人脸检测成功后画白框，此时还没有标签字段
                    //人脸搜索匹配成功后白框变绿框，并标记出对应的人脸ID Label（建议用唯一ID 命名人脸图片）
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, cameraXFragment);
                        if (!result.isEmpty()) {
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
        switch (code) {
            case FACE_TOO_SMALL:
                Toast.makeText(this, R.string.come_closer_tips, Toast.LENGTH_SHORT).show();
                break;
            case TOO_MUCH_FACE:
                Toast.makeText(this, R.string.multiple_faces_tips, Toast.LENGTH_SHORT).show();
                break;

            case THRESHOLD_ERROR:
                binding.searchTips.setText(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                binding.searchTips.setText(R.string.no_mask_please); //默认无
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

            default:
                binding.searchTips.setText("Tips Code：" + code);
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