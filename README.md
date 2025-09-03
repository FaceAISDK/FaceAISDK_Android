
<div align=center>
<img src="https://github.com/user-attachments/assets/b1e0a9c4-8b43-4eb8-bf7a-7632901cfb2c" width = 7%  alt="点击查看详情"/>
</div>

<img src="https://badgen.net/badge/FaceAI%20SDK/%20%E5%BF%AB%E9%80%9F%E5%AE%9E%E7%8E%B0%E4%BA%BA%E8%84%B8%E8%AF%86%E5%88%AB%E5%8A%9F%E8%83%BD" />

<br>
<a href='https://play.google.com/store/apps/details?id=com.ai.face.verifyPub'><img alt='Get FaceAI On Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='60'/></a>
<br> 

# [关于「FaceAI SDK」](https://github.com/FaceAISDK/FaceAISDK_Android)

FaceAI SDK is on_device Offline Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK
FaceAI SDK包括人脸识别、活体检测、人脸录入检测以及[1：N以及M：N](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/Introduce_11_1N_MN.md) 人脸搜索，可快速集成实现端侧人脸识别，人脸搜索等功能。

Android SDK可支持Android[5,15] **所有功能都在设备终端离线执行，SDK本身不用联网，不保存不上传任何人脸信息敏感资料更具隐私安全**
动作活体支持张嘴、微笑、眨眼、摇头、点头 随机两种组合验证（支持去除特定的动作），支持系统摄像头和UVC协议双目摄像头，宽动态值大于105Db成像清晰抗逆光。
开发人员也可以自定义摄像头管理，把帧数据送入到SDK。

FaceAI SDK产品说明与API文档：https://github.com/FaceAISDK/FaceAISDK_Android/blob/publish/FaceAISDK产品说明及API文档.pdf

![端侧设备端离线机器学习优点](images/whyOfflineSDK.png)

**其他平台**  

**iOS SDK：** https://github.com/FaceAISDK/FaceAISDK_iOS  
**Uni App：** https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS  
**Android：** https://github.com/FaceAISDK/FaceAISDK_Android  

**其他实现**  
**React native** https://github.com/zkteco-home/react-native-face-ai  
**Flutter** need your helps

<div align=center>
<img src="https://github.com/user-attachments/assets/84da1e48-9feb-4eba-bc53-17c70e321111" width = 17%  />
</div>

## 当前版本说明 V2025.09.03

- 工信部安全合规要求以及Google Play上架合规
- 添加人脸图体验优化，暴露出人脸特征向量，保留bitmap
- 1:1 人脸识别支持传入人脸特征向量取代Bitmap
- 更新整理SDK API，方便后期用户无感升级SDK
- 人脸搜索模块加快万人库初始化速度（小米13 1万张人脸从4秒到99毫秒）
- 人脸搜索速度更新，万张人脸库搜索速度毫秒级（新版本SDK需重新迁移同步人脸一次）
- 暴露出相机管理源码CameraXFragment以便用户在自定义设备更好管理摄像头

建议[Fork] + [Star] 本项目Repo以便第一手获取更新：[FaceAISDK_Android](https://github.com/FaceAISDK/FaceAISDK_Android)

## [使用场景和区别](https://github.com/FaceAISDK/FaceAISDK_Android/blob/main/doc/Introduce_11_1N_MN.md)

【1:1】 移动考勤签到、App免密登录、刷脸授权、刷脸解锁、巡更打卡真人校验

【1:N】 小区门禁、公司门禁、智能门锁、智慧校园、机器人、智能家居、社区、酒店等

【M:N】 公安布控、人群追踪 监控等 (测试效果可使用images/MN_face_search_test.jpg 模拟)

## 接入集成使用
   
先在[「GitHub网站」](https://github.com/FaceAISDK/FaceAISDK_Android)下载最新接入SDK 接入代码导入到Android Studio。  
Demo聚焦SDK的核心功能演示，细节并不完善，需要你根据你的业务需求自行完善。

1.  去蒲公英下载APK Demo体验各种功能，查验是否满足业务需求；人脸搜索可以一键导入App内置人脸图也可录入你自己的
2.  更新GitHub 最新的代码，花1天左右时间熟悉SDK API 和对应的注释备注，断点调试一下基本功能；熟悉后再接入到主工程
3.  欲速则不达，一定要先跑成功SDK接入指引Demo。熟悉后再接入到主工程验证匹配业务功能；有问题可以GitHub 提issues

人脸识别已经验证过高中低配置设备，人脸搜索速度表现在几款设备统计如下：

| 设备型号         | 启动初始化速度 | 搜索速度(毫秒) |
|:---------------|:-------:|:---------|
| 小米 13         |  79 ms  | 66 ms    | 
| RK3568-SM5     | 686 ms  | 520 ms   | 
| 华为 P8          | 798 ms  | 678 ms   | 
| 联想Pad2024      | 245 ms  | 197 ms   |

其中硬件配置要求参考：![硬件配置要求](doc/硬件配置要求说明.md)
更多说明：https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA


**工程目录结构简要介绍**

| 模块           | 描述                                           |
|---------------|----------------------------------------------|
| Demo          | Demo主工程，implementation project(':faceAILib') |
| faceAILib     | 子Module，FaceAISDK 所有功能都在module 中演示           |
| /verify/\*    | 1:1 人脸检测识别，活体检测页面，静态人脸对比                     |
| /search/\*    | 1:N 人脸搜索识别，人脸库增删改管理等财政                       |
| /addFaceImage | 人脸识别和搜索共用的添加人脸照片录入模块                         |
| /UVCCamera/\* | UVC协议双目红外摄像头人脸识别，人脸搜索，一般是自自定义的硬件             |
| /SysCamera/\* | 手机，平板自带的系统相机，一般系统摄像头打开就能看效果                  |

*   1.调整JDK版本到java 17。AS设置Preferences -> Build -> Gradle -> JDK的版本为 17

*   2.最好翻墙科学上网同步AGP Gradle 插件7.4.2(或者更新AGP),然后同步其他依赖

*   3.Demo工程成功运行后，根据你的业务需求重点熟悉对应模块后再集成到你的主工程

*   4.**集成到你的主工程**，首先Gradle 中引入依赖
    implementation 'io.github.FaceAISDK:Android:版本号' //及时升级到github最新版

*   5.解决项目工程中的第三方依赖库和主工程的冲突比如CameraX的版本等，Target SDK不同导致的冲突

    目前SDK开发使用**java17. kotlin 1.9.22，AGP 7.x **打包，如果你的项目较老还在使用
    kapt, kotlin-android-extensions导致集成冲突，建议尽快升级项目或者VIP联系定制


## Demo APK 下载体验

<div align=center>
<img src="https://www.pgyer.com/app/qrcode/faceVerify" width = 19%   alt="请点击上面下载地址"/>
</div>

更多历史版本查看这里： https://www.pgyer.com/faceVerify


 .
![FaceAISDK](images/who_are_you.png)  
  
都看到这了，顺手帮忙点个赞吧🎉
