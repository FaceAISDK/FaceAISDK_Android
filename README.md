<br>
 <a href='https://play.google.com/store/apps/details?id=com.ai.face.verifyPub'><img alt='Get FaceAI On Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='50'/></a>
<br> 

# [About「FaceAISDK」](https://github.com/FaceAISDK/FaceAISDK_Android)

on_device Offline Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK  
人脸识别、活体检测、人脸录入检测以及[1：N以及M：N](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/Introduce_11_1N_MN.md) 人脸搜索SDK，可完全离线实现端侧人脸识别，人脸搜索等功能。

SDK可支持Android[5,16] **SDK所有功能都不用联网，不上传不存储任何人脸信息敏感资料更具隐私安全**
动作活体支持张嘴、微笑、眨眼、摇头、点头 随机1-2种组合验证，支持UVC协议USB摄像头，需成像清晰宽动态值>105Db。

| 🚀 | 🔑 | 📡 | 💰 |
| :--- | :--- | :--- | :--- |
| **高效集成** | **数据安全** | **可离线使用** | **节约费用** |
| 少量简洁的 SDK API 可快速接入，节省研发维护费用 | 在设备本地执行推断，无需将用户数据发送到云端 | 无需网络连接或在云端运行服务，小场景一台设备就能 Hold 业务需求 | 在设备端运行机器学习功能，减少云端费用 |

首页说明： https://faceaisdk.github.io/index/

##  V2026.04.22

- default Java version 11
- update readme

更多历史版本说明参考 [历史版本SDK更新记录](doc/历史版本SDK更新记录.md)


## 如何使用
  FaceAISDK托管在mavenCentral,确保在repositories中添加mavenCentral()配置
  ```
    implementation 'io.github.FaceAISDK:Android:Version' //如果你使用的是UVC协议USB摄像头你还需依赖UVCAndroid
  ```

**工程目录结构简要介绍**

| 模块            | 描述                                 |
|---------------|------------------------------------|
| FaceSDKLib    | 子Module，FaceAISDK 所有功能都在module 中演示 |
| /verify/\*    | 1:1 人脸检测识别，活体检测页面，静态人脸对比      |
| /search/\*    | 1:N 人脸搜索识别，人脸库增删改管理等财政          |
| /addFace/\*   | 1:1和1:N共用的通过SDK相机添加人脸获取人脸特征值    |
| /SysCamera/\* | 手机，平板自带的系统相机，一般系统摄像头打开就能看效果 |
| /UVCCamera/\* | UVC协议USB摄像头人脸识别，人脸搜索，一般是自自定义的硬件 |


## [使用场景和区别](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/doc/Introduce_11_1N_MN.md)

【1:1】 移动考勤签到、App免密登录、刷脸授权、刷脸解锁、巡更打卡真人校验

【1:N】 小区门禁、公司门禁、智能门锁、智慧校园、机器人、智能家居、社区、酒店等

**GitHub SDK API Demo 地址**  
**iOS SDK：** https://github.com/FaceAISDK/FaceAISDK_iOS  
**Android：** https://github.com/FaceAISDK/FaceAISDK_Android  
**Flutter：**  https://github.com/FaceAISDK/FaceAISDK_Flutter_Plugin   
**uniApp UTS：**  https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS    
**uniApp FaceSearch：**  https://github.com/FaceAISDK/FaceSearch_uniapp_plugin  


## Demo APK 下载体验  

<div align=center>
<img src="https://www.pgyer.com/app/qrcode/faceVerify" width = 19%   alt="click to launch"/>
</div>

 **顺手帮忙点个🌟Star吧，谢谢**

[更多说明，请参考：FaceAISDK产品说明及API文档](doc/FaceAISDK产品说明及API文档.pdf)