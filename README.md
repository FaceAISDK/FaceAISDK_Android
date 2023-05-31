#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

Offline Android Face Detection &amp; Recognition And  Liveness Detection & Anti Spoofing SDK 离线版Android人脸检测，人脸识别和活体检测反作弊 SDK

<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 20% height = 20% />
</div>


## 简要说明

  SDK包含动作活体、静默活体检测，1：1人脸识别以及1：N人脸识别查找，**所有处理都在设备终端离线执行，不收集人脸信息更具隐私安全**
  其中活体检测支持张嘴，微笑，眨眼，摇头，点头 随机两种组合验证（摇头点头也可拆分为左右上下4个动作），低端机离线验证速度正常。
  SDK支持Android 5+，实验室设备2016年低配置魅蓝Note3 ARM Cortex-A53  1.8GHz x4 + ARM Mali T860 图形处理器运行流畅

  实验室测试效果能覆盖95% 的中高低端机器，识别成功率>99% ；特殊DIY系统设备或特殊定制硬件如有问题请先提Issues附带系统版本，
  设备型号，错误log等信息。或anylife.zlb@gmail.com 交流

  注：CPU 建议64位 ARM Cortex-A53 1.8GHz以上配置，外接摄像头请切换为标志1
  
  ![image](https://user-images.githubusercontent.com/15169396/222224246-b83fca54-6cfb-4924-98a6-809b11da8761.png)


## 使用场景

   【1:1】 移动考勤真人校验，机场/卡口人证对齐，免密登录，酒店入驻、刷脸支付、刷脸解锁、真人校验

   【1:N】 智能门锁，考勤机，物业管理业主出入凭证等，（暂未优化1：N 识别速度！SM-9700百张底片最差5s左右，最好50ms）

    注：1：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android

## 接入使用
 
    #### 3.8.X 更新说明

    * 支持横屏模式，支持部分外接摄像头
    * 1：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android

    注：CPU 建议64位 ARM® Cortex®-A53™1.8GHz以上配置，外接摄像头请切换为标志1
    //Gradle
    implementation "io.github.anylifezlb:Face-Verification:3.8.7.alpha16"
    
    ``` 
    //更多说明请看代码和下载Demo体验
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //1：1 底片「底片请设置为正脸无遮挡，并如Demo裁剪为仅含人脸」
                .setFaceLibFolder(BASE_FACE_DIR_1N) //1：N 底片库 （1还是N 中检测只能有一种）
                .setGraphicOverlay(mGraphicOverlay) //遮罩层，人脸模型标记画面演示,只是辅助调试用
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
                    public void onFailed(int code) {

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

    * NaviActivity Demo 演示导航页面
    * Verify11Activity 人脸检测识别，活体检测页面（ 1：1）
    * AddBaseImageActivity 更换底片页面，仅供参考建议业务方使用自拍裁剪后使用本程序处理获取高质量底片
    * 1：N 识别暂未优化检索速度

    其中活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 申请，内容包括
    APP简要描述，App名称 ，包名 ，功能主页截屏和 下载链接5项内容。



## Demo 下载体验

   最新版体验下载地址： https://www.pgyer.com/faceVerify  
   微信或相机扫码后选择在浏览器中打开点击下载安装体验验证是否符合你的业务需求

<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/210045090-60c073df-ddbd-4747-8e24-f0dce1eccb58.png" width = 33% height = 33% />
</div>

## 服务定制

   如果 SDK 不能匹配你的应用场景需要特殊定制化，请发邮件到anylife.zlb@gmail.com
   也可以加微信 HaoNan19990322 （请标注为 人脸识别定制，否则添加不通过，谢谢）
   
   Github:  https://github.com/AnyLifeZLB/FaceVerificationSDK
   1：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android

## 常见问题
   常见问题请参考：https://github.com/AnyLifeZLB/FaceVerificationSDK/questions.md
   只是人脸识别不需要活体检测是不需要申请授权的，直接永久使用。


<div align=center>
<img src="https://github.com/AnyLifeZLB/FaceSearchSDK_Android/assets/15169396/82932d93-ea98-4b0d-be25-27ae5adf5dba" width = 33% height = 33% />
</div>   
