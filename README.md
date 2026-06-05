
# [关于「HiFaceSDK」](https://github.com/HiFaceSDK/HiFaceSDK_Android)

人脸识别、活体检测、人脸录入检测以及[1：N以及M：N](https://github.com/HiFaceSDK/HiFaceSDK_Android/blob/main/Introduce_11_1N_MN.md) 人脸搜索Android SDK，可完全离线实现端侧人脸识别，人脸搜索等功能  
on_device Offline Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK


| Item          | 介绍                           |
|:--------------|:-----------------------------|
| 🚀  **高效集成**  | 少量简洁的 SDK API 可快速接入，节省研发维护费用 |
| 🔑  **数据安全**  | 在设备本地执行推断，无需将用户数据发送到云端     |
| 💰  **节约费用**  | 在设备端运行人脸识别相关功能，减少云端费用        |
| 📡  **可离线使用** | 无需网络或在云端运行服务，简单业务场景单设备就能完整运行 |



## [使用场景](https://github.com/FaceSDKPro/HiFace_Android/blob/main/doc/Introduce_11_1N_MN.md)

| 场景类型        | 核心逻辑                       | 典型使用场景                  | 硬件/性能要求                   |
|:------------|:---------------------------|:------------------------|:--------------------------|
| **1:1人脸比对** | 验证当前人脸与预留特征是否一致| 设备解锁免密登录、人证核验、App 个人考勤  | 普通摄像头即可，计算量极小，毫秒级响应 |
| **1:N人脸搜索** | 人脸库中检索匹配当前人脸身份 | 小区/公司门禁闸机、校园考勤、会员识别     | 严格场景需控制人脸录入品质及配置宽动态摄像头|
| **M:N多人搜索** | 同时识别画面中出现的多张人脸 | 公安巡检监控、大型展会/活动签到、VIP识别  | 需高品质工业相机，要求大算力 GPU/NPU 设备 |

**一般场景根据业务需求建议阈值设置为 0.75-0.85**  
**高要求场景可设置阈值为0.85-0.9；并使用宽动态摄像头以及SDK录入人脸**  
**HiFaceSDK中人脸特征数据已经加密安全合规处理，其他系统无法使用**  

**顺手帮忙点个🌟Star吧，谢谢**  

##  V0.0.1
- 提高人脸搜索精度（阈值0.75-0.85） 
- GPU与CPU协同加速，提升性能


## 如何使用
  HiFaceSDK 托管在mavenCentral,确保在repositories中添加mavenCentral()配置
  ```
    //更多SDK API Demo见 https://github.com/FaceSDKPro/HiFace_Android
    api 'io.github.facesdkpro:HiFace:最新版本' //使用UVC协议摄像头还需依赖UVCAndroid
  ```

**工程目录结构简要介绍**

| 模块         | 描述                                |
|------------|-----------------------------------|
| HiFaceSDK  | 子Module，FaceAISDK 所有功能都在module 中演示 |
| verify     | 1:1 人脸检测识别，活体检测页面，静态人脸对比      |
| search     | 1:N 人脸搜索识别，人脸库增删改管理等财政         |
| addFace    | 1:1和1:N共用的通过SDK相机添加人脸获取人脸特征值   |
| SysCamera  | 手机，平板自带的系统相机，一般系统摄像头打开就能看效果 |
| UVCCamera  | UVC协议USB摄像头人脸识别，人脸搜索，一般是自定义的硬件 |


## Demo APK 下载体验  

<div align=center>
<img src="https://www.pgyer.com/app/qrcode/hiface" width = 19%   alt="click to launch"/>
</div>
更多SDK API Demo见 https://github.com/FaceSDKPro/HiFace_Android


