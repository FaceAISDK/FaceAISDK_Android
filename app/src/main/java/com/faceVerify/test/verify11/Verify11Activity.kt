package com.faceVerify.test.verify11

import androidx.appcompat.app.AppCompatActivity
import com.AI.FaceVerify.verify.FaceDetectorUtils
import android.os.Bundle
import com.faceVerify.test.R
import com.faceVerify.test.FaceApplication
import android.graphics.Bitmap
import com.AI.FaceVerify.utils.AiUtil
import com.AI.FaceVerify.verify.FaceProcessBuilder
import com.AI.FaceVerify.verify.ProcessCallBack
import android.content.DialogInterface
import com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_RESULT_ENUM
import com.AI.FaceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import android.graphics.ImageFormat
import androidx.lifecycle.LifecycleOwner
import androidx.core.content.ContextCompat
import android.app.Activity
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import kotlinx.android.synthetic.main.activity_verify.*
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * 1：1 的人脸识别比对
 *
 *
 */
class Verify11Activity : AppCompatActivity() {
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
        title = "1:1 人脸识别"
        
        back.setOnClickListener { v: View? ->
            finish()
        }

        val yourUniQueFaceId = intent.getStringExtra(FaceApplication.BASE_FACE_KEY)

        //要对比的人脸底片（都是1：1 比对）
        val file = File(
            FaceApplication.CACHE_BASE_FACE_DIR + FaceApplication.BASE_FACE_DIR_11,
            yourUniQueFaceId
        )
        val baseBitmap = AiUtil.compressPath(this@Verify11Activity, Uri.fromFile(file))

        //初始化引擎
        initVerify(baseBitmap)
        initCameraXAnalysis()
        Log.d("DEVELOP", isOpenDevelopmentSetting(this).toString() + "  " + isUSBDebugSetting(this))
    }

    /**
     * 初始化认证引擎
     *
     *
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名和功能简介到 anylife.zlb@gmail.com
     * @param baseBitmap 底片
     */
    private fun initVerify(baseBitmap: Bitmap) {
        // 1:N 比对 设置 setFaceLibFolder，1：1 比对设置BaseBitmap
        // 两个都设置优先1：1 识别， 都不设置报错
        val faceProcessBuilder = FaceProcessBuilder.Builder(this)
            .setThreshold(0.8f) //threshold（阈值）设置，范围仅限 0.7-0.9，默认0.8
            .setBaseBitmap(baseBitmap) //底片,请录入的时候保证底片质量
            .setLiveCheck(true) //是否需要活体检测，需要发送邮件，详情参考ReadMe
            .setProcessCallBack(object : ProcessCallBack() {
                override fun onCompleted(isMatched: Boolean) {
                    runOnUiThread {
                        if (isMatched) {
                            tips_view.text = "核验已通过，与底片为同一人！ "
                            AlertDialog.Builder(this@Verify11Activity)
                                .setMessage("核验已通过，与底片为同一人！")
                                .setCancelable(false)
                                .setPositiveButton(
                                    "知道了"
                                ) { dialog1: DialogInterface?, which: Int -> finish() }
                                .show()
                        } else {
                            tips_view.text = "核验不通过，与底片不符！ "
                            AlertDialog.Builder(this@Verify11Activity)
                                .setMessage("核验不通过，与底片不符！ ")
                                .setCancelable(false)
                                .setPositiveButton(
                                    "知道了"
                                ) { dialog1: DialogInterface?, which: Int -> finish() }
                                .show()
                        }
                    }
                }

                override fun onMostSimilar(imagePath: String) {
                    //only 1：N 人脸识别检测会有Callback
                }

                override fun onFailed(code: Int) {}
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
        runOnUiThread {
            when (actionCode) {
                ALIVE_DETECT_RESULT_ENUM.ACTION_TIME_OUT -> {
                    android.app.AlertDialog.Builder(this@Verify11Activity)
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

    /**
     * 开发者选项是否开启
     *
     * @return true 开启
     */
    fun isOpenDevelopmentSetting(activity: Activity): Boolean {
        return Settings.Secure.getInt(
            activity.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
    }

    /**
     * usb调试是否开启
     *
     * @return true 开启
     */
    fun isUSBDebugSetting(activity: Activity): Boolean {
        return Settings.Secure.getInt(activity.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
    }
}