package com.faceAI.demo.UVCCamera.addFace;

import static android.view.View.GONE;
import static com.ai.face.base.baseImage.BaseImageDispose.PERFORMANCE_MODE_FAST;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_MANY;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY;
import static com.faceAI.demo.FaceImageConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceImageConfig.CACHE_SEARCH_FACE_DIR;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import com.ai.face.base.baseImage.FaceEmbedding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity;
import com.faceAI.demo.UVCCamera.manger.CameraBuilder;
import com.faceAI.demo.UVCCamera.manger.UVCCameraManager;
import com.ai.face.base.baseImage.BaseImageCallBack;
import com.ai.face.base.baseImage.BaseImageDispose;
import com.ai.face.faceSearch.search.FaceSearchImagesManger;
import com.faceAI.demo.databinding.FragmentUvcCameraAddFaceBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 打开USB RGB摄像头 添加人脸
 * 更多UVC 摄像头参数设置 https://blog.csdn.net/hanshiying007/article/details/124118486
 * @author FaceAISDK.Service@gmail.com
 */
public class AddFace_UVCCameraFragment extends Fragment {
    private static final String TAG = "AddFace";
    public FragmentUvcCameraAddFaceBinding binding;
    public static String ADD_FACE_IMAGE_TYPE_KEY = "ADD_FACE_IMAGE_TYPE_KEY";
    private TextView tipsTextView;
    private BaseImageDispose baseImageDispose;
    private String faceID, addFaceImageType;
    private UVCCameraManager rgbCameraManager ; //添加人脸只用到 RBG camera
    private UVCCameraManager irCameraManager;
    private boolean isConfirmAdd = false; //确认期间停止人脸检测

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
        SharedPreferences sp = requireContext().getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);

        String s=sp.getString(RGB_UVC_CAMERA_SELECT,RGB_KEY_DEFAULT);
        CameraBuilder cameraBuilder = new CameraBuilder.Builder()
                .setCameraName("普通RGB摄像头")
                .setCameraKey(s)
                .setCameraView(binding.rgbCameraView)
                .setContext(requireContext())
                .setDegree(sp.getInt(RGB_UVC_CAMERA_DEGREE,0))
                .setHorizontalMirror(sp.getBoolean(RGB_UVC_CAMERA_MIRROR_H, false))
                .build();

        rgbCameraManager=new UVCCameraManager(cameraBuilder);

        rgbCameraManager.setFaceAIAnalysis(new UVCCameraManager.OnFaceAIAnalysisCallBack() {
            @Override
            public void onBitmapFrame(Bitmap bitmap) {
                if(!isConfirmAdd){
                    baseImageDispose.dispose(bitmap);
                }
            }
        });

    }


    /**
     * 确认人脸图
     *
     * @param bitmap 符合对应参数设置的SDK裁剪好的人脸图
     * @param silentLiveValue 静默活体分数，和摄像头有关，自行根据业务需求处理
     */
    private void confirmAddFaceDialog(Bitmap bitmap, float silentLiveValue) {
        ConfirmFaceDialog confirmFaceDialog=new ConfirmFaceDialog(requireContext(),bitmap,silentLiveValue);

        confirmFaceDialog.btnConfirm.setOnClickListener(v -> {
            faceID = confirmFaceDialog.faceIDEdit.getText().toString();

            if (!TextUtils.isEmpty(faceID)) {
                if (addFaceImageType.equals(AddFaceImageActivity.AddFaceImageTypeEnum.FACE_VERIFY.name())) {
                    float[] faceEmbedding = baseImageDispose.saveBaseImageGetEmbedding(bitmap, CACHE_BASE_FACE_DIR, faceID);//保存人脸底图,并返回人脸特征向量
                    FaceEmbedding.saveEmbedding(requireContext(),faceID,faceEmbedding); //保存特征向量
                    Toast.makeText(requireContext(), "录入成功", Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                } else {
                    //人脸搜索(1:N ，M：N )保存人脸
                    String faceName = confirmFaceDialog.faceIDEdit.getText().toString() + ".jpg";
                    String filePathName = CACHE_SEARCH_FACE_DIR + faceName;
                    // 一定要用SDK API 进行添加删除，不要直接File 接口文件添加删除，不然无法同步人脸SDK中特征值的更新
                    FaceSearchImagesManger.Companion.getInstance(requireActivity().getApplication())
                            .insertOrUpdateFaceImage(bitmap, filePathName, new FaceSearchImagesManger.Callback() {
                                @Override
                                public void onSuccess(@NonNull Bitmap bitmap, @NonNull float[] faceEmbedding) {
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
                Toast.makeText(requireContext(), R.string.input_face_id_tips, Toast.LENGTH_SHORT).show();
            }
        });

        confirmFaceDialog.btnCancel.setOnClickListener(v -> {
            confirmFaceDialog.dialog.dismiss();
            baseImageDispose.retry();
            isConfirmAdd=false;
        });

        confirmFaceDialog.dialog.show();
    }


    /**
     * 人脸确认框View 管理
     */
    public class ConfirmFaceDialog{
        public AlertDialog dialog;
        public Button btnConfirm,btnCancel;
        public EditText faceIDEdit;
        public ConfirmFaceDialog(Context context,Bitmap bitmap,float silentLiveValue){
            dialog = new AlertDialog.Builder(context).create();
            View dialogView = View.inflate(context, R.layout.dialog_confirm_base, null);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setView(dialogView);
            dialog.setCanceledOnTouchOutside(false);
            ImageView basePreView = dialogView.findViewById(R.id.preview);
            Glide.with(context)
                    .load(bitmap)
                    .transform(new RoundedCorners(22))
                    .into(basePreView);
            btnConfirm = dialogView.findViewById(R.id.btn_ok);
            btnCancel = dialogView.findViewById(R.id.btn_cancel);
            faceIDEdit = dialogView.findViewById(R.id.edit_text);
            faceIDEdit.setText(faceID);
            if (addFaceImageType.equals(AddFaceImageActivity.AddFaceImageTypeEnum.FACE_VERIFY.name()) && !TextUtils.isEmpty(faceID)) {
                faceIDEdit.setVisibility(GONE); //制作UTS等插件传过来的FaceID,用户不能再二次编辑
            }else {
                faceIDEdit.requestFocus();
            }
            TextView livenessScore = dialogView.findViewById(R.id.liveness_score);
            livenessScore.setText("Liveness Score: "+ silentLiveValue);
        }

        public void show(){
            dialog.show();
        }

        public void dismiss(){
            dialog.dismiss();
        }
    }



    private void addFaceInit() {
        tipsTextView = binding.tipsView;
        binding.back.setOnClickListener(v -> requireActivity().finish());
        addFaceImageType = requireActivity().getIntent().getStringExtra(ADD_FACE_IMAGE_TYPE_KEY);
        faceID = requireActivity().getIntent().getStringExtra(USER_FACE_ID_KEY);

        /**
         * context 需要是
         *
         * 2 PERFORMANCE_MODE_ACCURATE 精确模式 人脸要正对摄像头，严格要求
         * 1 PERFORMANCE_MODE_FAST 快速模式 允许人脸方位可以有一定的偏移
         * 0 PERFORMANCE_MODE_EASY 简单模式 允许人脸方位可以「较大」的偏移
         */
        baseImageDispose = new BaseImageDispose(requireContext(), PERFORMANCE_MODE_FAST, new BaseImageCallBack() {
            @Override
            public void onCompleted(Bitmap bitmap, float silentLiveValue) {
                isConfirmAdd=true;
                requireActivity().runOnUiThread(() -> confirmAddFaceDialog(bitmap, silentLiveValue));
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
            //整理返回提示，2025.0815
            case NO_FACE_REPEATEDLY:
                tipsTextView.setText(R.string.no_face_detected_tips);
                break;
            case FACE_TOO_MANY:
                tipsTextView.setText(R.string.multiple_faces_tips);
                break;
            case FACE_TOO_SMALL:
                tipsTextView.setText(R.string.come_closer_tips);
                break;
            case FACE_TOO_LARGE:
                tipsTextView.setText(R.string.far_away_tips);
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
        }
    }

}
