package com.faceAI.demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.faceAI.demo.UVCCamera.manger.select.DeviceListDialogFragment
import com.faceAI.demo.databinding.ActivityFaceAiSettingsBinding
import com.herohan.uvcapp.CameraHelper


/**
 * 前后摄像头，角度切换等参数设置
 *
 * 更多UVC 摄像头参数设置参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
class FaceAISettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceAiSettingsBinding

    companion object {
        //系统摄像头相关
        const val FRONT_BACK_CAMERA_FLAG = "cameraFlag"
        const val SYSTEM_CAMERA_DEGREE = "cameraDegree"

        //UVC 相机旋转 镜像管理。神奇，竟然有相机两个不同步，那分开管理
        const val RGB_UVC_CAMERA_DEGREE = "RGB_UVCCameraDegree"
        const val RGB_UVC_CAMERA_MIRROR_H = "RGB_UVCCameraHorizontalMirror"
        const val IR_UVC_CAMERA_DEGREE = "IR_UVCCameraDegree"
        const val IR_UVC_CAMERA_MIRROR_H = "IR_UVCCameraHorizontalMirror"

        //手动选择指定摄像头
        const val RGB_UVC_CAMERA_SELECT = "RGB_UVC_CAMERA_SELECT"
        const val IR_UVC_CAMERA_SELECT = "IR_UVC_CAMERA_SELECT"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceAiSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener {
            this@FaceAISettingsActivity.finish()
        }

        val sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE)

        //1.切换系统相机前后
        binding.switchCamera.setOnClickListener {
            if (sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 1) == 1) {
                sharedPref.edit(commit = true) { putInt(FRONT_BACK_CAMERA_FLAG, 0) }
                Toast.makeText(baseContext, "Front camera now", Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit(commit = true) { putInt(FRONT_BACK_CAMERA_FLAG, 1) }
                Toast.makeText(baseContext, "Back/USB Camera", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 切换系统相机旋转角度
        val degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, 4) % 5
        val degreeStr = when (degree) {
            0 -> "0°"
            1 -> "90°"
            2 -> "180°"
            3 -> "270°"
            else -> "Default"
        }
        binding.cameraDegreeText.text = getString(R.string.camera_degree_set) + degreeStr

        /**
         * 共5个值，默认屏幕方向Display.getRotation()和Surface.ROTATION_0,ROTATION_90,ROTATION_180,ROTATION_270
         * {@link Surface.ROTATION_0}
         */
        binding.switchCameraDegree.setOnClickListener {
            val degree = (sharedPref.getInt(SYSTEM_CAMERA_DEGREE, 4) + 1) % 5
            sharedPref.edit(commit = true) { putInt(SYSTEM_CAMERA_DEGREE, degree) }
            val degreeStr = when (degree) {
                0 -> "0°"
                1 -> "90°"
                2 -> "180°"
                3 -> "270°"
                else -> "Default"
            }
            binding.cameraDegreeText.text = getString(R.string.camera_degree_set) + degreeStr
        }


        //==========USB摄像头（UVC协议）管理 更多参考https://github.com/shiyinghan/UVCAndroid =========
        //UVC RGB摄像头角度旋转设置
        binding.rgbUvcCameraSwitch.setOnClickListener {
            var rgbDegree = sharedPref.getInt(RGB_UVC_CAMERA_DEGREE, 0)
            rgbDegree += 90
            rgbDegree %= 360
            if (rgbDegree < 0) {
                rgbDegree += 360
            }
            sharedPref.edit(commit = true) { putInt(RGB_UVC_CAMERA_DEGREE, rgbDegree) }
            Toast.makeText(
                baseContext,
                "RGB Camera degree: $rgbDegree",
                Toast.LENGTH_SHORT
            ).show()
        }

        //RGB画面左右水平翻转
        binding.rgbUvcCameraHorizontal.setOnClickListener {
            val rgbHorizontalMirror = !sharedPref.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false)
            sharedPref.edit(commit = true) {
                putBoolean(
                    RGB_UVC_CAMERA_MIRROR_H,
                    rgbHorizontalMirror
                )
            }
            Toast.makeText(
                baseContext,
                "RGB CameraHorizontal: $rgbHorizontalMirror",
                Toast.LENGTH_SHORT
            ).show()
        }


        //UVC IR摄像头角度旋转设置
        binding.irUvcCameraSwitch.setOnClickListener {
            var irDegree = sharedPref.getInt(IR_UVC_CAMERA_DEGREE, 0)
            irDegree += 90
            irDegree %= 360
            if (irDegree < 0) {
                irDegree += 360
            }
            sharedPref.edit(commit = true) { putInt(IR_UVC_CAMERA_DEGREE, irDegree) }
            Toast.makeText(
                baseContext,
                "IRCamera degree: $irDegree",
                Toast.LENGTH_SHORT
            ).show()
        }

        //IR画面左右水平翻转
        binding.irUvcCameraHorizontal.setOnClickListener {
            val horizontalMirror = !sharedPref.getBoolean(IR_UVC_CAMERA_MIRROR_H, false)
            sharedPref.edit(commit = true) { putBoolean(IR_UVC_CAMERA_MIRROR_H, horizontalMirror) }
            Toast.makeText(
                baseContext,
                "IRCameraHorizontal: $horizontalMirror",
                Toast.LENGTH_SHORT
            ).show()
        }

        //RGB摄像头选择
        binding.rgbUvcCameraSelect.setOnClickListener {
            selectCamera("RGB 摄像头选择",RGB_UVC_CAMERA_SELECT,sharedPref);
        }

        //IR摄像头选择
        binding.irUvcCameraSelect.setOnClickListener {
            selectCamera("IR 摄像头选择",IR_UVC_CAMERA_SELECT,sharedPref);
        }

    }


    /**
     * 选择摄像头
     */
    private fun selectCamera(cameraName: String,cameraKey: String,sharedPref: SharedPreferences) {
        val mCameraHelper = CameraHelper()
        val mDeviceDialog =
            DeviceListDialogFragment(
                mCameraHelper,
                cameraName
            )
        mDeviceDialog.setOnDeviceItemSelectListener { usbDevice ->
            sharedPref.edit(commit = true) { putString(cameraKey, usbDevice.productName.toString()) }
            Toast.makeText(baseContext, usbDevice.productName.toString(), Toast.LENGTH_SHORT).show()
            mDeviceDialog.dismiss()
        }

        mDeviceDialog.show(supportFragmentManager, "device_list")

    }

}