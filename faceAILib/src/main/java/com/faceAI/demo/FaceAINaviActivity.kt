package com.faceAI.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import com.ai.face.core.utils.FaceAICameraType
import com.ai.face.faceVerify.verify.FaceVerifyUtils
import com.faceAI.demo.FaceAISettingsActivity.Companion.UVC_CAMERA_TYPE
import com.faceAI.demo.SysCamera.camera.CustomCameraActivity
import com.faceAI.demo.SysCamera.search.FaceSearchNaviActivity
import com.faceAI.demo.SysCamera.verify.FaceVerifyNaviActivity
import com.faceAI.demo.SysCamera.verify.LivenessDetectActivity
import com.faceAI.demo.SysCamera.verify.TwoFaceImageVerifyActivity
import com.faceAI.demo.UVCCamera.liveness.Liveness_UVCCameraActivity
import com.faceAI.demo.base.AbsBaseActivity
import com.faceAI.demo.base.utils.performance.DevicePerformance
import com.faceAI.demo.databinding.ActivityFaceAiNaviBinding

/**
 * SDK 接入演示Demo，请先熟悉本Demo跑通主要流程后再集成到你的主工程 验证业务
 *
 * @author FaceAISDK.Service@gmail.com
 */
class FaceAINaviActivity : AbsBaseActivity() {
    private lateinit var viewBinding: ActivityFaceAiNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFaceAiNaviBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //人脸图保存路径初始化
        setCameraType()

        // 摄像头类型选择 Camera type select
        viewBinding.cameraTypeSelect.setOnClickListener {
            switchCameraType()
        }

        // 1:1 人脸识别
        viewBinding.faceVerify.setOnClickListener {
            val verifyIntent = Intent(baseContext, FaceVerifyNaviActivity::class.java)
            startActivity(verifyIntent)
        }

