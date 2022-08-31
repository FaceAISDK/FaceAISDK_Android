#  [FaceVerificationSDK](https://github.com/AnyLifeZLB/FaceVerificationSDK)

Offline Android Face Detection &amp; Recognition And Alive Detect SDK 离线版Android人脸检测，人脸识别和活体检测SDK

<div align=center>
<img src="https://user-images.githubusercontent.com/15169396/182627098-0ca24289-641b-4593-bf7c-72b09c4bf94e.jpeg" width = 20% height = 20% />
</div>


## 简要说明

  本SDK仅供Android 5.0 以上系统使用，包含人脸检测，人脸识别和活体检测，所有功能都是离线使用。SDK 总体积大小约为14.x MB，其中：
  * 人脸检测 2.x MB
  * 识别对比 5.x MB
  * 活体检测 6.x MB；
  
  后期模型数据可以不绑定SDK，以便减少APK 包体积，支持模型自定义私有化部署！
 
  离线模型MFN，解释器 TensorFlow Lite2.9.0 和机器学习套件ML KIT，相机预览和流处理CameraX 
  前期测试效果能覆盖99.5 % 的中低端机器，识别成功率>99.9%。本统计数据仅供参考，以目前手机的配置，足以跑起来整个工程，
  但是多年以前的老旧机型和部分OEM 的设备运行就有点卡顿了，这类设备市面的保有量非常非常低，几乎可以忽略。
  可以概括为正常的手机都没有问题，除了部分山寨组装机，定制机。

## 使用场景

  门禁，考勤系统，人脸识别防盗门，智能楼宇，智能工地，智能校园等场景


## 接入使用
 
    当前版本：1.1.0
    * 修复部分手机闪退问题
    * 添加认别超时检测
    * 优化使用体验
    

    //Android studio gradle 引入（Eclipse？Not support now !）
    implementation "io.github.anylifezlb:Face-Verification:1.0.0-beta02"
    
    ``` 
    //更多说明请看代码和下载Demo体验
    
            FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(0.8f)                 //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
                .setBaseBitmap(baseBitmap)          //底片,请录入的时候保证底片质量
                .setGraphicOverlay(mGraphicOverlay) //遮罩层，人脸模型标记画面演示,可不传。
                .setLiveCheck(true)                 //是否需要活体检测，需要发送邮件，详情参考ReadMe
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onCompleted(boolean isMatched) {
      
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
   
    更多使用说明下载参考本Repo，里面有比较详尽的使用方法，其中 

    * NaviActivity Demo 演示导航页面
    * UpdateBaseFaceActivity 更换底片页面
    * VerifyActivity 人脸检测识别，活体检测页面

    其中活体检测的使用需要你发送邮件到anylife.zlb@gmail.com 申请，内容包括
    APP简要描述，App名称 ，包名 和 下载链接。
   


## Demo 下载

   请前往下载： https://beta.bugly.qq.com/crx4 (托管服务如果失效请github打包)

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ad9d5c616f91466a81dab0282c9daae7~tplv-k3u1fbpfcp-watermark.image?)

   
   
   
