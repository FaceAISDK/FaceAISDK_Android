<div align="right">
  <img src="https://badgen.net/badge/FaceAI%20SDK/%E5%BF%AB%E9%80%9F%E5%AE%9E%E7%8E%B0%E4%BA%BA%E8%84%B8%E8%AF%86%E5%88%AB%E5%8A%9F%E8%83%BD" alt="FaceAI SDK · 快速实现人脸识别功能" />
</div>

[English](README.md) | **中文**

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.ai.face.verifyPub">
    <img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="50" alt="在 Google Play 获取 FaceAI" />
  </a>
</p>

# [FaceAISDK Android 版](https://github.com/FaceAISDK/FaceAISDK_Android)

端侧离线人脸检测、识别、活体防伪以及 [1:N / M:N 人脸搜索](Document/Introduce_11_1N_MN.md) SDK。

人脸识别、人脸搜索等功能均可在设备端完全离线运行。SDK 支持 Android 8–17。

> **隐私优先：** SDK 所有功能均无须联网，不上传、不存储任何人脸敏感信息。

SDK 支持静默活体检测，以及张嘴、微笑、眨眼、摇头、点头等动作活体检测；同时支持 UVC 协议 USB 摄像头（建议成像清晰、宽动态值 WDR > 105 dB）。

| 🚀 **高效集成** | 🔑 **数据安全** | 📡 **离线可用** | 💰 **节约成本** |
| :--- | :--- | :--- | :--- |
| 通过少量简洁的 SDK API 快速接入，降低研发与维护成本。 | 在设备本地执行推理，无须将用户数据发送到云端。 | 无须网络连接或云端服务，一台设备即可承载小型场景的业务需求。 | 在设备端运行机器学习功能，减少云端基础设施费用。 |

## 版本更新 · v2026.11.01

- 新增 Android 17 支持。
- 新版人脸搜索 SDK：[FaceSDKPro](https://github.com/FaceSDKPro)
- **注意：** FaceAISDK 与 HelloFace 的人脸特征不互通。

更多历史版本说明请参阅 [SDK 更新记录](Document/历史版本SDK更新记录.md)。

## 如何使用

FaceAISDK 已托管至 Maven Central。请确保在 `repositories` 中添加 `mavenCentral()`，然后引入依赖：

```gradle
api 'io.github.FaceAISDK:Android:Version' // 使用 UVC 协议摄像头时还须依赖 UVCAndroid
```

### 工程目录结构

| 模块 | 描述 |
| :--- | :--- |
| `FaceSDKLib` | 子模块，演示 FaceAISDK 的全部功能。 |
| `verify` | 1:1 人脸检测与识别、活体检测、静态人脸对比。 |
| `search` | 1:N 人脸搜索与识别，以及人脸库的增删改管理。 |
| `addFace` | 1:1 与 1:N 共用组件，通过 SDK 相机添加人脸并获取人脸特征值。 |
| `SysCamera` | 手机、平板自带的系统相机，通常打开即可查看效果。 |
| `UVCCamera` | 基于 UVC 协议 USB 摄像头的人脸识别与搜索，通常用于自定义硬件。 |

## [使用场景](Document/Introduce_11_1N_MN.md)

- **1:1** — 移动考勤签到、App 免密登录、刷脸授权、刷脸解锁、巡更打卡真人校验。
- **1:N** — 小区门禁、公司门禁、智能门锁、智慧校园、机器人、智能家居、社区、酒店等。

## SDK Demo

| 平台 | 仓库 |
| :--- | :--- |
| iOS | [FaceAISDK_iOS](https://github.com/FaceAISDK/FaceAISDK_iOS) |
| Android | [FaceAISDK_Android](https://github.com/FaceAISDK/FaceAISDK_Android) |
| Flutter | [FaceRecognition_Flutter](https://github.com/FaceAISDK/FaceRecognition_Flutter) |
| uniApp UTS | [FaceAISDK_uniapp_UTS](https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS) |
| React Native | [FaceRecognition_ReactNative](https://github.com/FaceAISDK/FaceRecognition_ReactNative) |

> 如果 FaceAISDK 对你有帮助，欢迎为项目点亮 ⭐，谢谢！

## Demo APK 下载体验

<p align="center">
  <a href="https://www.pgyer.com/faceVerify">
    <img src="https://www.pgyer.com/app/qrcode/faceVerify" width="180" alt="扫描二维码下载 FaceAISDK Demo APK" />
  </a>
</p>

更多说明请参阅 [FaceAISDK 产品说明及 API 文档](Document/FaceAISDK产品说明及API文档.pdf)。
