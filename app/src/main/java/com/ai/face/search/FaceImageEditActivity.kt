package com.ai.face.search

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.face.FaceApplication.Companion.CACHE_SEARCH_FACE_DIR
import com.ai.face.R
import com.ai.face.base.baseImage.BaseImageDispose
import com.ai.face.base.utils.FaceFileProviderUtils
import com.ai.face.faceSearch.search.FaceSearchImagesManger
import com.ai.face.faceSearch.utils.BitmapUtils
import com.ai.face.search.SearchNaviActivity.Companion.copyManyTestFaceImages
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.lzf.easyfloat.EasyFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Locale
import java.util.UUID

/**
 * 增删改 编辑人脸图片,演示怎样使用SDK API进行人脸的增删改查
 * 仅仅是测试验证和演示，不是SDK 接入的一部分
 *
 */
class FaceImageEditActivity : AppCompatActivity() {

    private val faceImageList: MutableList<String> = ArrayList()
    private lateinit var faceImageListAdapter: FaceImageListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_image_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        val mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val gridLayoutManager: LinearLayoutManager = GridLayoutManager(this, 3)
        mRecyclerView.layoutManager = gridLayoutManager
        loadImageList()

        faceImageListAdapter = FaceImageListAdapter(faceImageList)
        mRecyclerView.adapter = faceImageListAdapter

        //长按删除对应的照片
        faceImageListAdapter.setOnItemLongClickListener { _, _, i ->
            AlertDialog.Builder(this@FaceImageEditActivity)
                .setTitle("确定要删除" + File(faceImageList[i]).name)
                .setMessage("删除后对应的人将无法被程序识别")
                .setPositiveButton("确定") { _: DialogInterface?, _: Int ->
                    FaceSearchImagesManger.IL1Iii.getInstance(application)
                        ?.deleteFaceImage(faceImageList[i])
                    loadImageList()
                    faceImageListAdapter.notifyDataSetChanged()
                }
                .setNegativeButton("取消") { _: DialogInterface?, _: Int -> }
                .show()
            false
        }
        faceImageListAdapter.setEmptyView(R.layout.empty_layout)

        //不要多次重复复制会闪退，演示不防护了
        faceImageListAdapter.emptyLayout?.setOnClickListener { v: View? ->
            Toast.makeText(baseContext, "复制中...", Toast.LENGTH_LONG).show()
            SearchNaviActivity.showAppFloat(baseContext)

            CoroutineScope(Dispatchers.IO).launch {
                copyManyTestFaceImages(application)
                delay(800)

                MainScope().launch {
                    loadImageList()
                    faceImageListAdapter.notifyDataSetChanged()
                    EasyFloat.dismiss("speed")
                }
            }
        }

        if (intent.extras?.getBoolean("isAdd") == true) {
            dispatchTakePictureIntent()
        }

    }

    /**
     * 加载本地管理的人脸库图片
     * 本演示Demo图片人脸放在filesDir.path + "/faceSearch" 是为了简化不想管理权限申请，放在此目录的数据卸载App会一起清除
     * 建议App 接入的时候根据情况自身处理存储目录管理
     *
     */
    private fun loadImageList() {
        faceImageList.clear()
        val file = File(CACHE_SEARCH_FACE_DIR)
        val subFaceFiles = file.listFiles()
        if (subFaceFiles != null) {
            Arrays.sort(subFaceFiles, object : Comparator<File> {
                override fun compare(f1: File, f2: File): Int {
                    val diff = f1.lastModified() - f2.lastModified()
                    return if (diff > 0) -1 else if (diff == 0L) 0 else 1
                }

                override fun equals(obj: Any?): Boolean {
                    return true
                }
            })
            for (index in subFaceFiles.indices) {
                // 判断是否为文件夹
                if (!subFaceFiles[index].isDirectory) {
                    val filename = subFaceFiles[index].name
                    val lowerCaseName = filename.trim { it <= ' ' }.lowercase(Locale.getDefault())
                    if (lowerCaseName.endsWith(".jpg")
                        || lowerCaseName.endsWith(".png")
                        || lowerCaseName.endsWith(".jpeg")
                    ) {
                        faceImageList.add(subFaceFiles[index].path)
                    }
                }
            }
        }
    }


    class FaceImageListAdapter(results: MutableList<String>) :
        BaseQuickAdapter<String, BaseViewHolder>(R.layout.adapter_face_image_list_item, results) {
        override fun convert(helper: BaseViewHolder, item: String) {
            Glide.with(context).load(item)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into((helper.getView<View>(R.id.image) as ImageView))
        }
    }


    /**
     * 确认是否保存底图
     *
     * @param bitmap
     */
    private fun showConfirmDialog(bitmap: Bitmap) {

        //裁剪掉周边，保存人脸的部分
        val bitmapCrop = BaseImageDispose(baseContext).cropFaceBitmap(bitmap)

        if (bitmapCrop == null) {
            Toast.makeText(this, "没有检测到人脸", Toast.LENGTH_LONG).show()
            return
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val dialog = builder.create()
        val dialogView = View.inflate(this, R.layout.dialog_confirm_base, null)

        //设置对话框布局
        dialog.setView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        val basePreView = dialogView.findViewById<ImageView>(R.id.preview)
        basePreView.setImageBitmap(bitmap)
        val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text) //face id

        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.requestFocus()

        btnOK.setOnClickListener { v: View? ->
            if (!TextUtils.isEmpty(editText.text.toString())) {
                val name = editText.text.toString() + ".jpg"

                Toast.makeText(baseContext, "处理中...", Toast.LENGTH_LONG).show()
                //Kotlin 混淆操作后协程操作失效了，因为是异步操作只能等一下
                CoroutineScope(Dispatchers.IO).launch {
                    FaceSearchImagesManger.IL1Iii.getInstance(application)
                        ?.insertOrUpdateFaceImage(
                            bitmap,
                            CACHE_SEARCH_FACE_DIR + File.separatorChar + name
                        )
                    delay(300)
                    MainScope().launch {
                        loadImageList()
                        faceImageListAdapter.notifyDataSetChanged()
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(baseContext, "请确认ID 名字", Toast.LENGTH_LONG).show()
            }
        }
        btnCancel.setOnClickListener { v: View? ->
            dialog.dismiss()
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    /**
     * 处理自拍 录入人脸
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            var bitmap = BitmapUtils.Companion().getFixedBitmap(currentPhotoPath!!, contentResolver)

            //裁剪人脸，并压缩大小，防止人脸录入 OOM 闪退，1:N 搜索成功暂停0.5秒
            //平板电脑会有问题，待修复！！！
            bitmap = BaseImageDispose(baseContext).cropFaceBitmap(bitmap)

            //加一个确定ID的操作，ID不要再确认了
            showConfirmDialog(bitmap)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                dispatchTakePictureIntent()
                true
            }

            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    private var currentPhotoPath: String? = null

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val imageFileName = "JPEG_" + UUID.randomUUID() + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    /**
     * 准备去拍照
     *
     */
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FaceFileProviderUtils.getUriForFile(
                    this,
                    FaceFileProviderUtils.getAuthority(this),
                    photoFile
                )
                //前置摄像头 1:1
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }


    companion object {
        const val REQUEST_TAKE_PHOTO = 1
    }

}