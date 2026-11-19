<div align="right">
  <img src="https://badgen.net/badge/FaceAI%20SDK/Fast%20Face%20Recognition" alt="FaceAI SDK · Fast Face Recognition" />
</div>

**English** | [中文](README_CN.md)

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.ai.face.verifyPub">
    <img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="50" alt="Get FaceAI on Google Play" />
  </a>
</p>

# [FaceAISDK for Android](https://github.com/FaceAISDK/FaceAISDK_Android)

An on-device SDK for offline face detection, recognition, liveness anti-spoofing, and [1:N / M:N face search](Document/Introduce_11_1N_MN.md).

Face recognition, face search, and related features are implemented entirely on the edge. The SDK supports Android 8–17.

> **Privacy first:** All SDK features work offline. No face data is uploaded or stored.

The SDK supports silent liveness detection and action-based liveness detection, including mouth opening, smiling, blinking, head shaking, and nodding. It also supports UVC-compatible USB cameras (clear imaging and WDR > 105 dB are recommended).

| 🚀 **Efficient Integration** | 🔑 **Data Security** | 📡 **Offline Use** | 💰 **Cost Saving** |
| :--- | :--- | :--- | :--- |
| Simple SDK APIs enable quick integration and reduce development and maintenance costs. | Inference runs locally on the device, so user data is never sent to the cloud. | No network connection or cloud service is required; a single device can handle small-scale scenarios. | On-device machine learning reduces cloud infrastructure costs. |

## What's New · v2026.10.09

- Added support for Android 17.
- New face search SDK: [FaceSDKPro](https://github.com/FaceSDKPro)
- **Note:** Face features from FaceAISDK and HelloFace are not interchangeable.

See the [SDK update history](Document/历史版本SDK更新记录.md) for earlier releases.

## How to Use

FaceAISDK is hosted on Maven Central. Make sure `mavenCentral()` is included in your repositories configuration, then add the dependency:

```gradle
api 'io.github.FaceAISDK:Android:Version' // UVC cameras also require the UVCAndroid dependency
```

### Project Structure

| Module | Description |
| :--- | :--- |
| `FaceSDKLib` | Submodule demonstrating all FaceAISDK features. |
| `verify` | 1:1 face detection and recognition, liveness detection, and static face comparison. |
| `search` | 1:N face search and recognition, plus face library management (CRUD). |
| `addFace` | Shared component for 1:1 and 1:N workflows; adds faces and obtains feature vectors through the SDK camera. |
| `SysCamera` | System camera support for phones and tablets; generally works immediately after opening. |
| `UVCCamera` | Face recognition and search using UVC-compatible USB cameras, typically for custom hardware. |

## [Use Cases](Document/Introduce_11_1N_MN.md)

- **1:1** — Mobile attendance, passwordless app login, face authorization, face unlock, and patrol check-in verification.
- **1:N** — Residential and company access control, smart locks, smart campuses, robots, smart homes, communities, hotels, and more.

## SDK Demos

| Platform | Repository |
| :--- | :--- |
| iOS | [FaceAISDK_iOS](https://github.com/FaceAISDK/FaceAISDK_iOS) |
| Android | [FaceAISDK_Android](https://github.com/FaceAISDK/FaceAISDK_Android) |
| Flutter | [FaceRecognition_Flutter](https://github.com/FaceAISDK/FaceRecognition_Flutter) |
| uniApp UTS | [FaceAISDK_uniapp_UTS](https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS) |
| React Native | [FaceRecognition_ReactNative](https://github.com/FaceAISDK/FaceRecognition_ReactNative) |

> If FaceAISDK helps you, please consider giving the project a ⭐. Thank you!

## Demo APK

<p align="center">
  <a href="https://www.pgyer.com/faceVerify">
    <img src="https://www.pgyer.com/app/qrcode/faceVerify" width="180" alt="Scan the QR code to download the FaceAISDK demo APK" />
  </a>
</p>

For more information, see the [FaceAISDK product description and API documentation](Document/FaceAISDK产品说明及API文档.pdf).
