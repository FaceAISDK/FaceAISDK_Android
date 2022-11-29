package com.faceVerify.test.verify1N

import android.content.DialogInterface
import android.os.Bundle
import com.faceVerify.test.R
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy

import com.AI.FaceVerify.verify.FaceProcessBuilder
import com.AI.FaceVerify.verify.FaceVerifyUtils
import com.AI.FaceVerify.verify.ProcessCallBack
import com.AI.FaceVerify.verify.VerifyStatus.*
import com.AI.FaceVerify.view.CameraXAnalyzeFragment
import com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL
import com.faceVerify.test.FaceApplication
import kotlinx.android.synthetic.main.activity_verify_1n.*


/**
 * 1：N 的人脸识别比对
 *
 *
 */
class Verify1NActivity : AppCompatActivity() {

    private var faceDetectorUtils: FaceVerifyUtils = FaceVerifyUtils()

    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceDetectorUtils.destroyProcess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_verify_1n)

        back.setOnClickListener { finish() }

        //初始化引擎
        initFaceVerify()

        val cameraXFragment = CameraXAnalyzeFragment.newInstance(CAMERA_ORIGINAL)

        //VerifyCameraXFragment 封装了相机的处理，UI 定制暴露给业务层自由修改
        cameraXFragment.setOnAnalyzerListener(object : CameraXAnalyzeFragment.onAnalyzeData {
            override fun analyze(imageProxy: ImageProxy) {
                if (this@Verify1NActivity.isDestroyed || this@Verify1NActivity.isFinishing) return
                faceDetectorUtils.goVerify(imageProxy, face_cover.margin)
            }

            override fun analyze(rgbBytes: ByteArray, w: Int, h: Int) {

            }
        })

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_camerax, cameraXFragment).commit()

    }

    /**
     * 1：N 人脸识别比对，验证的时候可以百度自行插入N 张人脸底图到对应目录
     *
     */
    private fun initFaceVerify() {
        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        val faceProcessBuilder = FaceProcessBuilder.Builder(this)
            .setThreshold(0.88f) //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
            .setFaceLibFolder(FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_1N_VALUE) //N 底片库
            .setLiveCheck(false) //是否需要活体检测，需要发送邮件，详情参考ReadMe
            .setVerifyTimeOut(10)     //  活体检测支持设置超时时间 9-16 秒
            .setProcessCallBack(object : ProcessCallBack() {
                override fun onCompleted(isMatched: Boolean) {
                    //only 1：1 will callback
                }

                override fun onMostSimilar(imagePath: String) {
                    runOnUiThread {
                        AlertDialog.Builder(this@Verify1NActivity)
                            .setMessage("最佳匹配：$imagePath")
                            .setCancelable(false)
                            .setPositiveButton(
                                "知道了", null
                            )
                            .show()
                    }
                }

                override fun onFailed(code: Int) {

                }

                override fun onProcessTips(actionCode: Int) {
                    showAliveDetectTips(actionCode)
                }
            })
            .create()
        faceDetectorUtils.setDetectorParams(faceProcessBuilder)
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     */
    private fun showAliveDetectTips(actionCode: Int) {

        if (this@Verify1NActivity.isDestroyed || this@Verify1NActivity.isFinishing) return

        runOnUiThread {
            when (actionCode) {
                VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT -> {
                    android.app.AlertDialog.Builder(this@Verify1NActivity)
                        .setMessage("检测超时了！")
                        .setCancelable(false)
                        .setPositiveButton(
                            "再来一次"
                        ) { dialog1: DialogInterface?, which: Int ->
                            //Demo 只是把每种状态抛出来，用户可以自己根据需求改造
                            faceDetectorUtils.retryVerify()
                        }
                        .show()
                }

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE -> tips_view.text = "画面没有检测到人脸"
                VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED -> tips_view.text = "活体检测失败了"
                VERIFY_DETECT_TIPS_ENUM.ACTION_OK -> tips_view.text = "已经完成活体检测"

                ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE -> tips_view.text = "请张嘴"
                ALIVE_DETECT_TYPE_ENUM.SMILE -> tips_view.text = "请微笑"
                ALIVE_DETECT_TYPE_ENUM.BLINK -> tips_view.text = "请轻眨眼"
                ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD -> tips_view.text = "请缓慢左右摇头"
                ALIVE_DETECT_TYPE_ENUM.NOD_HEAD -> tips_view.text = "请缓慢上下点头"

            }
        }
    }

}
