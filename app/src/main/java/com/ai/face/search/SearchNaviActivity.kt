package com.ai.face.search

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.FaceApplication.Companion.CACHE_SEARCH_FACE_DIR
import com.ai.face.R
import com.ai.face.databinding.ActivityFaceSearchNaviBinding
import com.ai.face.faceSearch.search.FaceSearchImagesManger
import com.airbnb.lottie.LottieAnimationView
import com.lzf.easyfloat.EasyFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 演示导航Navi，目前支持千张图片1秒级搜索，后续聚焦降低App体积和精确度
 *
 *
 */
class SearchNaviActivity : AppCompatActivity(), PermissionCallbacks {

    private lateinit var binding: ActivityFaceSearchNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNeededPermission()

        //
        binding.faceSearch.setOnClickListener {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearchJavaActivity::class.java)
            )
        }


        //验证复制图片
        binding.copyFaceImages.setOnClickListener {

            binding.copyFaceImages.isClickable = false
            Toast.makeText(baseContext, "复制中...", Toast.LENGTH_LONG).show()
            showAppFloat(baseContext)
            CoroutineScope(Dispatchers.Main).launch {
                copyManyTestFaceImages(application)
                EasyFloat.dismiss("speed")
                //Dismiss Dialog
                Toast.makeText(baseContext, "已经复制导入验证图片", Toast.LENGTH_SHORT).show()
            }
        }



        binding.changeCamera.setOnClickListener {
            val sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE)

            if (sharedPref.getInt("cameraFlag", 0) == 1) {
                sharedPref.edit().putInt("cameraFlag", 0).apply()
                Toast.makeText(
                    baseContext,
                    "已切换前置摄像头",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sharedPref.edit().putInt("cameraFlag", 1).apply()
                Toast.makeText(
                    baseContext,
                    "已切换后置/外接摄像头",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        binding.editFaceImage.setOnClickListener {
            startActivity(Intent(baseContext, FaceImageEditActivity::class.java).putExtra("isAdd",false))
        }

        binding.addFaceImage.setOnClickListener {
            startActivity(Intent(baseContext, FaceImageEditActivity::class.java).putExtra("isAdd",true))
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
            EasyPermissions.requestPermissions(this, "请授权相机使用权限！", 11, *perms)
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
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {}


    companion object {


        fun showAppFloat(context: Context) {
            EasyFloat.with(context)
                .setTag("speed")
                .setGravity(Gravity.CENTER, 0, 0)
                .setDragEnable(false)
                .setLayout(R.layout.float_loading) {
                    val entry: LottieAnimationView = it.findViewById(R.id.entry)
                    entry.setAnimation(R.raw.waiting)
                    entry.loop(true)
                    entry.playAnimation()
                }
                .show()
        }


        /**
         * 拷贝Assert 目录下的图片到App 指定目录，所涉及的人脸全为AI生成不涉及
         *
         */
        suspend fun copyManyTestFaceImages(context: Application) = withContext(Dispatchers.IO) {
            val assetManager = context.assets
            val subFaceFiles = context.assets.list("")
            if (subFaceFiles != null) {
                for (index in subFaceFiles.indices) {
                    FaceSearchImagesManger.c.getInstance(context)?.insertOrUpdateFaceImage(
                        getBitmapFromAsset(
                            assetManager,
                            subFaceFiles[index]
                        ), CACHE_SEARCH_FACE_DIR + File.separatorChar + subFaceFiles[index]
                    )
                }
            }
        }

        private fun getBitmapFromAsset(assetManager: AssetManager, strName: String): Bitmap? {
            val istr: InputStream
            var bitmap: Bitmap?
            try {
                istr = assetManager.open(strName)
                bitmap = BitmapFactory.decodeStream(istr)
            } catch (e: IOException) {
                return null
            }
            return bitmap
        }

    }

}