<div align=right>
  <img src="https://badgen.net/badge/FaceAI%20SDK/Fast%20Face%20Recognition"/>
</div>

English | [中文](README_CN.md)

<br>
 <a href='https://play.google.com/store/apps/details?id=com.ai.face.verifyPub'><img alt='Get FaceAI On Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='50'/></a>
<br> 

# [About Android "FaceAISDK"](https://github.com/FaceAISDK/FaceAISDK_Android)

on_device Offline Face Detection, Recognition, Liveness Detection Anti-Spoofing and [1:N/M:N](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/Introduce_11_1N_MN.md) Face Search SDK.  
Completely offline implementation of edge-side face recognition, face search, and other functions.

SDK supports Android [8, 16]. **All SDK functions work offline, no data is uploaded or stored, ensuring privacy and security.**  
Supports silent liveness detection and action liveness (mouth opening, smiling, blinking, head shaking, head nodding). Supports UVC protocol USB cameras (requires clear imaging, WDR > 105dB).

 🚀 | 🔑 | 📡 | 💰 |
 :--- | :--- | :--- | :--- |
 **Efficient Integration** | **Data Security** | **Offline Use** | **Cost Saving** |
 Simple SDK APIs for quick integration, saving R&D costs | Inference performed locally on device, no user data sent to cloud | No network connection or cloud services required, one device can handle small scenarios | Machine learning on device reduces cloud expenses |

## V2026.06.21
- AddFace returns 640*480 original image
- 16KB alignment optimization
- Android 17 compatibility preprocessing (To be verified)

For more version history, refer to [SDK Update Records](Document/历史版本SDK更新记录.md)

## How to Use
FaceAISDK is hosted on mavenCentral. Ensure you add `mavenCentral()` to your repositories configuration.
```gradle
api 'io.github.FaceAISDK:Android:Version' // UVC protocol cameras also require UVCAndroid dependency
```

**Project Structure Overview**

 Module | Description |
------------|-----------------------------------|
 FaceSDKLib | Sub-module, all FaceAISDK features are demonstrated in this module |
 verify | 1:1 face detection & recognition, liveness detection, static face comparison |
 search | 1:N face search & recognition, face library management (CRUD) |
 addFace | Shared component for 1:1 and 1:N to add faces and get feature vectors via SDK camera |
 SysCamera | System camera (phones, tablets), works immediately after opening |
 UVCCamera | UVC protocol USB camera face recognition & search, usually for custom hardware |

## [Scenarios](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/doc/Introduce_11_1N_MN.md)

**【1:1】** Mobile attendance, app password-less login, face authorization, face unlock, patrol check-in verification.

**【1:N】** Residential access control, company access control, smart locks, smart campus, robots, smart home, community, hotels, etc.

## GitHub SDK API Demo Links
**iOS SDK:** https://github.com/FaceAISDK/FaceAISDK_iOS  
**Android:** https://github.com/FaceAISDK/FaceAISDK_Android  
**Flutter:** https://github.com/FaceAISDK/FaceRecognition_Flutter
**uniApp UTS:** https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS  
**React Native:** https://github.com/FaceAISDK/FaceRecognition_ReactNative

**Please give us a 🌟Star if it helps you, thank you!**

## Demo APK Download

<div align=center>
<img src="https://www.pgyer.com/app/qrcode/faceVerify" width = 19% alt="click to launch"/>
</div>

[For more details, please refer to: FaceAISDK Product Description and API Documentation](Document/FaceAISDK产品说明及API文档.pdf)
