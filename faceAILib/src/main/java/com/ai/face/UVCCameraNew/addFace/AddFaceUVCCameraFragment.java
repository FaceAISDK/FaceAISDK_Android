package com.ai.face.UVCCameraNew.addFace;

import static android.app.Activity.RESULT_OK;
import static android.view.View.VISIBLE;

import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.ai.face.R;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentBinocularCameraAddFaceBinding;
import com.ai.face.databinding.FragmentBinocularCameraBinding;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.MultiCameraFragment;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.utils.ToastUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 打开双目摄像头（两个摄像头，camera.getUsbDevice().getProductName()监听输出名字），并获取预览数据进一步处理
 * 添加人脸仅仅用了RGB 摄像头
 */
public class AddFaceUVCCameraFragment extends MultiCameraFragment implements ICameraStateCallBack {
    public static final String RBG_CAMERA = "RGB"; //RGB 摄像头名字中的关键字
    public static final String IR_CAMERA = "IR";   //IR 摄像头名字中的关键字
    public static final int PREVIEW_WIDTH = 1280, PREVIEW_HEIGHT = 960; //请根据你的摄像头支持设定类似分辨率

    public MultiCameraClient.ICamera rgbCamera;
    public MultiCameraClient.ICamera irCamera;

    public FragmentBinocularCameraAddFaceBinding mViewBinding;

    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    public AddFaceUVCCameraFragment() {
        // Required empty public constructor
    }

    @Override
    protected void onCameraAttached(@NonNull MultiCameraClient.ICamera camera) {
        String productName = camera.getUsbDevice().getProductName();
        if (!TextUtils.isEmpty(productName) && productName.contains(RBG_CAMERA)) {
            rgbCamera = camera;
        }
        if (!TextUtils.isEmpty(productName) && productName.contains(IR_CAMERA)) {
            irCamera = camera;
        }
        mViewBinding.multiCameraTip.setVisibility(View.GONE);
    }

    @Override
    protected void onCameraDetached(@NonNull MultiCameraClient.ICamera camera) {
        camera.closeCamera();

        //两个摄像头都没有打开
        if (!irCamera.isCameraOpened() && !rgbCamera.isCameraOpened()) {
            mViewBinding.multiCameraTip.setVisibility(VISIBLE);
        }
    }


    @NonNull
    @Override
    public MultiCameraClient.ICamera generateCamera(@NonNull Context context, @NonNull UsbDevice usbDevice) {
        return new CameraUVC(context, usbDevice);
    }

    @Override
    protected void initView() {
        super.initView();
        openDebug(true);

        BrightnessUtil.setBrightness(requireActivity(), 1.0f);
        addFaceInit();
    }

