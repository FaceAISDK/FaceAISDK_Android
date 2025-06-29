package com.faceAI.demo.UVCCamera.addFace;

import static com.faceAI.demo.FaceAIConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceAIConfig.CACHE_SEARCH_FACE_DIR;
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
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_DEGREE;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_MIRROR_H;
import static com.faceAI.demo.FaceAISettingsActivity.RGB_UVC_CAMERA_SELECT;
import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;
import static com.faceAI.demo.UVCCamera.manger.UVCCameraManager.RGB_KEY_DEFAULT;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.faceAI.demo.R;
import com.faceAI.demo.UVCCamera.manger.CameraBuilder;
import com.faceAI.demo.UVCCamera.manger.UVCCameraManager;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.base.utils.BrightnessUtil;
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.faceAI.demo.databinding.FragmentUvcCameraAddFaceBinding;

import org.jetbrains.annotations.NotNull;

/**
 * 打开USB RGB摄像头 添加人脸
 * 更多UVC 摄像头参数设置 https://blog.csdn.net/hanshiying007/article/details/124118486
 */
public class AddFace_UVCCameraFragment extends Fragment {
    private static final String TAG = "AddFace";
    public FragmentUvcCameraAddFaceBinding binding;
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;
    //如果启用活体检测，根据自身情况完善业务逻辑
    private boolean isRealFace = true;
    private UVCCameraManager rgbCameraManager ; //添加人脸只用到 RBG camera
    private UVCCameraManager irCameraManager;

    //是1:1 还是1:N 人脸搜索添加人脸
    public enum AddFaceImageTypeEnum {
        FACE_VERIFY, FACE_SEARCH;
    }

    public AddFace_UVCCameraFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUvcCameraAddFaceBinding.inflate(inflater, container, false);
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
        rgbCameraManager.releaseCameraHelper();//释放 RGB camera 资源
        if(irCameraManager!=null){
            irCameraManager.releaseCameraHelper();
        }
    }

    private void initRGBCamara() {
        SharedPreferences sp = requireContext().getSharedPreferences("FaceAISDK", Context.MODE_PRIVATE);

        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("普通RGB摄像头")
                .setCameraKey(sp.getString(RGB_UVC_CAMERA_SELECT,RGB_KEY_DEFAULT))
                .setCameraView(binding.rgbCameraView)
                .setContext(requireContext())
                .setDegree(sp.getInt(RGB_UVC_CAMERA_DEGREE,0))
                .setHorizontalMirror(sp.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager=new UVCCameraManager(cameraBuilder);

        rgbCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                baseImageDispose.dispose(bitmap);
            }
        });

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
