package com.faceVerify.test.verify11

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.AI.FaceVerify.verify.FaceVerifyUtils
import android.os.Bundle
import com.faceVerify.test.R
import com.faceVerify.test.FaceApplication
import android.graphics.Bitmap
import com.AI.FaceVerify.utils.AiUtil
import com.AI.FaceVerify.verify.FaceProcessBuilder
import com.AI.FaceVerify.verify.ProcessCallBack
import android.content.DialogInterface
import com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import com.AI.FaceVerify.verify.VerifyStatus.*
import com.AI.FaceVerify.view.CameraXAnalyzeFragment
import com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL
import com.faceVerify.test.utils.VoicePlayer
import kotlinx.android.synthetic.main.activity_verify_11.*
import java.io.File

/**
 * 1：1 的人脸识别 + 动作活体检测 SDK 接入演示Demo
 * 静默活体检测 版本已经发布，抢先体验请发送邮件
 *
 */
class Verify11Activity : AppCompatActivity() {

    private var faceVerifyUtils: FaceVerifyUtils = FaceVerifyUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_11)
        title = "1:1 人脸识别"

        back.setOnClickListener {
            finish()
        }

        val yourUniQueFaceId = intent.getStringExtra(FaceApplication.USER_ID_KEY)

        // 0 ,前置摄像头       1，后置摄像头    部分外接摄像头支持可能是1
        val cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL,getSharedPreferences(
            "faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag",0))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_camerax, cameraXFragment).commit()

        //要对比的人脸底片，业务方自行处理
        val file = File(
            FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_11_VALUE,
            yourUniQueFaceId
        )

        //预留的底图，缓存在本地私有目录
        val baseBitmap = AiUtil.compressPath(baseContext, Uri.fromFile(file))

        //1.初始化引擎，各种参数配置
        initFaceVerify(baseBitmap)

        //2.初始化相机管理VerifyCameraXFragment，数据回掉格式为ImageProxy（可支持ByteArray）
        cameraXFragment.setOnAnalyzerListener(object : CameraXAnalyzeFragment.onAnalyzeData {

            override fun analyze(imageProxy: ImageProxy) {
                if (this@Verify11Activity.isDestroyed || this@Verify11Activity.isFinishing) return

                //第二个参数i 是指圆直径R-margin 为边长的正方形区域为分析区域,是为了剪裁圆形所在正方形框内的图像进行分析
//                faceVerifyUtils.goVerify(imageProxy, face_cover.margin);

                //3.给人脸识别 活体检测引擎喂数据流 imageProxy
                faceVerifyUtils.goVerify(imageProxy, 2)

            }

            //拓展接口方法，暂不用
            override fun analyze(rgbBytes: ByteArray, w: Int, h: Int) {}
        })
    }


    /**
     * 初始化认证引擎
     *
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名和功能简介到 anylife.zlb@gmail.com
     *
     * @param baseBitmap 底片
     */
    private fun initFaceVerify(baseBitmap: Bitmap) {
        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        val faceProcessBuilder = FaceProcessBuilder.Builder(this)
            .setThreshold(0.85f)       //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
            .setBaseBitmap(baseBitmap) //底片,请录入的时候保证底片质量
            .setLiveCheck(true)        //是否需要活体检测，需要发送邮件，详情参考ReadMe
            .setVerifyTimeOut(15)      //活体检测支持设置超时时间 9-16 秒
            .setMotionStepSize(1)      //随机动作验证活体的步骤个数，支持1-2个步骤

//            .setGraphicOverlay(faceTips)//正式环境请去除设置

            .setProcessCallBack(object : ProcessCallBack() {
                override fun onCompleted(isMatched: Boolean) {
                    runOnUiThread {
                        if (isMatched) {
                            //各种形式的提示，根据业务需求选择
                            tips_view.text = "核验已通过，与底片为同一人！ "
                            face_cover.setTipText("核验已通过，与底片为同一人！");

                            Toast.makeText(this@Verify11Activity, "验证通过", Toast.LENGTH_LONG).show();
                            Handler(Looper.getMainLooper()).postDelayed(
                                { this@Verify11Activity.finish() }, 1000  //时间根据业务需求自由修改
                            )

                            VoicePlayer.getInstance().play(R.raw.verify_success)

                        } else {
                            tips_view.text = "核验不通过，与底片不符！ "
                            face_cover.setTipText("核验不通过，与底片不符！ ");

                            AlertDialog.Builder(this@Verify11Activity)
                                .setMessage("核验不通过，与底片不符！ ")
                                .setCancelable(false)
                                .setPositiveButton(
                                    "知道了"
                                ) { dialog1: DialogInterface?, which: Int -> finish() }
                                .show()

                            VoicePlayer.getInstance().play(R.raw.verify_failed)

                        }
                    }
                }


                override fun onFailed(code: Int) {
                    //严重错误直接阻断主流程的
                }

                override fun onProcessTips(actionCode: Int) {
                    showAliveDetectTips(actionCode)
                }


                override fun onTimeOutStart(p0: Float) {
                    face_cover.startCountDown(p0)
                }

                //静默活体检测得分大于0.85 可以认为是真人
                //静默活体检测&炫光活体检测Alpha 版本已经发布，抢先体验请发送邮件
                override fun onSilentAntiSpoofing(scoreValue: Float) {
                    runOnUiThread {
                        silent_Score.text = "静默活体可靠系数：$scoreValue"
                    }
                }

            })
            .create()

        faceVerifyUtils.setDetectorParams(faceProcessBuilder)
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     *
     */
    private fun showAliveDetectTips(actionCode: Int) {

        if (this@Verify11Activity.isDestroyed || this@Verify11Activity.isFinishing) return

        runOnUiThread {
            when (actionCode) {

                VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT -> {
                    android.app.AlertDialog.Builder(this@Verify11Activity)
                        .setMessage("检测超时了！")
                        .setCancelable(false)
                        .setPositiveButton(
                            "再来一次"
                        ) { dialog1: DialogInterface?, which: Int ->
                            //Demo 只是把每种状态抛出来，用户可以自己根据需求改造
                            faceVerifyUtils.retryVerify()
                        }
                        .show()
                }


                VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY -> {
                    tips_view.text = "多次切换画面或无人脸"
                    face_cover.setTipText("多次切换画面或无人脸");

                    android.app.AlertDialog.Builder(this@Verify11Activity)
                        .setMessage("多次切换画面或无人脸，停止识别。\n识别过程请保持人脸在画面中")
                        .setCancelable(false)
                        .setPositiveButton("知道了") { dialog1: DialogInterface?, which: Int ->
                            finish()
                        }
                        .show()
                }

                //5次相比阈值太低就判断为非同一人
                VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS -> tips_view.text = "人脸比对中..."

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE -> tips_view.text = "画面没有检测到人脸"
                VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED -> tips_view.text = "活体检测失败了"
                VERIFY_DETECT_TIPS_ENUM.ACTION_OK ->{
                    VoicePlayer.getInstance().play(R.raw.face_camera)
                    tips_view.text = "请保持正对屏幕"
                }

                ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE ->{
                    VoicePlayer.getInstance().play(R.raw.open_mouse)
                    tips_view.text = "请张嘴"
                }
                ALIVE_DETECT_TYPE_ENUM.SMILE -> {
                    tips_view.text = "请微笑"
                    VoicePlayer.getInstance().play(R.raw.smile)
                }
                ALIVE_DETECT_TYPE_ENUM.BLINK -> {
                    VoicePlayer.getInstance().play(R.raw.blink)
                    tips_view.text = "请轻眨眼"
                }
                ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD -> {
                    VoicePlayer.getInstance().play(R.raw.shake_head)
                    tips_view.text = "请缓慢左右摇头"
                }
                ALIVE_DETECT_TYPE_ENUM.NOD_HEAD ->{
                    VoicePlayer.getInstance().play(R.raw.nod_head)
                    tips_view.text = "请缓慢上下点头"
                }

//                ALIVE_DETECT_TYPE_ENUM.NO_MOUSE -> tips_view_2.text = "请勿遮挡嘴巴"
//                ALIVE_DETECT_TYPE_ENUM.NO_NOSE -> tips_view_2.text = "请勿遮挡鼻子"
//                ALIVE_DETECT_TYPE_ENUM.NO_EYE -> tips_view_2.text = "请勿遮挡眼睛"
//                ALIVE_DETECT_TYPE_ENUM.LAND_MARK_ALL -> tips_view_2.text = ""

            }
        }
    }




    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceVerifyUtils.destroyProcess()
        face_cover.destroyView()
    }

}
