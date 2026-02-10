package com.faceAI.demo.SysCamera.search

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import com.ai.face.core.utils.FaceAICameraType
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import com.faceAI.demo.FaceAISettingsActivity
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
class FaceSearchNaviActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var binding: ActivityFaceSearchNaviBinding
    private var cameraType = FaceAICameraType.SYSTEM_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceSearchNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkNeededPermission()

        val sharedPref = getSharedPreferences("FaceAISDK_SP", MODE_PRIVATE)
        cameraType = sharedPref.getInt(
            FaceAISettingsActivity.Companion.UVC_CAMERA_TYPE,
            FaceAICameraType.SYSTEM_CAMERA
        )

        when (cameraType) {
            FaceAICameraType.SYSTEM_CAMERA -> {
                binding.cameraMode.setText(R.string.camera_type_system)
            }
            FaceAICameraType.UVC_CAMERA_RGB -> {
                binding.cameraMode.setText(R.string.camera_type_uvc_rgb)
            }
            FaceAICameraType.UVC_CAMERA_RGB_IR -> {
                binding.cameraMode.setText(R.string.camera_type_uvc_rgb_ir)
            }
        }

        binding.back.setOnClickListener {
            this@FaceSearchNaviActivity.finish()
        }

        //批量导入导出人脸特征数据。 SDK不需要图片，如果业务也不需要图片，强烈建议转位人脸特征
        binding.insertFaceFeatures.setOnClickListener {
            //批量导出人脸数据
            //val faceSearchFeatures:List<FaceSearchFeature> =FaceSearchFeatureManger.getInstance(this).queryAllFaceFaceFeature()
            //val faceSearchFeature: FaceSearchFeature? =FaceSearchFeatureManger.getInstance(this).queryFaceFeatureByID("test")
            //FaceSearchFeatureManger.getInstance(this).insertFeatures(faceSearchFeatures) //数组对象
            val num=FaceSearchFeatureManger.getInstance(this).insertFeatures(JSONFaceFeatures.testJsonStrings) //json格式
            Toast.makeText(baseContext, "Done,$num", Toast.LENGTH_SHORT).show()
        }

        //1:N 人脸搜索
        binding.faceSearch1n.setOnClickListener {
            if (cameraType == FaceAICameraType.SYSTEM_CAMERA) {
                val intent = Intent(baseContext, FaceSearch1NActivity::class.java)
                intent.putExtra(FaceSearch1NActivity.THRESHOLD_KEY, 0.88f)
                intent.putExtra(FaceSearch1NActivity.SEARCH_ONE_TIME, true)
                intent.putExtra(FaceSearch1NActivity.NEED_FACE_LIVE, false)
                intent.putExtra(FaceSearch1NActivity.IS_CAMERA_SIZE_HIGH, false) //默认给false
                intent.putExtra(FaceSearch1NActivity.CAMERA_ID, CameraSelector.LENS_FACING_BACK)
                startActivity(intent)
            } else {
                //UVC 参数后期再完善
                startActivity(Intent(baseContext, FaceSearch_UVCCameraActivity::class.java))
            }
        }

        //1:N 人脸搜索.包含活体检测
        binding.faceSearchWithLive.setOnClickListener {
            if (cameraType == FaceAICameraType.SYSTEM_CAMERA) {
                val intent = Intent(baseContext, FaceSearch1N_LivenessActivity::class.java)
                intent.putExtra(FaceSearch1NActivity.THRESHOLD_KEY, 0.88f)
                intent.putExtra(FaceSearch1NActivity.SEARCH_ONE_TIME, true)
                intent.putExtra(FaceSearch1NActivity.NEED_FACE_LIVE, true)
                intent.putExtra(FaceSearch1NActivity.IS_CAMERA_SIZE_HIGH, false) //默认给false
                intent.putExtra(FaceSearch1NActivity.CAMERA_ID, CameraSelector.LENS_FACING_BACK)
                startActivity(intent)
            } else {
                //UVC 参数后期再完善
                startActivity(Intent(baseContext, FaceSearch_UVCCameraActivity::class.java))
            }
        }

        //MN人脸搜索，Beta版本，仅仅系统摄像头，不包含UVC协议摄像头
        binding.systemCameraSearchMn.setOnClickListener {
            if (cameraType == FaceAICameraType.SYSTEM_CAMERA) {
                startActivity(Intent(baseContext, FaceSearchMNActivity::class.java))
            }else{
                Toast.makeText(baseContext, "MN Search only support system camera", Toast.LENGTH_SHORT).show()
            }
        }

        //统一跳转到人脸管理页面，在FaceSearchImageMangerActivity中根据不同摄像头跳转
        binding.faceSearchAddFace.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchDataMangerActivity::class.java)
                    .putExtra("isAdd", true)
            )
        }

        //兼容某些场景还需要通过图片方式导入人脸库
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

        //demo为了演示更直观，带人脸图管理，长按删除（SDK工作实际不需要图只要人脸特征）
        binding.mangerFaceWithImage.setOnClickListener {
            startActivity(
                Intent(baseContext, FaceSearchDataMangerActivity::class.java).putExtra(
                    "isAdd",
                    false
                )
            )
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