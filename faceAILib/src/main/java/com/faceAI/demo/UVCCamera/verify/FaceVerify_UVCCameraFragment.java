package com.faceAI.demo.UVCCamera.verify;

import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.ai.face.base.baseImage.FaceEmbedding;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.faceAI.demo.FaceImageConfig;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.faceAI.demo.base.utils.BrightnessUtil;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.bumptech.glide.Glide;
import com.faceAI.demo.R;


/**
 * 演示USB 双目摄像头1:1人脸识别，活体检测
 * USB带红外双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * <p>
 * AbstractBinocularUVCCameraFragment 是摄像头相关处理，「调试的时候USB摄像头一定要固定住屏幕正上方」
 * <p>
 * 默认LivenessType.IR需要你的摄像头是双目红外摄像头，如果仅仅是RGB 摄像头请使用LivenessType.SILENT_MOTION
 * <p>
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 *
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceVerify_UVCCameraFragment extends AbsFaceVerify_UVCCameraFragment {
    private TextView tipsTextView, secondTipsTextView, scoreText;
    private final float silentLivenessThreshold = 0.85f; //根据你的摄像头采用合适的静默活体阈值

    public FaceVerify_UVCCameraFragment() {
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
     * 初始化人脸识别底图
     */
    void initFaceVerify() {
        faceVerifyUtils = new FaceVerifyUtils();

        //1:1 人脸对比，摄像头实时采集的人脸和预留的人脸底片对比。（动作活体人脸检测完成后开始1:1比对）
        String faceID = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);
        float[] faceEmbedding = FaceEmbedding.loadEmbedding(requireContext(), faceID);

        if(faceEmbedding.length==0){
            //本地没有faceID对应的人脸特征向量
            //你的业务代码，从你的服务器拿到对应的人脸特征向量，或提示录入人脸并同步数据到你的服务器，SDK不存储敏感数据
            Toast.makeText(requireContext(),"本地无对应的人脸特征",Toast.LENGTH_LONG).show();
        }else{
            initFaceVerificationParam(faceEmbedding);
        }

        //显示一下对应的人
        String savedFacePath = FaceImageConfig.CACHE_BASE_FACE_DIR + faceID;
        Bitmap baseBitmap = BitmapFactory.decodeFile(savedFacePath);
        Glide.with(requireActivity())
                .load(baseBitmap)
                .transform(new CircleCrop())
                .into(binding.baseFace);
    }


    /**
     * 初始化认证引擎，LivenessType.IR需要你的摄像头是双目红外摄像头，如果仅仅是RGB 摄像头请使用LivenessType.SILENT_MOTION
     *
     * @param faceEmbedding 1:1 人脸识别对比的底片
     */
    void initFaceVerificationParam(float[] faceEmbedding){
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getContext())
                .setThreshold(0.84f)                    //阈值设置，范围限 [0.75,0.95] ,低配摄像头可适量放低，默认0.85
                .setFaceEmbedding(faceEmbedding)        //1:1 人脸识别对比底片人脸特征库
                .setLivenessType(FaceLivenessType.IR) //IR 是指红外静默，MOTION 是有动作可以指定1-2 个
                .setLivenessDetectionMode(MotionLivenessMode.FAST)//硬件配置低用FAST动作活体模式，否则用精确模式
                .setSilentLivenessThreshold(silentLivenessThreshold) //静默活体阈值 [0.8,0.99]
//                .setExceptMotionLivelessType(ALIVE_DETECT_TYPE_ENUM.SMILE) //动作活体去除微笑 或其他某一种
                .setMotionLivenessStepSize(1)           //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(12) //动作活体检测，支持设置超时时间 [9,22] 秒 。API 名字0410 修改
