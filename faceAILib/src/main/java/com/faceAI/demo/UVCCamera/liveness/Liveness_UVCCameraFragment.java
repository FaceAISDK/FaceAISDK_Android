package com.faceAI.demo.UVCCamera.liveness;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.search.ImageToast;
import com.faceAI.demo.base.utils.BrightnessUtil;
import com.faceAI.demo.base.utils.VoicePlayer;

/**
 * UVC协议USB摄像头活体检测 Liveness Detection with UVC USB Camera
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class Liveness_UVCCameraFragment extends AbsLiveness_UVCCameraFragment {
    private TextView tipsTextView, secondTipsTextView, scoreText;
    private FaceLivenessType faceLivenessType = FaceLivenessType.SILENT_MOTION;//活体检测类型
    private float silentLivenessThreshold = 0.85f; //静默活体分数通过的阈值,摄像头成像能力弱的自行调低
    private int motionStepSize = 2; //动作活体的个数
    private int motionTimeOut = 7; //动作超时秒
    private int exceptMotionLiveness = -1; //1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头

    public Liveness_UVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void initViews() {
        super.initViews();
        scoreText = binding.silentScore;
        tipsTextView = binding.tipsView;
        secondTipsTextView = binding.secondTipsView;
        binding.back.setOnClickListener(v -> requireActivity().finish());
        BrightnessUtil.setBrightness(requireActivity(), 0.9f);  //高亮白色背景屏幕光可以当补光灯
    }

    /**
     * 初始化认证引擎，LivenessType.IR需要你的摄像头是双目红外摄像头，如果仅仅是RGB 摄像头请使用LivenessType.SILENT_MOTION
     *
     */
    void initFaceLivenessParam(){
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getContext())
                .setLivenessOnly(true)
                .setCameraType(cameraType)
                .setLivenessType(faceLivenessType) //活体检测可以静默&动作活体组合，静默活体效果和摄像头成像能力有关(宽动态>105Db)
                .setSilentLivenessThreshold(silentLivenessThreshold)  //静默活体阈值 [0.88,0.98]
                .setMotionLivenessStepSize(motionStepSize)           //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(motionTimeOut)            //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
                .setLivenessDetectionMode(MotionLivenessMode.FAST)  //硬件配置低用FAST动作活体模式，否则用精确模式
                .setMotionLivenessTypes("1,2,3,4,5")                //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
                .setProcessCallBack(new ProcessCallBack() {
                    /**
                     * 活体检测完成，动作活体没有超时（如有），静默活体设置了需要（不需要返回00）
                     *
                     * @param silentLivenessValue
                     * @param bitmap
                     */
                    @Override
                    public void onLivenessDetected(float silentLivenessValue, Bitmap bitmap) {
                        requireActivity().runOnUiThread(() -> {
                            tipsTextView.setText(R.string.liveness_detection_done);
                            VoicePlayer.getInstance().addPayList(R.raw.verify_success);

                            if(FaceSDKConfig.isDebugMode(requireContext())){
                                scoreText.setText("RGB Live:"+silentLivenessValue);
                                new ImageToast().show(requireContext(), bitmap, "活体检测完成");
                                new AlertDialog.Builder(requireActivity())
                                        .setTitle("Debug模式提示")
                                        .setMessage("活体检测完成，其中RGB Live分数="+silentLivenessValue)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                            requireActivity().finish();
                                        })
                                        .setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify())
                                        .show();
                            }
                        });
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showFaceLivenessTips(i);
                    }

                    //动作活体检测时间限制倒计时百分比
                    @Override
                    public void onTimeCountDown(float percent) {

                    }

                    /**
                     * 严重错误
                     * @param code 错误代码编码看对应的文档
                     * @param message
                     */
                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getContext(), "onFailed错误：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    void showFaceLivenessTips(int actionCode) {
        if (!requireActivity().isDestroyed() && !requireActivity().isFinishing()) {
            Log.e("RGBUVC","---- "+actionCode);
            requireActivity().runOnUiThread(() -> {
                switch (actionCode) {
                    // 动作活体检测完成了
                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.ALIVE_CHECK_DONE:
                        VoicePlayer.getInstance().play(R.raw.verify_success);
                        setTips(R.string.liveness_detection_done);
                        setSecondTips(0);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_IMAGE_NULL:
                        setTips(R.string.ir_image_error);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_LIVE_FAILED:
                        setTips(R.string.ir_live_error);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        setTips(R.string.face_verifying);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_BASE_IMG:
                        setTips(R.string.no_base_face_bitmap);
                        break;
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        setTips(R.string.motion_liveness_detection_failed);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        setTips(R.string.repeat_open_close_mouse);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        setTips(R.string.motion_smile);
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        setTips(R.string.motion_blink_eye);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        setTips(R.string.motion_shake_head);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        setTips(R.string.motion_node_head);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_TIME_OUT:
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.motion_liveness_detection_time_out)
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                            faceVerifyUtils.retryVerify();
                                        }
                                ).show();
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY:
                        setTips(R.string.no_face_or_repeat_switch_screen);
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.stop_verify_tips)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    requireActivity().finish();
                                })
                                .show();

                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        setSecondTips(R.string.no_face_detected_tips);
                        break;

                    // 单独使用一个textview 提示，防止上一个提示被覆盖。
                    // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                        setSecondTips(R.string.far_away_tips);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL:
                        setSecondTips(R.string.come_closer_tips);
                        break;

                    //检测到正常的人脸，尺寸大小OK
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT:
                        setSecondTips(0);
                        break;

                }
            });
        }
    }


    private void setTips(int resId) {
        tipsTextView.setText(resId);
    }

    /**
     * 第二行提示
     * @param resId
     */
    private void setSecondTips(int resId){
        if(resId==0){
            secondTipsTextView.setText("");
            secondTipsTextView.setVisibility(View.INVISIBLE);
        }else {
            secondTipsTextView.setVisibility(View.VISIBLE);
            secondTipsTextView.setText(resId);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (faceVerifyUtils != null) {
            faceVerifyUtils.destroyProcess();
        }
    }


    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     */
    public void onStop() {
        super.onStop();
        if (faceVerifyUtils != null) {
            faceVerifyUtils.pauseProcess();
        }
    }


    /**
     * 请断点调试保证bitmap 的方向正确； RGB和IR Bitmap大小相同，画面同步
     *
     * @param bitmap
     * @param type
     */
    private Bitmap rgbBitmap, irBitmap;
    private boolean rgbReady = false, irReady = false;

    /**
     * UVC协议USB摄像头设置数据，送数据到SDK 引擎
     *
     * @param bitmap
     * @param type
     */
    void faceLivenessSetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type) {

        if(cameraType== FaceAICameraType.UVC_CAMERA_RGB){
            faceVerifyUtils.goVerifyWithBitmap(bitmap);
        }else{
            if (type.equals(FaceVerifyUtils.BitmapType.IR)) {
                irBitmap = bitmap;
                irReady = true;
            } else if (type.equals(FaceVerifyUtils.BitmapType.RGB)) {
                rgbBitmap = bitmap;
                rgbReady = true;
            }

            if (irReady && rgbReady) {
                //送数据进入SDK
                faceVerifyUtils.goVerifyWithIR(irBitmap, rgbBitmap);
                irReady = false;
                rgbReady = false;
            }
        }
    }


}
