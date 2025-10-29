###  服务说明
   SDK 在低配早期魅蓝Note3（Android7，以及最新高配旗舰机型三星S25，小米15和小米pad 7pro
   经过严格测试验证；低配设备和成像不清晰的宽动态值小于105Db 的摄像头某些功能可能会表现不友好，建议按照说明配置对应参数
   详细参考：https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA
  
   SDK不读取任何敏感信息，严格限制运行获取权限仅需一个相机运行权限，充分保护隐私数据，不联网就能工作更不会收集上传人脸关键信息。
   SDK 目前托管在Maven central，SDK所有功能都是离线端侧运行。

### 1.集成SDK开发环境和Gradle 插件版本是怎样的？
   开发环境 Android Studio Iguana | 2025.1.4
   gradle 版本 7.4.2 ， gradle插件版本 7.6.6  
   **java17 , kotlin 1.9.22**

   如果你的项目还有kapt请迁移至KSP，kapt官方已经停止维护,否则可能需要处理冲突
   kotlin-android-extensions官方也已经停止维护，建议升级为viewbinding
   其他集成问题，请根据报错搜索解决方案,需要降级依赖版本配置VIP用户可以联系协助解决

### 2.支持哪些摄像头？
   SDK并不限制具体类型摄像头，当前支持System,UVC_RGB,UVC_RGB_IR，具体参数参考/Doc目录/硬件配置要求
   需根据你的硬件平台特性自行管理摄像头，Demo提供默认CameraXFragment,你也可以使用camera1管理系统摄像头
   取的摄像头帧数据处理后送人SDK 进行处理，使用外接摄像头性能（如帧率、数据传输延迟、缓冲区处理）
   可能不如内置相机，并且更容易受到连接稳定性和带宽的影响。推荐 HARDWARE_LEVEL_FULL/HARDWARE_LEVEL_3级别

   UVC摄像头的操作可以参考这个库：https://github.com/shiyinghan/UVCAndroid


### 3.人脸识别1:N 搜索支持人脸库大小与速度
  人脸搜索速度和设备配置有关，摄像头成像能力是硬性要求，需各种环境成像清晰宽动态值大于105DB以上
  下面表格统计几款高中低配置设备运行V2025.09.08 版SDK表现

  | 设备型号         | 启动初始化速度 | 搜索速度(毫秒) |
  |:---------------|:-------:|:---------|
  | 小米 13         |  79 ms  | 66 ms    | 
  | RK3568-SM5     | 686 ms  | 520 ms   | 
  | 华为 P8          | 798 ms  | 678 ms   | 
  | 联想Pad2024      | 245 ms  | 197 ms   |

   网盘分享的3000 张人脸图链接: https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face 提取码: Face
   测试验证可以放到faceAILib/src/assets 目录下，然后在Demo人脸库管理页面点击右上角复制导入本地人脸库验证大容量人脸
   本SDK 目前已经支持万人,大容量人脸库搜索速度快，但是建议根据场景权限缩减人脸库以便减小误差，同时使用成像清晰宽动态摄像头。
   

### 4.如何提升 1:N人脸识别的准确率？
   参考专题文章 https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA ,SDK不可用于金融，高安全要求场景
   某些场景我们提供以下方法供业务侧避免搜索匹配错误无可选处理路径

   ```
     /**
      * 匹配到的大于 Threshold的所有结果，如有多个很相似的人场景允许的话可以弹框让用户选择
      * SearchProcessBuilder setCallBackAllMatch(true) 才有数据返回 否则默认是空
      */
       @Override
       public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
          //已经按照降序排列，可以弹出一个列表框让用户再次确认
          Log.d("onFaceMatched",matchedResults.toString());
     }                   
   ```

### 5.SDK插件支持
   SDK 插件支持并不是十分完善，需要用户自行根据业务再完善，当前已有插件
   1.uniapp X : https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS
   2.uniApp : https://github.com/FaceAISDK/UniPlugin-FaceAISDK  
   3.React native: https://github.com/zkteco-home/react-native-face-ai
   欢迎各位大佬制作完善分享flutter 等插件

### 6.人脸识别的阈值设置说明
   1:1 和 1:N 人脸识别都有相应的API设置阈值setThreshold(0.88f) //阈值设置，范围限 [0.75 , 0.95]
   阈值设置越大结果可信度越高，比如你设置为0.75可能和你同性别弟弟妹妹就能通过你的人脸识别
   但是越不是越高越好，设置越高需要你录入的人脸品质高以及摄像头成像能力也高（宽动态>105 Db可抗逆光）

   RGB静默活体对摄像头型号和使用场景有一定要求，建议采用成像能力强，抗逆光干扰品质较好摄像头

### 7.FaceAI SDK版权说明
   FaceAI SDK 使用开源+自研封装实现，非虹软，Face++，商汤 商业方案二次包装

### 8.调整Target SDK （如target SDK 28）后依赖冲突怎么处理？或者外部依赖的版本需要强制为某个版本怎么处理
   根据Compile SDK 不同，各自项目依赖体系不一样
   主工程和SDK 中的依赖有冲突需要统一依赖,可以参考下面方式处理
   比如TargetSDK 还是 28 的camera_version降低到 1.2.3（最后支持TargetSDK 28）
   更多错误请自行Google，百度搜索解决方法，集成问题根据错误查找对应解决方法

   **以下代码配置应该放到主模块build.gradle里面**
   ```
   def camera_version = "1.2.3" //Android 16KB Alignment 需要升级到1.4.2 以上版本
   configurations.configureEach {
     resolutionStrategy {
       force   
        "androidx.camera:camera-core:$camera_version",
        "androidx.camera:camera-camera2:$camera_version",
        "androidx.camera:camera-lifecycle:$camera_version",
        "androidx.camera:camera-view:$camera_version"
     }
   }
   ```

### 9.为什么小朋友群体1:N ，M：N 搜索误识别率较高？
  小朋友的五官差异相对成年人确实没有那么大，需要专门为小朋友群体训练人脸识别模型了，SDK demo 为通用模型
  默认不支持低龄儿童场景使用。  

### 10.能通过File 操作直接把人脸照片放到制定目录就开始人脸搜索吗？

   不能直接通过File操作，必须要通过SDK API进行，因为要提取人脸特征向量和建立搜索库索引才能快速搜索
   如FaceSearchImagesManger.Companion.getInstance().insertOrUpdateFaceImage()

### 11.人脸识别隐私合规
   SDK 已经通过合规检查，业务方也应符合相关法律法规的要求，主要是2点
   1.不能随意大规模收集，传播人脸信息
   2.人脸识别不能是唯一业务校验通道（比如登录除了人脸识别还要支持账号密码，小区门禁还需提供扫码或刷卡）
   更多参考 https://www.cac.gov.cn/2025-03/21/c_1744174262156096.htm


