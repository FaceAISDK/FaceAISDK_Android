package com.faceAI.demo.UVCCamera.verify;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.INVISIBLE;
import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_SELECT;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_SELECT;
import static com.faceAI.demo.FaceAISettingsActivity.UVC_CAMERA_TYPE;
import static com.faceAI.demo.UVCCamera.manger.UVCCameraManager.IR_KEY_DEFAULT;
import static com.faceAI.demo.UVCCamera.manger.UVCCameraManager.RGB_KEY_DEFAULT;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ai.face.core.utils.FaceAICameraType;
import com.faceAI.demo.UVCCamera.manger.CameraBuilder;
import com.faceAI.demo.UVCCamera.manger.UVCCameraManager;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.faceAI.demo.databinding.FragmentUvcCameraBinding;

/**
 * 1:1 人脸识别活体检测 abstract 基础类
 * <p>
 * UVC协议USB摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * <p>
 * 也可以支持仅仅RGB 的USB 摄像头，「调试的时候USB摄像头一定要固定住屏幕正上方」
 * <p>
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public abstract class AbsFaceVerify_UVCCameraFragment extends Fragment {
    private static final String TAG = AbsFaceVerify_UVCCameraFragment.class.getSimpleName();
    public FragmentUvcCameraBinding binding;
    public FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    public int cameraType = FaceAICameraType.UVC_CAMERA_RGB; //UVC 可以单RGB或者RGB+IR
    private UVCCameraManager rgbCameraManager;//RBG camera
    private UVCCameraManager irCameraManager; //近红外摄像头

    //人脸识别相关的方法
    abstract void initFaceVerify();
    abstract void initFaceVerificationParam(float[] face);
    abstract void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore);
    abstract void showFaceVerifyTips(int actionCode);
    abstract void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);

    public AbsFaceVerify_UVCCameraFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUvcCameraBinding.inflate(inflater, container, false);

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("FaceAISDK_SP", MODE_PRIVATE);
        cameraType = sharedPref.getInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA);

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

    //初始化RGB摄像头
    private void initRGBCamara() {
        SharedPreferences sharedPref = requireContext().getSharedPreferences("FaceAISDK_SP", MODE_PRIVATE);
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("UVC RGB Camera")
                .setCameraKey(sharedPref.getString(RGB_UVC_CAMERA_SELECT, RGB_KEY_DEFAULT))
                .setCameraView(binding.rgbCameraView)
                .setContext(requireContext())
                .setDegree(sharedPref.getInt(RGB_UVC_CAMERA_DEGREE, 0))
                .setHorizontalMirror(sharedPref.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager = new UVCCameraManager(cameraBuilder);
        rgbCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.OnCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
                initFaceVerify();
                //RGB 打开了就继续去打开IR
                if (cameraType == FaceAICameraType.UVC_CAMERA_RGB_IR) {
                    initIRCamara();
                } else {
                    binding.irCameraView.setVisibility(INVISIBLE);
                }
            }
        });


        rgbCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.RGB);
            }
        });
    }

    /**
     * 初始化IR 摄像头
     */
    private void initIRCamara() {
        SharedPreferences sp = requireContext().getSharedPreferences("FaceAISDK_SP", MODE_PRIVATE);
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("红外IR摄像头")
                .setCameraKey(sp.getString(IR_UVC_CAMERA_SELECT, IR_KEY_DEFAULT))
                .setCameraView(binding.irCameraView)
                .setContext(requireContext())
                .setDegree(sp.getInt(IR_UVC_CAMERA_DEGREE, 0))
                .setHorizontalMirror(sp.getBoolean(IR_UVC_CAMERA_MIRROR_H, false))
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

        irCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.IR);
            }
        });

    }

}
