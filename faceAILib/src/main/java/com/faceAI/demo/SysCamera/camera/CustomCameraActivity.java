package com.faceAI.demo.SysCamera.camera;

import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.camera.core.CameraSelector;

import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.R;
import com.faceAI.demo.base.AbsBaseActivity;

import java.nio.ByteBuffer;

/**
 * 自定义调试管理摄像头，把SDK 中的源码暴露出来放在 {@link FaceCameraXFragment}
 *
 */
public class CustomCameraActivity extends AbsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);
        setTitle("Custom Camera");

        findViewById(R.id.back).setOnClickListener(v -> {
            finish();
        });

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        int cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT);
        int degree = sharedPref.getInt( SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        //画面旋转方向 默认屏幕方向Display.getRotation()和Surface.ROTATION_0,_90,_180,_270
        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing) //前后摄像头
                .setLinearZoom(0.0001f)    //焦距范围[0f,1.0f]，参考{@link CameraControl#setLinearZoom(float)}
                .setRotation(degree)      //画面旋转方向
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing()) {
                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] data = toByteArray(buffer);
                // Convert bytes to unsigned integers and calculate average
                double sum = 0;
                for (byte b : data) {
                    sum += (b & 0xFF); // Convert to unsigned int
                }
                double luma = sum / data.length;
                Log.d("luma","luma = "+luma);
            }
        });
    }


    private byte[] toByteArray(ByteBuffer buffer) {
        buffer.rewind();    // Rewind the buffer to zero
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);   // Copy the buffer into a byte array
        return data;        // Return the byte array
    }


}

