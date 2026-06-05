package com.hiface.demo.SysCamera.search

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdk.hiface.core.utils.FaceAICameraType
import com.sdk.hiface.search.FaceSearchFeature
import com.sdk.hiface.search.FaceSearchFeatureManger
import com.hiface.demo.FaceAISettingsActivity
import com.hiface.demo.R
import com.hiface.demo.UVCCamera.search.FaceSearch_UVCCameraActivity
import com.hiface.demo.databinding.ActivityFaceSearchNaviBinding
import com.tencent.mmkv.MMKV
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

/**
 * 人脸识别搜索 演示导航Navi
 */
class FaceSearchNaviActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var binding: ActivityFaceSearchNaviBinding
    private var cameraType = FaceAICameraType.SYSTEM_CAMERA
    private val mmkv by lazy { MMKV.defaultMMKV() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkNeededPermission()

        cameraType = mmkv.decodeInt(FaceAISettingsActivity.UVC_CAMERA_TYPE, FaceAICameraType.SYSTEM_CAMERA)

        binding.cameraMode.setText(when (cameraType) {
            FaceAICameraType.SYSTEM_CAMERA -> R.string.camera_type_system
            FaceAICameraType.UVC_CAMERA_RGB -> R.string.camera_type_uvc_rgb
            else -> R.string.camera_type_uvc_rgb_ir
        })

        binding.back.setOnClickListener { finish() }

        //批量导入导出人脸特征数据
        binding.insertFaceFeatures.setOnClickListener {
            val num = FaceSearchFeatureManger.getInstance(this).insertFeatures(JSONFaceFeatures.testJsonStrings)
            Toast.makeText(baseContext, "Done,$num", Toast.LENGTH_SHORT).show()
        }

        //1:N 人脸搜索
        binding.faceSearch1n.setOnClickListener {
            if (cameraType == FaceAICameraType.SYSTEM_CAMERA) {
                startActivity(Intent(this, FaceSearch1NActivity::class.java))
            } else {
                startActivity(Intent(this, FaceSearch_UVCCameraActivity::class.java))
            }
        }

        //图片人脸搜索
        binding.faceSearchByImage.setOnClickListener {
            startActivity(Intent(this, FaceSearchByImageActivity::class.java))
        }

        //MN人脸搜索
        binding.systemCameraSearchMn.setOnClickListener {
            if (cameraType == FaceAICameraType.SYSTEM_CAMERA) {
                startActivity(Intent(this, FaceSearchMNActivity::class.java))
            } else {
                Toast.makeText(this, "MN Search only support system camera", Toast.LENGTH_SHORT).show()
            }
        }

        binding.faceSearchAddFace.setOnClickListener {
            startActivity(Intent(this, FaceSearchDataMangerActivity::class.java).putExtra("isAdd", true))
        }

        binding.copyFaceImages.setOnClickListener {
            binding.copyFaceImages.visibility = View.INVISIBLE
            CopyFaceImageUtils.copyTestFaceImages(this, object : CopyFaceImageUtils.Callback {
                override fun onComplete(successCount: Int, failureCount: Int) {
                    Toast.makeText(this@FaceSearchNaviActivity, "Success：$successCount Failed:$failureCount", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.mangerFaceWithImage.setOnClickListener {
            startActivity(Intent(this, FaceSearchDataMangerActivity::class.java).putExtra("isAdd", false))
        }
    }

    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(this, "Camera Permission to add face image！", 11, *perms)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {}
}