    /**
     * 确认是否保存人脸底图
     */
    private void showConfirmDialog(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(requireContext(), R.layout.dialog_confirm_base, null);

        //设置对话框布局
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(false);
        ImageView basePreView = dialogView.findViewById(R.id.preview);
        basePreView.setImageBitmap(bitmap);

        Button btnOK = dialogView.findViewById(R.id.btn_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.requestFocus();
        editText.setText(faceID);
        btnOK.setOnClickListener(v -> {
            faceID = editText.getText().toString();

            if (!TextUtils.isEmpty(faceID)) {
                if (addFaceImageType.equals(AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                    Toast.makeText(requireContext(), "Add 1:1 Face Image Finish", Toast.LENGTH_SHORT).show();
                    //1:1 人脸识别保存人脸底图
                    baseImageDispose.saveBaseImage(bitmap, CACHE_BASE_FACE_DIR, faceID, 300);
                    dialog.dismiss();
                    requireActivity().finish();
                } else {
                    //1:N ，M：N 人脸搜索保存人脸
                    dialog.dismiss();
                    Intent intent = new Intent();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] bitmapByte = baos.toByteArray();
                    intent.putExtra("picture_data", bitmapByte);
                    intent.putExtra("picture_name", editText.getText().toString());
                    requireActivity().setResult(RESULT_OK, intent);
                    requireActivity().finish();
                }
            } else {
                Toast.makeText(requireContext(), "Input FaceID Name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            baseImageDispose.retry();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    private void addFaceInit() {

        tipsTextView = mViewBinding.tipsView;
        mViewBinding.back.setOnClickListener(v -> requireActivity().finish());

        addFaceImageType = requireActivity().getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);
        faceID = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);

        /**
         * BaseImageDispose 第一个参数是否启用活体检测，录入照片没必要，部分定制SDK 会需要
         * context 需要是
         */
        baseImageDispose = new BaseImageDispose(false, requireContext(), new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                requireActivity().runOnUiThread(() -> showConfirmDialog(bitmap));
            }

            @Override
            public void onProcessTips(int actionCode) {
                requireActivity().runOnUiThread(() -> {
                    switch (actionCode) {
                        case HEAD_CENTER:
                            tipsTextView.setText(R.string.keep_face_tips); //2秒后确认图像
                            break;
                        case TILT_HEAD:
                            tipsTextView.setText(R.string.no_tilt_head_tips);
                            break;

                        case HEAD_LEFT:
                            tipsTextView.setText(R.string.head_turn_left_tips);
                            break;
                        case HEAD_RIGHT:
                            tipsTextView.setText(R.string.head_turn_right_tips);
                            break;
                        case HEAD_UP:
                            tipsTextView.setText(R.string.no_look_up_tips);
                            break;
                        case HEAD_DOWN:
                            tipsTextView.setText(R.string.no_look_down_tips);
                            break;
                        case NO_FACE:
                            tipsTextView.setText(R.string.no_face_detected_tips);
                            break;
                        case MANY_FACE:
                            tipsTextView.setText(R.string.multiple_faces_tips);
                            break;
                        case SMALL_FACE:
                            tipsTextView.setText(R.string.come_closer_tips);
                            break;
                        case AlIGN_FAILED:
                            tipsTextView.setText(R.string.align_face_error_tips);
                            break;
                        case NOT_REAL_HUMAN:
                            tipsTextView.setText(R.string.not_real_face);
                            break;
                    }
                });
            }
        });
    }


    @Override
    protected void onCameraConnected(@NonNull MultiCameraClient.ICamera camera) {
        String productName = camera.getUsbDevice().getProductName();
        Log.d("onCameraConnected", "---  发现新摄像头: --- " + productName);

        //处理 RGB Camera
        if (!TextUtils.isEmpty(productName) && productName.contains(RBG_CAMERA)) {
            camera.openCamera(mViewBinding.rgbCameraTextureView, getCameraRequest());
            camera.setCameraStateCallBack(this);
            camera.setRenderSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
            camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
                Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes), width, height, 0, 0, false);
                if (bitmap != null) {
                    baseImageDispose.dispose(bitmap);
                }
            });

            UsbDevice device = camera.getUsbDevice();
            if (!hasPermission(device)) {
                requestPermission(device);
            }
        }

        //处理 IR Camera
        if (!TextUtils.isEmpty(productName) && productName.contains(IR_CAMERA)) {
            camera.openCamera(mViewBinding.irCameraTextureView, getCameraRequest());
            camera.setCameraStateCallBack(this);
            camera.setRenderSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
            camera.addPreviewDataCallBack((bytes, width, height, dataFormat) -> {
                // 人脸录入IR 数据不用处理
                Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(ByteBuffer.wrap(bytes), width, height, 0, 0, false);
            });

            UsbDevice device = camera.getUsbDevice();
            if (!hasPermission(device)) {
                requestPermission(device);
            }
        }

    }


    @Override
    protected void onCameraDisConnected(@NonNull MultiCameraClient.ICamera iCamera) {
        iCamera.closeCamera();
    }


    @Override
    public void onCameraState(@NonNull MultiCameraClient.ICamera iCamera, @NonNull State code, @Nullable String s) {
        if (code == State.ERROR) {
            ToastUtils.show("UVC Camera 错误" + s);
        }
    }


    @Nullable
    @Override
    protected View getRootView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup) {
        mViewBinding = FragmentBinocularCameraAddFaceBinding.inflate(layoutInflater, viewGroup, false);
        return mViewBinding.getRoot();
    }

    private CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(PREVIEW_WIDTH)
                .setPreviewHeight(PREVIEW_HEIGHT)
                .setRenderMode(CameraRequest.RenderMode.NORMAL) //NV21
                .create();
    }


}