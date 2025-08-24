package com.faceAI.demo.base

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import com.faceAI.demo.R

/**
 * 方便根据Demo App 找到对应的代码
 *
 */
open class BaseActivity : AppCompatActivity() , PermissionCallbacks{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNeededPermission()

    }



    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
        //存储照片在某些目录需要,Manifest.permission.WRITE_EXTERNAL_STORAGE
        val perms = arrayOf(Manifest.permission.CAMERA)

        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.facesdk_camera_permission),
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

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {

    }


    /**
     * 当用户点击了不再提醒的时候的处理方式
     */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(
            this,
            R.string.facesdk_camera_permission,
            Toast.LENGTH_SHORT
        )
            .show()
    }
}