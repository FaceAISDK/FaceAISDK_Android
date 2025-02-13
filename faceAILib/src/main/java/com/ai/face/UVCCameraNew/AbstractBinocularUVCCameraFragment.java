package com.ai.face.UVCCameraNew;

import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentBinocularCameraBinding;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.MultiCameraFragment;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.utils.ToastUtils;

import java.nio.ByteBuffer;

/**
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 */
public abstract class AbstractBinocularUVCCameraFragment extends MultiCameraFragment implements ICameraStateCallBack {
    public static final String RBG_CAMERA = "RGB"; //RGB 摄像头名字中的关键字
    public static final String IR_CAMERA = "IR";   //IR 摄像头名字中的关键字
    public static final int PREVIEW_WIDTH = 640, PREVIEW_HEIGHT = 480;

    public MultiCameraClient.ICamera rgbCamera;
    public MultiCameraClient.ICamera irCamera;

    public FragmentBinocularCameraBinding mViewBinding;
    public FaceVerifyUtils faceVerifyUtils;

    //人脸识别相关的方法
    abstract void initFaceVerifyBaseBitmap();

    abstract void initFaceVerificationParam(Bitmap baseBitmap);

    abstract void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore);

    abstract void showFaceVerifyTips(int actionCode);

    abstract void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type);

    public AbstractBinocularUVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    protected void onCameraAttached(@NonNull MultiCameraClient.ICamera camera) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //如果你的系统还小于Android 5.0 可能getProductName 会为空，自行适配
            Toast.makeText(getContext(), "SDK_INT<LOLLIPOP 请修改实现", Toast.LENGTH_LONG).show();
            return;
        }

        String productName = camera.getUsbDevice().getProductName();
        if (!TextUtils.isEmpty(productName) && productName.contains(RBG_CAMERA)) {
            rgbCamera = camera;
        }
        if (!TextUtils.isEmpty(productName) && productName.contains(IR_CAMERA)) {
            irCamera = camera;
        }
        mViewBinding.multiCameraTip.setVisibility(View.GONE);
    }

    @Override
    protected void onCameraDetached(@NonNull MultiCameraClient.ICamera camera) {
        camera.closeCamera();

        //两个摄像头都没有打开
        if (!irCamera.isCameraOpened() && !rgbCamera.isCameraOpened()) {
            mViewBinding.multiCameraTip.setVisibility(VISIBLE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String productName = camera.getUsbDevice().getProductName();
            if (!TextUtils.isEmpty(productName) && productName.contains(RBG_CAMERA)) {
                rgbCamera = null;
            }
            if (!TextUtils.isEmpty(productName) && productName.contains(IR_CAMERA)) {
                irCamera = null;
            }
        } else {
            Toast.makeText(getContext(), "SDK_INT<LOLLIPOP 请修改实现细节", Toast.LENGTH_LONG).show();
        }
    }


    @NonNull
    @Override
    public MultiCameraClient.ICamera generateCamera(@NonNull Context context, @NonNull UsbDevice usbDevice) {
        return new CameraUVC(context, usbDevice);
    }


    @Override
    protected void onCameraConnected(@NonNull MultiCameraClient.ICamera camera) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String productName = camera.getUsbDevice().getProductName();
            Log.d("onCameraConnected", "---  发现新摄像头: --- " + productName);

            //处理 RGB Camera
            if (!TextUtils.isEmpty(productName) && productName.contains(RBG_CAMERA)) {
                initFaceVerifyBaseBitmap();
                camera.openCamera(mViewBinding.rgbCameraTextureView, getCameraRequest());
                camera.setCameraStateCallBack(this);
                camera.setRenderSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

                camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
//                    LogUtils.d("CAMERA","RGB: "+width+"  "+height+" "+dataFormat.name());
                    Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes), width, height, 0, 0, false);
                    if (bitmap != null) {
                        faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.RGB);
                    }
                });

                UsbDevice device = camera.getUsbDevice();
                if (!hasPermission(device)) {
                    requestPermission(device);
                }
            }

            //处理 IR Camera
            if (!TextUtils.isEmpty(productName) && productName.contains(IR_CAMERA)) {
                camera.openCamera(mViewBinding.irCameraTextureView, getCameraRequest());
                camera.setCameraStateCallBack(this);
                camera.setRenderSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
                camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
//                    LogUtils.d("CAMERA","IR: "+width+"  "+height+" "+dataFormat.name());
                    Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes), width, height, 0, 0, false);
                    if (bitmap != null) {
                        faceVerifySetBitmap(bitmap, FaceVerifyUtils.BitmapType.IR);
                    }
                });

                UsbDevice device = camera.getUsbDevice();
                if (!hasPermission(device)) {
                    requestPermission(device);
                }
            }

        } else {
            Toast.makeText(getContext(), "SDK_INT<LOLLIPOP 请修改实现细节吧", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onCameraDisConnected(@NonNull MultiCameraClient.ICamera iCamera) {
        iCamera.closeCamera();
    }


    @Override
    public void onCameraState(@NonNull MultiCameraClient.ICamera iCamera, @NonNull State code, @Nullable String s) {
        if (code == State.ERROR) {
            ToastUtils.show("UVC Camera 错误" + s);
        }
    }


    @Nullable
    @Override
    protected View getRootView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup) {
        mViewBinding = FragmentBinocularCameraBinding.inflate(layoutInflater, viewGroup, false);
        return mViewBinding.getRoot();
    }

    private CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(PREVIEW_WIDTH)
                .setPreviewHeight(PREVIEW_HEIGHT)
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .create();
    }


}