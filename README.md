#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

Offline Android Face Detection &amp; Recognition And Alive Detect SDK 离线版Android人脸检测，人脸识别和活体检测SDK

<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 20% height = 20% />
</div>


## 简要说明

  本SDK支持 Android 5.0 +，包含人脸检测，人脸识别和动作活体检测，所有功能都是离线使用。
  活体检测支持 张嘴，微笑，眨眼，摇头，点头 5种方式随机两种组合验证，低端机离线验证速度正常。

  前期测试效果能覆盖99.5 % 的中低端机器，识别成功率>99.7%，本统计数据仅供参考。
  特殊系统设备特殊硬件，如有问题请先提Issues，其他定制化需求请联系 anylife.zlb@gmail.com 交流
  
  ![Preview](http://user-images.githubusercontent.com/15169396/187821824-c74e50dc-06c9-4573-8806-6f45d5c7d7dc.png)


## 使用场景

   【1:1】 识别手机考勤系统，机场/卡口人证对齐，免密码登录，酒店入驻、刷脸支付、刷脸解锁 

   【1:N】 考勤机，物业管理业主出入凭证等，（暂未优化1：N 识别速度！）


## 接入使用
 
    #### 更新说明 （3.3.3以后 使用方式简化，安全升级。不兼容升级请参考Demo精简接入）

    * 简化调用使用方式 ！
    * 活体检测支持设置超时时间 9-16 秒 
    * 开放1:N 识别（暂未优化，需要提升速率！）
    * 开发自定义 threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8


    //Gradle
    implementation "io.github.anylifezlb:Face-Verification:3.3.3"
    
    ``` 
    //更多说明请看代码和下载Demo体验
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //1：1 底片「底片请设置为正脸无遮挡，并如Demo裁剪为仅含人脸」
                .setFaceLibFolder(BASE_FACE_DIR_1N) //1：N 底片库 （1还是N 中检测只能有一种）
                .setGraphicOverlay(mGraphicOverlay) //遮罩层，人脸模型标记画面演示,只是辅助调试用
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setVerifyTimeOut(10)               //活体检测支持设置超时时间 9-16 秒
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
    APP简要描述，App名称 ，包名 ，功能主页截屏，logo 和 下载链接6项内容。


## Demo 下载

   请前往下载： https://www.pgyer.com/faceVerify (托管服务如果失效请github 下载代码打包)

![蒲公英下载](https://user-images.githubusercontent.com/15169396/204509617-d36f3990-b63e-4eb8-9437-be8200e1aeed.png)


   
<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/205933436-46e77bda-66f5-49fe-88d0-91d07931b758.png" width = 40% height = 40% />
</div>
