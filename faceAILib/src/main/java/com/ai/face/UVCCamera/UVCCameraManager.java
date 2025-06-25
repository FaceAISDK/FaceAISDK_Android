package com.ai.face.UVCCamera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCParam;
import com.serenegiant.widget.AspectRatioSurfaceView;
import java.util.List;

/**
 * USB摄像头（UVC协议）管理。如果本SDK Demo不能管理你的定制摄像头，请参考https://github.com/shiyinghan/UVCAndroid
 * 熟悉后可以自己实现一个 UsbCameraManager来管理你的摄像头各种适配
 *
 */
public class UVCCameraManager {
    private final static String TAG = "UsbCameraManager";
    private ICameraHelper mCameraHelper;
    private AspectRatioSurfaceView cameraView;
    private boolean autoAspectRatio = true;

    //同时打开多个摄像头时, 设这个值为 true
    private boolean openingMultiCamera = true;
    private int previewHeight = -1;
    private IFrameCallback frameCallback;
    private int framePixelFormat;

    public interface onCameraStatusCallBack {
        void onAttach(UsbDevice device);
        void onDeviceOpen(UsbDevice device, boolean isFirstOpen);
    }

    private onCameraStatusCallBack onCameraStatuesCallBack;

    public void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    public void setOnCameraStatuesCallBack(onCameraStatusCallBack callBack) {
        onCameraStatuesCallBack = callBack;
    }


    /**
     * 使用结束后, 释放 camera
     */
    public void releaseCameraHelper() {
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }


    public void setCameraView(@NonNull AspectRatioSurfaceView cameraView, boolean autoAspectRatio) {
        this.cameraView = cameraView;
        this.autoAspectRatio = autoAspectRatio;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }


    /**
     * 根据摄像头的名字来选择使用哪个摄像头
     *
     */
    public UsbDevice selectCameraWithKey(String keyword, Context context) {
        UsbDevice selectedDevice = null;

        initCameraHelper();
        mCameraHelper.closeCamera();
        //不同厂家生产的摄像头有点差异，请开发者自己实现匹配逻辑
        final List<UsbDevice> list = mCameraHelper.getDeviceList();
        for (UsbDevice device : list) {
            String name=device.getProductName();
            if(TextUtils.isEmpty(name)){
                Toast.makeText(context,"摄像头名称为空，请检测匹配",Toast.LENGTH_SHORT).show();
            }else if (name.contains(keyword)) {
                selectedDevice = device;
                mCameraHelper.selectDevice(device);
                break;
            }
        }
        return selectedDevice;
    }


    /**
     * 设置回调,分析每帧数据
     *
     * @param callback
     * @param pixelFormat
     */
    public void onFaceAIAnalysis(IFrameCallback callback, int pixelFormat) {
        frameCallback = callback;
        framePixelFormat = pixelFormat;
    }

    public void setAutoAspectRatio(boolean autoAspectRatio) {
        this.autoAspectRatio = autoAspectRatio;
    }

    public void setOpeningMultiCamera(boolean openingMultiCamera) {
        this.openingMultiCamera = openingMultiCamera;
    }

    @NonNull
    public ICameraHelper getCameraHelper() {
        initCameraHelper();
        return mCameraHelper;
    }



    @Nullable
    public Size getCurrentPreviewSize() {
        initCameraHelper();
        return mCameraHelper.getPreviewSize();
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {

            if (onCameraStatuesCallBack != null) {
                onCameraStatuesCallBack.onAttach(device);
            }
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            initCameraHelper();
            if (openingMultiCamera) {
                //参考 uvc camera demo 的 MultiCameraNewActivity,
                //发现他如果同时打开多个摄像头, 需要这么配置
                UVCParam param = new UVCParam();
                param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
                mCameraHelper.openCamera(param);
            } else {
                mCameraHelper.openCamera();
            }

            if (onCameraStatuesCallBack != null) {
                onCameraStatuesCallBack.onDeviceOpen(device, isFirstOpen);
            }

        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            initCameraHelper();
            Size previewSize = null;

            if (previewHeight > 0) {
                // 支持打开摄像头分辨率列表
                List<Size> supportedSizeList = mCameraHelper.getSupportedSizeList();

                if (supportedSizeList != null) {
                    for (Size size : supportedSizeList) {
                        if (size.height == previewHeight && size.type == 7) {
                            previewSize = size;
                            break;
                        }
                    }
                    if (previewSize != null) {
                        mCameraHelper.setPreviewSize(previewSize);
                    }
                }
            }

            mCameraHelper.startPreview();

            if (cameraView != null) {
                if (autoAspectRatio) {

                    Size size = mCameraHelper.getPreviewSize();
                    if (previewSize != null) {
                        size = previewSize;
                    }
                    if (size != null) {
                        int width = size.width;
                        int height = size.height;
                        //auto aspect ratio
                        cameraView.setAspectRatio(width, height);
                    }
                }
                mCameraHelper.addSurface(cameraView.getHolder().getSurface(), false);
                if (frameCallback != null) {
                    mCameraHelper.setFrameCallback(frameCallback, framePixelFormat);
                }
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {

            if (cameraView != null) {
                initCameraHelper();
                mCameraHelper.removeSurface(cameraView.getHolder().getSurface());
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) {

        }

        @Override
        public void onDetach(UsbDevice device) {

        }

        @Override
        public void onCancel(UsbDevice device) {

        }

    };

}
