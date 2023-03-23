package com.faceVerify.test.verify1N

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import com.AI.FaceVerify.verify.FaceProcessBuilder
import com.AI.FaceVerify.verify.FaceVerifyUtils
import com.AI.FaceVerify.verify.ProcessCallBack
import com.AI.FaceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM
import com.AI.FaceVerify.view.CameraXAnalyzeFragment
import com.AI.FaceVerify.view.CameraXAnalyzeFragment.CAMERA_ORIGINAL
import com.bumptech.glide.Glide
import com.faceVerify.test.FaceApplication
import com.faceVerify.test.R
import kotlinx.android.synthetic.main.activity_verify_1n.*
import java.io.File


/**
 * 1：N 的人脸识别比对，还没有完善封装
 *
 * 底片库太多识别时间还不够理想，预计23年二季度完善
 *
 *
 */
class Verify1NActivity : AppCompatActivity() {

    private var faceDetectorUtils: FaceVerifyUtils = FaceVerifyUtils()

    private var score: Float=0f  //如果开启了活体检测，业务需要判断一下当前得分再下一步

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
            .setThreshold(0.85f) //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
            .setFaceLibFolder(FaceApplication.BASE_FACE_PATH + FaceApplication.DIR_1N_VALUE) //N 底片库文件夹路径
            .setLiveCheck(true) //是否需要活体检测，需要发送邮件，详情参考ReadMe
            .setProcessCallBack(object : ProcessCallBack() {

                override fun onSilentAntiSpoofing(s: Float) {
                    score=s
                    //静默活体分值，防止作弊
                    face_cover.setTipText("活体得分：$score")
                    result_layout.visibility= INVISIBLE

                }

                override fun onMostSimilar(imagePath: String) {
                    runOnUiThread {

                        if(score>9){
                            //假如你需要活体检测,得分> Hold 才不是作弊
                        }

                        face_cover.setTipText("")

                        if(!TextUtils.isEmpty(imagePath)){
                            result_layout.visibility= VISIBLE
                            Glide.with(baseContext).load(imagePath).into(result_image)

                            val file = File(imagePath)
                            result_text.text=file.name
                        }

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

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE -> {
                    result_layout.visibility= INVISIBLE
                    face_cover.setTipText( "没有检测到人脸")
                }

                VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY -> {
                    result_layout.visibility= INVISIBLE
                    face_cover.setTipText( "没有检测到人脸")
                }


                VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS -> {

                    Handler().postDelayed(Runnable {
                        face_cover.setTipText( "检索中...")
                    }, 1000)


                }
            }
        }
    }


    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceDetectorUtils.destroyProcess()
    }


}
