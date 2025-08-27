package com.faceAI.demo.UVCCamera.manger;

import android.content.Context;
import com.serenegiant.widget.AspectRatioSurfaceView;


/**
 * 构建管理USB摄像头（UVC协议）的参数集合Builder
 *
 */
public final  class CameraBuilder {
    private final String cameraName;  // RGB 摄像头还是， 红外IR摄像头
    private final String cameraKey;   //RGB，IR 红外摄像头设备device.getProductName() 一般会有RGB/IR 字样关键字，也有不规范命名
    private final Context context;    //上下文
    private final AspectRatioSurfaceView cameraView; //预览界面
    private final int degree;         //摄像头旋转角度，0，90，180，270
    private final boolean horizontalMirror;   //是否水平镜像
//    private boolean verticalMirror;     //是否垂直镜像

    private CameraBuilder(Builder builder) {
        this.cameraName= builder.cameraName;
        this.cameraKey = builder.cameraKey;
        this.context = builder.context;
        this.cameraView = builder.cameraView;
        this.degree = builder.degree;
        this.horizontalMirror = builder.horizontalMirror;
//        this.verticalMirror = builder.verticalMirror;
    }


    public static class Builder {
        private String cameraName;  // RGB 摄像头还是， 红外IR摄像头
        private String cameraKey;   //RGB，IR 红外摄像头设备device.getProductName() 一般会有RGB/IR 字样关键字，也有不规范命名
        private Context context;    //上下文
        private AspectRatioSurfaceView cameraView; //预览界面
        private int degree;         //摄像头旋转角度，0，90，180，270
        private boolean horizontalMirror;   //是否水平镜像
//        private boolean verticalMirror;     //是否垂直镜像

        public Builder setCameraName(String cameraName) {
            this.cameraName = cameraName;
            return this;
        }

        //RGB，IR 红外摄像头设备device.getProductName() 一般会有RGB/IR 字样关键字，也有不规范命名
        public Builder setCameraKey(String cameraKey) {
            this.cameraKey = cameraKey;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setCameraView(AspectRatioSurfaceView cameraView) {
            this.cameraView = cameraView;
            return this;
        }

        public Builder setDegree(int degree) {
            this.degree = degree;
            return this;
        }

        public Builder setHorizontalMirror(boolean horizontalMirror) {
            this.horizontalMirror = horizontalMirror;
            return this;
        }

//        public Builder setVerticalMirror(boolean verticalMirror) {
//            this.verticalMirror = verticalMirror;
//            return this;
//        }


        public CameraBuilder build() {
            return new CameraBuilder(this);
        }
    }


    public String getCameraName() {
        return cameraName;
    }

    public String getCameraKey() {
        return cameraKey;
    }

    public Context getContext() {
        return context;
    }

    public AspectRatioSurfaceView getCameraView() {
        return cameraView;
    }

    public int getDegree() {
        return degree;
    }

    public boolean isHorizontalMirror() {
        return horizontalMirror;
    }

//    public boolean isVerticalMirror() {
//        return verticalMirror;
//    }
}