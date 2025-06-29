package com.faceAI.demo.UVCCamera.search;

import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.IR_UVC_CAMERA_SELECT;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_SELECT;
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

import com.faceAI.demo.UVCCamera.manger.CameraBuilder;
import com.faceAI.demo.UVCCamera.manger.UVCCameraManager;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.faceAI.demo.databinding.FragmentFaceSearchUvcCameraBinding;

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
    private  UVCCameraManager rgbCameraManager ;//RBG camera
    private  UVCCameraManager irCameraManager ; //近红外摄像头

    abstract void initFaceSearchParam();

    abstract void showFaceSearchPrecessTips(int code);

    abstract void faceSearchSetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);

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
        if(irCameraManager!=null){
            irCameraManager.releaseCameraHelper();
        }
    }

    private void initRGBCamara() {
        SharedPreferences sp = requireContext().getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("普通RGB摄像头")
                .setCameraKey(sp.getString(RGB_UVC_CAMERA_SELECT,RGB_KEY_DEFAULT))
                .setCameraView(binding.rgbCameraView)
                .setContext(requireContext())
                .setDegree(sp.getInt(RGB_UVC_CAMERA_DEGREE,0))
                .setHorizontalMirror(sp.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager=new UVCCameraManager(cameraBuilder);

        rgbCameraManager.setOnCameraStatuesCallBack(new UVCCameraManager.OnCameraStatusCallBack() {
            @Override
            public void onAttach(UsbDevice device) {

            }

            @Override
            public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
                initFaceSearchParam();

                //RGB 打开了就继续去打开IR
                initIRCamara();
            }
        });

        rgbCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                //设备硬件可以加个红外检测有人靠近再启动人脸搜索检索服务，不然机器一直工作发热性能下降老化快
                faceSearchSetBitmap(bitmap, FaceVerifyUtils.BitmapType.RGB);
            }
        });

    }

    /**
     * 初始化IR 摄像头
     *
     */
    private void initIRCamara() {
        SharedPreferences sp = requireContext().getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);

        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("IR摄像头")
                .setCameraKey(sp.getString(IR_UVC_CAMERA_SELECT,IR_KEY_DEFAULT))
                .setCameraView(binding.irCameraView)
                .setContext(requireContext())
                .setDegree(sp.getInt(IR_UVC_CAMERA_DEGREE,0))
                .setHorizontalMirror(sp.getBoolean(IR_UVC_CAMERA_MIRROR_H, false))
                .build();

        irCameraManager=new UVCCameraManager(cameraBuilder);

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
                faceSearchSetBitmap(bitmap, FaceVerifyUtils.BitmapType.IR);
            }
        });

    }

}
