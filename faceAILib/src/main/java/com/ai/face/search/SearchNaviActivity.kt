package com.ai.face.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.databinding.ActivityFaceSearchNaviBinding
import com.ai.face.search.CopyFaceImageUtils.Companion.copyTestFaceImage
import com.ai.face.search.CopyFaceImageUtils.Companion.showAppFloat
import com.lzf.easyfloat.EasyFloat
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

/**
 * 人脸识别搜索 演示导航Navi，目前支持千张图片1秒级搜索，后续聚焦降低App体积和精确度
 * 测试验证人脸库图片位于/assert 目录，更多的人脸图片请使用Ai 生成
 *
 */
class SearchNaviActivity : AppCompatActivity(), PermissionCallbacks {

    private lateinit var binding: ActivityFaceSearchNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNeededPermission()

        binding.faceSearch1n.setOnClickListener {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearch1NActivity::class.java)
            )
        }

        binding.faceSearchMn.setOnClickListener {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearchMNActivity::class.java)
            )
        }


        //验证复制图片
        binding.copyFaceImages.setOnClickListener {
            binding.copyFaceImages.isClickable = false
            Toast.makeText(baseContext, "Copying...", Toast.LENGTH_LONG).show()
            showAppFloat(baseContext)

            copyTestFaceImage(application, object : CopyFaceImageUtils.Companion.Callback {
                override fun onSuccess() {
                    EasyFloat.hide("speed")
                }
                override fun onFailed(msg: String) {}
            })
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.mN1.setOnClickListener {
            val uri =
                Uri.parse("https://github.com/AnyLifeZLB/FaceVerificationSDK/blob/main/Introduce_11_1N_MN.md")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }


        binding.changeCamera.setOnClickListener {
            val sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE)
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


        binding.editFaceImage.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java).putExtra(
                    "isAdd",
                    false
                )
            )
        }

        binding.addFaceImage.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java).putExtra(
                    "isAdd",
                    true
                )
            )
        }


    }

    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Camera Permission to add face image！",
                11,
                *perms
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    /**
     * 当用户点击了不再提醒的时候的处理方式
     */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {}


}