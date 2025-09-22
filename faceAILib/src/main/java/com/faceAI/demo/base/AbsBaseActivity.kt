package com.faceAI.demo.base

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.faceAI.demo.R
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


/**
 * 相机权限管理
 *
 */
open class AbsBaseActivity : AppCompatActivity(), PermissionCallbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        //this.enableEdgeToEdge() //启用"Edge-to-Edge"（从边缘到边缘）的用户界面模式

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
                this, getString(R.string.facesdk_camera_permission), 11, *perms
            )
        }
    }

    /**
     * this.enableEdgeToEdge() //启用"Edge-to-Edge"（从边缘到边缘）的用户界面模式
     *
     */
    open fun applyWindowInsets(){
        try {
            ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main)
            ) { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }catch (e: Exception){
            Log.e("setOnApplyWindow",e.message.toString());
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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

        AlertDialog.Builder(this)
            .setMessage(R.string.facesdk_camera_permission)
            .setPositiveButton(R.string.go_setting, { dialogInterface, i ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            })
            .show()

    }



}