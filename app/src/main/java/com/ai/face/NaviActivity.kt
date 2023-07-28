package com.ai.face

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.face.FaceApplication.Companion.CACHE_BASE_FACE_DIR
import com.ai.face.FaceApplication.Companion.FACE_DIR_KEY
import com.ai.face.FaceApplication.Companion.USER_ID_KEY
import com.ai.face.base.utils.DeviceFingerprint
import com.ai.face.search.SearchNaviActivity
import com.ai.face.verify.AddBaseImageActivity
import com.ai.face.utils.VoicePlayer
import com.ai.face.verify.Verify_11_javaActivity
import kotlinx.android.synthetic.main.activity_navi.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.io.File

/**
 *
 * Demo 提供java 和 Kotlin 接入演示 ，仅仅是演示如何接入SDK，根据业务场景用户自行修改符合需求
 *
 * 1：N 人脸识别检索可以独立引入 https://github.com/AnyLifeZLB/FaceSearchSDK_Android
 *
 * 更多请发邮件 anylife.zlb@gmail.com
 *
 * 或加微信 HaoNan19990322 （请标注人脸识别定制，否则添加不通过，谢谢）交流
 *
 * 2022.07.29
 */
class NaviActivity : AppCompatActivity(), PermissionCallbacks {

    private var yourUniQueFaceId = "18707611416"  //微信号

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        checkNeededPermission()

        face_verify_card.setOnClickListener {

            //可以自己录一张人脸底片，业务方可以根据自己的要求改写testBaseImgName 处理
            val file = File(CACHE_BASE_FACE_DIR, yourUniQueFaceId)
            if (BitmapFactory.decodeFile(file.path) != null) {
                startActivity(
                    Intent(this@NaviActivity, Verify_11_javaActivity::class.java)
                        .putExtra(USER_ID_KEY, yourUniQueFaceId)
                )
            } else {
                Toast.makeText(this@NaviActivity, "请先录入人脸底片", Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                        .putExtra(USER_ID_KEY, yourUniQueFaceId)
                        .putExtra(FACE_DIR_KEY, CACHE_BASE_FACE_DIR)
                )
            }
        }


        //添加1：1人脸识别底片
        update_base_image.setOnClickListener {
            startActivity(
                Intent(this@NaviActivity, AddBaseImageActivity::class.java)
                    .putExtra(USER_ID_KEY, yourUniQueFaceId)
                    .putExtra(FACE_DIR_KEY, CACHE_BASE_FACE_DIR)
            )
        }


        verify_1_n.setOnClickListener {
                startActivity(
                    Intent(this@NaviActivity, SearchNaviActivity::class.java)
                )
        }


        more_about_me.setOnClickListener {
            startActivity(Intent(this@NaviActivity, AboutUsActivity::class.java))
        }

        //用于绑定设备授权查看
        device_info.text="设备指纹:"+DeviceFingerprint.getDeviceFingerprint()


        change_camera.setOnClickListener {
            val sharedPref = getSharedPreferences(
                "faceVerify", Context.MODE_PRIVATE)

            if(sharedPref.getInt("cameraFlag",0)==1){
                sharedPref.edit().putInt("cameraFlag",0).apply()
                Toast.makeText(
                    baseContext,
                    "已切换前置摄像头",
                    Toast.LENGTH_LONG
                ).show()
            }else{
                sharedPref.edit().putInt("cameraFlag",1).apply()
                Toast.makeText(
                    baseContext,
                    "已切换后置/外接摄像头",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        VoicePlayer.getInstance().init(this)
    }



    /**
     * 统一全局的拦截权限请求，给提示
     *
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)

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

}