##  服务政策

本SDK1：1人脸识别不含活体检测完全免费不限制设备个数和时长
1：N（M：N） 和活体检测需要授权，为了Hold 注意基本费用收点成本费交个朋友。
所有产品都可以免费是试用期间有次数限制，过期有提示接入授权


### 0.特殊定制硬件配置板子支持程度
  通用版本的开发适配都是基于手机和平板进行的，有着标准的摄像头等外设；为了保证通用版本最大的兼容性，识别过程以一种通用的方式进行，
  如果你的设备硬件配置有加速器支持（ GPU 和数字信号处理器DSP）以及运行较高的Android 系统，定制SDK运行的效率和准确度都会有相应的提升

### 1.移动端是否包含iOS 端？
  暂时没有，现在1：N （M：N）人脸搜索门禁都是Android定制设备，优化完善SDK 需要花费大量的业余时间，目前iOS SDK 的封装暂无人力投入更新，老版本差异太大已经停止更新

### 2.人脸识别1:N 搜索场景中，当画面出现人脸大于1个是搜索谁？
    演示版本是搜索扫描到的第一个人脸，付费版本是画面中最大的人脸

### 3.是否支持外接USB 摄像头
   如果你的系统摄像头采用标准的 Android Camera2 API 和摄像头 HIDL 接口大部分都支持，也已经支持部分USB外接摄像头，其他的需要根据自定义开发板情况评估

### 4.人脸识别1:N 搜索是否能支持N>万人以上
   本SDK 目前已经支持万人以上,专门的1：N 大容量搜索已经迁移到了[FaceSearchSDK_Android](https://github.com/AnyLifeZLB/FaceSearchSDK_Android)，
   理论上设备硬件配置支持的话N可大于万张以上

### 5.SDK 包体积是否能裁剪？
   通用版本的SDK 包目前有不同的用户在使用中，考虑到通用性 兼容性不会再裁剪，如果你的App是离线+在线混合模式使用
   可以在有网络情况更新升级模型算法，需要私有部署。目前1：N（M：N） 识别有独立部署包[FaceSearchSDK_Android](https://github.com/AnyLifeZLB/FaceSearchSDK_Android)

### 6.识别的灵敏度准确率参数
   目前人脸检测的环节只要人脸像素大于222就能识别，相识度setThreshold(0.8f) //阈值设置，范围限 [0.75 , 0.95] 识别可信度，也是识别灵敏度
   
### 7.除了支持Android 手机和平板，其他Android 设备是否支持？
   其他基于Android的定制设备只要是符合Google规范没有魔改一般是支持的，可能部分外接摄像头种类不支持需要定制兼容，需邮寄定制硬件调试

### 8.是否免费？
   1:1 人脸识别含有活体检测功能费用 5K，不限终端个数种类，永久有效。
   1:N /M:N人脸搜索识别功能都需要申请license授权，根据终端个数和种类费用8K，永久有效。
   所有授权都是绑定App包名和签名进行，价格差异在于是否要定制特性开发

### 9.摄像头方向调整相关
   目前SDK 的摄像头预览，分析画面都是在内部处理，画面方向和手机，Pad 的姿态（传感器反向）决定，画面能随横竖屏自动切换
这个特性和你的系统相机是一致的。但是有部分用户的设备是自定义的基于Android系统的，软硬件都是自己定义，如果你有需求改变画面
方向建议你在系统底层修改摄像头适配，可以先用系统相机验证。如果无法修改系统底层代码适配需要定制的联系我，最好发Email 描述
anylife.zlb@gmail.com
   


### 10.调整Target SDK 后依赖冲突怎么处理？或者外部依赖的版本需要强制为某个版本怎么处理
   如App 目录下的代码处理
   `
   //依赖有冲突需要统一依赖,可以参考下面方式处理
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
   
### 11.开发环境和Gradle 插件版本是怎样的？
  开发环境 Android Studio Iguana | 2023.2.1
  gradle插件版本 7.4.2  gradle 版本 7.5 










