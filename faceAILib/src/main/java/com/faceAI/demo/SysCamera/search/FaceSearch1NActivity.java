package com.faceAI.demo.SysCamera.search;

import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCH_PREPARED;
import static com.faceAI.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;
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

import com.ai.face.core.utils.FaceAICameraType;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.camera.core.CameraSelector;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.faceAI.demo.SysCamera.camera.MyCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.databinding.ActivityFaceSearchBinding;
import java.util.List;

/**
 * 1:N 人脸搜索识别「1:N face search」，https://github.com/FaceAISDK/FaceAISDK_Android
 * <p>
 * 1. 使用的宽动态（室内大于105DB,室外大于120DB）高清抗逆光摄像头；**保持镜头整洁干净（汗渍 油污）**
 * 2. 录入高质量清晰正脸图，脸部清晰
 * 3. 光线环境好否则加补光灯，人脸无遮挡，没有化浓妆 或 粗框眼镜墨镜、口罩等大面积遮挡
 * 4. 人脸图大于 300*300（人脸部分区域大于200*200）五官清晰无遮挡，图片不能有多人脸
 * <p>
 * 怎么提高人脸搜索识别系统的准确度？https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
 * <p>
 * 网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
 * 可复制到工程目录 ./faceAILib/src/main/assert 的人脸库,在管理页面一键导入模拟插入多张人脸图
 * <p>
 * 摄像头管理源码开放在 {@link MyCameraXFragment}
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceSearch1NActivity extends AbsBaseActivity {
    //如果设备在弱光环境没有补光灯，UI界面背景多一点白色的区域，利用屏幕的光作为补光
    private ActivityFaceSearchBinding binding;
    private MyCameraXFragment cameraXFragment; //可以使用开放的摄像头管理源码MyCameraFragment，自行管理摄像头
    private boolean pauseSearch =false; //控制是否送数据到SDK进行搜索
    private int cameraLensFacing;

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

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT); //默认前置
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //1. 摄像头相关参数配置
        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.001f)     //焦距范围[0f,1.0f]，参考 {@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)   //画面旋转方向
                .create();

        //可以不用SDK 内部相机管理，自定义摄像头参考MyCameraFragment，源码开放自由修改
        cameraXFragment = MyCameraXFragment.newInstance(cameraXBuilder);
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
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
                .setThreshold(0.85f) //阈值范围限 [0.85 , 0.95] 识别可信度，阈值高摄像头成像品质宽动态值以及人脸底片质量也要高
                .setCallBackAllMatch(true) //默认是false,是否返回所有的大于设置阈值的搜索结果
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setSearchIntervalTime(1900) //默认2000，范围[1500,3000]毫秒。搜索成功后的继续下一次搜索的间隔时间，不然会一直搜索一直回调结果
                .setMirror(cameraLensFacing == CameraSelector.LENS_FACING_FRONT) //后面版本去除次参数
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * onMostSimilar 是返回搜索到最相似的人脸，有可能光线弱，人脸底片不合规导致错误匹配
                     * 业务上可以添加容错处理，onFaceMatched会返回所有大于设置阈值的结果并排序好
                     *
                     * 强烈建议使用支持宽动态的高品质摄像头，录入高品质人脸
                     * SearchProcessBuilder setCallBackAllMatch(true) onFaceMatched才会回调
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
                        //已经按照降序排列，可以弹出一个列表框
                        Log.d("onFaceMatched","符合设定阈值的结果: "+matchedResults.toString());
                    }

                    /**
                     * 最相似的人脸搜索识别结果，得分最高
                     * @param faceID  人脸ID
                     * @param score   相似度值
                     * @param bitmap  场景图，可以用来做使用记录log
                     */
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        Bitmap mostSimilarBmp = BitmapFactory.decodeFile(CACHE_SEARCH_FACE_DIR + faceID);
                        new ImageToast().show(getApplicationContext(), mostSimilarBmp, faceID.replace(".jpg"," ")+score);
                        VoicePlayer.getInstance().play(R.raw.success);
                        binding.graphicOverlay.clearRect();
                    }

                    /**
                     * 返回的人脸光线亮度，如果摄像头不支持宽动态（室内105db,室外120db），请硬件添加自动补光感应灯
                     * @param brightness
                     */
                    @Override
                    public void onFaceBrightness(float brightness) {
                        //测试阶段，先在测试模式打开提示，大约11月中旬正式发布
                        if(FaceSDKConfig.isDebugMode(getBaseContext())){
                            if(brightness>180){
                                Toast.makeText(getBaseContext(),"光线过亮:"+brightness,Toast.LENGTH_SHORT).show();
                            }else if(brightness<80){
                                Toast.makeText(getBaseContext(),"光线过暗:"+brightness,Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    /**
                     * 检测到人脸的位置信息，画框用
                     */
                    @Override
                    public void onFaceDetected(List<FaceSearchResult> result) {
                        //画框UI代码完全开放，用户可以根据情况自行改造
                        binding.graphicOverlay.drawRect(result, cameraXFragment.getScaleX(),cameraXFragment.getScaleY());
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
            if (!isDestroyed() && !isFinishing()&&!pauseSearch) {
                //runSearch() 方法第二个参数是指圆形人脸框到屏幕边距，有助于加快裁剪图像
                FaceSearchEngine.Companion.getInstance().runSearchWithImageProxy(imageProxy, 0);
            }
        });


        //模拟自行管理摄像头采集人脸转为Bitmap持续送入到SDK引擎中（单帧图SDK限制不会返回结果）
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                //0a_Search 放在Asset 并提前通过Demo导入该目录人脸入库,模拟持续获取摄像头数据传入SDK
//                Bitmap bitmap = BitmapUtils.getBitmapFromAsset(FaceSearch1NActivity.this, "0a_Search.png");
//                FaceSearchEngine.Companion.getInstance().runSearchWithBitmap(bitmap); //不要在主线程调用
//            }
//        }, 200, 1000);

    }


    /**
     * 显示人脸搜索识别提示，根据Code码显示对应的提示,用户根据自己业务处理细节
     *
     * @param code 提示Code码
     */
    private void showFaceSearchPrecessTips(int code) {
//        binding.secondSearchTips.setText("");
        switch (code) {
            case NO_MATCHED:
                //本次没有搜索匹配到结果，下一帧继续
                setSecondTips(R.string.no_matched_face);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，使用SDK API插入人脸
                setSearchTips(R.string.face_dir_empty);
                Toast.makeText(this, R.string.face_dir_empty, Toast.LENGTH_LONG).show();
                break;

            case EMGINE_INITING:
                setSearchTips(R.string.sdk_init);
                break;

            case SEARCH_PREPARED:
                //initSearchParams 后引擎需要加载人脸库等初始化，完成会回调
                setSearchTips(R.string.keep_face_tips);
                break;

            case  SEARCHING:
                //后期将废除本状态
                setSearchTips(R.string.keep_face_tips);
                break;

            case NO_LIVE_FACE:
                setSearchTips(R.string.no_face_detected_tips);
                break;

            case FACE_TOO_SMALL:
                setSecondTips(R.string.come_closer_tips);
                break;

            // 单独使用一个textview 提示，防止上一个提示被覆盖。
            // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
            case FACE_TOO_LARGE:
                setSecondTips(R.string.far_away_tips);
                break;

            //检测到正常的人脸，尺寸大小OK
            case FACE_SIZE_FIT:
                setSecondTips(0);
                break;

            case TOO_MUCH_FACE:
                Toast.makeText(this, R.string.multiple_faces_tips, Toast.LENGTH_SHORT).show();
                break;

            case THRESHOLD_ERROR:
                setSearchTips(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                setSearchTips(R.string.no_mask_please);
                break;

            default:
                binding.searchTips.setText("搜索提示：" + code);
                break;
        }
    }

    private void setSearchTips(int resId) {
        binding.searchTips.setText(resId);
    }

    /**
     * 第二行提示
     * @param resId
     */
    private void setSecondTips(int resId){
        if(resId==0){
            binding.secondSearchTips.setText("");
            binding.secondSearchTips.setVisibility(View.INVISIBLE);
        }else {
            binding.secondSearchTips.setText(resId);
            binding.secondSearchTips.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 停止人脸搜索，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pauseSearch=false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseSearch=true;
    }
}