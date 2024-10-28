package com.ai.face.search

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import com.ai.face.MyFaceApplication.CACHE_SEARCH_FACE_DIR
import com.ai.face.R
import com.ai.face.faceSearch.search.FaceSearchImagesManger
import com.airbnb.lottie.LottieAnimationView
import com.lzf.easyfloat.EasyFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 拷贝200 百张工程目录Assert下的人脸测试图（AI生成）
 *
 * 封装Utils供Java 代码调用。使用Kotlin 协程能极大的简化代码结构
 */
class CopyFaceImageUtils {

    companion object {

        interface Callback {
            fun onSuccess()
            fun onFailed(msg: String)
        }

        /**
         * 提供一个包含CallBack 的方法给Java 调用，java没有协程序=
         *
         * @param context
         */
        fun copyTestImage(context: Application, callBack: Callback) {
            CoroutineScope(Dispatchers.IO).launch {
                copyAssertTestFaceImages(context)
                delay(800)
                MainScope().launch {
                    callBack.onSuccess()
                }
            }
        }


        /**
         * 等待动画
         */
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
         * 拷贝Assert 目录下的图片到App 指定目录，所涉及的人脸全为AI生成
         *
         */
        suspend fun copyAssertTestFaceImages(context: Application) = withContext(Dispatchers.IO) {
            val assetManager = context.assets
            val subFaceFiles = context.assets.list("")
            if (subFaceFiles != null) {
                for (index in subFaceFiles.indices) {
                    FaceSearchImagesManger.IL1Iii.getInstance(context).insertOrUpdateFaceImage(
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