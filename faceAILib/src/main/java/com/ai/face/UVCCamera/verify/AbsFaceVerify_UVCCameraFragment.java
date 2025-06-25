package com.ai.face.UVCCamera.verify;


import static com.ai.face.FaceAIConfig.UVC_CAMERA_HEIGHT;
import static com.ai.face.FaceAIConfig.UVC_CAMERA_WIDTH;

import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ai.face.UVCCamera.UVCCameraManager;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentBinocularCameraBinding;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

/**
 * 1:1 人脸识别活体检测 abstract 基础类
 *
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * <p>
 * 也可以支持仅仅RGB 的USB 摄像头，「调试的时候USB摄像头一定要固定住屏幕正上方」
 * <p>
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public abstract class AbsFaceVerify_UVCCameraFragment extends Fragment {
    private static final String TAG = AbsFaceVerify_UVCCameraFragment.class.getSimpleName();

    public FragmentBinocularCameraBinding binding;
    public FaceVerifyUtils faceVerifyUtils;
    private final UVCCameraManager rgbCameraManager = new UVCCameraManager();//RBG camera
    private final UVCCameraManager irCameraManager = new UVCCameraManager(); //近红外摄像头


    //人脸识别相关的方法
    abstract void initFaceVerifyBaseBitmap();

    abstract void initFaceVerificationParam(Bitmap baseBitmap);

    abstract void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore);

    abstract void showFaceVerifyTips(int actionCode);

    abstract void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);


    public AbsFaceVerify_UVCCameraFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBinocularCameraBinding.inflate(inflater, container, false);
        initViews();
        initRGBCamara();
        return binding.getRoot();
    }

    public void initViews() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rgbCameraManager.releaseCameraHelper();
        irCameraManager.releaseCameraHelper();
    }

    //初始化RGB摄像头
    private void initRGBCamara() {
        rgbCameraManager.initCameraHelper();
        rgbCameraManager.setOpeningMultiCamera(true);
        rgbCameraManager.setCameraView(binding.rgbCameraTextureView, true);

        //根据device.getProductName()来匹配RGB摄像头。可能关键字不是这个，请自行匹配
        rgbCameraManager.selectCameraWithKey("RGB",requireContext());

        rgbCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.onCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
                //RGB 打开了就继续去打开IR
                initIRCamara();

                initFaceVerifyBaseBitmap();
            }
        });


        rgbCameraManager.setPreviewHeight(UVC_CAMERA_HEIGHT);
        rgbCameraManager.onFaceAIAnalysis(frame -> {
            //每秒30 帧，这里尽量少的复杂处理逻辑
            Size currentPreviewSize = rgbCameraManager.getCurrentPreviewSize();
            int width = UVC_CAMERA_WIDTH;
            int height = UVC_CAMERA_HEIGHT;
            if (currentPreviewSize != null) {
                width = currentPreviewSize.width;
                height = currentPreviewSize.height;
            }
            Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(frame, width, height, 0, 0, false);
            if (bitmap != null) {
                faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.RGB);
            }
        }, UVCCamera.PIXEL_FORMAT_NV21);
    }

    /**
     * 初始化IR 摄像头
     */
    private void initIRCamara() {
        irCameraManager.initCameraHelper();
        irCameraManager.setOpeningMultiCamera(true);
        irCameraManager.setCameraView(binding.irCameraTextureView, true);
        //根据device.getProductName()来匹配IR红外摄像头。可能关键字不是这个，请自行匹配
        irCameraManager.selectCameraWithKey("IR",requireContext());

        irCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.onCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {
            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {

            }
        });

        irCameraManager.setPreviewHeight(UVC_CAMERA_HEIGHT);
        irCameraManager.onFaceAIAnalysis(frame -> {
            Size currentPreviewSize = irCameraManager.getCurrentPreviewSize();
            int width = UVC_CAMERA_WIDTH;
            int height = UVC_CAMERA_HEIGHT;
            if (currentPreviewSize != null) {
                width = currentPreviewSize.width;
                height = currentPreviewSize.height;
            }
            Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(frame, width, height, 0, 0, false);
            if (bitmap != null) {
                faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.IR);
            }
        }, UVCCamera.PIXEL_FORMAT_NV21);
    }

}
