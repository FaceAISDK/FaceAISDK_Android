package com.hiface.demo.UVCCamera.verify;

import static com.hiface.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import static com.sdk.hiface.recognize.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.*;
import static com.sdk.hiface.recognize.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.*;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.sdk.hiface.core.utils.FaceAICameraType;
import com.sdk.hiface.recognize.liveness.FaceLivenessType;
import com.sdk.hiface.recognize.FaceProcessBuilder;
import com.sdk.hiface.recognize.FaceVerifyUtils;
import com.sdk.hiface.recognize.ProcessCallBack;
import com.hiface.demo.SysCamera.search.ImageToast;
import com.hiface.demo.base.utils.TTSPlayer;
import com.hiface.demo.R;
import com.tencent.mmkv.MMKV;

/**
 * 演示UVC协议USB摄像头1:1人脸识别，活体检测
 */
public class FaceVerify_UVCCameraFragment extends AbsFaceVerify_UVCCameraFragment {
    private TextView tipsTextView, secondTipsTextView, scoreText;
    private final FaceLivenessType faceLivenessType = FaceLivenessType.MOTION;  //活体检测类型

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
    }


    /**
     * 初始化人脸识别底图 人脸特征值
     */
    void initFaceVerifyFeature() {
        String faceID = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);
        String faceFeature = MMKV.defaultMMKV().decodeString(faceID);
        if (!TextUtils.isEmpty(faceFeature)) {
            initFaceVerificationParam(faceFeature);
        } else {
            Toast.makeText(requireContext(), "faceFeature isEmpty ! ", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 初始化认证引擎
     */
    void initFaceVerificationParam(String faceFeature) {
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getContext())
                .setThreshold(0.8f)
                .setFaceFeature(faceFeature)
                .setCameraType(cameraType)
                .setLivenessType(faceLivenessType)
                .setMotionLivenessStepSize(1)
                .setMotionLivenessTimeOut(12)
                .setStopVerifyNoFaceRealTime(false)
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float livenessValue, Bitmap bitmap) {
                        showVerifyResult(isMatched, similarity, livenessValue);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showFaceVerifyTips(i);
                    }

                    @Override
                    public void onTimeCountDown(float percent) {
                    }

                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getContext(), "onFailed错误：" + message, Toast.LENGTH_LONG).show();
                    }
                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);
    }

    void  showVerifyResult(boolean isVerifyMatched, float similarity, float livenessValue) {
        if (isVerifyMatched&&(livenessValue>0.75||faceLivenessType.equals(FaceLivenessType.NONE))) {
            TTSPlayer.getInstance().playTTS(R.string.face_verify_success);
            new ImageToast().show(requireContext(), getString(R.string.face_verify_success));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                requireActivity().finish();
            }, 500);
        } else {
            TTSPlayer.getInstance().playTTS(R.string.face_verify_failed);
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.face_verify_failed_title)
                    .setMessage(R.string.face_verify_failed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.know, (dialogInterface, i) -> {
                        requireActivity().finish();
            }).setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify()).show();
        }
    }

    void showFaceVerifyTips(int actionCode) {
        if (!requireActivity().isDestroyed() && !requireActivity().isFinishing()) {
                switch (actionCode) {
                    case MOTION_LIVE_SUCCESS:
                        TTSPlayer.getInstance().playTTS(R.string.keep_face_visible);
                        setMainTips(R.string.keep_face_visible);
                        break;
                    case IR_IMAGE_NULL:
                        setMainTips(R.string.ir_image_error);
                        break;
                    case IR_LIVE_FAILED:
                        setMainTips(R.string.ir_live_error);
                        break;
                    case ACTION_PROCESS:
                        setMainTips(R.string.face_verifying);
                        break;
                    case OPEN_MOUSE:
                        TTSPlayer.getInstance().playTTS(R.string.repeat_open_close_mouse);
                        setMainTips(R.string.repeat_open_close_mouse);
                        break;
                    case SMILE:
                        setMainTips(R.string.motion_smile);
                        TTSPlayer.getInstance().playTTS(R.string.motion_smile);
                        break;
                    case BLINK:
                        TTSPlayer.getInstance().playTTS(R.string.motion_blink_eye);
                        setMainTips(R.string.motion_blink_eye);
                        break;
                    case SHAKE_HEAD:
                        setMainTips(R.string.motion_shake_head);
                        setMainTips(R.string.motion_shake_head);
                        break;
                    case NOD_HEAD:
                        setMainTips(R.string.motion_node_head);
                        setMainTips(R.string.motion_node_head);
                        break;
                    case MOTION_LIVE_TIMEOUT:
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.motion_liveness_detection_time_out)
                                .setCancelable(false)
                                .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                            faceVerifyUtils.retryVerify();
                                        }
                                ).show();
                        break;
                    case NO_FACE_REPEATEDLY:
                        setMainTips(R.string.no_face_or_repeat_switch_screen);
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.stop_verify_tips)
                                .setCancelable(false)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    requireActivity().finish();
                                })
                                .show();
                        break;
                    case FACE_TOO_LARGE:
                        setSecondTips(R.string.far_away_tips);
                        break;
                    case FACE_TOO_SMALL:
                        setSecondTips(R.string.come_closer_tips);
                        break;
                    case FACE_SIZE_FIT:
                        setSecondTips(0);
                        break;
                }
        }
    }

    private Bitmap rgbBitmap, irBitmap;
    private boolean rgbReady = false, irReady = false;

    @Override
    void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type) {
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
                faceVerifyUtils.goVerifyWithIR(irBitmap, rgbBitmap);
                irReady = false;
                rgbReady = false;
            }
        }
    }

    private void setMainTips(int resId) {
        tipsTextView.setText(resId);
    }

    private void setSecondTips(int resId) {
        if (resId == 0) {
            secondTipsTextView.setText("");
        } else {
            secondTipsTextView.setText(resId);
        }
    }
}
