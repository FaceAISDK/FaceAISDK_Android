package com.faceAI.demo.SysCamera.search;

/**
 * https://github.com/FaceAISDK/FaceAISDK_Android
 */
public class ImageBean {
    public String path; //完整的路径
    public String name; //仅仅名字
    public long lastModified; // 新增字段

    public ImageBean(String path, String name, long lastModified) {
        this.path = path;
        this.name = name;
        this.lastModified = lastModified;
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
