
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
- 迁移1:N （M：N） 到独立新库 https://github.com/AnyLifeZLB/FaceSearchSDK_Android


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


















