package com.ai.face.search

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR
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
 * 拷贝200张工程目录Assert下的人脸测试图方便你验证效果
 *
 * warming：不要直接使用File 操作保存人脸，要用SDK API 处理人脸图入库
 *
 * 人脸图要求：
 * 1.尽量使用较高配置设备和摄像头，光线不好带上补光灯
 * 2.录入高质量的人脸图，人脸清晰，背景简单（证件照输入目前优化中）
 * 3.光线环境好，检测的人脸无遮挡，化浓妆或佩戴墨镜口罩帽子等
 * 4.人脸照片要求300*300（人脸部分区域大于200*200）裁剪好的仅含人脸的正方形照片，背景纯色，否则要后期处理
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
         * 人脸图规范要求 大于 300*300（人脸部分区域大于200*200）的光线充足无遮挡的正面人脸如（./images/face_example.jpg)
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
                    entry.setAnimation(R.raw.loading2)
                    entry.loop(true)
                    entry.playAnimation()
                }
                .show()
        }


        /**
         * 把工程目录Assert 下的图片插入到App 指定人脸搜索库
         * 你也可以参考把服务器网络图片下载后插入到人脸搜索库
         */
        private suspend fun copyAssertTestFaceImages(context: Application) = withContext(Dispatchers.IO) {
            val asset = context.assets
            val faceFiles = context.assets.list("")
            if (faceFiles != null) {
                for (index in faceFiles.indices) {
                    //网络图下载后转为Bitmap 一样的处理方式
                    val originBitmap=getBitmapFromAsset(asset, faceFiles[index])
                    if(originBitmap!=null){
                        //本地库保存的路径
                        val fileName=CACHE_SEARCH_FACE_DIR + faceFiles[index]

                        //insertOrUpdateFaceImage 处理人脸图入库，里面会检测裁剪人脸，图像量化处理；
                        // 插入失败请看onFailed log
                        FaceSearchImagesManger.Companion.getInstance(context).insertOrUpdateFaceImage(
                            originBitmap, fileName,object :FaceSearchImagesManger.Callback {
                                override fun onSuccess() {
                                    Log.d("Add Face","Add Face successful：  "+faceFiles[index]);
                                }
                                override fun onFailed(msg: String) {
                                    Log.e("Add Face","Add Face onFailed：  "+faceFiles[index]);
                                }

                            }
                        )
                    }else{
                        Log.e("Add Face","获取Assert 目录文件图片失败 : "+faceFiles[index]);
                    }

                }
            }
        }


        /**
         * 读取Assert 目录的测试验证人脸图
         */
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