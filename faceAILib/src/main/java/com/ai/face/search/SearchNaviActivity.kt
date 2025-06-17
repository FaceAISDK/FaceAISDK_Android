package com.ai.face.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.FaceAISettingsActivity
import com.ai.face.R
import com.ai.face.UVCCamera.addFace.AddFaceUVCCameraActivity
import com.ai.face.UVCCamera.addFace.AddFaceUVCCameraFragment
import com.ai.face.addFaceImage.AddFaceImageActivity
import com.ai.face.databinding.ActivityFaceSearchNaviBinding
import com.ai.face.search.CopyFaceImageUtils.Companion.copyTestFaceImage
import com.ai.face.search.CopyFaceImageUtils.Companion.showAppFloat
import com.ai.face.search.uvcCameraSearch.FaceSearchUVCCameraActivity
import com.ai.face.verify.FaceVerifyWelcomeActivity
import com.lzf.easyfloat.EasyFloat
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

/**
 * 人脸识别搜索 演示导航Navi，目前支持千张图片秒级搜索
 * 测试验证人脸库图片位于/assert 目录，更多的人脸图片请使用Ai 生成
 *
 * 使用的宽动态（人脸搜索必须大于110DB）高清抗逆光摄像头；保持镜头干净（用纯棉布擦拭油污）
 *
 */
class SearchNaviActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var binding: ActivityFaceSearchNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkNeededPermission()
        binding.back.setOnClickListener {
            this@SearchNaviActivity.finish()
        }

        binding.systemCameraSearch.setOnClickListener {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearch1NActivity::class.java)
            )
        }

        binding.systemCameraAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java).putExtra(
                    "isAdd",
                    true
                ).putExtra("isBinocularCamera", false)
            )
        }

        binding.binocularCameraSearch.setOnClickListener {
            showConnectUVCCameraDialog()
        }

        binding.binocularCameraAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java)
                    .putExtra("isAdd", true)
                    .putExtra("isBinocularCamera", true)
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


        binding.editFaceImage.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java).putExtra(
                    "isAdd",
                    false
                )
            )
        }

    }


    /**
     * 提示
     *
     */
    private fun showConnectUVCCameraDialog() {
        //一天提示一次
        val sharedPref = getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE)
        val showTime = sharedPref.getLong("showUVCCameraDialog", 0)
        if (System.currentTimeMillis() - showTime < 12 * 60 * 60 * 1000) {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearchUVCCameraActivity::class.java)
            )
        } else {
            val builder = AlertDialog.Builder(this)
            val dialog = builder.create()
            val dialogView = View.inflate(this, R.layout.dialog_connect_uvc_camera, null)
            //设置对话框布局
            dialog.setView(dialogView)
            val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)
            btnOK.setOnClickListener {
                startActivity(
                    Intent(this@SearchNaviActivity, FaceSearchUVCCameraActivity::class.java)
                )
                dialog.dismiss()
            }
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            sharedPref.edit().putLong("showUVCCameraDialog", System.currentTimeMillis()).commit()
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
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {

    }


}