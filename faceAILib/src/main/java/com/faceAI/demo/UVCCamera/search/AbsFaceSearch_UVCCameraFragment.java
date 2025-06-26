package com.faceAI.demo.UVCCamera.search;

import static com.faceAI.demo.FaceAIConfig.UVC_CAMERA_HEIGHT;
import static com.faceAI.demo.FaceAIConfig.UVC_CAMERA_WIDTH;

import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.faceAI.demo.UVCCamera.UVCCameraManager;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.faceAI.demo.databinding.FragmentFaceSearchUvcCameraBinding;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

/**
 * UVC协议双目摄像头人脸搜索识别 abstract 基类，管理摄像头
 *
 * 使用宽动态（人脸搜索须大于105DB）抗逆光摄像头；保持镜头干净（用纯棉布擦拭油污）
 *
 * 也可以支持仅仅RGB 的USB 摄像头，「调试的时候USB摄像头一定要固定住屏幕正上方」保证角度合适
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 *
 */
public abstract class AbsFaceSearch_UVCCameraFragment extends Fragment {
    private static final String TAG = AbsFaceSearch_UVCCameraFragment.class.getSimpleName();
    public FragmentFaceSearchUvcCameraBinding binding;
    private final UVCCameraManager rgbCameraManager = new UVCCameraManager();//RBG camera
    private final UVCCameraManager irCameraManager = new UVCCameraManager(); //近红外摄像头

    abstract void initFaceSearchParam();

    abstract void showFaceSearchPrecessTips(int code);

    abstract void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);

    public AbsFaceSearch_UVCCameraFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFaceSearchUvcCameraBinding.inflate(inflater, container, false);
        initViews();
        initRGBCamara();
        return binding.getRoot();
    }

    public void initViews(){
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rgbCameraManager.releaseCameraHelper();
        irCameraManager.releaseCameraHelper();
    }

    private void initRGBCamara() {
        rgbCameraManager.initCameraHelper();
        rgbCameraManager.setCameraView(binding.rgbCameraTextureView,true);

        //根据device.getProductName()来匹配RGB摄像头。可能关键字不是这个，请自行根据你的摄像头匹配
        rgbCameraManager.selectCameraWithKey("RGB",requireContext());

        rgbCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.onCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
                //RGB 打开了就继续去打开IR
                initIRCamara();
                initFaceSearchParam();
            }
        });


        rgbCameraManager.setPreviewHeight(UVC_CAMERA_HEIGHT);
        rgbCameraManager.onFaceAIAnalysis(frame -> {

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
     *
     */
    private void initIRCamara() {
        irCameraManager.initCameraHelper();
        irCameraManager.setCameraView(binding.irCameraTextureView,true);

        //根据device.getProductName()来匹配IR红外摄像头。可能关键字不是这个，请自行根据你的摄像头匹配
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
