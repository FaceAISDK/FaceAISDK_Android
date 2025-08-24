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
import com.ai.face.base.baseImage.FaceAIUtils
import com.faceAI.demo.BuildConfig
import com.faceAI.demo.R


/**
 * 从相册选人脸图
 *
 * @author FaceAISDK.Service@gmail.com
 */
abstract class AbsAddFaceFromAlbumActivity : AppCompatActivity() {
    private var mFileSelector: FileSelector? = null

    // 从相册选择
    abstract fun disposeSelectImage(faceID:String,disposedBitmap: Bitmap, faceEmbedding: FloatArray)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileOperator.init(application, BuildConfig.DEBUG)
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
                    if (!results.isNullOrEmpty()) {
                        val bitmapSelected = MediaStore.Images.Media.getBitmap(contentResolver, results[0].uri)


                        //非FaceAI SDK的人脸可能是不规范的没有经过校准的人脸图（证件照，多人脸，过小等）
                        FaceAIUtils.Companion.getInstance(application)
                            .disposeBaseFaceImage(baseContext, bitmapSelected, object : FaceAIUtils.Callback {
                                    override fun onSuccess(bitmap: Bitmap, faceEmbedding: FloatArray) {
                                        showConfirmDialog(bitmap,faceEmbedding)
                                    }

                                    //底片处理异常的信息回调
                                    override fun onFailed(msg: String, errorCode: Int) {
                                        Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
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
    private fun showConfirmDialog(bitmap: Bitmap,faceEmbedding: FloatArray) {
        val builder = AlertDialog.Builder(this)
        val dialog = builder.create()
        val dialogView = View.inflate(this, R.layout.dialog_confirm_base, null)

        //设置对话框布局
        dialog.setView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        val basePreView = dialogView.findViewById<ImageView>(R.id.preview)
        basePreView.setImageBitmap(bitmap)

        val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)
        editText.requestFocus()
        editText.visibility = View.VISIBLE

        btnOK.setOnClickListener { v: View? ->
            val faceID = editText.text.toString()
            if (!TextUtils.isEmpty(faceID)) {
                disposeSelectImage(faceID,bitmap,faceEmbedding)
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




    companion object {
        const val REQUEST_ADD_FACE_IMAGE = 1882
    }

}