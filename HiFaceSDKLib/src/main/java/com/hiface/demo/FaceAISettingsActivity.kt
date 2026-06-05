package com.hiface.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import com.hiface.demo.UVCCamera.manger.select.DeviceListDialogFragment
import com.hiface.demo.databinding.ActivityFaceAiSettingsBinding
import com.herohan.uvcapp.CameraHelper
import com.tencent.mmkv.MMKV

/**
 * 前后摄像头，角度切换等参数设置
 *
 * 更多UVC 摄像头参数设置参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
class FaceAISettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceAiSettingsBinding
    private val mmkv by lazy { MMKV.defaultMMKV() }

    companion object {
        //系统摄像头相关
        const val FRONT_BACK_CAMERA_FLAG = "cameraFlag"
        const val SYSTEM_CAMERA_DEGREE = "cameraDegree"
        const val UVC_CAMERA_TYPE = "UVC_CAMERA_TYPE" //UVC 协议相机类型，是否带IR

        //UVC 相机旋转 镜像管理
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
        binding.back.setOnClickListener { finish() }

        //1.切换系统相机前后摄像头
        binding.switchCamera.setOnClickListener {
            val current = mmkv.decodeInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT)
            val next = if (current == 1) 0 else 1
            mmkv.encode(FRONT_BACK_CAMERA_FLAG, next)
            Toast.makeText(this, if (next == 0) "Front camera now" else "Back/USB Camera", Toast.LENGTH_SHORT).show()
        }

        // 2.切换系统相机旋转角度
        updateCameraDegreeUI()
        binding.switchCameraDegree.setOnClickListener {
            val degreeSys = (mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, 3) + 1) % 4
            mmkv.encode(SYSTEM_CAMERA_DEGREE, degreeSys)
            updateCameraDegreeUI()
        }

        //UVC RGB摄像头角度旋转设置
        binding.rgbUvcCameraSwitch.setOnClickListener {
            val rgbDegree = (mmkv.decodeInt(RGB_UVC_CAMERA_DEGREE, 0) + 90) % 360
            mmkv.encode(RGB_UVC_CAMERA_DEGREE, rgbDegree)
            Toast.makeText(this, "RGB Camera degree: $rgbDegree", Toast.LENGTH_SHORT).show()
        }

        //RGB画面左右水平翻转
        binding.rgbUvcCameraHorizontal.setOnClickListener {
            val mirror = !mmkv.decodeBool(RGB_UVC_CAMERA_MIRROR_H, false)
            mmkv.encode(RGB_UVC_CAMERA_MIRROR_H, mirror)
            Toast.makeText(this, "RGB CameraHorizontal: $mirror", Toast.LENGTH_SHORT).show()
        }

        //UVC IR摄像头角度旋转设置
        binding.irUvcCameraSwitch.setOnClickListener {
            val irDegree = (mmkv.decodeInt(IR_UVC_CAMERA_DEGREE, 0) + 90) % 360
            mmkv.encode(IR_UVC_CAMERA_DEGREE, irDegree)
            Toast.makeText(this, "IRCamera degree: $irDegree", Toast.LENGTH_SHORT).show()
        }

        //IR画面左右水平翻转
        binding.irUvcCameraHorizontal.setOnClickListener {
            val mirror = !mmkv.decodeBool(IR_UVC_CAMERA_MIRROR_H, false)
            mmkv.encode(IR_UVC_CAMERA_MIRROR_H, mirror)
            Toast.makeText(this, "IRCameraHorizontal: $mirror", Toast.LENGTH_SHORT).show()
        }

        //RGB摄像头选择
        binding.rgbUvcCameraSelect.setOnClickListener {
            selectCamera("RGB 摄像头选择", RGB_UVC_CAMERA_SELECT)
        }

        //IR摄像头选择
        binding.irUvcCameraSelect.setOnClickListener {
            selectCamera("IR 摄像头选择", IR_UVC_CAMERA_SELECT)
        }
    }

    private fun updateCameraDegreeUI() {
        val degree = mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, 0) % 4
        val degreeStr = when (degree) {
            0 -> "0°"
            1 -> "90°"
            2 -> "180°"
            3 -> "270°"
            else -> "0°"
        }
        binding.cameraDegreeText.text = getString(R.string.camera_degree_set) + degreeStr
    }

    private fun selectCamera(cameraName: String, cameraKey: String) {
        val mCameraHelper = CameraHelper()
        val mDeviceDialog = DeviceListDialogFragment(mCameraHelper, cameraName)
        mDeviceDialog.setOnDeviceItemSelectListener { usbDevice ->
            mmkv.encode(cameraKey, usbDevice.productName.toString())
            Toast.makeText(this, usbDevice.productName.toString(), Toast.LENGTH_SHORT).show()
            mDeviceDialog.dismiss()
        }
        mDeviceDialog.show(supportFragmentManager, "device_list")
    }
}
