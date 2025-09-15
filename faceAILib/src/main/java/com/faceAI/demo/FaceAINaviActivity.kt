package com.faceAI.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.ai.face.faceVerify.verify.FaceVerifyUtils
import com.faceAI.demo.SysCamera.camera.CustomCameraActivity
import com.faceAI.demo.SysCamera.search.SearchNaviActivity
import com.faceAI.demo.SysCamera.verify.FaceVerifyWelcomeActivity
import com.faceAI.demo.SysCamera.verify.LivenessDetectActivity
import com.faceAI.demo.SysCamera.verify.TwoFaceImageVerifyActivity
import com.faceAI.demo.databinding.ActivityFaceAiNaviBinding


/**
 * SDK 接入演示Demo，请先熟悉本Demo跑通住流程后再集成到你的主工程验证业务
 *
 */
class FaceAINaviActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityFaceAiNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFaceAiNaviBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)



        //人脸图保存路径初始化
        FaceImageConfig.init(this)

        //分享
        viewBinding.shareLayout.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_faceai_sdk_content))
            intent.type = "text/plain"
            startActivity(intent)
        }

        //1:1 人脸识别
        viewBinding.faceVerifyCard.setOnClickListener {
            val enumIntent = Intent(baseContext, FaceVerifyWelcomeActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(
                FaceVerifyWelcomeActivity.FACE_VERIFY_DATA_SOURCE_TYPE,
                FaceVerifyWelcomeActivity.DataSourceType.Android_HAL
            )
            enumIntent.putExtras(bundle)
            startActivity(enumIntent)
        }

        // 人脸搜索(系统相机和双目USB UVC 摄像头都支持)
        viewBinding.faceSearch.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, SearchNaviActivity::class.java))
        }

        // 参数设置
        viewBinding.paramsSetting.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, FaceAISettingsActivity::class.java))
        }

        // 系统相机自定义调试
        viewBinding.customCamera.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, CustomCameraActivity::class.java))
        }

        viewBinding.systemInfo.setOnClickListener {
            printDeviceInfo()
        }

        // 长按打印Log 信息
        viewBinding.systemInfo.setOnLongClickListener {
            FaceVerifyUtils().printInfo(this@FaceAINaviActivity);
            return@setOnLongClickListener true
        }

        //双目摄像头，请确认你的双目UVC摄像头参数符合程序要求
        viewBinding.binocularCamera.setOnClickListener {
            val uvcCameraModeIntent = Intent(baseContext, FaceVerifyWelcomeActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(
                FaceVerifyWelcomeActivity.FACE_VERIFY_DATA_SOURCE_TYPE,
                FaceVerifyWelcomeActivity.DataSourceType.UVC
            )
            uvcCameraModeIntent.putExtras(bundle)
            startActivity(uvcCameraModeIntent)
        }


        viewBinding.moreAboutMe.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, AboutFaceAppActivity::class.java))
        }

        viewBinding.livenessDetection.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, LivenessDetectActivity::class.java))
        }

        //两张静态人脸图中人脸相似度 对比
        viewBinding.twoFaceVerify.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, TwoFaceImageVerifyActivity::class.java))
        }

        showTipsDialog()
    }


    /**
     * 设备系统信息
     *
     */
    private fun printDeviceInfo() {
        val deviceInfo = arrayOf(
            " ",
            "型号：${android.os.Build.MODEL}",
            "主板：${android.os.Build.BOARD}",
            "设备标识：${android.os.Build.FINGERPRINT}",
            "Android SDK版本号：${android.os.Build.VERSION.SDK_INT}",
            "HARDWARE：${android.os.Build.HARDWARE}",
            "主机（HOST）：${android.os.Build.HOST}",
            " "
        )
        AlertDialog.Builder(this@FaceAINaviActivity)
            .setItems(deviceInfo) { _, _ ->
            }.show()
    }


    /**
     * SDK Demo 演示试用说明
     *
     */
    private fun showTipsDialog() {
        val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        val showTime = sharedPref.getLong("showFaceAISDKTips", 0)
        if (System.currentTimeMillis() - showTime > 31 * 60 * 60 * 1000) {

            val builder = AlertDialog.Builder(this)
            val dialog = builder.create()
            val dialogView = View.inflate(this, R.layout.dialog_face_sdk_tips, null)
            //设置对话框布局
            dialog.setView(dialogView)
            val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)
            btnOK.setOnClickListener {
                sharedPref.edit(commit = true) {
                    putLong(
                        "showFaceAISDKTips",
                        System.currentTimeMillis()
                    )
                }
                dialog.dismiss()
            }
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

        }

    }


}