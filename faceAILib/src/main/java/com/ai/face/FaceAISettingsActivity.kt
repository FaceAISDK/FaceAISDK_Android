package com.ai.face

import android.content.Context
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.databinding.ActivityFaceAiSettingsBinding

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
        binding.back.setOnClickListener{
            this@FaceAISettingsActivity.finish()
        }

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

        binding.switchCameraDegree.setOnClickListener{
            val sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE)
            val degree=(sharedPref.getInt("cameraDegree",0)+1)%3
            sharedPref.edit().putInt("cameraDegree",degree).commit()
            Surface.ROTATION_0

            val degreeStr=when(degree){
                0->"0°"
                1->"90°"
                2->"180°"
                else->"270°"
                }

            Toast.makeText(
                baseContext,
                "Camera degree $degreeStr",
                Toast.LENGTH_SHORT).show()
        }

        //双目摄像头设置更多参考 https://blog.csdn.net/hanshiying007/article/details/124118486
        binding.binocularBrightSetting.setOnClickListener{

        }


    }





}