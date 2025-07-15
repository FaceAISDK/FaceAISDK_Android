package com.faceAI.demo.SysCamera.diyCamera;

import android.graphics.ImageFormat;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.faceAI.demo.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * 系统相机CameraX的管理，相机管理SDK提供了默认的一套CameraX 封装，
 * 用户可能需要高级定制，可以根据CustomCameraXFragment自行改造
 *
 * 更多参考Google 官方：https://developer.android.com/media/camera/camerax?hl=zh-cn
 */
public class CustomCameraXFragment extends Fragment {

    private final int cameraLensFacing = CameraSelector.LENS_FACING_FRONT; //默认是前置摄像头
    private final int rotation = Surface.ROTATION_0; //默认角度
    private final float linearZoom = 0.01f;

    private PreviewView previewView;
    private float scaleX = 0f, scaleY = 0f;

    private onAnalyzeData analyzeDataCallBack;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;

    private View rootView;

    public CustomCameraXFragment() {
        // Required empty public constructor
    }

    public void setOnAnalyzerListener(onAnalyzeData callback) {
        this.analyzeDataCallBack = callback;
    }

    public interface onAnalyzeData {
        void analyze(@NonNull ImageProxy imageProxy); //Default
    }

    public static CustomCameraXFragment newInstance() {
        CustomCameraXFragment fragment = new CustomCameraXFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        initCameraXSetting();
    }


    /**
     * 初始化相机,使用CameraX
     *
     */
    private void initCameraXSetting() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        //图像预览和摄像头原始数据回调 暴露，以便后期格式转换和处理
        //图像编码默认格式 YUV_420_888。
        cameraProviderFuture.addListener(() -> {

            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceAI SDK", "\ncameraProviderFuture.get() 发生错误！\n" + e.toString());
            }


            //分辨率不用设置太高，太高负荷重，关键是摄像头的成像能力，
            Preview preview = new Preview.Builder()
                    .setTargetRotation(rotation)
                    .setTargetResolution(new Size(960, 720))
                    .build();

            ImageAnalysis imageAnalysis= new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(rotation)
                    .setTargetResolution(new Size(960, 720))
                    .build();


            cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraLensFacing)
                        .build();

            previewView = rootView.findViewById(R.id.previewView);

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
                    imageProxy -> {
                        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
                            throw new IllegalArgumentException("Invalid image format");
                        }

                        if (scaleX == 0f || scaleY == 0f) {
                            setScaleXY(imageProxy);
                        }

                        try {
                            if (analyzeDataCallBack != null) {
                                analyzeDataCallBack.analyze(imageProxy);
                            }
                        }catch (Exception e){
                            Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
                        }finally {
                            imageProxy.close();
                        }

                    });

            try {
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview, imageAnalysis);
                camera.getCameraControl().setLinearZoom(linearZoom);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

            } catch (Exception e) {
                Log.e("CameraX error", "FaceAI SDK:" + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 计算缩放比例
     *
     */
    private void  setScaleXY(ImageProxy imageProxy){
        float max = imageProxy.getWidth();
        float min = imageProxy.getHeight();
        if (max < min) { //交换
            float temp = max;
            max = min;
            min = temp;
        }
        if (previewView.getWidth() > previewView.getHeight()) {
            scaleX = (float) previewView.getWidth() / max;
            scaleY = (float) previewView.getHeight() / min;
        } else {
            scaleX = (float) previewView.getWidth() / min;
            scaleY = (float) previewView.getHeight() / max;
        }
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }


    //释放相机，重新初始化
    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraProvider.unbindAll();
    }


}