        // 人脸搜索(系统相机和UVC 摄像头都支持) Face Search(support System&UVC camera)
        viewBinding.faceSearch.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, FaceSearchNaviActivity::class.java))
        }

        // 参数设置 FaceAI Settings
        viewBinding.paramsSetting.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, FaceAISettingsActivity::class.java))
        }

        // 活体检测 livenessDetection
        viewBinding.livenessDetection.setOnClickListener {
            val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
            val uvcCameraType = sharedPref.getInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA)

            if(uvcCameraType== FaceAICameraType.SYSTEM_CAMERA){
                startActivity(Intent(this@FaceAINaviActivity, LivenessDetectActivity::class.java))
            }else{
                startActivity(Intent(this@FaceAINaviActivity, Liveness_UVCCameraActivity::class.java))
            }
        }

        // 两张静态人脸图中人脸相似度对比，two face image similarity compare
        viewBinding.twoFaceVerify.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, TwoFaceImageVerifyActivity::class.java))
        }


        // 录入分享人脸信息
        viewBinding.addFace.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, ShareFaceFeatureActivity::class.java))
        }

        // 分享FaceAISDK
        viewBinding.shareLayout.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_faceai_sdk_content))
            intent.type = "text/plain"
            startActivity(intent)
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
            FaceVerifyUtils().printInfo(this@FaceAINaviActivity)
            finish()
            return@setOnLongClickListener true
        }

        viewBinding.moreAboutMe.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, AboutFaceAppActivity::class.java))
        }

        showTipsDialog()
    }


    /**
     * 设备系统信息，日志打印出来，dialog也可以直接复制
     */
    private fun printDeviceInfo() {
        // 1. 获取性能分值     2 高性能， 1 中配   0 低配
        val performance = DevicePerformance.getDevicePerformance(this@FaceAINaviActivity)

        val deviceInfo = arrayOf(
            "Release：${android.os.Build.VERSION.RELEASE}",
            "Model：${android.os.Build.MODEL}",
            "Board：${android.os.Build.BOARD}",
            "FingerPrint：${android.os.Build.FINGERPRINT}",
            "Performance: $performance"
        )

        val fullInfoString = deviceInfo.joinToString(separator = "\n")
        Log.d("Device Info", fullInfoString)

        AlertDialog.Builder(this@FaceAINaviActivity)
            .setTitle("Device Info")
            .setItems(deviceInfo, null)
            .setNegativeButton(R.string.copy_device_info) { dialog, _ ->
                copyToClipboard(fullInfoString)
                Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setPositiveButton(R.string.know) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 实现复制到剪贴板的辅助方法
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Device Info", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * 切换相机类型（1.系统相机  2.UVC外接RGB摄像头  3.UVC外接RGB+IR摄像头）
     *
     */
    private fun switchCameraType() {
        val builderSingle = AlertDialog.Builder(this@FaceAINaviActivity)
        builderSingle.setIcon(android.R.drawable.ic_menu_camera)
        builderSingle.setTitle(R.string.camera_type_select)
        val arrayAdapter =
            ArrayAdapter<String?>(this@FaceAINaviActivity,
                android.R.layout.select_dialog_item)
        arrayAdapter.add(getString(R.string.camera_type_system))
        arrayAdapter.add(getString(R.string.camera_type_uvc_rgb))
        arrayAdapter.add(getString(R.string.camera_type_uvc_rgb_ir))
        builderSingle.setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
        builderSingle.setAdapter(arrayAdapter) { dialog, which ->
            val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
            when (which) {
                0 -> {
                    sharedPref.edit(commit = true) { putInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA) }
                }
                1 -> {
                    sharedPref.edit(commit = true) { putInt(UVC_CAMERA_TYPE, FaceAICameraType.UVC_CAMERA_RGB) }
                }
                else -> {
                    sharedPref.edit(commit = true) { putInt(UVC_CAMERA_TYPE, FaceAICameraType.UVC_CAMERA_RGB_IR) }
                }
            }
            setCameraType()
        }
        builderSingle.show()
    }

    /**
     *  当前的相机类型
     */
    private  fun setCameraType() {
        val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        val uvcCameraType = sharedPref.getInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA)
        when (uvcCameraType) {
            FaceAICameraType.SYSTEM_CAMERA -> {
                viewBinding.cameraTypeSelect.text = getString(R.string.camera_type_system)
            }
            FaceAICameraType.UVC_CAMERA_RGB -> {
                viewBinding.cameraTypeSelect.text = getString(R.string.camera_type_uvc_rgb)
            }
            else -> {
                viewBinding.cameraTypeSelect.text = getString(R.string.camera_type_uvc_rgb_ir)
            }
        }
    }


    /**
     * SDK Demo 演示试用说明
     *
     */
    private fun showTipsDialog() {

        val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        val showTime = sharedPref.getLong("showTipsDialog", 0)
        if (System.currentTimeMillis() - showTime > 300 * 60 * 60 * 1000) {
            val builder = AlertDialog.Builder(this)
            val dialog = builder.create()
            val dialogView = View.inflate(this, R.layout.dialog_face_sdk_tips, null)
            //设置对话框布局
            dialog.setView(dialogView)

            val checkBox = dialogView.findViewById<AppCompatImageView>(R.id.privacy_read_checkbox)
            checkBox.setOnClickListener {
                checkBox.isSelected=!checkBox.isSelected
            }
            val privacy = dialogView.findViewById<AppCompatTextView>(R.id.privacy_read_content_view)
            privacy.setOnClickListener {
                val uri = Uri.parse("https://mp.weixin.qq.com/s/NojZKpNvKO8Bv-_yz6YyWw")
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.data = uri
                startActivity(intent)
            }
            val btnOK = dialogView.findViewById<Button>(R.id.share_face_feature)
            btnOK.setOnClickListener {
                if(!checkBox.isSelected){
                    Toast.makeText(this,R.string.login_privacy_policy, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                sharedPref.edit(commit = true) {
                    putLong(
                        "showTipsDialog",
                        System.currentTimeMillis()
                    )
                }
                dialog.dismiss()

                //检测配置等级，运行一下看看兼容性
                val performance=DevicePerformance.getDevicePerformance(this@FaceAINaviActivity)
            }

            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }


}