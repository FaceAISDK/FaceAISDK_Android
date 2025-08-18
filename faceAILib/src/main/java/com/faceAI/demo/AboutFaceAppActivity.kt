package com.faceAI.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.faceAI.demo.databinding.ActivityAboutFaceAppBinding


/**
 * 关于我们
 *
 */
class AboutFaceAppActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityAboutFaceAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAboutFaceAppBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.moreAboutMe.setOnClickListener {
            val uri = Uri.parse("https://mp.weixin.qq.com/s/_ro9zBfzAmkpazL-QAPi9w")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }

        viewBinding.back.setOnClickListener {
            this.finish()
        }

        viewBinding.newAppCheck.setOnClickListener {
            val uri = Uri.parse("https://www.pgyer.com/faceVerify")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }

        viewBinding.newAppCheck.setText("当前版本：${getVersionName(this)}  查看版本列表")

        viewBinding.systemInfo.setOnClickListener {
            printDeviceInfo()
        }

        viewBinding.wechat.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("wechat", "FaceAISDK")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        viewBinding.email.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Creates a new text clip to put on the clipboard
            val clip: ClipData = ClipData.newPlainText("email", "FaceAISDK.Service@gmail.com")

            // Set the clipboard's primary clip. 复制
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()

            return@setOnLongClickListener true
        }

    }


    public fun getVersionName(context: Context): String? {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
            return null
        }
    }


    /**
     * 设备系统信息
     *
     */
    private fun printDeviceInfo() {
        val deviceInfo = arrayOf(
            "制造商：${android.os.Build.MANUFACTURER}",
            "型号：${android.os.Build.MODEL}",
            "主板：${android.os.Build.BOARD}",
            "设备标识：${android.os.Build.FINGERPRINT}",
            "版本号：${android.os.Build.ID}",
            "Android SDK版本号：${android.os.Build.VERSION.SDK_INT}",
            "Android 版本（RELEASE）：${android.os.Build.VERSION.RELEASE}",
            "DISPLAY：${android.os.Build.DISPLAY}",
            "HARDWARE：${android.os.Build.HARDWARE}",
            "主机（HOST）：${android.os.Build.HOST}",
        )
        AlertDialog.Builder(this@AboutFaceAppActivity)
            .setItems(deviceInfo) { dialog, which ->
            }.show()
    }


}