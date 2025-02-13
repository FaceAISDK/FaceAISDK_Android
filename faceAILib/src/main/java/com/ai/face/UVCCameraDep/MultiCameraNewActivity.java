//package com.ai.face.usbCamera;
//
//import android.hardware.usb.UsbDevice;
//import android.os.Bundle;
//import android.os.ConditionVariable;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.util.Log;
//import android.view.SurfaceHolder;
//import android.view.View;
//import android.widget.Button;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.ai.face.R;
//import com.herohan.uvcapp.CameraHelper;
//import com.herohan.uvcapp.ICameraHelper;
//import com.serenegiant.usb.IFrameCallback;
//import com.serenegiant.usb.Size;
//import com.serenegiant.usb.UVCCamera;
//import com.serenegiant.usb.UVCParam;
//import com.serenegiant.widget.AspectRatioSurfaceView;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
///**
// * 多个USB Camera
// *
// */
//public class MultiCameraNewActivity extends AppCompatActivity implements View.OnClickListener {
//
//    private static final boolean DEBUG = true;
//    private static final String TAG = MultiCameraNewActivity.class.getSimpleName();
//
//    private static final int DEFAULT_WIDTH = 640;
//    private static final int DEFAULT_HEIGHT = 480;
//
//    private ICameraHelper mCameraHelperRGB;
//    private ICameraHelper mCameraHelperIR;
//
//    private AspectRatioSurfaceView svCameraViewRGB;
//    private AspectRatioSurfaceView svCameraViewIR;
//
//    private UsbDevice mUsbDeviceRGB;
//    private UsbDevice mUsbDeviceIR;
//    private ConcurrentLinkedQueue<UsbDevice> mReadyUsbDeviceList = new ConcurrentLinkedQueue<>();
//    private ConditionVariable mReadyDeviceConditionVariable = new ConditionVariable();
//
//    private final Object mSync = new Object();
//
//    private boolean mIsCameraRGBConnected = false;
//    private boolean mIsCameraIRConnected = false;
//
//    private DeviceListDialogFragment mDeviceListDialogRGB;
//    private DeviceListDialogFragment mDeviceListDialogIR;
//
//    private HandlerThread mHandlerThread;
//    private Handler mAsyncHandler;
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_multi_camera_new);
//        setTitle("multi_camera_new");
//
//        initViews();
//        initListeners();
//
//        mHandlerThread = new HandlerThread(TAG);
//        mHandlerThread.start();
//        mAsyncHandler = new Handler(mHandlerThread.getLooper());
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        mHandlerThread.quitSafely();
//        mAsyncHandler.removeCallbacksAndMessages(null);
//    }
//
//    private void initViews() {
//        setCameraViewRGB();
//        setCameraViewIR();
//    }
//
//    private void initListeners() {
//        Button btnOpenCameraRGB = findViewById(R.id.btnOpenCameraRGB);
//        btnOpenCameraRGB.setOnClickListener(this);
//        Button btnCloseCameraRGB = findViewById(R.id.btnCloseCameraRGB);
//        btnCloseCameraRGB.setOnClickListener(this);
//
//        Button btnOpenCameraIR = findViewById(R.id.btnOpenCameraIR);
//        btnOpenCameraIR.setOnClickListener(this);
//        Button btnCloseCameraIR = findViewById(R.id.btnCloseCameraIR);
//        btnCloseCameraIR.setOnClickListener(this);
//    }
//
//    private void setCameraViewRGB() {
//        svCameraViewRGB = findViewById(R.id.svCameraViewRGB);
//        svCameraViewRGB.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//
//        svCameraViewRGB.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(@NonNull SurfaceHolder holder) {
//                if (mCameraHelperRGB != null) {
//                    mCameraHelperRGB.addSurface(holder.getSurface(), false);
//                }
//            }
//
//            @Override
//            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//                if (mCameraHelperRGB != null) {
//                    mCameraHelperRGB.removeSurface(holder.getSurface());
//                }
//            }
//        });
//    }
//
//    private void setCameraViewIR() {
//        svCameraViewIR = findViewById(R.id.svCameraViewIR);
//        svCameraViewIR.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//
//        svCameraViewIR.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(@NonNull SurfaceHolder holder) {
//                if (mCameraHelperIR != null) {
//                    mCameraHelperIR.addSurface(holder.getSurface(), false);
//                }
//            }
//
//            @Override
//            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//                if (mCameraHelperIR != null) {
//                    mCameraHelperIR.removeSurface(holder.getSurface());
//                }
//            }
//        });
//    }
//
//    @Override
//    protected void onStart() {
//        if (DEBUG) Log.d(TAG, "onStart:");
//        super.onStart();
//        initCameraHelper();
//    }
//
//    @Override
//    protected void onStop() {
//        if (DEBUG) Log.d(TAG, "onStop:");
//        super.onStop();
//        clearCameraHelper();
//    }
//
//    public void initCameraHelper() {
//        if (DEBUG) Log.d(TAG, "initCameraHelper:");
//        if (mCameraHelperRGB == null) {
//            mCameraHelperRGB = new CameraHelper();
//            mCameraHelperRGB.setStateCallback(mStateListenerRGB);
//        }
//
//        if (mCameraHelperIR == null) {
//            mCameraHelperIR = new CameraHelper();
//            mCameraHelperIR.setStateCallback(mStateListenerIR);
//        }
//    }
//
//    private void clearCameraHelper() {
//        if (DEBUG) Log.d(TAG, "clearCameraHelper:");
//        if (mCameraHelperRGB != null) {
//            mCameraHelperRGB.release();
//            mCameraHelperRGB = null;
//        }
//
//        if (mCameraHelperIR != null) {
//            mCameraHelperIR.release();
//            mCameraHelperIR = null;
//        }
//    }
//
//    private void selectDeviceRGB(final UsbDevice device) {
//        if (DEBUG) Log.v(TAG, "selectDeviceRGB:device=" + device.getDeviceName());
//        mUsbDeviceRGB = device;
//
//        mAsyncHandler.post(() -> {
//            waitCanSelectDevice(device);
//
//            if (mCameraHelperRGB != null) {
//                mCameraHelperRGB.selectDevice(device);
//            }
//        });
//    }
//
//    private void selectDeviceIR(final UsbDevice device) {
//        if (DEBUG) Log.v(TAG, "selectDeviceIR:device=" + device.getDeviceName());
//        mUsbDeviceIR = device;
//
//        mAsyncHandler.post(() -> {
//            waitCanSelectDevice(device);
//
//            if (mCameraHelperIR != null) {
//                mCameraHelperIR.selectDevice(device);
//            }
//        });
//    }
//
//    /**
//     * wait for only one camera need request permission
//     *
//     * @param device
//     */
//    private void waitCanSelectDevice(UsbDevice device) {
//        mReadyUsbDeviceList.add(device);
//        while (mReadyUsbDeviceList.size() > 1) {
//            mReadyDeviceConditionVariable.block();
//            mReadyDeviceConditionVariable.close();
//        }
//    }
//
//    /**
//     * remove ready camera that wait  for select
//     *
//     * @param device
//     */
//    private void removeSelectedDevice(UsbDevice device) {
//        mReadyUsbDeviceList.remove(device);
//        mReadyDeviceConditionVariable.open();
//    }
//
//    private final ICameraHelper.StateCallback mStateListenerRGB = new ICameraHelper.StateCallback() {
//        private final String LOG_PREFIX = "ListenerRGB#";
//
//        @Override
//        public void onAttach(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onAttach:");
//            synchronized (mSync) {
//                if (mUsbDeviceRGB == null && !device.equals(mUsbDeviceIR)) {
//                    selectDeviceRGB(device);
//                }
//            }
//        }
//
//        @Override
//        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceOpen:");
//            if (mCameraHelperRGB != null && device.equals(mUsbDeviceRGB)) {
//                UVCParam param = new UVCParam();
//                param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
//                mCameraHelperRGB.openCamera(param);
//            }
//
//            removeSelectedDevice(device);
//        }
//
//        @Override
//        public void onCameraOpen(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraOpen:");
//            if (mCameraHelperRGB != null && device.equals(mUsbDeviceRGB)) {
//                mCameraHelperRGB.startPreview();
//
//                Size size = mCameraHelperRGB.getPreviewSize();
//                if (size != null) {
//                    int width = size.width;
//                    int height = size.height;
//                    //auto aspect ratio
//                    svCameraViewRGB.setAspectRatio(width, height);
//                }
//
//                mCameraHelperRGB.addSurface(svCameraViewRGB.getHolder().getSurface(), false);
//
//
////                mCameraHelperRGB.setFrameCallback(new IFrameCallback() {
////                    @Override
////                    public void onFrame(ByteBuffer byteBuffer) {
////
////                    }
////                },UVCCamera.PIXEL_FORMAT_BGR);
//
////                mCameraHelperRGB.setFrameCallback((ByteBuffer frame) -> {
////                    if (mCustomFPS != null) {
////                        //Refresh FPS
////                        mCustomFPS.doFrame();
////                    }
//
////                    byte[] nv21 = new byte[frame.remaining()];
////                    frame.get(nv21, 0, nv21.length);
////                    Bitmap bitmap = mNv21ToBitmapRGB.nv21ToBitmap(nv21, size.width, size.height);
////                }, UVCCamera.PIXEL_FORMAT_NV21);
//
//
//
//                mIsCameraRGBConnected = true;
//            }
//        }
//
//        @Override
//        public void onCameraClose(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraClose:");
//            if (device.equals(mUsbDeviceRGB)) {
//                if (mCameraHelperRGB != null) {
//                    mCameraHelperRGB.removeSurface(svCameraViewRGB.getHolder().getSurface());
//                }
//
//                mIsCameraRGBConnected = false;
//            }
//        }
//
//        @Override
//        public void onDeviceClose(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceClose:");
//        }
//
//        @Override
//        public void onDetach(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDetach:");
//            if (device.equals(mUsbDeviceRGB)) {
//                mUsbDeviceRGB = null;
//            }
//
//            removeSelectedDevice(device);
//        }
//
//        @Override
//        public void onCancel(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCancel:");
//            if (device.equals(mUsbDeviceRGB)) {
//                mUsbDeviceRGB = null;
//            }
//
//            removeSelectedDevice(device);
//        }
//    };
//
//    private final ICameraHelper.StateCallback mStateListenerIR = new ICameraHelper.StateCallback() {
//        private final String LOG_PREFIX = "ListenerIR#";
//
//        @Override
//        public void onAttach(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onAttach:");
//            synchronized (mSync) {
//                if (mUsbDeviceIR == null && !device.equals(mUsbDeviceRGB)) {
//                    selectDeviceIR(device);
//                }
//            }
//        }
//
//        @Override
//        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceOpen:");
//            if (mCameraHelperIR != null && device.equals(mUsbDeviceIR)) {
//                UVCParam param = new UVCParam();
//                param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
//                mCameraHelperIR.openCamera(param);
//            }
//
//            removeSelectedDevice(device);
//        }
//
//        @Override
//        public void onCameraOpen(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraOpen:");
//            if (mCameraHelperIR != null && device.equals(mUsbDeviceIR)) {
//                mCameraHelperIR.startPreview();
//
//                Size size = mCameraHelperIR.getPreviewSize();
//                if (size != null) {
//                    int width = size.width;
//                    int height = size.height;
//                    //auto aspect ratio
//                    svCameraViewIR.setAspectRatio(width, height);
//                }
//
//                mCameraHelperIR.addSurface(svCameraViewIR.getHolder().getSurface(), false);
//
////                mCameraHelperIR.setFrameCallback(frame -> {
//////                    if (mCustomFPS != null) {
//////                        //Refresh FPS
//////                        mCustomFPS.doFrame();
//////                    }
////                    byte[] nv21 = new byte[frame.remaining()];
////                    frame.get(nv21, 0, nv21.length);
////                    Bitmap bitmap = mNv21ToBitmapRGB.nv21ToBitmap(nv21, size.width, size.height);
////                }, UVCCamera.PIXEL_FORMAT_NV21);
//
//                mIsCameraIRConnected = true;
//            }
//        }
//
//        @Override
//        public void onCameraClose(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraClose:");
//            if (device.equals(mUsbDeviceIR)) {
//                if (mCameraHelperIR != null) {
//                    mCameraHelperIR.removeSurface(svCameraViewIR.getHolder().getSurface());
//                }
//
//                mIsCameraIRConnected = false;
//            }
//        }
//
//        @Override
//        public void onDeviceClose(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceClose:");
//        }
//
//        @Override
//        public void onDetach(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDetach:");
//            if (device.equals(mUsbDeviceIR)) {
//                mUsbDeviceIR = null;
//            }
//
//            removeSelectedDevice(device);
//        }
//
//        @Override
//        public void onCancel(UsbDevice device) {
//            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCancel:");
//            if (device.equals(mUsbDeviceIR)) {
//                mUsbDeviceIR = null;
//            }
//
//            removeSelectedDevice(device);
//        }
//    };
//
//    @Override
//    public void onClick(View v) {
//        if (v.getId() == R.id.btnOpenCameraRGB) {
//            // select a uvc device
//            showDeviceListDialogRGB();
//        } else if (v.getId() == R.id.btnCloseCameraRGB) {
//            // close camera
//            if (mCameraHelperRGB != null && mIsCameraRGBConnected) {
//                mCameraHelperRGB.closeCamera();
//            }
//        } else if (v.getId() == R.id.btnOpenCameraIR) {
//            // select a uvc device
//            showDeviceListDialogIR();
//        } else if (v.getId() == R.id.btnCloseCameraIR) {
//            // close camera
//            if (mCameraHelperIR != null && mIsCameraIRConnected) {
//                mCameraHelperIR.closeCamera();
//            }
//        }
//    }
//
//    private void showDeviceListDialogRGB() {
//        mDeviceListDialogRGB = new DeviceListDialogFragment(mCameraHelperRGB, mIsCameraRGBConnected ? mUsbDeviceRGB : null);
//        mDeviceListDialogRGB.setOnDeviceItemSelectListener(usbDevice -> {
//            if (mCameraHelperRGB != null && mIsCameraRGBConnected) {
//                mCameraHelperRGB.closeCamera();
//            }
//            selectDeviceRGB(usbDevice);
//        });
//
//        mDeviceListDialogRGB.show(getSupportFragmentManager(), "device_list_rgb");
//    }
//
//    private void showDeviceListDialogIR() {
//        mDeviceListDialogIR = new DeviceListDialogFragment(mCameraHelperIR, mIsCameraIRConnected ? mUsbDeviceIR : null);
//        mDeviceListDialogIR.setOnDeviceItemSelectListener(usbDevice -> {
//            if (mCameraHelperIR != null && mIsCameraIRConnected) {
//                mCameraHelperIR.closeCamera();
//            }
//            selectDeviceIR(usbDevice);
//        });
//
//        mDeviceListDialogIR.show(getSupportFragmentManager(), "device_list_ir");
//    }
//}