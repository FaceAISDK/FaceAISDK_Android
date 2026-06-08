package com.faceAI.demo.SysCamera.verify

import com.ai.face.faceSearch.search.Image2FaceFeature
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
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.faceAI.demo.BuildConfig
import com.faceAI.demo.R
import java.io.InputStream

/**
 * 从相册选人脸图,提取特征值（并没有对人脸角度等校验）。
 * 强烈建议通过FaceAISDK 添加人脸
 *
 * @author FaceAISDK.Service@gmail.com
 */
abstract class AbsAddFaceFromAlbumActivity : AppCompatActivity() {
    private var mFileSelector: FileSelector? = null

    // 从相册选择
    abstract fun disposeSelectImage(faceID: String, disposedBitmap: Bitmap, faceFeature: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileOperator.init(application, BuildConfig.DEBUG)
    }

    /**
     * 处理照片选择，详情参考三方库 https://github.com/javakam/FileOperator
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

    /*
     * 从相册选人脸图,提取特征值（并没有对人脸角度等校验）。
     * 强烈建议通过FaceAISDK 添加人脸
    */
    public fun chooseFaceImage() {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
            singleFileMaxSize = 9242880
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
            .setSingleFileMaxSize(9145728, "The size of a single picture cannot exceed 3M !")
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
                    results?.firstOrNull()?.uri?.let { selectUri -> // 只有 uri 不为空才进入
                        val faceName = FileUtils.getFileNameFromUri(selectUri) ?: "faceID"

                        // 1. 获取原始 Bitmap
                        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectUri)

                        // 2. 获取图片的 EXIF 旋转角度 (解决三星等手机拍照旋转问题)
                        val rotationDegree = getExifRotationDegree(selectUri)

                        // 3. 根据角度修正 Bitmap
                        val bitmapSelected = if (rotationDegree != 0) {
                            rotateBitmap(originalBitmap, rotationDegree)
                        } else {
                            originalBitmap
                        }

                        //非FaceAI SDK的人脸可能是不规范的没有经过校准的人脸图需要使用异步方法处理
                        Image2FaceFeature.getInstance(application).getFaceFeatureByBitmap(bitmapSelected, faceName, object : Image2FaceFeature.Callback {
                            override fun onFailed(msg: String) {
                                Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                            }

                            override fun onSuccess(
                                bitmap: Bitmap,
                                faceID: String,
                                faceFeature: String
                            ) {
                                showConfirmDialog(bitmap, faceID, faceFeature)
                            }
                        })
                    }
                }

                override fun onError(e: Throwable?) {

                }
            })
            .choose()
    }


    /**
     * 确认是否保存人脸底图
     */
    private fun showConfirmDialog(bitmap: Bitmap, faceID: String, faceFeature: String) {
        val builder = AlertDialog.Builder(this)
        val dialog = builder.create()
        val dialogView = View.inflate(this, R.layout.dialog_confirm_add_face, null)

        //设置对话框布局
        dialog.setView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        val basePreView = dialogView.findViewById<ImageView>(R.id.preview)
        basePreView.setImageBitmap(bitmap)

        val btnOK = dialogView.findViewById<Button>(R.id.share_face_feature)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)
        editText.requestFocus()
        editText.hint = faceID //
        editText.visibility = View.VISIBLE

        btnOK.setOnClickListener { v: View? ->
            val finalFaceID = editText.text.toString()
            if (!TextUtils.isEmpty(finalFaceID)) {
                disposeSelectImage(finalFaceID, bitmap, faceFeature)
                dialog.dismiss()
            } else {
                Toast.makeText(baseContext, "Input FaceID Name", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { v: View? ->
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    /**
     * 根据 Uri 读取图片的 EXIF 信息，获取旋转角度
     */
    private fun getExifRotationDegree(uri: Uri): Int {
        var degree = 0
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exifInterface = ExifInterface(inputStream)
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
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return degree
    }

    /**
     * 根据角度旋转 Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        // 释放原始 bitmap 内存
        if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    companion object {
        const val REQUEST_ADD_FACE_IMAGE = 1882
    }

}