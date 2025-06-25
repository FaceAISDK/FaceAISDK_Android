package com.ai.face.UVCCamera.search;

import static com.ai.face.FaceAIConfig.PREVIEW_HEIGHT;
import static com.ai.face.FaceAIConfig.PREVIEW_WIDTH;

import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ai.face.UVCCamera.camera.UsbCameraEnumHelpGG235;
import com.ai.face.UVCCamera.camera.UsbCameraManager;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentFaceSearchUvcCameraBinding;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

/**
 * UVC协议双目摄像头人脸搜索识别 abstract 基类，管理摄像头
 *
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 *
 * 也可以支持仅仅RGB 的USB 摄像头，「调试的时候USB摄像头一定要固定住屏幕正上方」
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 * 演示Demo 默认都不启用红外活体检测
 */
public abstract class AbsFaceSearch_UVCCameraFragment extends Fragment {
    private static final String TAG = AbsFaceSearch_UVCCameraFragment.class.getSimpleName();
    public FragmentFaceSearchUvcCameraBinding binding;
    private final UsbCameraManager rgbCameraManager = new UsbCameraManager();//RBG camera
    private final UsbCameraManager irCameraManager = new UsbCameraManager(); //近红外摄像头

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
        rgbCameraManager.setOpeningMultiCamera(true);
        rgbCameraManager.setCameraView(binding.rgbCameraTextureView,true);
        rgbCameraManager.selectUsbCamera(UsbCameraEnumHelpGG235.RGB);

        rgbCameraManager.setOnCameraStatuesCallBack(new UsbCameraManager.onCameraStatusCallBack() {
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


        rgbCameraManager.setPreviewHeight(PREVIEW_HEIGHT);
        rgbCameraManager.onFaceAIAnalysis(frame -> {

            Size currentPreviewSize = rgbCameraManager.getCurrentPreviewSize();
            int width = PREVIEW_WIDTH;
            int height = PREVIEW_HEIGHT;
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
        irCameraManager.setOpeningMultiCamera(true);
        irCameraManager.setCameraView(binding.irCameraTextureView,true);
        irCameraManager.selectUsbCamera(UsbCameraEnumHelpGG235.IR);

        irCameraManager.setOnCameraStatuesCallBack(new UsbCameraManager.onCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {
            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {

             }
        });

        irCameraManager.setPreviewHeight(PREVIEW_HEIGHT);
        irCameraManager.onFaceAIAnalysis(frame -> {
            Size currentPreviewSize = irCameraManager.getCurrentPreviewSize();
            int width = PREVIEW_WIDTH;
            int height = PREVIEW_HEIGHT;
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
