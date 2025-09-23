package com.faceAI.demo.SysCamera.camera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.ai.face.base.view.camera.CameraXBuilder;


/**
 * 老相机。https://gxh-apologize.github.io/2019/05/24/Camera%E7%9A%84%E4%B8%80%E4%BA%9B%E6%80%BB%E7%BB%93/
 * 暂未完善
 */
public class Camera1Fragment extends Fragment {
    private Camera1Preview mPreview;
    private Camera mCamera;
    int mNumberOfCameras;
    int mCurrentCamera;  // Camera ID currently chosen
    int mCameraCurrentlyLocked;  // Camera ID that's actually acquired
    // The first rear facing camera
    int mDefaultCameraId;

    private Camera1Preview.OnCameraData onCameraData;



    public static Camera1Fragment newInstance(CameraXBuilder cameraXBuilder) {
        Camera1Fragment fragment = new Camera1Fragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 设置相机数据回调
     * @param onCameraData
     */
    public void setCameraDataCallBack(Camera1Preview.OnCameraData onCameraData) {
        this.onCameraData=onCameraData;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create a container that will hold a SurfaceView for camera previews
        mPreview = new Camera1Preview(this.getActivity());
        // Find the total number of cameras available
        mNumberOfCameras = Camera.getNumberOfCameras();
        // Find the ID of the rear-facing ("default") camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                mCurrentCamera = mDefaultCameraId = i;
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mPreview;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Use mCurrentCamera to select the camera desired to safely restore
        // the fragment after the camera has been changed
        mCamera = Camera.open(mCurrentCamera);
        mCameraCurrentlyLocked = mCurrentCamera;
        mPreview.setCamera(mCamera);
        mPreview.setOnCameraData(onCameraData);

    }

    @Override
    public void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

}

