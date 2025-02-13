package com.ai.face.UVCCameraNew;

import static android.view.View.VISIBLE;
import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.ai.face.FaceAIConfig;
import com.ai.face.R;
import com.ai.face.base.baseImage.FaceAIUtils;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.base.view.FaceCoverView;
import com.ai.face.databinding.FragmentBinocularCameraBinding;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.ai.face.faceVerify.verify.VerifyStatus;
import com.ai.face.faceVerify.verify.VerifyUtils;
import com.ai.face.faceVerify.verify.liveness.LivenessDetectionMode;
import com.ai.face.faceVerify.verify.liveness.LivenessType;
import com.ai.face.utils.VoicePlayer;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.MultiCameraFragment;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.utils.ToastUtils;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

/**
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 *
 */
public class BinocularUVCCameraFragment extends MultiCameraFragment implements ICameraStateCallBack {
    private static final String RBG_CAMERA = "RGB"; //RGB 摄像头名字中的关键字
    private static final String IR_CAMERA = "IR";   //IR 摄像头名字中的关键字

    private static final int PREVIEW_WIDTH=640,PREVIEW_HEIGHT=480;


    private FragmentBinocularCameraBinding mViewBinding;
    private MultiCameraClient.ICamera rgbCamera;
    private MultiCameraClient.ICamera irCamera;

    private TextView tipsTextView, secondTipsTextView, scoreText;
    private FaceCoverView faceCoverView;
    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();


    public BinocularUVCCameraFragment() {
        // Required empty public constructor
    }


    //下面是处理人脸识别相关处理
    /**
     * 初始化人脸识别底图
     *
     */
    private void initFaceVerifyBaseBitmap(){
        //1:1 人脸对比，摄像头实时采集的人脸和预留的人脸底片对比。（动作活体人脸检测完成后开始1:1比对）
        String yourUniQueFaceId = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);
        String savedFacePath = FaceAIConfig.CACHE_BASE_FACE_DIR + yourUniQueFaceId;
        //2.先去Path 路径读取有没有faceID 对应的人脸，如果没有从网络其他地方同步
        Bitmap baseBitmap = BitmapFactory.decodeFile(savedFacePath);

