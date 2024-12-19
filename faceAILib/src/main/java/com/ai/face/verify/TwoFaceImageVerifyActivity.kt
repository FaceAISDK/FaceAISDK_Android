package com.ai.face.verify

import ando.file.core.FileOperator
import ando.file.core.FileUtils
import ando.file.selector.FileSelectCallBack
import ando.file.selector.FileSelectCondition
import ando.file.selector.FileSelectOptions
import ando.file.selector.FileSelectResult
import ando.file.selector.FileSelector
import ando.file.selector.FileType
import ando.file.selector.IFileType
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.BuildConfig
import com.ai.face.base.baseImage.FaceAIUtils
import com.ai.face.databinding.ActivityTwoFaceImageVerifyBinding
import com.ai.face.faceVerify.verify.VerifyUtils
import com.ai.face.utils.fileUtils.MyFileUtils


/**
 * 两张图片中人脸相似度
 *
 * 裁剪出图片中的人脸部分进行相似度比较，如果某一张照片中没有检测到人脸，则相似度返回为0。
 *
 * 不适合用本方法来大规模的并发进行人脸照片相似度比较，因为bitmap 的操作以及提取向量值很耗费资源
 *
 * TwoFaceImageVerifyActivity 采用Kotlin 演示，使用java 的同学请自行翻译，有兴趣的同学可以重命名后提交新的PR
 */
class TwoFaceImageVerifyActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityTwoFaceImageVerifyBinding
    private var mFileSelector: FileSelector? = null

    //保存2张选择照片中裁剪出的人脸图
    private  var bitmapMap: HashMap<String, Bitmap> = HashMap(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityTwoFaceImageVerifyBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        FileOperator.init(application, BuildConfig.DEBUG)

        viewBinding.back.setOnClickListener { this@TwoFaceImageVerifyActivity.finish() }


        viewBinding.image1.tag="image1"
        viewBinding.image1.setOnClickListener {
            chooseFile(viewBinding.image1)
        }


        viewBinding.image2.tag="image2"
        viewBinding.image2.setOnClickListener {
            chooseFile(viewBinding.image2)
        }


        viewBinding.goVerify.setOnClickListener {
            // 不能两张图直接比较，要先经过 checkFaceQuality 检测裁剪图片中的人脸
            // FaceAIUtils.Companion.getInstance(application).checkFaceQuality(
           val simi=VerifyUtils.evaluateFaceSimi(
                baseContext,
                bitmapMap[viewBinding.image1.tag],
                bitmapMap[viewBinding.image2.tag]
            ) //evaluateFaceSimi传人的两个bitmap 有是空的，则相似度直接返回0

            Toast.makeText(baseContext, "人脸相似度：$simi",Toast.LENGTH_SHORT).show()
        }

    }


    /**
     * 处理照片选择，详情参考三方库 https://github.com/javakam/FileOperator
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_FACE_IMAGE) {
                //处理多项选择后的处理
                mFileSelector?.obtainResult(requestCode, resultCode, data)
            }
        }
    }


    /**
     * 选照片，大图在低端设备建议配置压缩策略
     * 使用详情参考 https://github.com/javakam/FileOperator
     *
     */
    private fun chooseFile(view: TextView) {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "A single picture does not exceed 5M !"
            allFilesMaxSize = 9242880
            allFilesMaxSizeTip = "The total size of the picture does not exceed 10M !"
            minCount = 1
            maxCount = 1

            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null
                            && !uri.path.isNullOrBlank()
                            && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_ADD_FACE_IMAGE)
            .setMinCount(1, "Choose at least one picture!")
            .setSingleFileMaxSize(3145728, "The size of a single picture cannot exceed 3M !")
            .setExtraMimeTypes("image/*")
            .applyOptions(optionsImage)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE) && (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(
                        uri
                    ))
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    if (results.isNullOrEmpty()) {
                        return
                    }

                    if (results.isNotEmpty()){
                        disposeSelectResult(results,view)
                    }
                }

                override fun onError(e: Throwable?) {

                }
            })
            .choose()
    }

    /**
     * 裁剪出照片中的人脸储存到bitmapMap
     */
    fun disposeSelectResult(results: List<FileSelectResult>,view: TextView) {
        val fileName = MyFileUtils.getFileMetaData(
            baseContext, results[0].uri
        ).displayName

        view.text = fileName

        val bitmap=MediaStore.Images.Media.getBitmap(
            baseContext.contentResolver,
            results[0].uri
        )

        view.background = BitmapDrawable(resources, bitmap)

        FaceAIUtils.Companion.getInstance(application).checkFaceQuality(bitmap,object : FaceAIUtils.Callback{
            override fun onSuccess(bitmap: Bitmap) {
                bitmapMap[view.tag.toString()] = bitmap
            }

            override fun onFailed(msg: String, errorCode: Int) {
                Toast.makeText(baseContext,msg,Toast.LENGTH_LONG).show()
                bitmapMap.remove(view.tag.toString()) //没有检测出人脸则移除上一次可能有的数据
            }
        })
    }


    companion object {
        const val REQUEST_ADD_FACE_IMAGE = 1
    }

}