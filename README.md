<img src="https://badgen.net/badge/FaceAI%20SDK/%20%E5%BF%AB%E9%80%9F%E5%AE%9E%E7%8E%B0%E4%BA%BA%E8%84%B8%E8%AF%86%E5%88%AB%E5%8A%9F%E8%83%BD" />


<div align=center>
<img src="https://github.com/user-attachments/assets/84da1e48-9feb-4eba-bc53-17c70e321111" width = 20% height = 20% />
</div>


# [关于 FaceAI SDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

On_device Offline Android Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK  

FaceAI SDK是设备端可离线不联网Android 人脸识别、活体检测、人脸图质量检测以及人脸搜索（1:N和M:N）SDK，可快速集成实现人脸识别，人脸搜索功能。  

<div align=center>
<img src="https://github.com/user-attachments/assets/b1e0a9c4-8b43-4eb8-bf7a-7632901cfb2c" width = 22% height = 22% />
</div>


![设备端离线机器学习优点](https://github.com/user-attachments/assets/06b0d254-964f-4277-9198-9d5fa920d95c)


## 当前版本说明 V1.8.80.kt1_7 (建议升级到最新版本)
- 依赖降级到kotlin 1.7.20，java 11
- 降级依赖，防止和使用kotlin-android-extensions 等老项目冲突
- 

 建议[Fork] + [Star] 本项目Repo以便第一手获取更新：[FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK) 

 **人脸识别FaceAI SDK 接入源码，请及时更新最新版本** https://github.com/AnyLifeZLB/FaceVerificationSDK  
 国内代码托管地址Gitee:https://gitee.com/onDeviceAI/FaceVerificationSDK (不方便翻墙请邮件联系获取最新代码)



## 简要说明

SDK包含动作活体、静默活体检测，[1：1人脸识别以及1：N , M:N人脸识别检索](https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/Introduce_11_1N_MN.md)，**所有功能都在设备终端离线执行，SDK本身不用联网，不保存不上传任何人脸信息敏感资料更具隐私安全**

其中活体检测支持张嘴、微笑、眨眼、摇头、点头 随机两种组合验证（支持去除特定的动作），低端机离线验证速度正常。

SDK支持Android 5+，建议设备配置 CPU为八核64位2.4GHz以上  摄像头RGB 宽动态镜头分辨率720p以上，帧率大于30并且无拖影。

实验室测试效果能覆盖95% 的中高低端机器，识别成功率>99%；**特殊DIY系统或特殊定制硬件，外接USB摄像头等**如有问题请先提Issues附带系统版本、设备型号、错误log等信息；
或发邮件到 anylife.zlb@gmail.com ，VIP用户添加 微信：HaoNan19990322 / WhatsApp: +8618707611416



## [使用场景和区别](https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/doc/Introduce_11_1N_MN.md)

   【1:1】 移动考勤真人校验、App免密登录、刷脸支付、刷脸解锁、真人校验

   【1:N】 小区门禁、公司门禁、智能门锁、智慧校园、景区、工地、社区、酒店等

   【M:N】 公安布控、人群追踪 监控等等 (测试效果可使用 MN_face_search_test.jpg 模拟)


## 接入使用

*   1.首先Gradle 中引入依赖
    implementation 'io.github.anylifezlb:FaceAISDK:1.？.?' //FaceAISDK 请及时升级到最新

*   2.更新本SDK 接入演示代码到最新版本，**熟悉后**Copy Demo代码到你的主工程

*   3.解决项目工程中的第三方依赖库和主工程的冲突比如CameraX的版本等，Target SDK不同导致的冲突

*   4.调整JDK版本到java 11 以上。Android Studio设置Preferences-Build-Gradle-JDK的版本为 11+

*   5.集成过程中的问题可以GitHub 提issues或者详细描述你遇到的问题发邮件到<anylife.zlb@gmail.com>

    目前SDK默认使用java11. kotlin 1.7.20 打包，如果你的项目较老还在使用
    kapt, kotlin-android-extensions导致集成冲突，建议尽快升级项目或者VIP联系定制

    更多使用说明下载SDK源码工程代码到Android Studio 以及下载Demo APK到手机体验
    里面有详尽的注释说明和使用方法介绍，SDK源码熟悉完成后大概2小时可集成到你的主工程

**其中**

| 模块            | 描述                       |
| ------------- | ------------------------ |
| appMain       | 主工程，faceAILib 是人脸识别相关源码  |
| faceAILib     | 人脸识别Lib module模块         |
| /verify/\*    | 1:1 人脸检测识别，活体检测页面，静态人脸对比 |
| /search/\*    | 1:N 和 M：N 人脸识别搜索页面，人脸库管理 |
| /addFaceImage | 人脸识别和搜索共用的添加人脸照片录入模块     |

## Demo 下载体验

   扫码后选择在浏览器中打开点击下载安装，或直接输入地址 https://www.pgyer.com/faceVerify

<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/210045090-60c073df-ddbd-4747-8e24-f0dce1eccb58.png" width = 22% height = 22% />
</div>
  

## 服务定制

   如果 SDK 不能匹配你的应用场景需要特殊定制化，请发邮件到anylife.zlb@gmail.com
   也可以加微信 [HaoNan19990322 ，备注人脸识别](images/WechatIMG24.jpg)
   
   欢迎关注Fork+Star获取最新动态，提出使用建议等 https://github.com/AnyLifeZLB/FaceVerificationSDK

## 提升接入效率，提高SDK识别准确率

### 提升接入效率

1.  去蒲公英下载APK Demo 体验SDK 的基本功能，看看是否满足业务需求；人脸搜索可以一键导入200+张人脸图再录入你自己的

2.  更新GitHub 最新的代码，花1天左右时间熟悉SDK API 和对应的注释备注，断点调试一下基本功能；熟悉后再接入到主工程

3.  欲速则不达，一定要先跑成功SDK 接入Demo。熟悉后再接入到主工程验证匹配业务功能。有问题可以GitHub 提issues

### 提高SDK识别准确率

1.  使用高配置硬件参数设备和摄像头抗干扰强
2.  录入高质量的人脸图，如（images/face\_example.jpg）（证件照输入目前优化中）
3.  光线环境好，检测的人脸无遮挡，没有化浓妆 或 佩戴墨镜、口罩
4.  人脸图大于 300*300（人脸部分区域大于200*200）五官清晰无遮挡



## 常见问题
   常见问题请参考：https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/doc/questions.md  
   所有的开发测试都在手机和平板进行，特殊定制硬件如 RK3288 等适配需要兼容适配


## 快速接入
   Demo 以main主工程 --> faceAiLib 的方式演示，熟悉本SDK 接入Demo 后可以先Copy faceAiLib到你主工程先跑起来
   再根据业务情况修改完善。  
   熟悉后大约2小时就能集成成功，丰富产品功能同时可大大降低公司研发投入实现降本增效。


![某司人脸识别App 提示UI](https://github.com/user-attachments/assets/4ccb015c-0f3e-48d4-842e-e2e34bb4af1e)
