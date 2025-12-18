package com.faceAI.demo.SysCamera.camera;


import android.content.Context;
import android.view.Surface;

/**
 * 参数构造，后期需要添加更多的配置
 * <p>
 * <p>
 * 2022.07.30 SZ
 */
public class FaceCameraXBuilder {
    private final float linearZoom;
    private final int cameraLensFacing; //默认是前置摄像头
    private final int rotation; //默认是前置摄像头
    private final boolean cameraSizeHigh; //是否高分辨率


    private FaceCameraXBuilder(Builder builder) {
        this.linearZoom = builder.linearZoom;
        this.cameraLensFacing = builder.cameraLensFacing;
        this.rotation = builder.rotation;
        this.cameraSizeHigh = builder.cameraSizeHigh;
    }

    public float getLinearZoom() {
        return linearZoom;
    }

    public int getCameraLensFacing() {
        return cameraLensFacing;
    }

    public int getRotation() {
        return rotation;
    }

    public boolean getCameraSizeHigh() {
        return cameraSizeHigh;
    }

    public static class Builder {
        private Context context;           //上下文环境，
        private float linearZoom = 0f;    //默认的大小
        private int cameraLensFacing = 0; //默认是前置摄像头
        private boolean cameraSizeHigh =false; //摄像头分辨率

        private int rotation = Surface.ROTATION_0; //默认是前置摄像头
        public Builder() {
        }

        public Builder setCameraLensFacing(int cameraLensFacing) {
            this.cameraLensFacing = cameraLensFacing;
            return this;
        }

        public Builder setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder setCameraSizeHigh(boolean cameraSizeHigh) {
            this.cameraSizeHigh = cameraSizeHigh;
            return this;
        }

        public Builder setLinearZoom(float linearZoom) {
            this.linearZoom = linearZoom;
            return this;
        }

        public FaceCameraXBuilder create() { // 构建，返回一个新对象
            return new FaceCameraXBuilder(this);
        }
    }


}