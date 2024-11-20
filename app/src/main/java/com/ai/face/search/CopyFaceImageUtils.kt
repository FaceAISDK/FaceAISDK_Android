package com.ai.face.search

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import com.ai.face.FaceAIApplication.CACHE_SEARCH_FACE_DIR
import com.ai.face.R
import com.ai.face.faceSearch.search.FaceSearchImagesManger
import com.airbnb.lottie.LottieAnimationView
import com.lzf.easyfloat.EasyFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * 拷贝200 百张工程目录Assert下的人脸测试图
 *
 * 人脸图要求：
 * 1.尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * 2.录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * 3.光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
 * 4.人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
 *
 *
 * 封装Utils供Java 代码调用。使用Kotlin 协程能极大的简化代码结构
 */
class CopyFaceImageUtils {

    companion object {

        interface Callback {
            fun onSuccess()
            fun onFailed(msg:String)
        }

        /**
         * 快速复制工程目录 ./app/src/main/assert目录下200+张 人脸图入库
         * 人脸图规范要求 大于 300*300的光线充足无遮挡的正面人脸如（./images/face_example.jpg)
         *
         * @param context
         * @param callBack
         */
        fun copyTestFaceImage(context: Application, callBack: Callback){
            CoroutineScope(Dispatchers.IO).launch {
                copyAssertTestFaceImages(context)
                delay(800)
                launch(Dispatchers.Main) {
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
        private suspend fun copyAssertTestFaceImages(context: Application) = withContext(Dispatchers.IO) {
            val assetManager = context.assets
            val subFaceFiles = context.assets.list("")
            if (subFaceFiles != null) {
                for (index in subFaceFiles.indices) {
                    val originBitmap=getBitmapFromAsset(
                        assetManager,
                        subFaceFiles[index]
                    )

                    if(originBitmap!=null){

                        //不需再要先剪裁一次人脸，insertOrUpdateFaceImage里面会检测裁剪人脸

                        val fileName=CACHE_SEARCH_FACE_DIR + subFaceFiles[index]

                        /**
                         *  人脸图要求：
                         *  1.尽量使用较高配置设备和摄像头，光线不好带上补光灯
                         *  2.录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
                         *  3.光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
                         *  4.人脸照片要求300*300 裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
                         */
                        FaceSearchImagesManger.IL1Iii.getInstance(context).insertOrUpdateFaceImage(
                            originBitmap, fileName,object :FaceSearchImagesManger.Callback {
                                override fun onSuccess() {
                                    Log.d("Add Face","Add Face successful"+subFaceFiles[index]);
                                }

                                override fun onFailed(msg: String) {
                                    Log.d("Add Face","Add Face onFailed"+subFaceFiles[index]);
                                }

                            }
                        )
                    }else{
                        Log.e("Add Face","获取Assert 目录文件图片失败 : "+subFaceFiles[index]);
                    }

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