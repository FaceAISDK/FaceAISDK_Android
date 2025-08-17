## 当前版本说明 V2025.08.18 （性能大优化，稳API版本）
- 录入人脸API暴露出人脸特征向量float[]，比图片更方便存储 更新 使用
- 1:1 人脸识别支持传入人脸特征向量取代人脸图片Bitmap
- 更新整理SDK API，然后期用户无感升级SDK
- 人脸搜索模块加快万人库初始化速度（小米13 1万张人脸从4秒到99毫秒）
- 人脸搜索速度更新，万张人脸库搜索速度毫米级（新版本SDK需重新迁移同步人脸一次）
- 暴露出相机管理源码CameraXFragment以便用户在自定义设备更好管理摄像头

人脸识别API 更新
1. 
2.

人脸搜索API更新
1. insertOrUpdateFaceImage 更新返回人脸向量特征
2.


## 当前版本说明 V2025.08.07
- 使用YUVLib NDK处理摄像头数据，提升低配设备体验
- CameraX新加释放相机API以便提前释放
- 优化重构后SDK初始化参数和提示细节

## 当前版本说明 V2025.07.31
- CameraX新加释放相机API以便提前释放
- 符合设定阈值的所有人脸搜索结果返回
- 优化重构后SDK初始化参数和提示细节
- 优化UVC双目摄像头匹配管理问题

# V2.0.0.Release （2.0.0 重构）
- 2.0系列重构版本，更新官网 链接说明地址等
- UVC协议相机管理库从本地AAR改为在线依赖
- 上线更新Google Play

# V2025.07.15
- 符合设定阈值的所有人脸搜索结果返回以及添加MN 多人脸搜索
- 优化人脸搜索和重构三方UVC摄像头管理库
- 优化低配设备人脸录入和识别活体校验优化

# V1.9.9
- kotlin默认版本降级为1.9.22
- AGP 默认版本降低为 7.4.2

# V1.9.8
- 升级编译java 版本到17， kotlin为2.1.0， AGP 8.x
- 人脸录入增加闭眼检测
  https://s01.oss.sonatype.org/#welcome 要停止服务，相关底层服务升级后接入方java kotlin apg版本也要升级

# V1.9.7
- 低配设备优化，正式包闪退问题解决
- 准备使用Maven Central Publishing Portal 发布SDK，废弃OSSRH

# V1.9.6
- 增加调节摄像头方向功能接口参数

# V1.9.5
- UVC 协议USB 摄像头支持
- 优化1:N人脸搜索，优化证件照录入和识别
- 完善UVC协议USB红外双目摄像头人脸识别

# V1.9.3  
- 删除不需要权限，上架Google Play认证
- 优化光线不佳活体检测和录入人脸活体校验
- 优化SDK体验，支持自定义管理摄像头
- 完善UVC协议USB红外双目摄像头人脸识别

# V1.9.0 
- 优化光线不佳活体检测
- 优化SDK体验，支持自定义管理摄像头
- 完善UVC协议USB红外双目摄像头人脸识别

# V1.8.80
- 多角度录入人脸，以便更精确的匹配
- 适配Android 15系统

# V1.8.70
- java version 从 11升级到17
- kotlin version 升级到 2.0.0
- 升级 com.google.devtools.ksp 到 2.0.0-1.0.21
- FaceSearchImagesManger.IL1Iii.getInstance 调用方式改为 FaceSearchImagesManger.Companion.getInstance
- AddFaceUtils.ILil.getInstance 调用方式改为 FaceAIUtils.Companion.getInstance

# V1.8.60
- 支持X86_64 Chrome OS
- 解决动作活体重构后出现不能识别通过问题
- 提升活体检测体验友善度

# V1.8.50
- 修复试用版本闪退问题
- 加快1:N 人脸搜索速度，性能优化
- 重新封装完善动作活体 静默活体，简化调用
- 修复大尺寸照片中人脸过小导致的人脸入库失败问题

# V1.8.40
- 大尺寸人脸照片输入裁剪

# V1.8.30
- 重构SDK接入方式，更加简单
- 录入人脸简化，添加人脸角度校验

# V1.8.25
- 注册人脸的尺寸
- 封装Kotlin协程，Java 调用更方便
- 人脸照片录入去除更多限制条件

# V1.8.19
- 开发更多基础功能

# V1.8.18
- 修复高清人脸带来的BUG，修复试用版随机闪退问题

# V1.8.16
- 新加录入人脸是返回高清人脸图和原图 onCompletedVIP(Bitmap bitmap, Bitmap bitmap1) {

# V1.8.15
- 升级项目TargetSDK = 34
- 升级相机管理，TargetSDK<28 的朋友请强制指定版本 

# V1.8.14
- 免费版本的录入人脸的质量也提升同VIP 版本
- 增加  processCallBack.onVerifyMatched(boolean,matchedBitmap) 回调（VIP）
- 增加  人脸质量检测 （VIP）

# V1.8.13
- 重新命名 FaceProcessBuilder 中的字段名称，准备国际化改造
- 性能优化，升级内核

# V 1.8.0
- 性能优化，解决BUG
- 解决基础版本SDK 重试问题

# V 1.7.0
- 活体检查仅仅使用静默活体的BUG

# V1.6.0 
- 解决静默活体的Bug

# V1.5.0
- 性能优化，解决BUG。以及可以单独使用静默活体检测不绑定动作活体

# V1.4.0
- 支持自定义摄像头改变Camera画面方向 等进行搜索

# V1.3.0
- 迁移1:N （M：N） 到独立新库 https://github.com/FaceAISDK/FaceSearchSDK_Android

# V1.2.0
- 识别画面人脸大小灵敏度122*122
- 搜索优化
- 防止高端手机人脸录入处理bitmap OOM内存溢出闪退
- 1:N 搜索成功暂停0.5秒

# V1.1.0
- 识别阈值灵敏度范围改为0.8 - 0.95
- 添加M：N 识别接入演示
- 人脸检测环节增加灵敏度
- 横竖屏切换人脸检索识别和画框
- 调整M：N识别的摄像头焦距

# V1.0.0
- 重构工程，快速接入SDK演示
- 可独立分离1：N 人脸识别的库
- 完善兼容性处理（定制设备需要联系）
- 加快1：N 识别速度，千张毫秒级别


















