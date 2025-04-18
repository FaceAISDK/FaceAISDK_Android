package com.ai.face.UVCCamera;

import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.ai.face.FaceAIConfig;
import com.ai.face.R;
import com.ai.face.base.baseImage.FaceAIUtils;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus;
import com.ai.face.faceVerify.verify.VerifyUtils;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode;
import com.ai.face.faceVerify.verify.liveness.MotionLivenessType;
import com.ai.face.utils.VoicePlayer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.jetbrains.annotations.NotNull;


/**
 * USB带红外双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 *
 * AbstractBinocularUVCCameraFragment 是摄像头相关处理，「调试的时候USB摄像头一定要固定住屏幕正上方」
 *
 * 默认LivenessType.IR需要你的摄像头是双目红外摄像头，如果仅仅是RGB 摄像头请使用LivenessType.SILENT_MOTION
 *
 * 更多UVC 摄像头使用参考 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class BinocularUVCCameraFragment extends AbstractBinocularUVCCameraFragment {

    private TextView tipsTextView, secondTipsTextView, scoreText;
    private FaceCoverView faceCoverView;
    private ImageView baseFaceImageView;
    private final float silentLivenessThreshold=0.85f;

    public BinocularUVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void initViews() {
        super.initViews();
        scoreText = binding.silentScore;
        tipsTextView = binding.tipsView;
        secondTipsTextView = binding.secondTipsView;
        faceCoverView = binding.faceCover;
        baseFaceImageView=binding.baseFace;
        binding.back.setOnClickListener(v -> requireActivity().finish());
        BrightnessUtil.setBrightness(requireActivity(), 1.0f);  //高亮白色背景屏幕光可以当补光灯
    }

    /**
     * 初始化人脸识别底图
     *
     */
    void initFaceVerifyBaseBitmap() {
        faceVerifyUtils = new FaceVerifyUtils();

        //1:1 人脸对比，摄像头实时采集的人脸和预留的人脸底片对比。（动作活体人脸检测完成后开始1:1比对）
        String yourUniQueFaceId = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);
        String savedFacePath = FaceAIConfig.CACHE_BASE_FACE_DIR + yourUniQueFaceId;

        //2.先去Path 路径读取有没有faceID 对应的处理好的人脸，如果没有从网络其他地方同步图片过来并进行合规处理
        Bitmap baseBitmap = BitmapFactory.decodeFile(savedFacePath);

        //如果本地已经有了合规处理好的人脸图
        if (baseBitmap != null) {
            //3.初始化引擎，各种参数配置
            initFaceVerificationParam(baseBitmap);
        } else {
            //模拟从网络等地方获取对应的人脸图，Demo 简化从Asset 目录读取
            Bitmap remoteBitmap = VerifyUtils.getBitmapFromAssert(requireActivity(), "0a_模拟证件照.jpeg");
            if (remoteBitmap == null) {
                Toast.makeText(getContext(), R.string.add_a_face_image, Toast.LENGTH_LONG).show();
                tipsTextView.setText(R.string.add_a_face_image);
                return;
            }

            //其他地方同步过来的人脸可能是不规范的没有经过校准的人脸图（证件照，多人脸，过小等）。disposeBaseFaceImage处理
            FaceAIUtils.Companion.getInstance(requireActivity().getApplication())
                    .disposeBaseFaceImage(remoteBitmap, savedFacePath, new FaceAIUtils.Callback() {
                        //处理优化人脸成功完成去初始化引擎
                        @Override
                        public void onSuccess(@NonNull Bitmap cropedBitmap) {
                            initFaceVerificationParam(cropedBitmap);
                        }

                        //底片处理异常的信息回调
                        @Override
                        public void onFailed(@NotNull String msg, int errorCode) {
                            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }


    /**
     * 初始化认证引擎，LivenessType.IR需要你的摄像头是双目红外摄像头，如果仅仅是RGB 摄像头请使用LivenessType.SILENT_MOTION
     *
     *
     * @param baseBitmap 1:1 人脸识别对比的底片
     */
    void initFaceVerificationParam(Bitmap baseBitmap) {
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getContext())
                .setThreshold(0.86f)                    //阈值设置，范围限 [0.8 , 0.95] 识别可信度，也是识别灵敏度
                .setBaseBitmap(baseBitmap)              //1:1 人脸识别对比的底片，仅仅需要SDK活体检测可以忽略比对结果
                .setLivenessType(MotionLivenessType.IR) //IR 是指红外静默，MOTION 是有动作可以指定1-2 个
                .setLivenessDetectionMode(MotionLivenessMode.FAST)//硬件配置低用FAST动作活体模式，否则用精确模式
                .setSilentLivenessThreshold(silentLivenessThreshold) //静默活体阈值 [0.8,0.99]
//                .setExceptMotionLivelessType(ALIVE_DETECT_TYPE_ENUM.SMILE) //动作活体去除微笑 或其他某一种
                .setMotionLivenessStepSize(1)           //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
                .setMotionLivenessTimeOut(12) //动作活体检测，支持设置超时时间 [9,22] 秒 。API 名字0410 修改
//                .setCompareDurationTime(5000) //动作活体通过后人脸对比时间，[3000,6000]毫秒。低配设备可以设置时间长一点，高配设备默认就行
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
                        faceCoverView.startCountDown(percent);
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

        Glide.with(requireActivity())
                .load(baseBitmap)
                .transform(new RoundedCorners(12))
                .into(baseFaceImageView);
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

            //1.静默活体分数判断 todo 最好SDK 自己判断
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
                        secondTipsTextView.setText("IR Camera Error");
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_IMAGE_NO_FACE_BUT_RGB_HAVE:
                        secondTipsTextView.setText(R.string.not_real_face);
                        break;


                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS:
                        tipsTextView.setText(R.string.face_verifying);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE:
                        tipsTextView.setText(R.string.no_face_detected_tips);
                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_NO_BASE_IMG:
                        tipsTextView.setText(R.string.no_base_face_bitmap);
                        break;
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_FAILED:
                        tipsTextView.setText(R.string.motion_liveness_detection_failed);
                        break;

//                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_OK:
//                        VoicePlayer.getInstance().play(R.raw.face_camera);
//                        tipsTextView.setText(R.string.keep_face_visible);
//                        break;

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
                                    //finish();  //是finish 还是retryVerify你自己定
                                    faceVerifyUtils.retryVerify();
                                })
                                .show();

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
//    public void onPause() {
//        super.onPause();
//        faceVerifyUtils.pauseProcess();
//    }


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
