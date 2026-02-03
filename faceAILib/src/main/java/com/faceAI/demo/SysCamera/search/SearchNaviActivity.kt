package com.faceAI.demo.SysCamera.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.edit
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import com.faceAI.demo.FaceAISettingsActivity.Companion.FRONT_BACK_CAMERA_FLAG
import com.faceAI.demo.R
import com.faceAI.demo.UVCCamera.search.FaceSearch_UVCCameraActivity
import com.faceAI.demo.databinding.ActivityFaceSearchNaviBinding
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

/**
 * 人脸识别搜索 演示导航Navi，目前支持千张图片秒级搜索
 * 测试验证人脸库图片位于/assert 目录，更多的人脸图片请使用Ai 生成
 *
 * 使用的宽动态（人脸搜索必须大于110DB）高清抗逆光摄像头；保持镜头干净（用纯棉布擦拭油污）
 *
 */
class SearchNaviActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var binding: ActivityFaceSearchNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkNeededPermission()
        binding.back.setOnClickListener {
            this@SearchNaviActivity.finish()
        }



        binding.insertFaceFeatures.setOnClickListener {
            //批量导出人脸数据
            //val faceSearchFeatures:List<FaceSearchFeature> =FaceSearchFeatureManger.getInstance(this).queryAllFaceFaceFeature()
            //val faceSearchFeature: FaceSearchFeature? =FaceSearchFeatureManger.getInstance(this).queryFaceFeatureByID("test")

            FaceSearchFeatureManger.getInstance(this).insertFeatures(JSONFaceFeatures.testJsonStrings) //json 格式
            //FaceSearchFeatureManger.getInstance(this).insertFeatures(faceSearchFeatures) //数组对象

            Toast.makeText(baseContext, "Done", Toast.LENGTH_SHORT).show()
        }


        binding.systemCameraSearch.setOnClickListener {
            val intent = Intent(baseContext, FaceSearch1NActivity::class.java)
            // 搜索阈值 (Float): 范围建议 0.80f - 0.95f
            intent.putExtra(FaceSearch1NActivity.THRESHOLD_KEY, 0.89f)
            // 是否仅搜索一次 (Boolean): true=搜索到结果后自动 finish 关闭页面
            intent.putExtra(FaceSearch1NActivity.SEARCH_ONE_TIME, true)
            // 是否开启高分辨率 (Boolean): true=高分辨率(适合远距离但性能下降,部分定制设备不支持), false=标准
            intent.putExtra(FaceSearch1NActivity.IS_CAMERA_SIZE_HIGH, false) //默认给false
            // 摄像头ID (Int): 使用 CameraSelector 的常量，通常 0 是后置，1 是前置
            // 如果没有 CameraSelector 依赖，直接传 Int 即可 (1 = LENS_FACING_FRONT)
            intent.putExtra(FaceSearch1NActivity.CAMERA_ID, CameraSelector.LENS_FACING_BACK)
            startActivity(intent)
        }

        binding.systemCameraSearchWithLive.setOnClickListener {
            startActivity(Intent(baseContext, FaceSearch1N_LivenessActivity::class.java))
            true
        }

        binding.systemCameraSearchMn.setOnClickListener {
            startActivity(Intent(baseContext, FaceSearchMNActivity::class.java))
        }

        binding.systemCameraAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java)
                    .putExtra("isAdd", true))
        }

        binding.uvcUsbCameraSearch.setOnClickListener {
            showConnectUVCCameraDialog()
        }

        binding.uvcUsbCameraAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java)
                    .putExtra("isAdd", true)
            )
        }

        //验证复制图片
        binding.copyFaceImages.setOnClickListener {
            binding.copyFaceImages.visibility= View.INVISIBLE
            CopyFaceImageUtils.copyTestFaceImages(
                baseContext,
                object : CopyFaceImageUtils.Callback {
                    override fun onComplete(successCount: Int, failureCount: Int) {
                        Toast.makeText(
                            baseContext,
                            "Success：$successCount Failed:$failureCount",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.switchCamera.setOnClickListener {
            val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
            if (sharedPref.getInt( FRONT_BACK_CAMERA_FLAG, 1) == 1) {
                sharedPref.edit(commit = true) { putInt( FRONT_BACK_CAMERA_FLAG, 0) }
                Toast.makeText(
                    baseContext,
                    "Front camera now",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sharedPref.edit(commit = true) { putInt( FRONT_BACK_CAMERA_FLAG, 1) }
                Toast.makeText(
                    baseContext,
                    "Back/USB Camera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.editFaceImage.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchImageMangerActivity::class.java).putExtra(
                    "isAdd",
                    false
                )
            )
        }
    }


    /**
     * 提示
     *
     */
    private fun showConnectUVCCameraDialog() {
        //一天提示一次
        val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        val showTime = sharedPref.getLong("showUVCCameraDialog", 0)
        if (System.currentTimeMillis() - showTime < 12 * 60 * 60 * 1000) {
            startActivity(
                Intent(this@SearchNaviActivity, FaceSearch_UVCCameraActivity::class.java)
            )
        } else {
            val builder = AlertDialog.Builder(this)
            val dialog = builder.create()
            val dialogView = View.inflate(this, R.layout.dialog_connect_uvc_camera, null)
            //设置对话框布局
            dialog.setView(dialogView)
            val btnOK = dialogView.findViewById<Button>(R.id.btn_ok)
            btnOK.setOnClickListener {
                startActivity(
                    Intent(this@SearchNaviActivity, FaceSearch_UVCCameraActivity::class.java)
                )
                dialog.dismiss()
            }
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            sharedPref.edit(commit = true) {
                putLong(
                    "showUVCCameraDialog",
                    System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * SDK接入方 自行处理权限管理
     */
    private fun checkNeededPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {

        } else {
            EasyPermissions.requestPermissions(this, "Camera Permission to add face image！", 11, *perms)
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

    }

}