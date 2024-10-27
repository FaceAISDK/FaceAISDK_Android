##  服务政策

本SDK1：1人脸识别不含活体检测完全免费不限制设备个数和时长
1：N（M：N） 和活体检测 VIP功能需要授权，为了覆盖基本费用收点成本费交个朋友。
所有收费项目都可以免费试用以便评估是否符合需求；试用结束后会有提示要接入授权


### 0.特殊定制硬件配置板子支持程度
  通用版本的开发适配都是基于手机和平板进行的，有着标准的摄像头外设；为了保证通用版本最大的兼容性，识别过程以一种通用的方式进行，
  如果你的设备硬件配置有加速器支持（ GPU 和数字信号处理器DSP）以及运行较高的Android 系统，定制SDK运行的效率和准确度都会有相应的提升
  

### 1.移动端是否包含iOS 端？
  iOS 有1:1 人脸是不能（含静默活体），以及1:N/M：N人脸识别。 SDK 接入参考：&……&……

### 2.人脸识别1:N 搜索场景中，当画面出现人脸大于1个是搜索谁？
    演示版本是搜索扫描到的第一个人脸，付费版本是画面中最大的人脸

### 3.是否支持外接USB 摄像头
   如果你的系统摄像头采用标准的 Android Camera2 API 和摄像头 HIDL 接口大部分都支持，也已经支持部分USB外接摄像头，其他的需要根据自定义开发板情况评估，你也可以自行管理摄像头和切换画面角度等，参考 https://github.com/AnyLifeZLB/BinocularCameraFaceSearch

### 4.人脸识别1:N 搜索是否能支持N>万人以上
   本SDK 目前已经支持万人以上,专门的1：N 大容量搜索已经迁移到了[FaceSearchSDK_Android](https://github.com/AnyLifeZLB/FaceSearchSDK_Android)，
   理论上设备硬件配置支持的话N可大于万张以上

### 5.如何提升 1:N/M：N 人脸识别的准确率？
    - 录入高质量的人脸图
    - 良好的设备性能和摄像头品质
    - 光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜 口罩

### 6.识别的灵敏度准确率参数
   目前人脸检测的环节只要人脸像素大于222就能识别，相识度setThreshold(0.8f) //阈值设置，范围限 [0.85 , 0.95] 识别可信度，也是识别灵敏度
   
### 7.除了支持Android 手机和平板，其他Android 设备是否支持？
   其他基于Android的定制设备只要是符合Google规范没有魔改一般是支持的，可能部分外接摄像头种类不支持需要定制兼容，需邮寄定制硬件调试。
   目前支持的Android 系统版本为Android8+ ，其他低版本Android 系统可以根据Demo案例使用强制降级依赖CameraX 和 TFLite

### 8.是否免费？
   1:1 人脸识别含有活体检测功能费用 5K (仅仅活体检测4K)，不限终端个数种类，永久有效。
   1:N /M:N人脸搜索识别功能都需要申请license授权，同类型终端费用8K，永久有效，不限制数量。
   所有授权都是绑定App包名和签名SHA1进行，需要新的功能和定制特性开发根据评估情况再协定。 
   「同一主体授权有多个变种App使用不同包名可以提供资料证明后协商优惠处理」
   
   收费项目需要试用请添加微信或邮件申请，并告知App 名称，包名 和 简单使用场景介绍

### 9.摄像头方向调整相关
   目前SDK 的摄像头预览，分析画面都是在内部处理，画面方向和手机，Pad 的姿态（传感器反向）决定，画面能随横竖屏自动切换
这个特性和你的系统相机是一致的。但是有部分用户的设备是自定义的基于Android系统的，软硬件都是自己定义，如果你有需求改变画面
方向建议你在系统底层修改摄像头适配，可以先用系统相机验证。如果无法修改系统底层代码适配需要定制的联系我，最好发Email 描述
anylife.zlb@gmail.com。
   如果你要自行管理摄像头接入，画面方向 旋转管理可以参考 https://github.com/AnyLifeZLB/BinocularCameraFaceSearch
   

### 10.调整Target SDK 后依赖冲突怎么处理？或者外部依赖的版本需要强制为某个版本怎么处理
   如App 目录下的代码处理
   `
   //依赖有冲突需要统一依赖,可以参考下面方式处理,其他依赖类似处理就行
   def camera_version = "1.2.3"
   configurations.configureEach {
   resolutionStrategy {
   force   "androidx.camera:camera-core:$camera_version",
   "androidx.camera:camera-camera2:$camera_version",
   "androidx.camera:camera-lifecycle:$camera_version",
   "androidx.camera:camera-view:$camera_version"
   }
   }
   `
   
### 11.集成SDK开发环境和Gradle 插件版本是怎样的？
  开发环境 Android Studio Iguana | 2024.2.1
  gradle插件版本 7.4.2  gradle 版本 7.5 
  java 1.8   kotlin 7.2

### 12.是否可以使用在金融，强安全场景
  SDK没有达到100% 的准确度，不建议使用在金融 强安全等严格要求领域，建议的场景如下：

  【1:1】 移动考勤真人校验，App免密登录，酒店入驻、刷脸支付、刷脸解锁、真人校验
  【1:N】 智能门锁，考勤机，通缉人员行踪搜索，智慧校园、景区、工地、社区、酒店等
  【M:N】 公安布控，人群追踪 监控等等**

### 13.能通过File 操作直接把人脸照片放到制定目录就开始人脸搜索吗？

    不行，必须要通过SDK 的API进行，因为要提取人脸特征值和建立搜索库索引才能快速搜索
    如FaceSearchImagesManger.IL1Iii.getInstance().insertOrUpdateFaceImage()

### 14.自定义摄像头（方向旋转，相机管理等），双目摄像头是否可以使用？
   支持自定义摄像头，可以在子线程持续输入bitmap 实时预览帧作为参数进行SDK 的调用。
   如果你要自行管理摄像头接入，画面方向 旋转管理可以参考 https://github.com/AnyLifeZLB/BinocularCameraFaceSearch





