package com.faceAI.demo.SysCamera.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import java.io.IOException;
import java.util.List;

/**
 * 仅供参考，暂未完善
 *
 * A simple wrapper around a Camera and a SurfaceView that renders a centered
 * preview of the Camera to the surface. We need to center the SurfaceView
 * because not all devices have cameras that support preview sizes at the same
 * aspect ratio as the device's display.
 */
public class Camera1Preview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera.Size mPreviewSize;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera mCamera;
    boolean mSurfaceCreated = false;

    private OnCameraData onCameraData;
    private Context context;

    public interface OnCameraData {
        void callBack(byte[] bytes, Camera camera); //Default
    }

    public void setOnCameraData(OnCameraData onCameraData) {
        this.onCameraData = onCameraData;
    }

    Camera1Preview(Context context) {
        super(context);
        this.context=context;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters()
                    .getSupportedPreviewSizes();
            if (mSurfaceCreated) requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
                    height);
        }

    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewHeight  = mPreviewSize.width;
                previewWidth = mPreviewSize.height;
            }
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height
                        / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width
                        / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }

        }
    }
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        if (mPreviewSize == null) requestLayout();
        mSurfaceCreated = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            if(size.width == 640 && size.height == 480) {
                optimalSize=size;
            }
        }
        return optimalSize;
    }



    public  int getCameraDisplayOrientation(Context context,
                                                   int cameraId) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = switch (rotation) {
            case Surface.ROTATION_0 -> 0;
            case Surface.ROTATION_90 -> 90;
            case Surface.ROTATION_180 -> 180;
            case Surface.ROTATION_270 -> 270;
            default -> 0;
        };

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }


    public int getRotation(int orientation) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(1, info);
        orientation = (orientation + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        return rotation;
    }




    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        holder.setFormat(ImageFormat.NV21);

        Camera.Parameters parameters = mCamera.getParameters();
        //转90度，反过来
        parameters.setPreviewSize(mPreviewSize.width,mPreviewSize.height);

//        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//        if (display.getRotation() == Surface.ROTATION_0) {
//            mCamera.setDisplayOrientation(90);
//        } else if (display.getRotation() == Surface.ROTATION_270) {
//            mCamera.setDisplayOrientation(180);
//        }

        int result=getCameraDisplayOrientation(context,1);
        mCamera.setDisplayOrientation(result);

        parameters.setRotation(getRotation(result));

        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.setPreviewCallback((bytes, camera) -> {
            if(onCameraData!=null){
                onCameraData.callBack(bytes,camera);
            }
        });
        requestLayout();
    }


}
