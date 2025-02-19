package com.ai.face.UVCCamera.addFace;

import static android.app.Activity.RESULT_OK;
import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.ai.face.UVCCamera.Constants.PREVIEW_HEIGHT;
import static com.ai.face.UVCCamera.Constants.PREVIEW_WIDTH;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD;
import static com.ai.face.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.fragment.app.Fragment;

import com.ai.face.R;
import com.ai.face.UVCCamera.camera.UsbCameraEnum;
import com.ai.face.UVCCamera.camera.UsbCameraManager;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.base.utils.DataConvertUtils;
import com.ai.face.databinding.FragmentBinocularCameraAddFaceBinding;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 打开USB RGB摄像头 添加人脸
 */
public class AddFaceUVCCameraFragment extends Fragment {

    private static final String TAG = "AddFace";

    public FragmentBinocularCameraAddFaceBinding binding;

    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;

    private final UsbCameraManager rgbCameraManager = new UsbCameraManager();//添加人脸只用到 RBG camera
    private final UsbCameraManager irCameraManager = new UsbCameraManager();

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    public AddFaceUVCCameraFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBinocularCameraAddFaceBinding.inflate(inflater, container, false);
        initView();
        return binding.getRoot();
    }

    private void initView() {
        initRGBCamara();
        addFaceInit();
        BrightnessUtil.setBrightness(requireActivity(), 1.0f);  //屏幕光当补光灯
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        irCameraManager.releaseCameraHelper();//释放 user camera 资源
        rgbCameraManager.releaseCameraHelper();//释放 file camera 资源
    }

    private void initRGBCamara() {
        rgbCameraManager.initCameraHelper();
        rgbCameraManager.setOpeningMultiCamera(true);
        rgbCameraManager.setCameraView(binding.rgbCameraTextureView,true);
        rgbCameraManager.selectUsbCamera(UsbCameraEnum.RGB);

        rgbCameraManager.setPreviewHeight(PREVIEW_HEIGHT);
        rgbCameraManager.setFrameCallback(new IFrameCallback() {
            @Override
            public void onFrame(ByteBuffer frame) {
                Size currentPreviewSize = rgbCameraManager.getCurrentPreviewSize();
                int width = PREVIEW_WIDTH;
                int height = PREVIEW_HEIGHT;
                if (currentPreviewSize != null) {
                    width = currentPreviewSize.width;
                    height = currentPreviewSize.height;
                }
                Bitmap bitmap = DataConvertUtils.NV21Data2Bitmap(frame, width, height, 0, 0, false);
                if (bitmap != null) {
                    baseImageDispose.dispose(bitmap);
                }
            }
        }, UVCCamera.PIXEL_FORMAT_NV21);
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

        tipsTextView = binding.tipsView;
        binding.back.setOnClickListener(v -> requireActivity().finish());

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
}
