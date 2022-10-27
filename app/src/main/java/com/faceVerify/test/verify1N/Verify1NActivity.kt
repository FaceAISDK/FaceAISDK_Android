package com.faceVerify.test.verify1N

import androidx.appcompat.app.AppCompatActivity
import com.AI.FaceVerify.verify.FaceDetectorUtils
import android.os.Bundle
import com.faceVerify.test.R
import com.AI.FaceVerify.verify.FaceProcessBuilder
import com.faceVerify.test.FaceApplication
import com.AI.FaceVerify.verify.ProcessCallBack
import android.content.DialogInterface
import com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM
import com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import android.graphics.ImageFormat
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_verify.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * 1：N 的人脸识别比对
 *
 *
 */
class Verify1NActivity : AppCompatActivity() {
    private var faceDetectorUtils: FaceDetectorUtils = FaceDetectorUtils()

    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceDetectorUtils.destroyProcess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        back.setOnClickListener { finish() }

        //初始化引擎
        initVerify()
        initCameraXAnalysis()
    }

    /**
     * 1：N 人脸识别比对，验证的时候可以百度自行插入N 张人脸底图到对应目录
     *
     */
    private fun initVerify() {
        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        val faceProcessBuilder = FaceProcessBuilder.Builder(this)
            .setThreshold(0.8f) //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
            .setFaceLibFolder(FaceApplication.CACHE_BASE_FACE_DIR + FaceApplication.BASE_FACE_DIR_1N) //N 底片库
            .setLiveCheck(false) //是否需要活体检测，需要发送邮件，详情参考ReadMe
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
                                "知道了"
                            ) { dialog1: DialogInterface?, which: Int -> finish() }
                            .show()
                    }
                }

                override fun onFailed(code: Int) {}
                override fun onProcessTips(actionCode: Int) {
                    showAliveDetectTips(actionCode)
                }
            })
            .create()
        faceDetectorUtils!!.setDetectorParams(faceProcessBuilder)
    }

    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     */
    private fun showAliveDetectTips(actionCode: Int) {
        runOnUiThread {
            when (actionCode) {
                ALIVE_DETECT_RESULT_ENUM.ACTION_TIME_OUT -> android.app.AlertDialog.Builder(this@Verify1NActivity)
                    .setMessage("检测超时了！")
                    .setCancelable(false)
                    .setPositiveButton(
                        "再来一次"
                    ) { dialog1: DialogInterface?, which: Int ->
                        //Demo 只是把每种状态抛出来，用户可以自己根据需求改造
                        faceDetectorUtils.retryVerify()
                    }
                    .show()
                ALIVE_DETECT_RESULT_ENUM.ACTION_NO_FACE_DETECT -> tips_view.text = "画面没有检测到人脸"
                ALIVE_DETECT_RESULT_ENUM.ACTION_FAILED -> tips_view.text = "活体检测失败了"
                ALIVE_DETECT_RESULT_ENUM.ACTION_OK -> tips_view.text = "已经完成活体检测"
                ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE -> tips_view.text = "请张嘴"
                ALIVE_DETECT_TYPE_ENUM.SMILE -> tips_view.text = "请微笑"
                ALIVE_DETECT_TYPE_ENUM.BLINK -> tips_view.text = "请轻眨眼"
                ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD -> tips_view.text = "请缓慢左右摇头"
                ALIVE_DETECT_TYPE_ENUM.NOD_HEAD -> tips_view.text = "请缓慢上下点头"
            }
        }
    }

    /**
     * 相机的兼容性问题处理有点繁琐，Google 也没法适配全部厂商
     *
     */
    fun initCameraXAnalysis() {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                // Camera provider is now guaranteed to be available
                val cameraProvider = cameraProviderFuture.get()

                // Set up the view finder use case to display camera preview
                val preview = Preview.Builder().build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy: ImageProxy ->
                    require(imageProxy.format == ImageFormat.YUV_420_888) { "Invalid image format" }
                    faceDetectorUtils.goVerify(imageProxy)
                    imageProxy.close()
                }


                //front camera default
                val lensFacing =
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK


                // Choose the camera by requiring a lens facing
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Attach use cases to the camera with the same lifecycle owner
                cameraProvider.bindToLifecycle(
                    (this as LifecycleOwner),
                    cameraSelector,
                    preview, imageAnalysis
                )


                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )
            } catch (e: InterruptedException) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            } catch (e: ExecutionException) {
            } catch (e: CameraInfoUnavailableException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }
}