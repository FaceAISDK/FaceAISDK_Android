package com.faceAI.demo.base

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.faceAI.demo.R
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


/**
 * 相机权限管理
 *
 */
open class AbsBaseActivity : AppCompatActivity(), PermissionCallbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNeededPermission()
    }


    open fun hideSystemUI() {
        // 1. 隐藏标题栏 (ActionBar)
        // 如果你的 AndroidManifest.xml 主题已经是 Theme.NoActionBar，这步可以省略
        val actionBar = supportActionBar
        actionBar?.hide()

        // 2. 核心：处理刘海屏/挖孔屏 (API 28+)
        // 如果不加这段，横屏时刘海区域会有黑边
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.setAttributes(params)
        }

        // 3. 隐藏状态栏和导航栏
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11+ 新方式
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                // 设置为“滑动显示暂时状态栏”，几秒后自动隐藏
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下旧方式
            // 使用 IMMERSIVE_STICKY 模式，用户滑动边缘才显示，随后自动隐藏
            window.decorView.setSystemUiVisibility(
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            )
        }
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