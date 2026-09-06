package com.faceAI.demo.SysCamera.addFace;

import static com.ai.face.base.addFace.AddFaceDispose.PERFORMANCE_MODE_ACCURATE;
import static com.ai.face.base.addFace.AddFaceDispose.PERFORMANCE_MODE_FAST;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.ai.face.base.addFace.AddFaceCallBack;
import com.ai.face.base.addFace.AddFaceDispose;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.view.FaceCoverView;

/**
 * 判断是否有人脸正脸，然后持续返回数据
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class CaptureFaceActivity extends AbsBaseActivity {
    public static String ADD_FACE_PERFORMANCE_MODE = "ADD_FACE_PERFORMANCE_MODE";
    private FaceCoverView faceCoverView;
    private AddFaceDispose addFaceDispose;
    private int addFacePerformanceMode = PERFORMANCE_MODE_FAST;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_capture_face);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        faceCoverView = findViewById(R.id.face_cover);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ADD_FACE_PERFORMANCE_MODE)) {
                addFacePerformanceMode = intent.getIntExtra(ADD_FACE_PERFORMANCE_MODE,PERFORMANCE_MODE_ACCURATE);
            }
        }


        /* 添加人脸,实时检测相机视频流人脸角度是否符合当前模式设置，并给予提示
         *
         *  2 PERFORMANCE_MODE_ACCURATE   精确模式 人脸要正对摄像头，严格要求角度
         *  1 PERFORMANCE_MODE_FAST       快速模式 允许人脸角度可以有一定的偏差
         *  0 PERFORMANCE_MODE_EASY       简单模式 允许人脸角度可以「较大」的偏差
         * -1 PERFORMANCE_MODE_NO_LIMIT   无限制模式 基本上检测到人脸就返回了
         */
        addFaceDispose = new AddFaceDispose(this, addFacePerformanceMode,true, new AddFaceCallBack() {
            /**
             * 人脸检测裁剪完成
             * @param cropped         SDK检测裁剪矫正后的Bitmap，20260227版本统一大小为224*224
             * @param silentScore     静默活体分数(摄像头品质有关)，needLivenessCheck=true才有值
             * @param origin          640*480 原图
             */
            @Override
            public void onCompleted(Bitmap cropped, float silentScore,Bitmap origin) {

            }

            @Override
            public void onProcessTips(int actionCode) {
                AddFaceTips(actionCode);
            }
        });


        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(0) //前后摄像头
                .setLinearZoom(0.12f)   //范围[0f,1.0f]，根据应用场景，自行适当调整焦距参数（摄像头需支持变焦）
                .setRotation(0)  //画面旋转角度0，90，180，270
                .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
                .create();

        FaceCameraXFragment cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing()) {
                //某些设备如果一直提示检测不到人脸，可以断点调试看看转化的Bitmap 是否有问题
                addFaceDispose.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy));
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camerax, cameraXFragment).commit();

    }


    /**
     * 添加人脸过程中的提示
     *
     */
    private void AddFaceTips(int tipsCode) {
        switch (tipsCode) {
            case NO_FACE_REPEATEDLY:
                faceCoverView.setTipsText(R.string.no_face_detected_tips);
                break;

            case FACE_TOO_SMALL:
                faceCoverView.setTipsText(R.string.come_closer_tips);
                break;
            case FACE_TOO_LARGE:
                faceCoverView.setTipsText(R.string.far_away_tips);
                break;
            case CLOSE_EYE:
                faceCoverView.setTipsText(R.string.no_close_eye_tips);
                break;
            case HEAD_CENTER:
                faceCoverView.setTipsText(R.string.keep_face_tips);
                break;
            case TILT_HEAD:
                faceCoverView.setTipsText(R.string.no_tilt_head_tips);
                break;
            case HEAD_LEFT:
                faceCoverView.setTipsText(R.string.head_turn_left_tips);
                break;
            case HEAD_RIGHT:
                faceCoverView.setTipsText(R.string.head_turn_right_tips);
                break;
            case HEAD_UP:
                faceCoverView.setTipsText(R.string.no_look_up_tips);
                break;
            case HEAD_DOWN:
                faceCoverView.setTipsText(R.string.no_look_down_tips);
                break;
        }
    }

}
