package com.ai.face.UVCCamera;

import static com.ai.face.UVCCamera.Constants.PREVIEW_HEIGHT;
import static com.ai.face.UVCCamera.Constants.PREVIEW_WIDTH;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ai.face.UVCCamera.camera.UsbCameraEnum;
import com.ai.face.UVCCamera.camera.UsbCameraManager;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentBinocularCameraBinding;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

/**
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 *
 * 也可以支持仅仅RGB 的USB 摄像头，需要购置可以插在手机上调试的红外双目摄像头可以找我推荐
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 *
 */
public abstract class AbstractBinocularUVCCameraFragment extends Fragment {
    private static final String TAG = AbstractBinocularUVCCameraFragment.class.getSimpleName();

    public FragmentBinocularCameraBinding binding;
    public FaceVerifyUtils faceVerifyUtils;
    private final UsbCameraManager rgbCameraManager = new UsbCameraManager();//RBG camera
    private final UsbCameraManager irCameraManager = new UsbCameraManager(); //近红外摄像头


    //人脸识别相关的方法
    abstract void initFaceVerifyBaseBitmap();

    abstract void initFaceVerificationParam(Bitmap baseBitmap);

    abstract void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore);

    abstract void showFaceVerifyTips(int actionCode);

    abstract void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);


    public AbstractBinocularUVCCameraFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBinocularCameraBinding.inflate(inflater, container, false);
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
        rgbCameraManager.selectUsbCamera(UsbCameraEnum.RGB);

        rgbCameraManager.setOnDeviceStatuesCallBack(new UsbCameraManager.OnDeviceStatuesCallBack() {
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


        rgbCameraManager.setPreviewHeight(PREVIEW_HEIGHT);
        rgbCameraManager.setFrameCallback(frame -> {
            //每秒30 帧，这里尽量少的复杂处理逻辑
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
        irCameraManager.selectUsbCamera(UsbCameraEnum.IR);

        irCameraManager.setOnDeviceStatuesCallBack(new UsbCameraManager.OnDeviceStatuesCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {

             }
        });


        irCameraManager.setPreviewHeight(PREVIEW_HEIGHT);

        irCameraManager.setFrameCallback(frame -> {
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
