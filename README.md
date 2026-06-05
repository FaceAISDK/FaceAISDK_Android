
# [关于「HiFaceSDK」](https://github.com/HiFaceSDK/HiFaceSDK_Android)

人脸识别、活体检测、人脸录入检测以及[1：N以及M：N](https://github.com/HiFaceSDK/HiFaceSDK_Android/blob/main/Introduce_11_1N_MN.md) 人脸搜索Android SDK，可完全离线实现端侧人脸识别，人脸搜索等功能  
on_device Offline Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK


| 🚀 | 🔑 | 📡 | 💰 |
| :--- | :--- | :--- | :--- |
| **高效集成** | **数据安全** | **可离线使用** | **节约费用** |
| 少量简洁的 SDK API 可快速接入，节省研发维护费用 | 在设备本地执行推断，无需将用户数据发送到云端 | 无需网络连接或在云端运行服务，小场景一台设备就能 Hold 业务需求 | 在设备端运行机器学习功能，减少云端费用 |


## [使用场景](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/doc/Introduce_11_1N_MN.md)

【1:1】 设备解锁、人证核验、App个人考勤免密登录。证明你就是你  
【1:N】 小区门禁、大堂闸机、公司考勤名单内验证。证明你在名单内  
【M:N】 多人同时识别，公安巡检等（高品质摄像头，大算力设备）

**不同场景、摄像头配置和人脸录入方式品质建议阈值设置0.75-0.85；**  
**高要求场景可设置阈值为0.85-0.9；要求使用同源设备以及SDK录入人脸**  
**HiFaceSDK中人脸特征数据已经加密合规处理**

**顺手帮忙点个🌟Star吧，谢谢**

##  V0.0.1
- init version 提高人脸搜索精度（阈值0.75-0.85） 
- GPU与CPU协同加速，提升性能

## 如何使用
  HiFaceSDK 托管在mavenCentral,确保在repositories中添加mavenCentral()配置
  ```
    api 'io.github.facesdkpro:HiFace:最新版本' //使用UVC协议摄像头还需依赖UVCAndroid
  ```

**工程目录结构简要介绍**
https://www.pgyer.com/app/qrcode/hiface?domain=www.pgyer.com
| 模块         | 描述                                |
|------------|-----------------------------------|
| HiFaceSDKLib | 子Module，FaceAISDK 所有功能都在module 中演示 |
| verify     | 1:1 人脸检测识别，活体检测页面，静态人脸对比      |
| search     | 1:N 人脸搜索识别，人脸库增删改管理等财政         |
| addFace    | 1:1和1:N共用的通过SDK相机添加人脸获取人脸特征值   |
| SysCamera  | 手机，平板自带的系统相机，一般系统摄像头打开就能看效果 |
| UVCCamera  | UVC协议USB摄像头人脸识别，人脸搜索，一般是自定义的硬件 |




## Demo APK 下载体验  

<div align=center>
<img src="https://www.pgyer.com/app/qrcode/hiface" width = 19%   alt="click to launch"/>
</div>



