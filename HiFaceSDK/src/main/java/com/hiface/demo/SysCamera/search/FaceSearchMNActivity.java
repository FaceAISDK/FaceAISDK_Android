package com.hiface.demo.SysCamera.search;

import static com.hiface.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.hiface.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.sdk.hiface.search.SearchProcessTipsCode.*;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;
import com.sdk.hiface.base.view.camera.CameraXBuilder;
import com.sdk.hiface.search.FaceSearchEngine;
import com.sdk.hiface.search.FaceSearchResult;
import com.sdk.hiface.search.SearchProcessBuilder;
import com.sdk.hiface.search.SearchProcessCallBack;
import com.tencent.mmkv.MMKV;

import com.hiface.demo.R;
import com.hiface.demo.SysCamera.camera.FaceCameraXFragment;
import com.hiface.demo.base.AbsBaseActivity;
import com.hiface.demo.databinding.ActivityFaceSearchMnBinding;

import java.util.List;

/**
 * M:N 人脸搜索Beta，暂不支持活体检测。请尽量使用1:N人脸搜索
 *
 * 宽动态成像清晰摄像头，人脸正对摄像头
 * 提前在人脸库管理页面 点击右上角导入测试多人脸图，
 * 电脑上打开MN_face_search_test.jpg 手机摄像头对着图片就可以体验多人搜索
 *
 * 本功能要求设备硬件配置高，摄像头品质好。可以拿当前的各品牌手机旗舰机测试验证
 * @author FaceAISDK.Service@gmail.com
 */
@Deprecated
public class FaceSearchMNActivity extends AbsBaseActivity {
    private ActivityFaceSearchMnBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        binding = ActivityFaceSearchMnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.close.setOnClickListener(v -> finish());

        MMKV mmkv = MMKV.defaultMMKV();

        int cameraLensFacing = mmkv.decodeInt( FRONT_BACK_CAMERA_FLAG, 0);
        int degree = mmkv.decodeInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.1f)  //焦距范围[0f,1.0f]，根据应用场景，自行适当调整焦距参数（摄像头需支持变焦）
                .setRotation(degree)   //画面旋转方向
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        // 4.从标准默认的HAL CameraX 摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上,  摄像头RGB 宽动态(大于105Db)高清成像，光线不足设备加补光灯
        cameraXFragment.setOnAnalyzerListener(new FaceCameraXFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
                if (!isDestroyed() && !isFinishing()) {
                    FaceSearchEngine.Companion.getInstance().runSearchWithImageProxy(imageProxy, 0);
                }
            }

            @Override
            public void backImageSize(int imageWidth, int imageHeight) {
                //如果发现人脸框坐标左右镜像了，第三个参数置反一下就可以了
                binding.graphicOverlay.setCameraInfo(imageWidth,imageHeight,cameraXFragment.isFrontCamera());
            }
        });

        // 2.各种参数的初始化设置 （M：N 建议阈值放低）
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(FaceSearchMNActivity.this)
                .setLifecycleOwner(this)
                .setThreshold(0.8f)            //识别成功阈值设置，范围仅限 0.75-0.85 默认0.8
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_M) //1:N 搜索
                .setMirror(cameraLensFacing == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy左右翻转影响人脸框
                .setProcessCallBack(new SearchProcessCallBack() {


                    /**
                     * M：N人脸搜索结果
                     * @param matchedResults  所有大于设置阈值的结果
                     * @param searchBitmap    场景图用于log分析
                     * @param livenessValue   预留字段（M：N 暂无活体检测）
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap, float livenessValue) {
                        binding.graphicOverlay.drawRect(matchedResults); //搜索匹配成功的人脸坐标和Name,Score
                        String json = new Gson().toJson(matchedResults);
                        Log.d("onFaceMatched","符合设定阈值的结果: "+json);
                    }


                    /**
                     * 检测到人脸的位置信息，画框用.
                     * @param detectResult 人脸检测结果
                     */
                    @Override
                    public void onFaceDetected(List<FaceSearchResult> detectResult) {
                        //检测到的人脸坐标
                        binding.graphicOverlay.drawRect(detectResult);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showProcessTips(i);
                    }

                    @Override
                    public void onLog(String log) {
                        binding.message.setText(log);
                    }

                }).create();


        //3.初始化引擎
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);
    }


    /**
     * 显示提示
     */
    private void showProcessTips(int code) {
        switch (code) {
            case SEARCH_INITIALIZING:
                binding.searchTips.setText(R.string.sdk_init);
                break;

            case SEARCH_PREPARED:
                binding.searchTips.setText(R.string.keep_face_tips);
                break;

            case THRESHOLD_ERROR:
                binding.searchTips.setText(R.string.search_threshold_scope_tips);
                break;

            case LOCAL_FACE_DATABASE_EMPTY:
                //人脸库没有录入照片
                binding.searchTips.setText(R.string.local_face_database_empty);
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
