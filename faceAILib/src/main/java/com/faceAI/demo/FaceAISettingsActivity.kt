package com.faceAI.demo

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.faceAI.demo.databinding.ActivityFaceAiSettingsBinding


/**
 * 前后摄像头，角度切换等参数设置
 *
 * 更多UVC 摄像头参数设置参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
class FaceAISettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceAiSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceAiSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener {
            this@FaceAISettingsActivity.finish()
        }

        val sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE)

        //切换系统相机前后
        binding.switchCamera.setOnClickListener {
            val sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE)
            if (sharedPref.getInt("cameraFlag", 1) == 1) {
                sharedPref.edit(commit = true) { putInt("cameraFlag", 0) }
                Toast.makeText(
                    baseContext,
                    "Front camera now",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sharedPref.edit(commit = true) { putInt("cameraFlag", 1) }
                Toast.makeText(
                    baseContext,
                    "Back/USB Camera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val degree = sharedPref.getInt("cameraDegree", 4) % 5

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
         *
         */
        binding.switchCameraDegree.setOnClickListener {
            val degree = (sharedPref.getInt("cameraDegree", 4) + 1) % 5
            sharedPref.edit(commit = true) { putInt("cameraDegree", degree) }

            val degreeStr = when (degree) {
                0 -> "0°"
                1 -> "90°"
                2 -> "180°"
                3 -> "270°"
                else -> "Default"
            }

            Toast.makeText(
                baseContext,
                "Camera degree: $degreeStr",
                Toast.LENGTH_SHORT
            ).show()
        }



        //USB摄像头（UVC协议）管理。如果本SDK Demo不能管理你的定制摄像头，请参考https://github.com/shiyinghan/UVCAndroid

        //双目摄像头角度旋转设置
        binding.uvcCameraSwitch.setOnClickListener{
            var mPreviewRotation = sharedPref.getInt("uvcCameraDegree", 0)
            mPreviewRotation += 90
            mPreviewRotation %= 360
            if (mPreviewRotation < 0) {
                mPreviewRotation += 360
            }
            sharedPref.edit(commit = true) { putInt("uvcCameraDegree", mPreviewRotation) }
            Toast.makeText(
                baseContext,
                "Camera degree: $mPreviewRotation",
                Toast.LENGTH_SHORT
            ).show()
        }

        //画面左右水平翻转
        binding.uvcCameraHorizontal.setOnClickListener {
            val uvcCameraHorizontal = !sharedPref.getBoolean("uvcCameraHorizontal", false)
            sharedPref.edit(commit = true) { putBoolean("uvcCameraHorizontal", uvcCameraHorizontal) }
            Toast.makeText(
                baseContext,
                "uvcCameraHorizontal: $uvcCameraHorizontal",
                Toast.LENGTH_SHORT
            ).show()
        }

    }




}