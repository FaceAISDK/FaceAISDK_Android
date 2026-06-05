package com.hiface.demo

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
import androidx.core.net.toUri
import com.sdk.hiface.base.utils.performance.DevicePerformance
import com.sdk.hiface.core.utils.FaceAICameraType
import com.sdk.hiface.recognize.FaceVerifyUtils
import com.hiface.demo.FaceAISettingsActivity.Companion.UVC_CAMERA_TYPE
import com.hiface.demo.SysCamera.search.FaceSearchNaviActivity
import com.hiface.demo.SysCamera.verify.FaceVerifyNaviActivity
import com.hiface.demo.SysCamera.verify.LivenessDetectActivity
import com.hiface.demo.SysCamera.verify.TwoFaceImageVerifyActivity
import com.hiface.demo.UVCCamera.liveness.Liveness_UVCCameraActivity
import com.hiface.demo.base.AbsBaseActivity
import com.hiface.demo.databinding.ActivityFaceAiNaviBinding
import com.tencent.mmkv.MMKV

/**
 * SDK 接入演示Demo，请先熟悉本Demo跑通主要流程后再集成到你的主工程 验证业务
 *
 * @author FaceAISDK.Service@gmail.com
 */
class FaceAINaviActivity : AbsBaseActivity() {
    private lateinit var viewBinding: ActivityFaceAiNaviBinding
    private val mmkv by lazy { MMKV.defaultMMKV() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFaceAiNaviBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //人脸图保存路径初始化
        setCameraType()

        // 摄像头类型选择 Camera type select
        viewBinding.cameraTypeSelectLayout.setOnClickListener {
            switchCameraType()
        }

        // 1:1 人脸识别
        viewBinding.faceVerify.setOnClickListener {
            startActivity(Intent(this, FaceVerifyNaviActivity::class.java))
        }

        // 人脸搜索(系统相机和UVC 摄像头都支持) Face Search(support System&UVC camera)
        viewBinding.faceSearch.setOnClickListener {
            startActivity(Intent(this, FaceSearchNaviActivity::class.java))
        }

        // 参数设置 FaceAI Settings
        viewBinding.paramsSetting.setOnClickListener {
            startActivity(Intent(this, FaceAISettingsActivity::class.java))
        }

        // 活体检测 livenessDetection
        viewBinding.livenessDetection.setOnClickListener {
            val uvcCameraType = mmkv.decodeInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA)

            if (uvcCameraType == FaceAICameraType.SYSTEM_CAMERA) {
                startActivity(Intent(this, LivenessDetectActivity::class.java))
            } else {
                startActivity(Intent(this, Liveness_UVCCameraActivity::class.java))
            }
        }

        // 两张静态人脸图中人脸相似度对比，two face image similarity compare
        viewBinding.twoFaceVerify.setOnClickListener {
            startActivity(Intent(this, TwoFaceImageVerifyActivity::class.java))
        }

        viewBinding.updateLayout.setOnClickListener {
            val uri = "https://www.pgyer.com/hiface".toUri()
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }

        // 分享FaceAISDK
        viewBinding.shareFace.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_faceai_sdk_content))
            intent.type = "text/plain"
            startActivity(intent)
        }

        viewBinding.systemInfo.setOnClickListener {
            printDeviceInfo()
        }

        // 长按打印Log 信息
        viewBinding.systemInfo.setOnLongClickListener {
            FaceVerifyUtils().printInfo(this)
            finish()
            return@setOnLongClickListener true
        }

        viewBinding.moreAboutMe.setOnClickListener {
            startActivity(Intent(this, AboutFaceAppActivity::class.java))
        }

        showTipsDialog()
    }


    /**
     * 设备系统信息，日志打印出来，dialog也可以直接复制
     */
    private fun printDeviceInfo() {
        val performance = DevicePerformance.getDevicePerformance(this)
        val deviceInfo = arrayOf(
            "Release：${android.os.Build.VERSION.RELEASE}",
            "Model：${android.os.Build.MODEL}",
            "Board：${android.os.Build.BOARD}",
            "FingerPrint：${android.os.Build.FINGERPRINT}",
            "Performance: $performance"
        )

        val fullInfoString = deviceInfo.joinToString(separator = "\n")
        Log.d("Device Info", fullInfoString)

        AlertDialog.Builder(this)
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

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Device Info", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun switchCameraType() {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setIcon(android.R.drawable.ic_menu_camera)
        builderSingle.setTitle(R.string.camera_type_select)
        val arrayAdapter = ArrayAdapter<String?>(this, android.R.layout.select_dialog_item)
        arrayAdapter.add(getString(R.string.camera_type_system))
        arrayAdapter.add(getString(R.string.camera_type_uvc_rgb))
        arrayAdapter.add(getString(R.string.camera_type_uvc_rgb_ir))
        builderSingle.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        builderSingle.setAdapter(arrayAdapter) { _, which ->
            val type = when (which) {
                0 -> FaceAICameraType.SYSTEM_CAMERA
                1 -> FaceAICameraType.UVC_CAMERA_RGB
                else -> FaceAICameraType.UVC_CAMERA_RGB_IR
            }
            mmkv.encode(UVC_CAMERA_TYPE, type)
            setCameraType()
        }
        builderSingle.show()
    }

    private fun setCameraType() {
        val uvcCameraType = mmkv.decodeInt(UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA)
        viewBinding.cameraTypeSelect.text = getString(
            when (uvcCameraType) {
                FaceAICameraType.SYSTEM_CAMERA -> R.string.camera_type_system
                FaceAICameraType.UVC_CAMERA_RGB -> R.string.camera_type_uvc_rgb
                else -> R.string.camera_type_uvc_rgb_ir
            }
        )
    }

    private fun showTipsDialog() {
        val showTime = mmkv.decodeLong("showTipsDialog", 0)
        if (System.currentTimeMillis() - showTime > 300 * 60 * 60 * 1000) {
            val dialog = AlertDialog.Builder(this).create()
            val dialogView = View.inflate(this, R.layout.dialog_face_sdk_tips, null)
            dialog.setView(dialogView)

            val checkBox = dialogView.findViewById<AppCompatImageView>(R.id.privacy_read_checkbox)
            checkBox.setOnClickListener { checkBox.isSelected = !checkBox.isSelected }
            
            dialogView.findViewById<AppCompatTextView>(R.id.privacy_read_content_view).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    data = Uri.parse("https://mp.weixin.qq.com/s/NojZKpNvKO8Bv-_yz6YyWw")
                })
            }
            
            dialogView.findViewById<Button>(R.id.share_face_feature).setOnClickListener {
                mmkv.encode("showTipsDialog", System.currentTimeMillis())
                dialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                DevicePerformance.getDevicePerformance(this)
            }

            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }
}
