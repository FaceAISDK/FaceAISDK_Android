###  服务说明
  SDK 在低配早期魅蓝Note3（Android7，以及最新高配旗舰机型三星S25，小米15和小米pad 7pro
  经过严格测试验证；低配设备和成像不清晰的宽动态值小于105Db 的摄像头某些功能可能会表现不友好，建议按照说明配置对应参数
  详细参考：https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
  
  SDK不读取任何敏感信息，严格限制运行获取权限仅需一个相机运行权限，充分保护隐私数据，不联网就能工作更不会收集上传人脸关键信息。
  SDK 目前托管在Maven central，SDK所有功能都是离线端侧运行

### 1.集成SDK开发环境和Gradle 插件版本是怎样的？
  开发环境 Android Studio Iguana | 2025.1.1
  gradle插件版本 7.5  gradle 版本 7.4.2
  **java17 , kotlin 1.9.22**

  如果你的项目还有kapt请迁移至KSP，kapt 官方已经停止维护
  kotlin-android-extensions官方也已经停止维护，建议升级为viewbinding
  其他集成问题，请根据报错搜索解决方案,需要降级依赖版本配置 VIP 用户可以联系协助解决

### 2.是否支持外接UVC协议 USB摄像头
   如果你的系统摄像头采用标准的 Android Camera2 API 和摄像头 HIDL接口，SDK内部已经集成CameraX管理摄像头，也就是
   标准大厂生产的手机平板设备都是支持的。
   
   2025.06.29 以上版本已经默认支持了UVC 协议的USB红外双目摄像头，直接在手机上插上USB 连接摄像头就能体验，支持切换角度
   匹配红外，RGB摄像头。 更多UVC摄像头的操作可以参考这个库：https://github.com/shiyinghan/UVCAndroid

   ![红外双目](https://github.com/user-attachments/assets/3e96879d-0757-409e-894b-5d1d0e80231c)

### 3.人脸识别1:N 搜索支持人脸库大小与速度
   支持的人脸库大小与设备性能有关，SDK开发测试使用小米13，三星S24，CPU为骁龙8Gen2,镜头成像清晰。下面表格统计几款设备

| 设备型号，配置 | 人脸库大小 | 人脸搜索速度   |
|:--------|:-----:|:---------|
| 小米13    | 3000  | 1次循环     |
| 小米13    |  1 万  | 1次循环     |
| 三星S24   | 3000  | 1次循环     |
| 三星S24   |  1 万  | 1次循环     |

   网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
   本SDK 目前已经支持万人以上,大容量人脸库搜索速度快，但是建议根据场景权限缩减人脸库以便减小误差，同时使用成像清晰宽动态摄像头。

### 4.如何提升 1:N人脸识别的准确率？
    参考专题文章 https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA ,不可用于金融，高安全级别场景
    某些场景我们提供以下方法供业务侧避免搜索匹配错误无可选处理路径
```
     /**
      * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
      * SearchProcessBuilder setCallBackAllMatch(true) 才有数据返回 否则默认是空
      */
       @Override
       public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
          //已经按照降序排列，可以弹出一个列表框
          Log.d("onFaceMatched",matchedResults.toString());
     }                   
```

### 5.uniApp 原生插件支持
使用我们的公版uniAPP demo项目集成  https://github.com/FaceAISDK/UniPlugin-FaceAISDK
细节可以修改原生部分代码重新打包实现。

### 6.识别的灵敏度准确率参数
   目前人脸检测的环节只要人脸区域像素大于150就能识别，相识度setThreshold(0.88f) //阈值设置，范围限 [0.75 , 0.95] 识别可信度，也是识别灵敏度
   其他参数参考Demo 源码
   

### 7.FaceAI SDK 版权说明
   FaceAI SDK 使用开源+自研封装实现，非虹软(试用每年还要激活)，Face++，商汤 商业方案二次包装。SDK发布到三方maven central后和平台永久存在
   具体参考doc 目录LICENSE 说明

### 8.调整Target SDK （如target SDK 28）后依赖冲突怎么处理？或者外部依赖的版本需要强制为某个版本怎么处理
   根据Compile SDK 不同，各自项目依赖体系不一样
   主工程和SDK 中的依赖有冲突需要统一依赖,可以参考下面方式处理
   比如TargetSDK 还是 28 的camera_version降低到 1.2.3（最后支持TargetSDK 28）
   更多错误请自行Google，百度搜索解决方法，集成问题不是SDK内部原因，谢谢

   **以下代码配置应该放到主模块build.gradle里面**

   ```
   def camera_version = "1.2.3"
   configurations.configureEach {
   resolutionStrategy {
   force   "androidx.camera:camera-core:$camera_version",
   "androidx.camera:camera-camera2:$camera_version",
   "androidx.camera:camera-lifecycle:$camera_version",
   "androidx.camera:camera-view:$camera_version"
   }
   }
   ```

### 9.为什么小朋友群体1:N ，M：N 误识别率较高？
小朋友的五官差异相对成年人确实没有那么大，需要专门为小朋友群体训练人脸识别模型了，SDK demo 为通用模型

### 10.能通过File 操作直接把人脸照片放到制定目录就开始人脸搜索吗？

    不能直接通过File操作，必须要通过SDK API进行，因为要提取人脸特征值和建立搜索库索引才能快速搜索
    如FaceSearchImagesManger.Companion.getInstance().insertOrUpdateFaceImage()