//                .setCompareDurationTime(4500) //动作活体通过后人脸对比时间，[3000,6000]毫秒。低配设备可以设置时间长一点，高配设备默认就行
                .setStopVerifyNoFaceRealTime(true)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
                .setProcessCallBack(new ProcessCallBack() {
                    /**
                     * 1:1 人脸识别 活体检测 对比结束
                     *
                     * @param isMatched   true匹配成功（大于setThreshold）； false 与底片不是同一人
                     * @param similarity  与底片匹配的相似度值
                     * @param vipBitmap   识别完成的时候人脸实时图，仅授权用户会返回。可以拿这张图和你的服务器再次严格匹配
                     */
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float silentLivenessScore, Bitmap vipBitmap) {
                        showVerifyResult(isMatched, similarity, silentLivenessScore);
                    }

                    //人脸识别，活体检测过程中的各种提示
                    @Override
                    public void onProcessTips(int i) {
                        showFaceVerifyTips(i);
                    }

                    //动作活体检测时间限制倒计时百分比
                    @Override
                    public void onTimeCountDown(float percent) {
                        binding.faceCover.startCountDown(percent);
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
     * 检测1:1 人脸识别是否通过
     * <p>
     * 动作活体要有动作配合，必须先动作匹配通过再1：1 匹配
     * 静默活体不需要人配合，如果不需要静默活体检测，分数直接会被赋值 1.0
     */
    void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore) {
        requireActivity().runOnUiThread(() -> {
            scoreText.setText("SilentLivenessScore:" + silentLivenessScore);

            //1.静默活体分数判断
            if (silentLivenessScore < silentLivenessThreshold) {
                tipsTextView.setText(R.string.silent_anti_spoofing_error);
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.silent_anti_spoofing_error)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> requireActivity().finish())
                        .show();
            } else if (isVerifyMatched) {
                //2.和底片同一人
                tipsTextView.setText("Successful,similarity= " + similarity);
                VoicePlayer.getInstance().addPayList(R.raw.verify_success);
                new Handler(Looper.getMainLooper()).postDelayed(requireActivity()::finish, 1000);
            } else {
                //3.和底片不是同一个人
                tipsTextView.setText("Failed ！ similarity=" + similarity);
                VoicePlayer.getInstance().addPayList(R.raw.verify_failed);
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.face_verify_failed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> requireActivity().finish())
                        .setNegativeButton(R.string.retry, (dialog, which) -> {
                            faceVerifyUtils.retryVerify();
                        })
                        .show();

            }
        });
    }


    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     * <p>
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     */
    void showFaceVerifyTips(int actionCode) {
        if (!requireActivity().isDestroyed() && !requireActivity().isFinishing()) {
            requireActivity().runOnUiThread(() -> {
                switch (actionCode) {
                    // 动作活体检测完成了
                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.ALIVE_CHECK_DONE:
                        VoicePlayer.getInstance().play(R.raw.face_camera);
                        tipsTextView.setText(R.string.keep_face_visible);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_IMAGE_NULL:
                        tipsTextView.setText("IR Camera Error");
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_LIVE_FAILED:
                        tipsTextView.setText(R.string.ir_live_error);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        tipsTextView.setText(R.string.face_verifying);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_BASE_IMG:
                        tipsTextView.setText(R.string.no_base_face_bitmap);
                        break;
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        tipsTextView.setText(R.string.motion_liveness_detection_failed);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE:
                        VoicePlayer.getInstance().play(R.raw.open_mouse);
                        tipsTextView.setText(R.string.repeat_open_close_mouse);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SMILE: {
                        tipsTextView.setText(R.string.motion_smile);
                        VoicePlayer.getInstance().play(R.raw.smile);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.BLINK: {
                        VoicePlayer.getInstance().play(R.raw.blink);
                        tipsTextView.setText(R.string.motion_blink_eye);
                    }
                    break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD:
                        VoicePlayer.getInstance().play(R.raw.shake_head);
                        tipsTextView.setText(R.string.motion_shake_head);
                        break;

                    case VerifyStatus.ALIVE_DETECT_TYPE_ENUM.NOD_HEAD:
                        VoicePlayer.getInstance().play(R.raw.nod_head);
                        tipsTextView.setText(R.string.motion_node_head);
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
                        tipsTextView.setText(R.string.no_face_or_repeat_switch_screen);
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.stop_verify_tips)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    requireActivity().finish();
                                })
                                .show();

                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        secondTipsTextView.setText(R.string.no_face_detected_tips);
                        break;

                    // 单独使用一个textview 提示，防止上一个提示被覆盖。
                    // 也可以自行记住上个状态，FACE_SIZE_FIT 中恢复上一个提示
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE:
                        secondTipsTextView.setText(R.string.far_away_tips);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL:
                        secondTipsTextView.setText(R.string.come_closer_tips);
                        break;

                    //检测到正常的人脸，尺寸大小OK
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT:
                        secondTipsTextView.setText("");
                        break;

                }
            });
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
        faceVerifyUtils.pauseProcess();
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
     * 双目摄像头设置数据，送数据到SDK 引擎
     *
     * @param bitmap
     * @param type
     */
    void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type) {
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
