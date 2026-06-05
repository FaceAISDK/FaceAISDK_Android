package com.hiface.demo.SysCamera.verify;

public class VerifyStatue {
    public static final int  DEFAULT = 0;                  // 0   初始化状态，流程没有开始/中断
    public static final int  VERIFY_SUCCESS = 1;           // 1   人脸识别对比成功大于设置的threshold
    public static final int  VERIFY_FAILED = 2;            // 2   人脸识别对比识别小于设置的threshold
    public static final int  MOTION_LIVENESS_SUCCESS = 3;  // 3   动作活体检测成功（基本不用，还有后续动作）
    public static final int  MOTION_LIVENESS_TIMEOUT = 4;  // 4   动作活体超时
    public static final int  NO_FACE_MULTI = 5;            // 5   多次没有检测到人脸
    public static final int  NO_FACE_FEATURE = 6;          // 6   没有对应的人脸特征值
    public static final int  COLOR_LIVENESS_SUCCESS = 7;   // 7   炫彩活体成功
    public static final int  COLOR_LIVENESS_FAILED = 8;    // 8   炫彩活体失败
    public static final int  COLOR_LIVENESS_LIGHT_TOO_HIGH = 9; // 9炫彩活体失败，光线亮度过高
    public static final int  ALL_LIVENESS_SUCCESS = 10;    // 10  所有的活体检测完成(包括动作和炫彩)
    public static final int  SILENT_LIVENESS_FAILED = 11;  // 11  静默活体检测失败
    public static final int  NO_BASE_FACE_FEATURE = 12;    // 12  没有录入人脸信息
    public static final int  NOT_ALLOW_MULTI_FACES = 13;   // 13  多人脸出现在镜头
}
