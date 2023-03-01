#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

Offline Android Face Detection &amp; Recognition And Alive Detect SDK 离线版Android人脸检测，人脸识别和活体检测SDK

<div align=center>
<img src="http://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 20% height = 20% />
</div>


## 简要说明

  本SDK支持Android 5+，包含人脸检测识别，动作活体检测和静默活体防伪，所有处理都是离线终端执行，人脸信息不收集，更具隐私安全；
  其中活体检测支持张嘴，微笑，眨眼，摇头，点头 随机两种组合验证（摇头点头也可拆分为左右上下4个动作），低端机离线验证速度正常。

  前期测试效果能覆盖95% 的中高低端机器，识别成功率>99% ，实验数据仅供参考，最低端手机完整的兼容性功能通过设备为2016年低端机魅蓝Note3。
  特殊系统设备特殊硬件，如有问题请先提Issues附带系统版本，设备型号，错误log等信息，其他定制化需求请联系 anylife.zlb@gmail.com 交流
  
  ![image](https://user-images.githubusercontent.com/15169396/222224246-b83fca54-6cfb-4924-98a6-809b11da8761.png)


  静默活体检测&炫光活体检测Alpha 版本已经发布，抢先体验请发送邮件。


## 使用场景

   【1:1】 识别手机考勤系统，机场/卡口人证对齐，免密码登录，酒店入驻、刷脸支付、刷脸解锁
   【1:N】 考勤机，物业管理业主出入凭证等，（暂未优化1：N 识别速度！SM-9700百张底片最差5s左右，最好50ms）

## 接入使用
 
    #### 3.8.4 更新说明

    * 提高识别精确度和速度
    * 添加ARM 64位CPU支持
    * 添加静默活体检测，防止视频合成等欺骗 （苹果系统可以下载xPression测验）


    //Gradle
    implementation "io.github.anylifezlb:Face-Verification:3.8.4"
    
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
    * Verify11Activity 人脸检测识别，活体检测页面（ 1：1）
    * AddBaseImageActivity 更换底片页面，仅供参考建议业务方使用自拍裁剪后使用本程序处理获取高质量底片
    * 1：N 识别暂未优化

    其中活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 申请，内容包括
    APP简要描述，App名称 ，包名 ，功能主页截屏和 下载链接5项内容。


## Demo 下载体验

   最新版下载地址： https://www.pgyer.com/faceVerify 

   
<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/210045090-60c073df-ddbd-4747-8e24-f0dce1eccb58.png" width = 33% height = 33% />
</div>

## 服务定制

   如果你的应用场景SDK 不能匹配需要特殊定制化，请发邮件到anylife.zlb@gmail.com
   也可以加微信 HaoNan19990322 （请标注人脸识别定制，否则添加不通过，谢谢）
   
   Github: https://github.com/AnyLifeZLB/FaceVerificationSDK
   
   
   
   
   
