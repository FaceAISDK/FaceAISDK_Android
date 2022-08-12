package com.faceVerify.test

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import android.os.Bundle
import com.AI.FaceVerify.utils.AiUtil
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.faceVerify.test.FaceApplication.Companion.BASE_FACE_KEY
import com.faceVerify.test.FaceApplication.Companion.CACHE_BASE_FACE_DIR
import kotlinx.android.synthetic.main.activity_navi.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.File

/**
 * 演示导航 Navi
 *
 * Compile SDK 准备升级到32
 *
 *
 * V1.0.0 Alpha 01 仅供学习交流，未经授权严禁用于商业行为
 * 2022.07.30 SZ
 */
class NaviActivity : AppCompatActivity(), PermissionCallbacks {

    private var yourUniQueFaceId="18707611416" //wechat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)
        checkNeededPermission()

        face_verify_card.setOnClickListener {
            //可以自己录一张人脸底片，业务方可以根据自己的要求改写testBaseImgName 处理
            val file = File(CACHE_BASE_FACE_DIR , yourUniQueFaceId)
            if (AiUtil.compressPath(this@NaviActivity, Uri.fromFile(file)) != null) {
                startActivity(Intent(this@NaviActivity, VerifyActivity::class.java)
                    .putExtra(BASE_FACE_KEY, yourUniQueFaceId))
            } else {
                Toast.makeText(this@NaviActivity, "请先录入人脸底片", Toast.LENGTH_SHORT).show()
            }
        }

        more_about_me.setOnClickListener {
            val uri = Uri.parse("https://github.com/AnyLifeZLB/FaceVerificationSDK")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }

        update_base_image.setOnClickListener {
            startActivity(
                Intent(this@NaviActivity, UpdateBaseFaceActivity::class.java)
                    .putExtra(BASE_FACE_KEY, yourUniQueFaceId)
            )
        }
    }

    /**
     * 统一全局的拦截权限请求，给提示
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *perms)) {
        } else {
            EasyPermissions.requestPermissions(this, "", 11, *perms)
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
        Toast.makeText(this, "不授权无法使用App啊", Toast.LENGTH_SHORT).show()
    }

    fun onUpload(view: View?) {
        Toast.makeText(this, "分析优化中...", Toast.LENGTH_SHORT).show()
    }
}