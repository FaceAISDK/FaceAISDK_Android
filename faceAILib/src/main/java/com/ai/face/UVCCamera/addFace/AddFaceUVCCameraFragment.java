package com.ai.face.UVCCamera.addFace;

import static android.app.Activity.RESULT_OK;
import static androidx.camera.core.impl.utils.ContextUtil.getBaseContext;
import static com.ai.face.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.ai.face.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.UVCCamera.Constants.PREVIEW_HEIGHT;
import static com.ai.face.UVCCamera.Constants.PREVIEW_WIDTH;
import static com.ai.face.base.baseImage.BaseImageCallBack.AlIGN_FAILED;
import static com.ai.face.base.baseImage.BaseImageCallBack.MANY_FACE;
import static com.ai.face.base.baseImage.BaseImageCallBack.NOT_REAL_HUMAN;
import static com.ai.face.base.baseImage.BaseImageCallBack.NO_FACE;
import static com.ai.face.base.baseImage.BaseImageCallBack.SMALL_FACE;
import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE;
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
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 打开USB RGB摄像头 添加人脸
 * 更多UVC 摄像头参数设置 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class AddFaceUVCCameraFragment extends Fragment {
    private static final String TAG = "AddFace";

    public FragmentBinocularCameraAddFaceBinding binding;

    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;
    //如果启用活体检测，根据自身情况完善业务逻辑
    private boolean isRealFace = true;
    private final UsbCameraManager rgbCameraManager = new UsbCameraManager(); //添加人脸只用到 RBG camera
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
        irCameraManager.releaseCameraHelper();//释放 红外 camera 资源
        rgbCameraManager.releaseCameraHelper();//释放 RGB camera 资源
    }

    private void initRGBCamara() {
        rgbCameraManager.initCameraHelper();
        rgbCameraManager.setOpeningMultiCamera(true);
        rgbCameraManager.setCameraView(binding.rgbCameraTextureView, true);
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
    private void showConfirmDialog(Bitmap bitmap, float silentLiveValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(requireContext(), R.layout.dialog_confirm_base, null);

        //设置对话框布局
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(false);
        ImageView basePreView = dialogView.findViewById(R.id.preview);
        TextView realManTips = dialogView.findViewById(R.id.realManTips);

        if (!isRealFace) {
            realManTips.setVisibility(View.VISIBLE);
            realManTips.setText(getString(R.string.not_real_face_for_debug) + "(Value:" + silentLiveValue + ")");
        } else {
            realManTips.setVisibility(View.INVISIBLE);
        }
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
                    String faceName = editText.getText().toString() + ".jpg";
                    String filePathName = CACHE_SEARCH_FACE_DIR + faceName;
                    // 一定要用SDK API 进行添加删除，不要直接File 接口文件添加删除，不然无法同步人脸SDK中特征值的更新
                    FaceSearchImagesManger.Companion.getInstance(requireActivity().getApplication()).insertOrUpdateFaceImage(bitmap, filePathName, new FaceSearchImagesManger.Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "录入成功", Toast.LENGTH_SHORT).show();
                            requireActivity().finish();
                        }

                        @Override
                        public void onFailed(@NotNull String msg) {
                            Toast.makeText(requireContext(), "人脸图入库失败：：" + msg, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            } else {
                Toast.makeText(requireContext(), "Input FaceID Name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            isRealFace = true;
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
         * context 需要是
         *
         * 2 PERFORMANCE_MODE_ACCURATE 精确模式
         * 1 PERFORMANCE_MODE_FAST 快速模式
         */
        baseImageDispose = new BaseImageDispose(requireContext(), 2, new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap, float silentLiveValue) {
                requireActivity().runOnUiThread(() -> showConfirmDialog(bitmap, silentLiveValue));
            }

            @Override
            public void onProcessTips(int actionCode) {
                requireActivity().runOnUiThread(() -> {
                    AddFaceTips(actionCode);
                });
            }
        });
    }

    private void AddFaceTips(int actionCode) {
        switch (actionCode) {
            case NOT_REAL_HUMAN:
                Toast.makeText(requireContext(), R.string.not_real_face, Toast.LENGTH_LONG).show();
                binding.secondTipsView.setText(R.string.not_real_face);
                //公版Demo 为了方便调试不处理人脸活体，实际业务中请根据自身情况完善业务逻辑
                isRealFace = false;
                break;

            case CLOSE_EYE:
                tipsTextView.setText(R.string.no_close_eye_tips);
                break;

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

        }
    }


}
