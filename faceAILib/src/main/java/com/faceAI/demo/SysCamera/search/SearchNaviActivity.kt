package com.faceAI.demo.SysCamera.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.faceAI.demo.SysCamera.search.CopyFaceImageUtils.Companion.copyTestFaceImage
import com.faceAI.demo.SysCamera.search.CopyFaceImageUtils.Companion.showAppFloat
import com.faceAI.demo.UVCCamera.search.FaceSearch_UVCCameraActivity
import com.lzf.easyfloat.EasyFloat
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import androidx.core.content.edit
import com.faceAI.demo.FaceAISettingsActivity.Companion.FRONT_BACK_CAMERA_FLAG
import com.faceAI.demo.R
import com.faceAI.demo.databinding.ActivityFaceSearchNaviBinding

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
            startActivity(Intent(baseContext, FaceSearch1NActivity::class.java))
        }

        binding.systemCameraSearchMn.setOnClickListener {
            startActivity(Intent(baseContext, FaceSearchMNActivity::class.java))
        }

        binding.systemCameraAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java)
                    .putExtra("isAdd", true)
                    .putExtra("isBinocularCamera", false))
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
            if (sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 1) == 1) {
                sharedPref.edit(commit = true) { putInt( FRONT_BACK_CAMERA_FLAG, 0) }
                Toast.makeText(
                    baseContext,
                    "Front camera now",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sharedPref.edit(commit = true) { putInt( FRONT_BACK_CAMERA_FLAG, 1) }
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
                Intent(this@SearchNaviActivity, FaceSearch_UVCCameraActivity::class.java)
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
                    Intent(this@SearchNaviActivity, FaceSearch_UVCCameraActivity::class.java)
                )
                dialog.dismiss()
            }
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            sharedPref.edit(commit = true) {
                putLong(
                    "showUVCCameraDialog",
                    System.currentTimeMillis()
                )
            }
        }
    }


    /**
     * SDK接入方 自行处理权限管理
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {

        } else {
            EasyPermissions.requestPermissions(this, "Camera Permission to add face image！", 11, *perms)
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