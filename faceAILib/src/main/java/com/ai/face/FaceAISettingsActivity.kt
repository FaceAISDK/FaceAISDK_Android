package com.ai.face

import android.content.Context
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.databinding.ActivityFaceAiSettingsBinding
import androidx.core.content.edit

/**
 * 前后摄像头，角度切换等参数设置
 *
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
                sharedPref.edit().putInt("cameraFlag", 0).commit()
                Toast.makeText(
                    baseContext,
                    "Front camera now",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sharedPref.edit().putInt("cameraFlag", 1).commit()
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

        //UVC 协议摄像头是三方库 https://blog.csdn.net/hanshiying007/article/details/124118486
        binding.binocularBrightSetting.setOnClickListener {

        }

    }

}