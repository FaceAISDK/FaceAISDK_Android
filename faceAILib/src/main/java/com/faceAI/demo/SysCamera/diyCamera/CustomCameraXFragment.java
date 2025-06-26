package com.faceAI.demo.SysCamera.diyCamera;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.faceAI.demo.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * 这是系统相机，通过系统API 组件CameraX 处理，我们的手机平板自带的相机就是这种 （不是UVC协议USB 摄像头哈）
 * <p>
 * SDK 内部的相机管理裁剪后暴露出来，使用的是CameraX, 更多效果可以在这里尝试
 * 需要定制效果，把尝试成功的设置发给我，我们安排SDK支持
 */
public class CustomCameraXFragment extends Fragment {

    //人脸识别不需要太高分辨率，关键是摄像头要宽动态高清成像能力，分辨率太高需要高配置硬件处理
    public enum SIZE {
        DEFAULT,  //640*480
        MIDDLE,   //960*720
        HIGH_DEP  //1920*1080
    }

    private static final String CAMERA_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM";  //缩放比例
    private static final String CAMERA_LENS_FACING = "CAMERA_LENS_FACING";  //前后配置
    private static final String CAMERA_ROTATION = "CAMERA_ROTATION";  //旋转

    private static final String CAMERA_SIZE = "CAMERA_SIZE";  //旋转

    private SIZE cameraSize = SIZE.DEFAULT;
    private int cameraLensFacing = 0; //默认是前置摄像头
    private int rotation = Surface.ROTATION_0; //默认是前置摄像头

    private float linearZoom = 0.01f; //默认是后置摄像头

    private PreviewView previewView;

    private CameraSelector cameraSelector;

    private ProcessCameraProvider cameraProvider;

    private View rootView;

    public CustomCameraXFragment() {
        // Required empty public constructor
    }

//    setZoomRatio(float)作用： 按比例设置当前缩放, 有些相机不支持焦距可以试试这种

    public static CustomCameraXFragment newInstance() {
        CustomCameraXFragment xFragment = new CustomCameraXFragment();
        return xFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraLensFacing = getArguments().getInt(CAMERA_LENS_FACING, 0); //默认的摄像头
            linearZoom = getArguments().getFloat(CAMERA_LINEAR_ZOOM, 0.01f);
            rotation = getArguments().getInt(CAMERA_ROTATION, Surface.ROTATION_0);
            cameraSize = (SIZE) getArguments().getSerializable(CAMERA_SIZE);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_custom_camera, container, false);
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initCameraXAnalysis();
    }


    /**
     * 初始化相机,使用CameraX
     */
    private void initCameraXAnalysis() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        //图像预览和摄像头原始数据回调 暴露，以便后期格式转换和处理
        //图像编码默认格式 YUV_420_888。
        cameraProviderFuture.addListener(() -> {

            // Camera provider is now guaranteed to be available
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceAI SDK", "\ncameraProviderFuture.get() 发生错误！\n" + e.toString());
            }

            Preview preview;
            switch (cameraSize) {
                case MIDDLE -> {
                    preview = new Preview.Builder()
                            .setTargetRotation(rotation)
                            .setTargetResolution(new Size(960, 720))
                            .build();

                }
                case HIGH_DEP -> {
                    preview = new Preview.Builder()
                            .setTargetRotation(rotation)
                            .setTargetResolution(new Size(1920, 1080))
                            .build();
                } //下面两种都是默认
                default -> {
                    preview = new Preview.Builder()
                            .setTargetRotation(rotation)
                            .build();

                }
            }
            ;

            if (cameraLensFacing == 0) {
                // Choose the camera by requiring a lens facing
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
            } else {
                // Choose the camera by requiring a lens facing
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
            }


            previewView = rootView.findViewById(R.id.previewView);

            previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);


            try {
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview);

                camera.getCameraControl().setLinearZoom(linearZoom);

                //0-1f
                camera.getCameraControl().setZoomRatio(0.8f);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }



}