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
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.TOO_MUCH_FACE;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ai.face.R;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;


/**
 * USB带红外双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * <p>
 * AbstractBinocularUVCCameraFragment 是摄像头相关处理，「调试的时候USB摄像头一定要固定住屏幕正上方」
 * 需要使用IR活体请联系
 * <p>
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class FaceSearchUVCCameraFragment extends AbstractFaceSearchUVCCameraFragment {

    private final float silentLivenessThreshold = 0.85f;

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
     *
     */
    @Override
    void initFaceSearchParam() {
        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(requireActivity())
                .setLifecycleOwner(this)
                .setThreshold(0.88f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setProcessCallBack(new SearchProcessCallBack() {
                    /**
                     * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
                     */
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> result, Bitmap contextBitmap) {
                        binding.graphicOverlay.drawRect(result, scaleX, scaleY);
                    }

                    //得分最高的搜索结果
                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap) {
                        if(!isAdded()||requireActivity().isFinishing()||requireActivity().isDestroyed()){
                            return;
                        }

                        Glide.with(requireActivity())
                                .load(CACHE_SEARCH_FACE_DIR + faceID)
                                .skipMemoryCache(false)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transform(new RoundedCorners(12))
                                .into(binding.searchResult);
                        binding.tipsView.setText(faceID);
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
        binding.secondTipsView.setText("");

        switch (code) {
            default:
                binding.tipsView.setText("Tips Code：" + code);
                break;

            case FACE_TOO_SMALL:
                binding.secondTipsView.setText(R.string.come_closer_tips);
                break;

            // 单独使用一个textview 提示，防止上一个提示被覆盖。
            // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
            case FACE_TOO_LARGE:
                binding.secondTipsView.setText(R.string.far_away_tips);
                break;

            //检测到正常的人脸，尺寸大小OK
            case FACE_SIZE_FIT:
                binding.secondTipsView.setText("");
                break;

            case TOO_MUCH_FACE:
                Toast.makeText(getContext(), R.string.multiple_faces_tips, Toast.LENGTH_SHORT).show();
                break;

            case THRESHOLD_ERROR:
                binding.tipsView.setText(R.string.search_threshold_scope_tips);
                break;

            case MASK_DETECTION:
                binding.tipsView.setText(R.string.no_mask_please);
                break;

            case NO_LIVE_FACE:
                binding.tipsView.setText(R.string.no_face_detected_tips);
                binding.searchResult.setImageResource(R.drawable.face_logo);
                break;

            case EMGINE_INITING:
                binding.tipsView.setText(R.string.sdk_init);
                break;

            case FACE_DIR_EMPTY:
                //人脸库没有人脸照片，没有使用SDK API录入人脸
                binding.tipsView.setText(R.string.face_dir_empty);
                break;

            case NO_MATCHED:
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.tipsView.setText(R.string.no_matched_face);
                binding.searchResult.setImageResource(R.drawable.face_logo);
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
