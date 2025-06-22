package com.ai.face.search.uvcCameraSearch;

import static com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
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

import com.ai.face.R;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.search.ImageToast;
import com.ai.face.utils.VoicePlayer;

import java.util.List;


/**
 * USB带红外双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * <p>
 * AbstractBinocularUVCCameraFragment 是摄像头相关处理，「调试的时候USB摄像头一定要固定住屏幕正上方」
 *
 * 演示Demo 默认都不启用红外活体检测
 * <p>
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class FaceSearchUVCCameraFragment extends AbstractFaceSearchUVCCameraFragment {
    public FaceSearchUVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void initViews() {
        super.initViews();
        binding.back.setOnClickListener(v -> requireActivity().finish());
        BrightnessUtil.setBrightness(requireActivity(), 1.0f);  //高亮白色背景屏幕光可以当补光灯
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
                .setThreshold(0.88f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
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
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, scaleX, scaleY);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showFaceSearchPrecessTips(i);
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
//        binding.secondTipsView.setText("");
        switch (code) {
            default:
                binding.searchTips.setText("回调提示：" + code);
                break;

            case NO_MATCHED:
                //没有搜索匹配识别到任何人
                binding.secondSearchTips.setText(R.string.no_matched_face);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，没有使用SDK 插入人脸？
                binding.searchTips.setText(R.string.face_dir_empty);
                break;

            case SEARCHING:
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
                binding.secondSearchTips.setText(R.string.multiple_faces_tips);
                break;

            case THRESHOLD_ERROR:
                binding.searchTips.setText(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                binding.searchTips.setText(R.string.no_mask_please);
                break;

            case EMGINE_INITING:
                binding.searchTips.setText(R.string.keep_face_tips);
                break;

        }
    }


    float scaleX = 0f, scaleY = 0f;

    private void getScaleValue() {
        if (scaleX == 0f || scaleY == 0f) {
            float max = rgbBitmap.getWidth();
            float min = rgbBitmap.getHeight();
            if (max < min) { //交换
                float temp = max;
                max = min;
                min = temp;
            }
            if (binding.rgbCameraTextureView.getWidth() > binding.rgbCameraTextureView.getHeight()) {
                scaleX = (float) binding.rgbCameraTextureView.getWidth() / max;
                scaleY = (float) binding.rgbCameraTextureView.getHeight() / min;
            } else {
                scaleX = (float) binding.rgbCameraTextureView.getWidth() / min;
                scaleY = (float) binding.rgbCameraTextureView.getHeight() / max;
            }
        }
    }


    /**
     * 双目摄像头设置数据，送数据到SDK 引擎
     *
     * @param bitmap
     * @param type
     */
    void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type) {
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
