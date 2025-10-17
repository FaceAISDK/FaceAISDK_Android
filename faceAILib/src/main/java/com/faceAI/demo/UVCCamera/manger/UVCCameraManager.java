package com.faceAI.demo.UVCCamera.manger;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ai.face.base.utils.DataConvertUtils;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.serenegiant.opengl.renderer.MirrorMode;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCParam;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * USB摄像头（UVC协议）管理
 * 根据关键字keyword 是RGB/IR（不同厂商命名方式不一样）来区分双面摄像头哪个是RGB 摄像头哪个是红外
 * 默认的分辨率设置 写在{@link FaceSDKConfig},可以根据下面的方法来获取后修改合适的值
 * <p>
 * 如果本SDK Demo不能管理你的定制摄像头，请参考源码https://github.com/shiyinghan/UVCAndroid
 * 熟悉后可以自己实现一个 UsbCameraManager来管理你的摄像头各种适配
 * @author FaceAISDK.Service@gmail.com
 */
public class UVCCameraManager {
    // 配置UVC协议摄像头的分辨率，参考摄像头能支持的分辨率，mCameraHelper.getSupportedSizeList()可获取
    // 分辨率太高需要高性能的硬件配置。强烈建议摄像头的宽动态值 > 105DB
    public static final int UVC_CAMERA_WIDTH = 640;
    public static final int UVC_CAMERA_HEIGHT = 480;
//    public static final int UVC_CAMERA_WIDTH = 1920;
//    public static final int UVC_CAMERA_HEIGHT = 1080;

    //默认匹配的摄像头关键字，但并不是所有的摄像头命名都规范会带有这种关键字样,你可以手动选择
    public static final String RGB_KEY_DEFAULT ="RGB";
    public static final String IR_KEY_DEFAULT="IR";

    private ICameraHelper mCameraHelper;
    private boolean autoAspectRatio = true;   //摄像头画面自行管理，源码完全开放
    private int previewHeight = UVC_CAMERA_HEIGHT;
    private OnFaceAIAnalysisCallBack faceAIAnalysisCallBack;
    private OnCameraStatusCallBack onCameraStatuesCallBack;

    private CameraBuilder cameraBuilder;
    private Context context;

    private int width=UVC_CAMERA_WIDTH,height=UVC_CAMERA_HEIGHT;
    private Bitmap reuseBitmap=null;

    public interface OnCameraStatusCallBack {
        void onAttach(UsbDevice device);
        void onDeviceOpen(UsbDevice device, boolean isFirstOpen);
    }

    /**
     * 对每帧bitmap 进行分析，如果SDK上一帧还在处理就可以丢弃掉
     */
    public interface OnFaceAIAnalysisCallBack {
        void onBitmapFrame(Bitmap bitmap);
    }


    /**
     * 构造方法
     *
     * @param cameraBuilder
     */
    public UVCCameraManager(CameraBuilder cameraBuilder) {
        this.cameraBuilder = cameraBuilder;
        this.context=cameraBuilder.getContext().getApplicationContext();
        initCameraHelper();
        initUVCCamera();
    }


    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    public void setOnCameraStatuesCallBack(OnCameraStatusCallBack callBack) {
        onCameraStatuesCallBack = callBack;
    }

    /**
     * 使用结束后, 释放 camera
     *
     */
    public void releaseCameraHelper() {
        if (mCameraHelper != null) {

            mCameraHelper.setStateCallback(null );
            mCameraHelper.release();
            mCameraHelper = null;
        }

        faceAIAnalysisCallBack =null;
        onCameraStatuesCallBack = null; // 添加这行
        cameraBuilder=null;
    }


