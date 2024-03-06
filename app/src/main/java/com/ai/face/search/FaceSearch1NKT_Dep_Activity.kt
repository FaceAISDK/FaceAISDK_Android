package com.ai.face.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import com.ai.face.FaceApplication
import com.ai.face.R
import com.ai.face.base.view.CameraXFragment
import com.ai.face.databinding.ActivityFaceSearchBinding
import com.ai.face.faceSearch.search.FaceSearchEngine
import com.ai.face.faceSearch.search.SearchProcessBuilder
import com.ai.face.faceSearch.search.SearchProcessCallBack
import com.ai.face.faceSearch.search.SearchProcessTipsCode
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.io.File

/**
 * Kotlin Test
 * 1：N 和 M：N 人脸检索迁移到了 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 *
 * @DeprecatedSinceKotlin
 *
 *
 */
class FaceSearch1NKT_Dep_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceSearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tips.setOnClickListener { v: View? ->
            startActivity(Intent(this, FaceImageEditActivity::class.java))
        }

        val sharedPref = getSharedPreferences("faceVerify", MODE_PRIVATE)
        // 1. Camera 的初始化。第一个参数0/1 指定前后摄像头； 第二个参数linearZoom [0.1f,1.0f] 指定焦距，默认0.1
        val cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0))
        val cameraXFragment = CameraXFragment.newInstance(cameraLens, 0.11f)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
            .commit()
        cameraXFragment.setOnAnalyzerListener { imageProxy: ImageProxy ->
            //可以加个红外检测之类的，有人靠近再启动检索服务，不然机器老化快
            if (!isDestroyed && !isFinishing) {
                //runSearch() 方法第二个参数是指圆形人脸框到屏幕边距，有助于加快裁剪图像
                FaceSearchEngine.Companion().instance.runSearch(imageProxy, 10)
            }
        }

        // 2.各种参数的初始化设置，（硬件加速等仅VIP用户）
        val faceProcessBuilder = SearchProcessBuilder.Builder(this)
            .setLifecycleOwner(this)
            .setThreshold(0.79f) //阈值设置，范围限 [0.75 , 0.95] 识别可信度，也是识别灵敏度
            .setLicenceKey("yourLicense key") //合作的VIP定制客户群体需要
            .setImageFlipped(cameraLens == CameraSelector.LENS_FACING_FRONT) //手机的前置摄像头imageProxy 拿到的图可能左右翻转
            .setFaceLibFolder(FaceApplication.CACHE_SEARCH_FACE_DIR) //内部存储目录中保存N 个图片库的目录
            .setProcessCallBack(object : SearchProcessCallBack() {
                override fun onMostSimilar(similar: String) {
                    binding.searchTips.text = similar
                    Glide.with(baseContext)
                        .load(FaceApplication.CACHE_SEARCH_FACE_DIR + File.separatorChar + similar)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .transform(RoundedCorners(11))
                        .into(binding.image)
                }

                override fun onProcessTips(i: Int) {
                    showPrecessTips(i)
                }

                override fun onLog(log: String) {
                    binding.tips.text = log
                }
            }).create()


        //3.初始化引擎，是个耗时耗资源操作
        FaceSearchEngine.Companion().instance.initSearchParams(faceProcessBuilder)
    }

    /**
     * 显示提示
     *
     * @param code
     */
    private fun showPrecessTips(code: Int) {
        binding.image.setImageResource(R.mipmap.ic_launcher)
        when (code) {
            SearchProcessTipsCode.SEARCHING -> binding.searchTips.text = ""
            SearchProcessTipsCode.NO_MATCHED -> binding.searchTips.text = "Searching"
            SearchProcessTipsCode.MASK_DETECTION -> binding.searchTips.text = "请摘下口罩" //默认无
            SearchProcessTipsCode.NO_LIVE_FACE -> binding.searchTips.text = "未检测到人脸"
            SearchProcessTipsCode.EMGINE_INITING -> binding.searchTips.text = "初始化中"
            SearchProcessTipsCode.FACE_DIR_EMPTY -> binding.searchTips.text = "人脸库为空"
            SearchProcessTipsCode.THRESHOLD_ERROR -> binding.searchTips.text = "识别阈值范围为0.8-0.95"
            else -> binding.searchTips.text = "提示码：$code"
        }
    }

    /**
     * 销毁，停止
     */
    override fun onDestroy() {
        super.onDestroy()
        FaceSearchEngine.Companion().instance.stopSearchProcess()
    }

}