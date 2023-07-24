package com.ai.face.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import com.ai.face.FaceApplication
import com.ai.face.R
import com.ai.face.base.view.CameraXFragment
import com.ai.face.databinding.ActivityFaceSearchBinding
import com.ai.face.faceSearch.search.FaceSearchEngine
import com.ai.face.faceSearch.search.SearchProcessBuilder
import com.ai.face.faceSearch.search.SearchProcessCallBack
import com.ai.face.faceSearch.search.SearchProcessTipsCode
import com.ai.face.search.FaceImageEditActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.io.File

/**
 * Kotlin Test
 *
 */
class FaceSearchKTActivity : AppCompatActivity() {

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
                FaceSearchEngine.Companion().instance.runSearch(imageProxy, 0)
            }
        }


        // 2.各种参数的初始化设置
        val faceProcessBuilder = SearchProcessBuilder.Builder(application)
            .setLifecycleOwner(this)
            .setThreshold(0.82f) //识别成功阈值设置，范围仅限 [0.8 , 0.9] 建议0.8+
            .setLicenceKey("yourLicense key") //合作的VIP定制客户群体需要
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
            SearchProcessTipsCode.THRESHOLD_ERROR -> binding.searchTips.text =
                "识别阈值Threshold范围为0.8-0.9"

            SearchProcessTipsCode.MASK_DETECTION -> binding.searchTips.text = "请摘下口罩" //默认无
            SearchProcessTipsCode.NO_LIVE_FACE -> binding.searchTips.text = "未检测到人脸"
            SearchProcessTipsCode.EMGINE_INITING -> binding.searchTips.text = "初始化中"
            SearchProcessTipsCode.FACE_DIR_EMPTY -> binding.searchTips.text = "人脸库为空"
            SearchProcessTipsCode.NO_MATCHED -> {
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.searchTips.text = "暂无匹配人脸"
            }

            SearchProcessTipsCode.SEARCHING -> {
                binding.searchTips.text = "搜索中"
            }

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