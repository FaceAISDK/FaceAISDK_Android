package com.ai.face.usbCamera;//package com.ai.face.usbCamera;
//
//import static android.view.View.VISIBLE;
//
//import android.content.Context;
//import android.hardware.usb.UsbDevice;
//import android.os.Build;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.ai.face.databinding.FragmentBinocularCameraBinding;
//import com.jiangdg.ausbc.MultiCameraClient;
//import com.jiangdg.ausbc.base.MultiCameraFragment;
//import com.jiangdg.ausbc.callback.ICameraStateCallBack;
//import com.jiangdg.ausbc.camera.CameraUVC;
//import com.jiangdg.ausbc.camera.bean.CameraRequest;
//import com.jiangdg.ausbc.utils.ToastUtils;
//
///**
// * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
// *
// */
//public class BinocularUVCCameraFragment extends MultiCameraFragment implements ICameraStateCallBack {
//    private static final String RBG_CAMERA = "RGB"; //USB RGB 摄像头名字中的关键字
//    private static final String IR_CAMERA = "IR";  //USB IR 摄像头名字中的关键字
//
//    private FragmentBinocularCameraBinding mViewBinding;
//    private MultiCameraClient.ICamera rgbCamera;
//    private MultiCameraClient.ICamera irCamera;
//
//    public BinocularUVCCameraFragment() {
//        // Required empty public constructor
//    }
//
//
//    @Override
//    protected void onCameraAttached(@NonNull MultiCameraClient.ICamera camera) {
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            //如果你的系统还小于Android 5.0 可能getProductName 会为空，自行适配
//            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现",Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        String productName = camera.getUsbDevice().getProductName();
//
//        if (!TextUtils.isEmpty(productName)&&productName.contains("RGB")) {
//            rgbCamera = camera;
//        }
//
//        if (!TextUtils.isEmpty(productName)&&productName.contains("IR")) {
//            irCamera = camera;
//        }
//        mViewBinding.multiCameraTip.setVisibility(View.GONE);
//    }
//
//    @Override
//    protected void onCameraDetached(@NonNull MultiCameraClient.ICamera camera) {
//        camera.closeCamera();
//
//        //两个都没有打开
//        if (!irCamera.isCameraOpened() && !rgbCamera.isCameraOpened()) {
//            mViewBinding.multiCameraTip.setVisibility(VISIBLE);
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            String productName = camera.getUsbDevice().getProductName();
//            if (!TextUtils.isEmpty(productName)&&productName.contains("RGB")) {
//                rgbCamera = null;
//            }
//
//            if (!TextUtils.isEmpty(productName)&&productName.contains("IR")) {
//                irCamera = null;
//            }
//        }else {
//            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现",Toast.LENGTH_LONG).show();
//        }
//
//    }
//
//    @NonNull
//    @Override
//    public MultiCameraClient.ICamera generateCamera(@NonNull Context context, @NonNull UsbDevice usbDevice) {
//        return new CameraUVC(context, usbDevice);
//    }
//
//
//    @Override
//    protected void onCameraConnected(@NonNull MultiCameraClient.ICamera camera) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            String productName = camera.getUsbDevice().getProductName();
//            Log.d("onCameraConnected", "---  发现新摄像头: --- "+productName);
//
//            if (!TextUtils.isEmpty(productName)&&productName.contains("RGB")) {
//                camera.openCamera(mViewBinding.rgbCameraTextureView, getCameraRequest());
//                camera.setCameraStateCallBack(this);
//
//                camera.addPreviewDataCallBack((bytes, i, i1, dataFormat) -> {
//                    Log.e("BBBBBBBBBB", "RGB onPreviewData: "+dataFormat.name());
//                });
//
//                UsbDevice device = camera.getUsbDevice();
//                if (!hasPermission(device)) {
//                    requestPermission(device);
//                }
//            }
//
//            if (!TextUtils.isEmpty(productName)&&productName.contains("IR")) {
//                camera.openCamera(mViewBinding.irCameraTextureView, getCameraRequest());
//                camera.setCameraStateCallBack(this);
//
//                camera.addPreviewDataCallBack((bytes, i, i1, dataFormat) -> {
//                    Log.e("BBBBBBBBBB", "IR onPreviewData: "+dataFormat.name());
//                });
//
//                UsbDevice device = camera.getUsbDevice();
//                if (!hasPermission(device)) {
//                    requestPermission(device);
//                }
//            }
//
//        }else{
//            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现",Toast.LENGTH_LONG).show();
//        }
//
//    }
//
//    @Override
//    protected void onCameraDisConnected(@NonNull MultiCameraClient.ICamera iCamera) {
//        iCamera.closeCamera();
//    }
//
//
//    @Override
//    public void onCameraState(@NonNull MultiCameraClient.ICamera iCamera, @NonNull State code, @Nullable String s) {
//        if (code == State.ERROR) {
//            ToastUtils.show(s + "错误");
//        }
//    }
//
//
//    @Override
//    protected void initView() {
//        super.initView();
//
//        openDebug(true);
//    }
//
//    @Nullable
//    @Override
//    protected View getRootView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup) {
//        mViewBinding = FragmentBinocularCameraBinding.inflate(layoutInflater, viewGroup, false);
//        return mViewBinding.getRoot();
//    }
//
//    private CameraRequest getCameraRequest() {
//        return new CameraRequest.Builder()
//                .setPreviewWidth(640)
//                .setPreviewHeight(480)
//                .create();
//    }
//
//
//}