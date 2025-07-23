package com.faceAI.demo.SysCamera.search;

import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCH_PREPARED;
import static com.faceAI.demo.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.EMGINE_INITING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_DIR_EMPTY;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_SIZE_FIT;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_LARGE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_SMALL;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.MASK_DETECTION;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_LIVE_FACE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_MATCHED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCHING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.TOO_MUCH_FACE;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;

import com.faceAI.demo.AboutFaceAppActivity;
import com.faceAI.demo.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.camera.core.CameraSelector;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.faceAI.demo.base.BaseActivity;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.databinding.ActivityFaceSearchBinding;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 1:N 人脸搜索识别「1:N face search」
 *
 * 1.  使用的宽动态（大于110DB）高清抗逆光摄像头；**保持镜头整洁干净（会粘指纹油污的用纯棉布擦拭干净）**
 * 2.  录入高质量的人脸图，如（images/face\_example.jpg）（证件照输入目前优化中）
 * 3.  光线环境好否则加补光灯，人脸无遮挡，没有化浓妆 或 粗框眼镜墨镜、口罩等大面积遮挡
 * 4.  人脸图大于 300*300（人脸部分区域大于200*200）五官清晰无遮挡，图片不能有多人脸
 *
 * 怎么提高人脸搜索识别系统的准确度？https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
 *
 * 网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
 * 可复制工程目录 ./faceAILib/src/main/assert 下后在Demo 的人脸库管理页面一键导入模拟插入多张人脸图
 */
public class FaceSearch1NActivity extends BaseActivity {
    //如果设备在弱光环境没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchBinding binding;
    private CameraXFragment cameraXFragment;
    private boolean enginePrepared=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.close.setOnClickListener(v -> finish());

        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceSearchImageMangerActivity.class)
                    .putExtra("isAdd", false));
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //1. 摄像头相关参数配置
        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0f) //焦距范围[0f,1.0f]，参考 {@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)   //画面旋转方向
                .setSize(CameraXFragment.SIZE.DEFAULT) //相机的分辨率大小。分辨率越大画面中人像很小也能检测但是会更消耗CPU
                .create();

        cameraXFragment = CameraXFragment.newInstance(cameraXBuilder);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        initFaceSearchParam();

    }


    /**
     * 初始化人脸搜索参数
     */
    private void initFaceSearchParam() {

        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setThreshold(0.88f) //阈值范围限 [0.85 , 0.95] 识别可信度，阈值高摄像头成像品质宽动态值也要高
                .setCallBackAllMatch(true) //默认是false,是否返回所有的大于设置阈值的搜索结果
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setImageFlipped(cameraXFragment.getCameraLensFacing() == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
                .setProcessCallBack(new SearchProcessCallBack() {

                    // 得分最高最相似的人脸搜索识别结果
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        Bitmap mostSimilarBmp = BitmapFactory.decodeFile(CACHE_SEARCH_FACE_DIR + faceID);
                        new ImageToast().show(getApplicationContext(), mostSimilarBmp, faceID.replace(".jpg"," ")+score);
                        VoicePlayer.getInstance().play(R.raw.success);
                        binding.graphicOverlay.clearRect();
                    }

                    /**
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     * 但还是强烈建议使用高品质摄像头，录入高品质人脸
                     * SearchProcessBuilder setCallBackAllMatch(true) onFaceMatched才会回调
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
                        //已经按照降序排列，可以弹出一个列表框
                        Log.d("onFaceMatched","符合设定阈值的结果: "+matchedResults.toString());
                    }

                    /**
                     * 检测到人脸的位置信息，画框用
                     * @param result
                     */
                    @Override
                    public void onFaceDetected(List<FaceSearchResult> result) {
                        //画框UI代码完全开放，用户可以根据情况自行改造
                        binding.graphicOverlay.drawRect(result, cameraXFragment);
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


        //3.根据参数初始化引擎
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);

        // 4.从标准默认的HAL CameraX 摄像头中取数据实时搜索
        // 建议设备配置 CPU为八核64位2.4GHz以上,  摄像头RGB 宽动态(大于105Db)高清成像，光线不足设备加补光灯
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
            if (!isDestroyed() && !isFinishing()&&enginePrepared) {
                //runSearch() 方法第二个参数是指圆形人脸框到屏幕边距，有助于加快裁剪图像
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });


        //模拟自行管理摄像头采集人脸转为Bitmap持续送入到SDK引擎中（单帧图SDK限制不会返回结果）
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                //0a_Search 放在Asset 并提前通过Demo导入该目录人脸入库
//                Bitmap bitmap = BitmapUtils.getBitmapFromAsset(FaceSearch1NActivity.this, "0a_Search.png");
//                FaceSearchEngine.Companion.getInstance().runSearch(bitmap); //不要在主线程调用
//            }
//        }, 200, 1000);

    }


    /**
     * 显示人脸搜索识别提示，根据Code码显示对应的提示,用户根据自己业务处理细节
     *
     * @param code 提示Code码
     */
    private void showFaceSearchPrecessTips(int code) {
        binding.secondSearchTips.setText("");
        switch (code) {
            case NO_MATCHED:
                //没有搜索匹配识别到任何人
                binding.secondSearchTips.setText(R.string.no_matched_face);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，没有使用SDK API插入人脸？
                binding.searchTips.setText(R.string.face_dir_empty);
                break;

            case EMGINE_INITING:
                binding.searchTips.setText(R.string.sdk_init);
                break;

            case SEARCH_PREPARED:
                //initSearchParams 后引擎需要加载人脸库等初始化，完成会回调
                enginePrepared=true;
                binding.searchTips.setText(R.string.keep_face_tips);
                break;

            case  SEARCHING:
                binding.searchTips.setText(R.string.keep_face_tips);
                break;

            case NO_LIVE_FACE:
                binding.searchTips.setText(R.string.no_face_detected_tips);
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

            default:
                binding.searchTips.setText("回调提示：" + code);
                break;

        }
    }

    /**
     * 销毁，停止人脸搜索
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }

}