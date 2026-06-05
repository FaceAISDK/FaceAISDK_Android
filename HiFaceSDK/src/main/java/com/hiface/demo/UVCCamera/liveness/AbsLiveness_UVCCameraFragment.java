package com.hiface.demo.UVCCamera.liveness;

import static android.view.View.INVISIBLE;
import static com.hiface.demo.FaceAISettingsActivity.IR_UVC_CAMERA_DEGREE;
import static com.hiface.demo.FaceAISettingsActivity.IR_UVC_CAMERA_MIRROR_H;
import static com.hiface.demo.FaceAISettingsActivity.IR_UVC_CAMERA_SELECT;
import static com.hiface.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_DEGREE;
import static com.hiface.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_MIRROR_H;
import static com.hiface.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_SELECT;
import static com.hiface.demo.FaceAISettingsActivity.UVC_CAMERA_TYPE;
import static com.hiface.demo.UVCCamera.manger.UVCCameraManager.IR_KEY_DEFAULT;
import static com.hiface.demo.UVCCamera.manger.UVCCameraManager.RGB_KEY_DEFAULT;

import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hiface.demo.UVCCamera.manger.CameraBuilder;
import com.hiface.demo.UVCCamera.manger.UVCCameraManager;
import com.hiface.demo.databinding.FragmentUvcCameraLivenessBinding;
import com.sdk.hiface.core.utils.FaceAICameraType;
import com.sdk.hiface.recognize.FaceVerifyUtils;
import com.tencent.mmkv.MMKV;

/**
 * UVC协议USB摄像头活体检测 Liveness Detection with UVC USB Camera
 * 更多外接USB外接UVC摄像头**的操作参考这个大神的库：https://github.com/shiyinghan/UVCAndroid
 *
 * @author FaceAISDK.Service@gmail.com
 */
public abstract class AbsLiveness_UVCCameraFragment extends Fragment {
    public FragmentUvcCameraLivenessBinding binding;
    public FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    public int cameraType = FaceAICameraType.UVC_CAMERA_RGB; //UVC 可以单RGB 或者 RGB+IR
    private UVCCameraManager rgbCameraManager; //RBG camera
    private UVCCameraManager irCameraManager;  //近红外IR Camera

    abstract void initFaceLivenessParam();
    abstract void showFaceLivenessTips(int actionCode);
    abstract void faceLivenessSetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);
    public AbsLiveness_UVCCameraFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUvcCameraLivenessBinding.inflate(inflater, container, false);

        MMKV mmkv = MMKV.defaultMMKV();
        cameraType = mmkv.decodeInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA);

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
        if (irCameraManager != null) {
            irCameraManager.releaseCameraHelper();
        }
    }

    /**
     * 初始化UVC 协议RGB摄像头
     */
    private void initRGBCamara() {
        MMKV mmkv = MMKV.defaultMMKV();
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("UVC RGB Camera")
                .setCameraKey(mmkv.decodeString(RGB_UVC_CAMERA_SELECT, RGB_KEY_DEFAULT))
                .setCameraView(binding.rgbCameraView)
                .setContext(requireContext())
                .setDegree(mmkv.decodeInt(RGB_UVC_CAMERA_DEGREE, 0))
                .setHorizontalMirror(mmkv.decodeBool(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager = new UVCCameraManager(cameraBuilder);
        rgbCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.OnCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
                //RGB 打开了就继续去打开IR
                if (cameraType == FaceAICameraType.UVC_CAMERA_RGB_IR) {
                    initIRCamara();
                } else {
                    binding.irCameraView.setVisibility(INVISIBLE);
                }
                initFaceLivenessParam();
            }
        });


        rgbCameraManager.setFaceAIAnalysis(bitmap -> faceLivenessSetBitmap(bitmap, FaceVerifyUtils.BitmapType.RGB));
    }

    /**
     * 初始化IR 摄像头
     */
    private void initIRCamara() {
        MMKV mmkv = MMKV.defaultMMKV();
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("红外IR摄像头")
                .setCameraKey(mmkv.decodeString(IR_UVC_CAMERA_SELECT, IR_KEY_DEFAULT))
                .setCameraView(binding.irCameraView)
                .setContext(requireContext())
                .setDegree(mmkv.decodeInt(IR_UVC_CAMERA_DEGREE, 0))
                .setHorizontalMirror(mmkv.decodeBool(IR_UVC_CAMERA_MIRROR_H, false))
                .build();

        irCameraManager = new UVCCameraManager(cameraBuilder);

        irCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.OnCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {
            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {

            }
        });

        irCameraManager.setFaceAIAnalysis(bitmap -> faceLivenessSetBitmap(bitmap, FaceVerifyUtils.BitmapType.IR));

    }

}
