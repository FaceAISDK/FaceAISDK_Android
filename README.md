#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

On Device OffLine Android Face Detection &amp; Recognition And  Liveness Detection & Anti Spoofing SDK 离线版Android人脸检测，人脸识别和活体检测反作弊 SDK,包含1:1人脸對比和1:N检索识别

<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 10% height = 10% />
</div>

## 当前版本说明 2023-07-07
 FaceVerification经历了大版本重构，在使用方式 API 接口没有大改变，但是包名引入路径有修改，请按照Demo 方式修改。

- 本次迭代1：1 简化了接入流程实现1小时接入，动作活体可以选1-2个随机动作步骤； 
- 1：N识别极大的提升了识别搜索速率和精度，千张人脸检索识别速度在三星N9700速度小于1秒,硬件配置好可支持万张。

Gradle引入方式改为：

  implementation 'io.github.anylifezlb:FaceRecognition:1.0.2'

废弃Face-Verification，老版本只维护到3.x.y 建议使用方尽快迁移到新重构版本，VIP用户特别是使用1：N检索识别功能的用户请尽快迁移到新版本


 建议[Fork] + [Star] 关注订阅#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK) 以便获取最新更新消息


## 简要说明

SDK包含动作活体、静默活体检测，1：1人脸识别以及1：N人脸识别查找，**所有处理都在设备终端离线执行，SDK本身不用联网，不收集人脸信息更具隐私安全**

其中活体检测支持张嘴，微笑，眨眼，摇头，点头 随机两种组合验证（摇头点头也可拆分为左右上下4个动作），低端机离线验证速度正常。

SDK支持Android 5+，实验室设备2016年低配置魅蓝Note3 ARM Cortex-A53  1.8GHz x4 + ARM Mali T860 图形处理器运行流畅稳定

实验室测试效果能覆盖95% 的中高低端机器，识别成功率>99% ；特殊DIY系统或特殊定制硬件，外接USB摄像头等如有问题请先提Issues附带系统版本，设备型号，错误log等信息；
或发邮件到anylife.zlb@gmail.com ，VIP用户添加微信ID：18707611416



![FaceVerificationSDK](https://github.com/AnyLifeZLB/Android-Architecture/assets/15169396/0740b2f1-3973-487c-bade-4158efa3da87)



## 使用场景

   【1:1】 移动考勤真人校验，App免密登录，酒店入驻、刷脸支付、刷脸解锁、真人校验

   【1:N】 智能门锁，考勤机，通缉人员行踪搜索，智慧校园、景区、工地、社区、酒店等，（千张人脸仅仅耗时200 Ms ，三星N9700测试）

    注：1：N 人脸检索可以独立依赖，体积更小 https://github.com/AnyLifeZLB/FaceSearchSDK_Android

## 接入使用

    //Gradle
    implementation 'io.github.anylifezlb:FaceRecognition:1.0.0'
    
    ``` 
    //更多说明请看代码和下载Demo体验
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //1：1 底片「底片请设置为正脸无遮挡，并如Demo裁剪为仅含人脸」
                .setFaceLibFolder(BASE_FACE_DIR_1N) //1：N 底片库目录
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setVerifyTimeOut(10)               //活体检测支持设置超时时间 9-16 秒
                .setMotionStepSize(1)               //随机动作验证活体的步骤个数，支持1-2个步骤
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
                         //only 1：1 人脸识别检测会有Callback
                    }

                    @Override
                    public void onMostSimilar(String imagePath){
                        //only 1：N 人脸识别检测会有Callback
                    }

                    @Override
                    public void onProcessTips(int actionCode) {
                        showAliveDetectTips(actionCode);
                    }
                })
                .create();

        faceDetectorUtils.setDetectorParams(faceProcessBuilder);
    ```

   
    更多使用说明下载参考本Repo和下载Demo体验，里面有比较详尽的使用方法，其中 


  
    * NaviActivity  Demo 演示导航页面
    * /verify/目录  1:1 人脸检测识别，活体检测页面
    * /search/目录  1:N 人脸识别搜索页面，人脸库管理
    * 1：N 人脸检索可以独立依赖，体积更小 https://github.com/AnyLifeZLB/FaceSearchSDK_Android

    不含活体检测不需要license完全免费，包含活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 申请，内容包括
    APP简要描述，App名称 ，包名 ，功能主页截屏和 下载链接5项内容。



## Demo 下载体验

   最新版体验下载地址： https://www.pgyer.com/faceVerify  
   微信或相机扫码后选择在浏览器中打开点击下载安装体验验证是否符合你的业务需求

<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/210045090-60c073df-ddbd-4747-8e24-f0dce1eccb58.png" width = 30% height = 30% />
</div>

## 服务定制

   如果 SDK 不能匹配你的应用场景需要特殊定制化，请发邮件到anylife.zlb@gmail.com
   也可以加微信 HaoNan19990322 （请标注为 人脸识别定制，否则添加不通过，谢谢）
   
   欢迎关注Fork+Star获取最新动态 Github:  https://github.com/AnyLifeZLB/FaceVerificationSDK
  

## 常见问题
   常见问题请参考：https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/questions.md
   只是1:1 人脸识别不含活体检测是不需要申请授权的，直接永久使用；1：N需要授权，未明事宜请联系



