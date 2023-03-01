package com.faceVerify.test

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.AI.FaceVerify.utils.AiUtil
import com.AI.FaceVerify.utils.CopyFileUtils
import com.faceVerify.test.FaceApplication.Companion.BASE_FACE_PATH
import com.faceVerify.test.FaceApplication.Companion.DIR_11_VALUE
import com.faceVerify.test.FaceApplication.Companion.DIR_1N_VALUE
import com.faceVerify.test.FaceApplication.Companion.FACE_DIR_KEY
import com.faceVerify.test.FaceApplication.Companion.USER_ID_KEY
import com.faceVerify.test.utils.AboutUsActivity
import com.faceVerify.test.utils.AddBaseImageActivity
import com.faceVerify.test.verify11.Verify11Activity
import com.faceVerify.test.verify1N.Verify1NActivity
import kotlinx.android.synthetic.main.activity_navi.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.io.File

/**
 *
 * Demo 仅仅是演示如何接入SDK，根据业务场景用户自行修改符合需求
 *
 * 静默活体检测&炫光活体检测Alpha 版本已经发布，抢先体验请发送邮件
 *
 * 更多请发邮件 anylife.zlb@gmail.com
 * 或加微信 HaoNan19990322 （请标注人脸识别定制，否则添加不通过，谢谢）交流
 *
 *
 * 2022.07.29
 */
class NaviActivity : AppCompatActivity(), PermissionCallbacks {

    private var yourUniQueFaceId = "18707611416"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        checkNeededPermission()

        face_verify_card.setOnClickListener {
            //可以自己录一张人脸底片，业务方可以根据自己的要求改写testBaseImgName 处理
            val file = File(BASE_FACE_PATH+ DIR_11_VALUE, yourUniQueFaceId)
            if (AiUtil.compressPath(this@NaviActivity, Uri.fromFile(file)) != null) {
                startActivity(
                    Intent(this@NaviActivity, Verify11Activity::class.java)
                        .putExtra(USER_ID_KEY, yourUniQueFaceId)
                )
            } else {
                Toast.makeText(this@NaviActivity, "请先录入人脸底片", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                        .putExtra(USER_ID_KEY, yourUniQueFaceId)
                        .putExtra(FACE_DIR_KEY, DIR_11_VALUE)
                )
            }
        }


        //添加1：1人脸识别底片
        update_base_image.setOnClickListener {
            startActivity(
                Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                    .putExtra(USER_ID_KEY, yourUniQueFaceId)
                    .putExtra(FACE_DIR_KEY, DIR_11_VALUE)
            )
        }


        verify1n.setOnClickListener {
            //1:N 人脸比对，实际应用请自行管理底片的处理
            if (getFilesAllName(BASE_FACE_PATH+ DIR_1N_VALUE).isEmpty()) {
                Toast.makeText(this@NaviActivity, "请先添加底片", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                        .putExtra(USER_ID_KEY, yourUniQueFaceId)
                        .putExtra(FACE_DIR_KEY, DIR_1N_VALUE)
                )
            } else {
                startActivity(
                    Intent(this@NaviActivity, Verify1NActivity::class.java)
                )
            }
        }


        //添加1：N 人脸识别底片
        verify1n_add.setOnClickListener {

            CopyFileUtils.getInstance(this@NaviActivity)
                .copyAssetsToSD("baseImg", BASE_FACE_PATH + DIR_1N_VALUE)
                .setFileOperateCallback(object : CopyFileUtils.FileOperateCallback{
                    override fun onSuccess() {
                        Toast.makeText(baseContext, "1:N 底片复制成功", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailed(error: String?) {
                        Toast.makeText(baseContext, "操作失败:$error", Toast.LENGTH_SHORT).show()
                    }
                })


            startActivity(
                Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                    .putExtra(USER_ID_KEY, yourUniQueFaceId)
                    .putExtra(FACE_DIR_KEY, DIR_1N_VALUE)
            )
        }


        more_about_me.setOnClickListener {

            startActivity(
                Intent(this@NaviActivity, AboutUsActivity::class.java)
            )


//            val uri = Uri.parse("https://github.com/AnyLifeZLB/FaceVerificationSDK")
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.addCategory(Intent.CATEGORY_BROWSABLE)
//            intent.data = uri
//            startActivity(intent)
        }

    }


    /**
     * 获取底片文件夹下的所有文件
     *
     * @param path
     * @return
     */
    private fun getFilesAllName(path: String?): List<String> {
        val file = File(path)
        val files = file.listFiles()
        val fileNames: MutableList<String> = ArrayList()

        if (files == null) {
            Log.e("error", "空目录")
            return fileNames
        }
        for (i in files.indices) {
            if (!files[i].absolutePath.contains("tempTake")) {
                fileNames.add(files[i].absolutePath)
            }
        }
        return fileNames
    }


    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
        } else {
            EasyPermissions.requestPermissions(this, "相机和读取相册都仅仅是为了完成人脸识别所必需，请授权！", 11, *perms)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    /**
     * 当用户点击了不再提醒的时候的处理方式
     */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, "不授权无法使用App啊", Toast.LENGTH_SHORT).show()
    }


    fun onUpload(view: View?) {
        Toast.makeText(this, "分析优化中...", Toast.LENGTH_SHORT).show()
    }

}