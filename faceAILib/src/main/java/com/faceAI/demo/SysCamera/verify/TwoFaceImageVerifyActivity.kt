package com.faceAI.demo.SysCamera.verify

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
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.exifinterface.media.ExifInterface
import com.ai.face.faceSearch.search.Image2FaceFeature
import com.ai.face.faceVerify.verify.FaceVerifyUtils
import com.faceAI.demo.BuildConfig
import com.faceAI.demo.databinding.ActivityTwoFaceImageVerifyBinding
import java.io.InputStream


/**
 * 对比两张图片中人脸相似度，FaceVerifyUtils().evaluateFaceSimiByBitmap
 * 已优化：增加对三星等手机拍照旋转角度的自动矫正处理
 */
class TwoFaceImageVerifyActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityTwoFaceImageVerifyBinding
    private var mFileSelector: FileSelector? = null

    //保存2张选择照片中裁剪出的人脸图
    private var bitmapMap: HashMap<String, Bitmap> = HashMap(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityTwoFaceImageVerifyBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        FileOperator.init(application, BuildConfig.DEBUG)

        viewBinding.back.setOnClickListener { this@TwoFaceImageVerifyActivity.finish() }

        viewBinding.imageLeft.tag = "image1"
        viewBinding.imageLeft.setOnClickListener {
            chooseFile(viewBinding.imageLeft)
        }

        viewBinding.imageRight.tag = "image2"
        viewBinding.imageRight.setOnClickListener {
            chooseFile(viewBinding.imageRight)
        }

        viewBinding.goVerify.setOnClickListener {
            val simi = FaceVerifyUtils().evaluateFaceSimiByBitmap(
                baseContext,
                bitmapMap[viewBinding.imageLeft.tag],
                bitmapMap[viewBinding.imageRight.tag]
            )
            Toast.makeText(baseContext, "人脸相似度：$simi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_FACE_IMAGE) {
                mFileSelector?.obtainResult(requestCode, resultCode, data)
            }
        }
    }

    private fun chooseFile(view: TextView) {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
            singleFileMaxSize = 9242880
            allFilesMaxSize = 9242880
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
            .setExtraMimeTypes("image/*")
            .applyOptions(optionsImage)
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    if (!results.isNullOrEmpty()) {
                        disposeSelectResult(results, view)
                    }
                }

                override fun onError(e: Throwable?) {}
            })
            .choose()
    }

    /**
     * 裁剪并处理旋转角度
     */
    fun disposeSelectResult(results: List<FileSelectResult>, view: TextView) {
        results.firstOrNull()?.uri?.let { selectUri -> // 只有 uri 不为空才进入
            val faceName = FileUtils.getFileNameFromUri(selectUri) ?: "faceID"
            // 1. 获取原始 Bitmap
            val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectUri)

            // 2. 获取旋转角度并校正
            val degree = getExifRotationDegree(selectUri)
            val bitmapSelected = if (degree != 0) rotateBitmap(originalBitmap, degree) else originalBitmap

            view.text = faceName
            view.background = bitmapSelected.toDrawable(resources)

            // 提取特征值
            Image2FaceFeature.getInstance(this).getFaceFeatureByBitmap(bitmapSelected, faceName, object : Image2FaceFeature.Callback {
                override fun onSuccess(bitmap: Bitmap, faceID: String, faceFeature: String) {
                    bitmapMap[view.tag.toString()] = bitmap
                    view.background = bitmap.toDrawable(resources)
                }

                override fun onFailed(msg: String) {
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    bitmapMap.remove(view.tag.toString())
                }
            })
        }
    }

    /**
     * 读取图片的 EXIF 旋转信息
     */
    private fun getExifRotationDegree(uri: Uri): Int {
        var degree = 0
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            inputStream?.let {
                val exifInterface = ExifInterface(it)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                degree = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return degree
    }

    /**
     * 旋转 Bitmap 图像
     */
    private fun rotateBitmap(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        // 回收旧的 Bitmap 以释放内存
        if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    companion object {
        const val REQUEST_ADD_FACE_IMAGE = 1
    }
}