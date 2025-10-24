package com.faceAI.demo.UVCCamera.search;

import static com.ai.face.faceSearch.search.SearchProcessTipsCode.IR_LIVE_ERROR;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.search.ImageToast;
import com.faceAI.demo.base.utils.BrightnessUtil;
import com.faceAI.demo.base.utils.VoicePlayer;
import java.util.List;


/**
 * UVC协议USB摄像头人脸搜索识别业务逻辑管理Fragment
 *
 * 如果USB带红外的双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 *
 * AbsFaceSearch_UVCCameraFragment 是摄像头相关处理，「调试的时候USB摄像头一定要固定住屏幕正上方」
 *
 * 怎么提高人脸搜索识别系统的准确度？https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
 * 网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
 *
 */
public class FaceSearch_UVCCameraFragment extends AbsFaceSearch_UVCCameraFragment {
    public FaceSearch_UVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void initViews() {
        super.initViews();
        binding.close.setOnClickListener(v -> requireActivity().finish());
        BrightnessUtil.setBrightness(requireActivity(), 0.9f);  //高亮白色背景屏幕光可以当补光灯
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }

    /**
     * 请断点调试保证bitmap 的方向正确； RGB和IR Bitmap大小相同，画面同步
     *
     * @param bitmap
     * @param type
     */
    private Bitmap rgbBitmap, irBitmap;
    private boolean rgbReady = false, irReady = false;

    /**
     * 初始化人脸搜索参赛设置
     */
    @Override
    void initFaceSearchParam() {
        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(requireActivity())
                .setLifecycleOwner(this)
                .setCameraType(cameraType)
                .setThreshold(0.88f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
                .setCallBackAllMatch(true) //默认是false,是否返回所有的大于设置阈值的搜索结果
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA) //摄像头种类是UVC协议,和系统RGB摄像头要区分清楚
                .setSearchIntervalTime(1900) //默认2000，范围[1500,3000]毫秒。搜索成功后的继续下一次搜索的间隔时间，不然会一直搜索一直回调结果
                .setProcessCallBack(new SearchProcessCallBack() {

                    // 得分最高的搜索结果
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        Bitmap mostSimilarBmp = BitmapFactory.decodeFile(CACHE_SEARCH_FACE_DIR + faceID);
                        new ImageToast().show(requireContext(), mostSimilarBmp, faceID.replace(".jpg"," ")+score);
                        VoicePlayer.getInstance().play(R.raw.success);
                        binding.graphicOverlay.clearRect();
                    }

                    /**
                     * 人脸搜索匹配容错处理
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     * setCallBackAllMatch(true) 才有值
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
                        Log.e("onFaceMatched",matchedResults.toString());
                    }

                    /**
                     * 检测到的人脸，画框
                     * @param result
                     */
                    @Override
                    public void onFaceDetected(List<FaceSearchResult> result) {
                        //画框UI代码完全开放，用户可以根据情况自行改造
                        binding.graphicOverlay.drawRect(result, scaleX, scaleY);
                    }

                    @Override
                    public void onProcessTips(int code) {
                        showFaceSearchPrecessTips(code);
                    }

                    @Override
                    public void onLog(String log) {
                        binding.logText.setText(log);
                    }

                }).create();

        //3.初始化引擎，是个耗时耗资源操作
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);
    }

    @Override
    void showFaceSearchPrecessTips(int code) {
        switch (code) {

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，没有使用SDK 插入人脸？
                setSearchTips(R.string.face_dir_empty);
                break;

            case EMGINE_INITING:
                setSearchTips(R.string.sdk_init);
                break;

            case SEARCH_PREPARED, SEARCHING:
                setSearchTips(R.string.keep_face_tips);
                break;

            case IR_LIVE_ERROR:
//                binding.searchTips.setText(R.string.ir_live_error); //偶尔失败可以忽略
                break;

            case NO_LIVE_FACE:
                setSearchTips(R.string.no_face_detected_tips);
                break;

            case THRESHOLD_ERROR:
                setSearchTips(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                setSearchTips(R.string.no_mask_please);
                break;

            case NO_MATCHED:
                //没有搜索匹配识别到任何人
                setSecondTips(R.string.no_matched_face);
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
                setSecondTips(R.string.multiple_faces_tips);
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
            binding.secondSearchTips.setVisibility(View.VISIBLE);
            binding.secondSearchTips.setText(resId);
        }
    }

    /**
     * UVC协议USB摄像头设置数据，送数据到SDK 引擎
     * 设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
     *
     * @param bitmap
     * @param type
     */
    void faceSearchSetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type) {
        if(cameraType== FaceAICameraType.UVC_CAMERA_RGB){
            FaceSearchEngine.Companion.getInstance().runSearchWithBitmap(bitmap);
        }else{
            if (type.equals(FaceVerifyUtils.BitmapType.IR)) {
                irBitmap = bitmap;
                irReady = true;
            } else if (type.equals(FaceVerifyUtils.BitmapType.RGB)) {
                rgbBitmap = bitmap;
                rgbReady = true;
            }

            if (irReady && rgbReady) {
                getScaleValue();
                //送数据进入SDK
                FaceSearchEngine.Companion.getInstance().runSearchWithIR(irBitmap, rgbBitmap);
                irReady = false;
                rgbReady = false;
            }
        }

    }


    /**
     * 用来绘制人脸框
     *
     */
    float scaleX = 0f, scaleY = 0f;
    private void getScaleValue() {
        if (scaleX == 0f || scaleY == 0f) {
            scaleX = (float) binding.rgbCameraView.getWidth() / rgbBitmap.getWidth();
            scaleY = (float) binding.rgbCameraView.getHeight() / rgbBitmap.getHeight();
        }
    }
}
