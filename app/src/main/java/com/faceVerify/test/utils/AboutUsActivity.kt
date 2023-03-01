package com.faceVerify.test.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.faceVerify.test.R
import kotlinx.android.synthetic.main.activity_about_us.*


class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)


        more_about_me.setOnClickListener {
            val uri = Uri.parse("https://github.com/AnyLifeZLB/FaceVerificationSDK")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = uri
            startActivity(intent)
        }


        back.setOnClickListener{
            this.finish()
        }


        wechat.setOnLongClickListener{
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Creates a new text clip to put on the clipboard
            val clip: ClipData = ClipData.newPlainText("wechat", "HaoNan19990322")

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this,"已经复制",Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }


        email.setOnLongClickListener{
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Creates a new text clip to put on the clipboard
            val clip: ClipData = ClipData.newPlainText("email", "anylife.zlb@gmail.com")

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this,"已经复制",Toast.LENGTH_SHORT).show()

            return@setOnLongClickListener true
        }


    }
}