    /**
     * 根据摄像头的名字来选择使用哪个摄像头
     */
    private void initUVCCamera() {
        //不同厂家生产的摄像头有点差异，请开发者自己实现匹配逻辑
        final List<UsbDevice> list = mCameraHelper.getDeviceList();
        boolean isMatched = false;
        for (UsbDevice device : list) {
            String name = device.getProductName();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(context, "Camera ProductName empty", Toast.LENGTH_LONG).show();
            } else if (name.toLowerCase().contains(cameraBuilder.getCameraKey().toLowerCase())) { //忽略大小写
                isMatched = true; //匹配成功了
                mCameraHelper.selectDevice(device);
                //角度旋转，范围为 0 90 180 270
                mCameraHelper.setPreviewConfig(mCameraHelper.getPreviewConfig().setRotation(cameraBuilder.getDegree()));
                //是否水平左右翻转
                if (cameraBuilder.isHorizontalMirror()) {
                    mCameraHelper.setPreviewConfig(mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_HORIZONTAL));
                }
                break;
            }
        }
        if (!isMatched) {
            //Demo 需要允许用户手动去选择设置，傻瓜式操作
            Toast.makeText(context, cameraBuilder.getCameraName() +context.getString(R.string.uvc_camera_match_failed), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 设置回调,给人脸识别SDK分析每帧数据，帧率15～30
     */
    public void setFaceAIAnalysis(OnFaceAIAnalysisCallBack callBack) {
        faceAIAnalysisCallBack = callBack;
    }

    @Nullable
    public Size getCurrentPreviewSize() {
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
            //参考https://github.com/shiyinghan/UVCAndroid demo的MultiCameraNewActivity,
            UVCParam param = new UVCParam();
            param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
            mCameraHelper.openCamera(param);
            if (onCameraStatuesCallBack != null) {
                onCameraStatuesCallBack.onDeviceOpen(device, isFirstOpen);
            }
        }


        @Override
        public void onCameraOpen(UsbDevice device) {
            Size previewSize = null;
            if (previewHeight > 0) {
                // 摄像头支持的Size列表，选择一个合适分辨率和FPS。
                List<Size> supportedSizeList = mCameraHelper.getSupportedSizeList();
                if (supportedSizeList != null) {
                    for (Size size : supportedSizeList) {
                        //选择支持的分辨率
                        //sizeType=5时fps=15,sizeType=7时fps=30
                        if ((size.height == UVC_CAMERA_HEIGHT||size.width ==UVC_CAMERA_WIDTH)
                                && (size.type == 7||size.type == 5)) {
                            previewSize = size;
                            break;
                        }
                    }
                    if (previewSize != null) {
                        mCameraHelper.setPreviewSize(previewSize);
                         width = previewSize.width;
                         height = previewSize.height;
                         if(autoAspectRatio){
                             cameraBuilder.getCameraView().setAspectRatio(width, height);
                         }
                    }else{
                        //设置一个默认的摄像头分辨率
                        Size defSize=supportedSizeList.get(0);
                        mCameraHelper.setPreviewSize(defSize);
                        width = defSize.width;
                        height = defSize.height;
                        if(autoAspectRatio){
                            cameraBuilder.getCameraView().setAspectRatio(width, height);
                        }
                        String tips=context.getString(R.string.uvc_camera_resolution_error,UVC_CAMERA_WIDTH,UVC_CAMERA_HEIGHT,width,height);
                        //无匹配的分辨率提示
                        Toast.makeText(context,  tips, Toast.LENGTH_LONG).show();
                    }
                }
            }

            mCameraHelper.startPreview();

            if (cameraBuilder.getCameraView() != null) {
                mCameraHelper.addSurface(cameraBuilder.getCameraView().getHolder().getSurface(), true);

                mCameraHelper.setFrameCallback(new IFrameCallback() {
                    @Override
                    public void onFrame(ByteBuffer byteBuffer) {
                        //转为bitmap 后
                        if (faceAIAnalysisCallBack != null) {

//                            long t1=System.currentTimeMillis();
                            reuseBitmap = DataConvertUtils.NV21Data2Bitmap(byteBuffer, width, height,
                                    cameraBuilder.getDegree(), cameraBuilder.isHorizontalMirror());
//                            Log.e("DataConvertUtils",width+"转化用时："+(System.currentTimeMillis()-t1));

                            faceAIAnalysisCallBack.onBitmapFrame(reuseBitmap);
                        }
                    }
                }, UVCCamera.PIXEL_FORMAT_NV21);
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (cameraBuilder.getCameraView() != null) {
//                initCameraHelper();
                mCameraHelper.removeSurface(cameraBuilder.getCameraView().getHolder().getSurface());
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
