package com.ai.face

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.databinding.ActivityFaceAiNaviBinding
import com.ai.face.search.SearchNaviActivity
import com.ai.face.utils.SystemUtil
import com.ai.face.utils.VoicePlayer
import com.ai.face.verify.FaceVerifyWelcomeActivity
import com.ai.face.verify.TwoFaceImageVerifyActivity
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


/**
 * SDK 接入演示Demo，请先熟悉本Demo跑通住流程后再集成到你的主工程验证业务
 *
 */
class FaceAINaviActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var viewBinding: ActivityFaceAiNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFaceAiNaviBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        checkNeededPermission()

        //人脸图保存路径初始化
        FaceAIConfig.init(this)
        //语音提示
        VoicePlayer.getInstance().init(this)


        //分享
        viewBinding.shareLayout.setOnClickListener {
            val intent = Intent()
            intent.setAction(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_faceai_sdk_content))
            intent.setType("text/plain")
            startActivity(intent)
        }

        //1:1 人脸识别
        viewBinding.faceVerifyCard.setOnClickListener {

            val enumIntent = Intent(baseContext, FaceVerifyWelcomeActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(FaceVerifyWelcomeActivity.FACE_VERIFY_DATA_SOURCE_TYPE,
                FaceVerifyWelcomeActivity.DataSourceType.Android_HAL
            )
            enumIntent.putExtras(bundle)
            startActivity(enumIntent)
        }


        // 1:N， M：N 人脸搜索
        viewBinding.faceSearch.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, SearchNaviActivity::class.java))
        }


        //双目摄像头，请确认你的双目UVC摄像头参数符合程序要求
        viewBinding.binocularCamera.setOnClickListener {
            showConnectUVCCameraDialog()
        }

        viewBinding.moreAboutMe.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, AboutFaceAppActivity::class.java))
        }


        //两张静态人脸图中人脸相似度 对比
        viewBinding.twoFaceVerify.setOnClickListener {
            startActivity(Intent(this@FaceAINaviActivity, TwoFaceImageVerifyActivity::class.java))
        }


        //切换前后摄像头
        viewBinding.changeCamera.setOnClickListener {
            val sharedPref = getSharedPreferences(
                "faceVerify", Context.MODE_PRIVATE
            )
            if (sharedPref.getInt("cameraFlag", 0) == 1) {
                sharedPref.edit().putInt("cameraFlag", 0).apply()
                Toast.makeText(
                    baseContext,
                    "Front camera",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                sharedPref.edit().putInt("cameraFlag", 1).apply()
                Toast.makeText(
                    baseContext,
                    "Back/USB Camera",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        showSystemParameter()
    }


    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
        //自行管理你的存储 相机权限
        //存储照片在某些目录需要,Manifest.permission.WRITE_EXTERNAL_STORAGE
        val perms = arrayOf(Manifest.permission.CAMERA)

        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(
                this,
                "SDK Demo 相机和读取相册都仅仅是为了完成人脸识别所必需，请授权！",
                11,
                *perms
            )
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

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {

    }


    /**
     * 当用户点击了不再提醒的时候的处理方式
     */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, "Please Grant Permission To Run FaceAI SDK,请授权才能正常演示", Toast.LENGTH_SHORT)
            .show()
    }


    private fun showSystemParameter() {
        val TAG = "系统参数："
        Log.e(TAG, "签名SHA1：" + SystemUtil.getSHA1(baseContext))
        Log.e(TAG, "设备厂商：" + SystemUtil.getDeviceBrand())
        Log.e(TAG, "设备型号：" + SystemUtil.getSystemModel())
        Log.e(TAG, "Android系统版本号：" + SystemUtil.getSystemVersion())
    }


    /**
     * 确认是否连接好了双目摄像头
     *
     */
    private fun showConnectUVCCameraDialog() {
        val builder = AlertDialog.Builder(this)
        val dialog = builder.create()
        val dialogView = View.inflate(this, R.layout.dialog_connect_uvc_camera, null)

        //设置对话框布局
        dialog.setView(dialogView)

        val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)

        btnOK.setOnClickListener { v: View? ->
            val enumIntent = Intent(baseContext, FaceVerifyWelcomeActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(FaceVerifyWelcomeActivity.FACE_VERIFY_DATA_SOURCE_TYPE,
                FaceVerifyWelcomeActivity.DataSourceType.UVC
            )
            enumIntent.putExtras(bundle)
            startActivity(enumIntent)

//            startActivity(Intent(this@FaceAINaviActivity, BinocularUVCCameraActivity::class.java))
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }




}