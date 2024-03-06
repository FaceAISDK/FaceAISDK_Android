package com.ai.face.verify

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import com.ai.face.R
import com.ai.face.base.view.CameraXFragment
import com.ai.face.base.view.CameraXFragment.onAnalyzeData
import com.ai.face.databinding.ActivityLivenessDetectionBinding
import com.ai.face.faceVerify.verify.FaceProcessBuilder
import com.ai.face.faceVerify.verify.FaceVerifyUtils
import com.ai.face.faceVerify.verify.ProcessCallBack
import com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM
import com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM
import com.ai.face.utils.VoicePlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

/**
 * 人脸活体检测
 *
 */
class LivenessDetectionActivity : AppCompatActivity() {

    private var faceDetectorUtils: FaceVerifyUtils = FaceVerifyUtils()
    private lateinit var binding: ActivityLivenessDetectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLivenessDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "活体检测"

        val cameraXFragment =
            CameraXFragment.newInstance(
                getSharedPreferences("faceVerify", Context.MODE_PRIVATE).getInt("cameraFlag", 0)
            )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_camerax, cameraXFragment).commit()

        val faceProcessBuilder = FaceProcessBuilder
            .Builder(this@LivenessDetectionActivity)
            .setThreshold(0.79f)  //threshold（阈值）设置，范围仅限 0.75-0.95，默认0.8
            .setBaseBitmap(null)  //底片,没有底片说明只需要活体检测，不需要1:1 人脸对比
            .setLiveCheck(true)   //是否需要活体检测，需要发送邮件，详情参考 ReadMe
            .setVerifyTimeOut(15)
            .setGraphicOverlay(binding.faceTips)
            .setMotionStepSize(2) //动作活体检测几个步骤
            .setLicenceKey("Y29tLkFJLnRlc3Q=") //试用版，不要直接集成到正式APP
            .setProcessCallBack(object : ProcessCallBack() {
                override fun onAliveCheckPass(isPass: Boolean, bitmap: Bitmap) {
                    runOnUiThread {
                        Glide.with(baseContext)
                            .load(bitmap)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .transform(RoundedCorners(10))
                            .into(binding.callBack)
                        binding.coverView.setTipText("活体检测完成")

                    }
                }

                override fun onProcessTips(actionCode: Int) {
                    showAliveDetectTips(actionCode)
                }

                override fun onTimeOutStart(second: Float) {
                    binding.coverView.startCountDown(second)
                }

                override fun onSilentAntiSpoofing(scoreValue: Float) {
                    runOnUiThread {
                        binding.score.text = "静默活体得分：$scoreValue"
                    }
                }

            })
            .create()
        
        faceDetectorUtils.setDetectorParams(faceProcessBuilder)

        cameraXFragment.setOnAnalyzerListener(object : onAnalyzeData {
            override fun analyze(imageProxy: ImageProxy) {
                if (this@LivenessDetectionActivity.isDestroyed || this@LivenessDetectionActivity.isFinishing) return
                faceDetectorUtils.goVerify(imageProxy, 11)
            }
        })


        binding.back.setOnClickListener {
            finish()
        }


    }


    /**
     * 根据业务和UI交互修改你的UI
     *
     *
     */
    private fun showAliveDetectTips(actionCode: Int) {

        if (this@LivenessDetectionActivity.isDestroyed || this@LivenessDetectionActivity.isFinishing) return

        runOnUiThread {
            when (actionCode) {

                VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT ->
                    //这里不需要再维护一个TimeOut 了吧，可以利用CaverView 里面的
                    AlertDialog.Builder(this@LivenessDetectionActivity)
                        .setMessage("人脸识别检测超时！")
                        .setCancelable(false)
                        .setPositiveButton("再来一次") { _: DialogInterface?, _: Int ->
                            //Demo 只是把每种状态抛出来，用户可以自己根据需求改造
                            faceDetectorUtils.retryVerify()
                        }
                        .show()

                VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY -> {
                    binding.coverView.setTipText("多次切换画面或无人脸")
                    AlertDialog.Builder(this@LivenessDetectionActivity)
                        .setMessage("多次切换画面或无人脸，停止识别。\n识别过程请保持人脸在画面中")
                        .setCancelable(false)
                        .setPositiveButton("去查看") { _: DialogInterface?, _: Int ->
                            finish()
                        }
                        .show()
                }

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE -> binding.coverView.setTipText("画面没有检测到人脸")
                VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS -> binding.coverView.setTipText("人脸比对中...")  //5次相比阈值太低就判断为非同一人
                VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED -> binding.coverView.setTipText("活体检测失败了")
                VERIFY_DETECT_TIPS_ENUM.ACTION_OK -> {
                    binding.coverView.setTipText("请保持正对屏幕") //已经完成活体检测
                }

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_BASE_IMG -> {
                    Toast.makeText(this, "没有设置底片", Toast.LENGTH_LONG).show()
                    binding.coverView.setTipText("没有设置底片")
                }

                ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE -> binding.coverView.setTipText("请张嘴")
                ALIVE_DETECT_TYPE_ENUM.SMILE -> binding.coverView.setTipText("请微笑")
                ALIVE_DETECT_TYPE_ENUM.BLINK -> binding.coverView.setTipText("请轻眨眼")
                ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD -> {
                    binding.coverView.setTipText("请缓慢左右摇头")
                }

                ALIVE_DETECT_TYPE_ENUM.NOD_HEAD -> binding.coverView.setTipText("请缓慢上下点头")
                ALIVE_DETECT_TYPE_ENUM.INIT_ALIVE_ERROR -> binding.coverView.setTipText("启动活体检测失败")

                ALIVE_DETECT_TYPE_ENUM.ALIVE_CHECK_DONE -> {
                    binding.coverView.setTipText("检测通过，请正对摄像头")
                    VoicePlayer.getInstance().addPayList(R.raw.liveness_dection_done)
                }

            }
        }
    }


    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     *
     */
    override fun onPause() {
        super.onPause()
        faceDetectorUtils.pauseProcess()
    }


    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceDetectorUtils.destroyProcess()
        binding.coverView.destroyView() //停止动画,回收资源
    }


}