        if (baseBitmap != null) {
            //3.初始化引擎，各种参数配置
            initFaceVerificationParam(baseBitmap);
        } else {
            //人脸图的裁剪和保存最好提前完成，如果不是本SDK 录入的人脸可能人脸不标准
            //这里模拟从网络等地方获取，业务方自行决定，为了方便模拟我们放在Assert 目录
            Bitmap remoteBitmap = VerifyUtils.getBitmapFromAssert(requireActivity(), "yourFace.jpg");
            if (remoteBitmap==null){
                Toast.makeText(getContext(), R.string.add_a_face_image,Toast.LENGTH_LONG).show();
                tipsTextView.setText(R.string.add_a_face_image);
                return;
            }

            //人脸照片可能不是规范的正方形，非人脸区域过大甚至无人脸 多个人脸等情况，需要裁剪等处理
            //（检测人脸照片质量使用 checkFaceQuality方法，处理类同checkFaceQuality）
            FaceAIUtils.Companion.getInstance(requireActivity().getApplication())
                    .disposeBaseFaceImage(remoteBitmap, savedFacePath, new FaceAIUtils.Callback() {
                        @Override
                        public void onSuccess(@NonNull Bitmap cropedBitmap) {
                            initFaceVerificationParam(cropedBitmap);
                        }

                        /**
                         * VerifyStatus.
                         * @param msg
                         * @param errorCode VerifyStatus.VERIFY_DETECT_TIPS_ENUM.*
                         */
                        @Override
                        public void onFailed(@NotNull String msg, int errorCode) {
                            Log.e("ttt", msg);
                            Toast.makeText(getContext(),msg,Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }


    /**
     * 初始化认证引擎
     * <p>
     * 活体检测的使用需要你发送邮件申请，简要描述App名称，包名 签名SHA1和功能简介到 anylife.zlb@gmail.com
     *
     * @param baseBitmap 1:1 人脸识别对比的底片，如果仅仅需要活体检测，可以把App logo Bitmap 当参数传入并忽略对比结果
     */
    private void initFaceVerificationParam(Bitmap baseBitmap) {

        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(getContext())
                .setThreshold(0.88f)                    //阈值设置，范围限 [0.8 , 0.95] 识别可信度，也是识别灵敏度
                .setBaseBitmap(baseBitmap)              //1:1 人脸识别对比的底片，仅仅需要SDK活体检测可以忽略比对结果
                .setLivenessType(LivenessType.IR)   //活体检测可以有静默活体，动作活体或者组合也可以不需要活体NONE
                .setLivenessDetectionMode(LivenessDetectionMode.FAST)//硬件配置低用FAST动作活体模式，否则用精确模式
                .setSilentLivenessThreshold(0.88f)      //静默活体阈值 [0.88,0.99]
                .setVerifyTimeOut(10)                   //动作活体检测支持设置超时时间 [9,22] 秒
                .setMotionLivenessStepSize(1)           //StepSize
                .setProcessCallBack(new ProcessCallBack() {

                    /**
                     * 1:1 人脸识别 活体检测 对比结束
                     *
                     * @param isMatched   true 匹配成功（大于setThreshold）； false 与底片不是同一人
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
                    public void onFailed(int code,String message) {
                        Toast.makeText(getContext(),"onFailed错误："+message,Toast.LENGTH_LONG).show();
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
    private void showVerifyResult(boolean isVerifyMatched, float similarity, float silentLivenessScore) {
        requireActivity().runOnUiThread(() -> {
            scoreText.setText("SilentLivenessScore:" + silentLivenessScore);

            //1.静默活体分数判断
            if (silentLivenessScore < 0.9) {
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
    private void showFaceVerifyTips(int actionCode) {
        if (!requireActivity().isDestroyed() && !requireActivity().isFinishing()) {
            requireActivity().runOnUiThread(() -> {
                switch (actionCode) {

                    //5次相比阈值太低就判断为非同一人
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

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.ACTION_OK:
                        VoicePlayer.getInstance().play(R.raw.face_camera);
                        tipsTextView.setText(R.string.keep_face_visible);
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


                    //  @@@@@@@@@@@@@@ 下面是处理IR CAMERA 问题 @@@@@@@@@@@@@@@@@@
                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_IMAGE_NULL:
                        secondTipsTextView.setText(" IR_IMAGE_NULL ");

                        break;

                    case VerifyStatus.VERIFY_DETECT_TIPS_ENUM.IR_IMAGE_NO_FACE_BUT_RGB_HAVE:
                        secondTipsTextView.setText(" IR_IMAGE_NO_FACE_BUT_RGB_HAVE ");

                        break;


                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(faceVerifyUtils!=null){
            faceVerifyUtils.destroyProcess();
        }
    }



    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     */
//    protected void onPause() {
//        super.onPause();
//        faceVerifyUtils.pauseProcess();
//    }


    //下面是处理USB 摄像头相关
    @Override
    protected void onCameraAttached(@NonNull MultiCameraClient.ICamera camera) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //如果你的系统还小于Android 5.0 可能getProductName 会为空，自行适配
            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现",Toast.LENGTH_LONG).show();
            return;
        }

        String productName = camera.getUsbDevice().getProductName();
        if (!TextUtils.isEmpty(productName)&&productName.contains(RBG_CAMERA)) {
            rgbCamera = camera;
        }
        if (!TextUtils.isEmpty(productName)&&productName.contains(IR_CAMERA)) {
            irCamera = camera;
        }
        mViewBinding.multiCameraTip.setVisibility(View.GONE);
    }

    @Override
    protected void onCameraDetached(@NonNull MultiCameraClient.ICamera camera) {

        Log.d("onCameraDetached", "---  关闭 摄像头: --- ");

        camera.closeCamera();

        //两个都没有打开
        if (!irCamera.isCameraOpened() && !rgbCamera.isCameraOpened()) {
            mViewBinding.multiCameraTip.setVisibility(VISIBLE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String productName = camera.getUsbDevice().getProductName();
            if (!TextUtils.isEmpty(productName)&&productName.contains(RBG_CAMERA)) {
                rgbCamera = null;
            }
            if (!TextUtils.isEmpty(productName)&&productName.contains(IR_CAMERA)) {
                irCamera = null;
            }
        }else {
            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现细节",Toast.LENGTH_LONG).show();
        }
    }


    @NonNull
    @Override
    public MultiCameraClient.ICamera generateCamera(@NonNull Context context, @NonNull UsbDevice usbDevice) {
        return new CameraUVC(context, usbDevice);
    }


    @Override
    protected void onCameraConnected(@NonNull MultiCameraClient.ICamera camera) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String productName = camera.getUsbDevice().getProductName();
            Log.d("onCameraConnected", "---  发现新摄像头: --- "+productName);

            initFaceVerifyBaseBitmap();

            //处理 RGB Camera
            if (!TextUtils.isEmpty(productName)&&productName.contains(RBG_CAMERA)) {

                UsbDevice device = camera.getUsbDevice();
                if (!hasPermission(device)) {
                    requestPermission(device);
                }else{
                    CameraRequest cameraRequest=getCameraRequest();
                    camera.setRenderSize(PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    camera.openCamera(mViewBinding.rgbCameraTextureView,cameraRequest);
                    camera.setCameraStateCallBack(this);

                    camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
                        Bitmap bitmap= DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes),width,height,0,0,false);
                        if(bitmap!=null&&!requireActivity().isFinishing()&&!requireActivity().isDestroyed()){
                            faceVerifySetBitmap(bitmap,FaceVerifyUtils.BitmapType.RGB);
                        }
                    });
                }

            }


            //处理 IR Camera
            if (!TextUtils.isEmpty(productName)&&productName.contains(IR_CAMERA)) {
                UsbDevice device = camera.getUsbDevice();
                if (!hasPermission(device)) {
                    requestPermission(device);
                }else {
//                    camera.openCamera(mViewBinding.irCameraTextureView, getCameraRequest());
//                    camera.setCameraStateCallBack(this);
//                    camera.setRenderSize(PREVIEW_WIDTH,PREVIEW_HEIGHT);
//
//                    camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
//                        Bitmap bitmap= DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes),width,height,0,0,false);
//                        if(bitmap!=null&&!requireActivity().isFinishing()&&!requireActivity().isDestroyed()){
//                            faceVerifySetBitmap(bitmap,FaceVerifyUtils.BitmapType.IR);
//                        }
//                    });
                }
            }

        }else{
            Toast.makeText(getContext(),"SDK_INT<LOLLIPOP 请修改实现细节吧",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 请断点调试保证bitmap 的方向正确； RGB和IR Bitmap大小相同，画面同步
     *
     * @param bitmap
     * @param type
     */
    private Bitmap rgbBitmap,irBitmap;
    private boolean rgbReady=false,irReady=false;
    private void faceVerifySetBitmap(Bitmap bitmap, FaceVerifyUtils.BitmapType type){
        if(type.equals(FaceVerifyUtils.BitmapType.IR)){
            irBitmap=bitmap;
            irReady=true;
        }else if(type.equals(FaceVerifyUtils.BitmapType.RGB)){
            rgbBitmap=bitmap;
            rgbReady=true;
        }

        if(irReady&&rgbReady){
            faceVerifyUtils.goVerifyWithIR(irBitmap,rgbBitmap);
            irReady=false;
            rgbReady=false;
        }

    }


    @Override
    protected void onCameraDisConnected(@NonNull MultiCameraClient.ICamera iCamera) {
        iCamera.closeCamera();
    }


    @Override
    public void onCameraState(@NonNull MultiCameraClient.ICamera iCamera, @NonNull State code, @Nullable String s) {
        if (code == State.ERROR) {
            ToastUtils.show("UVC Camera 错误"+s );
        }
    }


    @Override
    protected void initView() {
        super.initView();

        scoreText =mViewBinding.silentScore;
        tipsTextView =mViewBinding.tipsView;
        secondTipsTextView =mViewBinding.secondTipsView;
        faceCoverView =mViewBinding.faceCover;

        openDebug(true);
    }

    @Nullable
    @Override
    protected View getRootView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup) {
        mViewBinding = FragmentBinocularCameraBinding.inflate(layoutInflater, viewGroup, false);
        return mViewBinding.getRoot();
    }

    private CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(PREVIEW_WIDTH)
                .setPreviewHeight(PREVIEW_HEIGHT)
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .create();
    }


}