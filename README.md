#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

Offline Android Face Detection &amp; Recognition And Alive Detect SDK 离线版Android人脸检测，人脸识别和活体检测SDK

<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 20% height = 20% />
</div>


## 简要说明

  本SDK仅供Android 5.0 以上系统使用，包含人脸检测，人脸识别和活体检测，所有功能都是离线使用。SDK 总体积大小约为14.x MB，其中：
  * 人脸识别 7.X MB
  * 活体检测 6.x MB；
  
  后期模型数据会解除绑定SDK，以便减少APK包发行体积，支持模型自定义私有化部署！

  前期测试效果能覆盖99.5 % 的中低端机器，识别成功率>99.7%，本统计数据仅供参考。以目前手机的配置，足以跑起来整个工程，
  但是多年以前的老旧机型和部分OEM 的设备运行就有点卡顿了，这类设备市面的保有量非常非常低，几乎可以忽略。
  可以概括为除了部分山寨组装机，定制机市面大厂销售的符合Google标准的手机都没有问题；其他车载，摄像头，门禁特殊设备特殊硬件
  如有问题请先提Issues

  SDK使用的离线模型为MFN，解释器为TensorFlow Lite和 机器学习套件ML，由于TensorFlow 的特性，下一步推理代理会转到GPU尝试
 （本SDK目前适配了Android 5-13，其他定制化需求请联系anylife.zlb@gmail.com交流 ）

  活体检测支持张嘴，微笑，眨眼，摇头，点头 随机两种组合验证，低端机离线验证速度正常。
  
  ![Preview](http://user-images.githubusercontent.com/15169396/187821824-c74e50dc-06c9-4573-8806-6f45d5c7d7dc.png)


## 使用场景

   【1:1】 识别手机考勤系统，机场/卡口人证对齐，免密码登录，酒店入驻、刷脸支付、刷脸解锁 

   【1:N】 考勤机，物业管理业主出入凭证等，（暂未优化1：N 识别速度！）


## 接入使用
 
    #### 当前版本：3.2.2（3.1.5+ 使用方式简化，不兼容升级）

    * 简化调用使用方式 ！
    * 提升活体检测速度，低端机体验
    * 开放1:N 识别（暂未优化，需要提升速率！）


    //Android studio gradle 引入（Eclipse？Not support now !）
    implementation "io.github.anylifezlb:Face-Verification:3.2.2"
    
    ``` 
    //更多说明请看代码和下载Demo体验
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //1：1 底片「底片请设置为正脸无遮挡，并如Demo裁剪为仅含人脸」
                .setFaceLibFolder(BASE_FACE_DIR_1N) //1：N 底片库 （1还是N 中检测只能有一种）
                .setGraphicOverlay(mGraphicOverlay) //遮罩层，人脸模型标记画面演示,可不传。
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考ReadMe
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
    * New11BaseFaceActivity 更换底片页面（1：1）
    * Verify11Activity 人脸检测识别，活体检测页面（1：1）
    * 1：N 识别暂未优化

    其中活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 申请，内容包括
    APP简要描述，App名称 ，包名 ，功能主页截屏，logo和 下载链接6项内容。


## Demo 下载

   请前往下载： https://beta.bugly.qq.com/nhwg (托管服务如果失效请github 下载代码打包)

![image](http://user-images.githubusercontent.com/15169396/189787317-c1d8d9f6-c38d-43de-bdc3-1cf77423fe46.png)

   
   
