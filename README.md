<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 10% height = 10% />
</div>


# [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

On_device Offline Android Face Detection 、Recognition 、Liveness Detection Anti Spoofing and 1:N/M:N Face Search SDK 
设备端离线 Android人脸检测、人脸识别、活体检测反作弊以及1:N / M:N人脸搜索 SDK 

![设备端离线机器学习优点](assert/whyOfflineSDK.png)

## 当前版本说明 V1.8.33 (仅仅维护1.8.20以上版本)
- 修复高清人脸带来的BUG，修复试用版随机闪退问题
- 1:N 人脸识别添加确认机制，提高搜索匹配精确度
- 修复大尺寸照片中人脸过小导致的人脸入库失败问题

 建议[Fork] + [Star] 以便获取最新更新 #  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK) 
 SDK 演示目前仅仅托管在GitHub，其他镜像版本大概率不是最新的，请移步到GitHub 更新最新的演示代码

## 简要说明

SDK包含动作活体、静默活体检测，[1：1人脸识别以及1：N , M:N人脸识别检索](https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/Face_1:1_1:N_M:N.md)，**所有功能都在设备终端离线执行，SDK本身不用联网，不保存不上传任何人脸信息敏感资料更具隐私安全**

其中活体检测支持张嘴、微笑、眨眼、摇头、点头 随机两种组合验证（摇头点头也可拆分为左右上下4个动作），低端机离线验证速度正常。

SDK支持Android 5+，建议设备配置 CPU为八核64位2.4GHz以上  摄像头RGB 宽动态镜头分辨率720p以上，帧率大于30并且无拖影。

实验室测试效果能覆盖95% 的中高低端机器，识别成功率>99%；**特殊DIY系统或特殊定制硬件，外接USB摄像头等**如有问题请先提Issues附带系统版本、设备型号、错误log等信息；
或发邮件到anylife.zlb@gmail.com ，VIP用户添加 微信：HaoNan19990322 / WhatsApp: +8618707611416



## [使用场景和区别](https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/Face_1:1_1:N_M:N.md)

   【1:1】 移动考勤真人校验，App免密登录，酒店入驻、刷脸支付、刷脸解锁、真人校验

   【1:N】 小区门禁，公司门禁，智能门锁，智慧校园、景区、工地、社区、酒店等

   【M:N】 公安布控，人群追踪 监控等等 (人脸搜索 Demo 测试可使用图片 face_search_test.jpg)


## 接入使用

    //1.首先Gradle 中引入依赖 
    implementation 'io.github.anylifezlb:FaceRecognition:1.?.?'  //目前仅维护1.8.20 以上版本，请及时升级
    //2.Camera相机的初始化。第一个参数0/1 指定前后摄像头；第二个参数linearZoom [0.01f,1.0f] 指定焦距，默认0.01
    CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLensFacing,0.2f);

    ``` 
    //3.人脸识别过程中各种参数的初始化。（更多说明请Github Clone代码体验,）
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.88f)                //threshold（阈值）设置，范围仅限[0.85-0.95]，默认0.88
                .setBaseBitmap(baseBitmap)          //1：1 底片「底片请设置为正脸无遮挡，并如Demo裁剪为仅含人脸」
                .setLiveNessDetection(true)         //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setLiveNessDetectionMode(FAST)     //活体检测模式，快速还是准确（仅动作活体，建议高配硬件用准确模式）
                .setVerifyTimeOut(16)               //活体检测支持设置超时时间 9-22 秒
                .setMotionStepSize(2)               //随机动作验证活体的步骤个数，支持1-2个步骤
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
                         // 1：1 人脸识别匹配成功
                    }
                    @Override
                    public void onMostSimilar(String similar) {
                         // 人脸检索识别
                    }
                })
                .create();

        faceDetectorUtils.setDetectorParams(faceProcessBuilder);
    ```

   
    更多使用说明下载参考本Repo和下载Demo体验，里面有比较详尽的使用方法，其中 

  
    * NaviActivity  Demo 演示导航页面
    * /verify/目录  1:1 人脸检测识别，活体检测页面
    * /search/目录  1:N 和 M：N 人脸识别搜索页面，人脸库管理

    不含活体检测不需要license完全免费，包含活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 确认授权
    内容包括App名称、包名（applicationId）、签名证书SHA1和简要描述和要申请  1:1 / 1:N / M:N 哪种类型、4项内容。


## Demo 下载体验

   扫码后选择在浏览器中打开点击下载安装，或直接输入地址 https://www.pgyer.com/faceVerify

<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/210045090-60c073df-ddbd-4747-8e24-f0dce1eccb58.png" width = 30% height = 30% />
</div>
  

## 服务定制

   如果 SDK 不能匹配你的应用场景需要特殊定制化，请发邮件到anylife.zlb@gmail.com
   也可以加微信 HaoNan19990322 （请标注为 人脸识别 ，否则会自动忽略添加，谢谢）
   
   欢迎关注Fork+Star获取最新动态  Github:  https://github.com/AnyLifeZLB/FaceVerificationSDK

## 提升接入效率，提高SDK识别准确率
   ### 提升接入效率
     - 1.去蒲公英下载APK Demo 体验SDK 的基本功能，看看是否满足业务需求；人脸搜索可以一键倒入200+张人脸图再录入你自己的
     - 2.更新GitHub 最新的代码，花1天左右时间熟悉SDK API 和对应的注释备注，断点调试一下基本功能；熟悉后再接入到主工程
     - 3.欲速则不达，熟悉后再接入到主工程试用匹配业务功能。有新需求可以GitHub 提issues或者邮件 微信联系


   ### 提高SDK准确率
     - 1.试用建议的设备配置和摄像头
     - 2.录入高质量的人脸图（证件照输入目前优化中）
     - 3.光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩

## 常见问题
   常见问题请参考：https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/questions.md
   只是1:1 人脸识别不含活体检测是不需要申请授权的，直接永久使用；1：N，M：N需要授权，未明事宜请联系
   所有的开发测试都在手机和平板进行，特殊定制硬件如RK3288 等适配需要兼容适配



