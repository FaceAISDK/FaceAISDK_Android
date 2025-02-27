package com.ai.face

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.ai.face.databinding.ActivityAboutFaceAppBinding

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
            val uri = Uri.parse("https://mp.weixin.qq.com/s/z3ZOvuZy2DeITZ7pZ1qV9Q")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }

        viewBinding.back.setOnClickListener {
            this.finish()
        }

        viewBinding.whatapp.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("WhatApp", "+8618707611416")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        viewBinding.wechat.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("wechat", "HaoNan19990322")
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
}