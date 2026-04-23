package com.faceAI.demo.base

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.faceAI.demo.R
import com.faceAI.demo.base.view.FaceCoverView
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import kotlin.math.min


/**
 * AbsBaseActivity for Face SDK API Demo
 *
 */
open class AbsBaseActivity : AppCompatActivity(), PermissionCallbacks {
    private var shouldHideSystemUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNeededPermission()

        adjustCameraMargin()
    }


    /**
     * 适配横竖屏，动态调整相机预览区域的边距为屏幕短边的 1/MARGIN_SIZE，注意和FaceCoverView内部设置保持一致
     */
     fun adjustCameraMargin() {
        val fragmentCamera = findViewById<View?>(R.id.fragment_camerax) // id = fragment_camerax
        if (fragmentCamera != null && fragmentCamera.layoutParams is MarginLayoutParams) {
            val width = getResources().displayMetrics.widthPixels
            val height = getResources().displayMetrics.heightPixels
            val shorterSide = min(width, height)
            val margin = shorterSide / FaceCoverView.MARGIN_SIZE

            val lp = fragmentCamera.layoutParams as MarginLayoutParams
            lp.setMargins(margin, margin, margin, margin)
            fragmentCamera.setLayoutParams(lp)
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldHideSystemUI) {
            applyImmersiveMode()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldHideSystemUI) {
            applyImmersiveMode()
        }
    }

    /**
     * full screen,hideSystemUI
     */
    open fun hideSystemUI() {
        shouldHideSystemUI = true
        applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        val actionBar = supportActionBar
        actionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.setAttributes(params)
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
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