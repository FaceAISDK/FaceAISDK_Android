package com.faceAI.demo.SysCamera.search;

/**
 * https://github.com/FaceAISDK/FaceAISDK_Android
 */
public class ImageBean {
    public String path;
    public String name;

    public ImageBean(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
