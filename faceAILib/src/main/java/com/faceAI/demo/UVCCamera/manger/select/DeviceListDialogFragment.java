package com.faceAI.demo.UVCCamera.manger.select;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.faceAI.demo.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.herohan.uvcapp.ICameraHelper;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 识别的UVC 摄像头列表选择,仅仅为了演示兼容更多摄像头类型，调试完成后可以根据关键字写死匹配
 *
 */
public class DeviceListDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;
    private String mTips;

    private OnDeviceItemSelectListener mOnDeviceItemSelectListener;

    private RecyclerView rvDeviceList;
    private TextView tvEmptyTip;

    public DeviceListDialogFragment(ICameraHelper cameraHelper, String tips) {
        mCameraHelperWeak = new WeakReference<>(cameraHelper);
        mTips = tips;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_device_list,null);
        rvDeviceList = view.findViewById(R.id.rvDeviceList);
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip);

        initDeviceList();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(mTips);
        builder.setView(view);
        builder.setNegativeButton(R.string.device_list_cancel_button, (dialog, which) -> {
            dismiss();
        });
        return builder.create();
    }

    private void initDeviceList() {
        if (mCameraHelperWeak.get() != null) {
            List<UsbDevice> list = mCameraHelperWeak.get().getDeviceList();
            if (list == null || list.size() == 0) {
                rvDeviceList.setVisibility(View.GONE);
                tvEmptyTip.setVisibility(View.VISIBLE);
            } else {
                rvDeviceList.setVisibility(View.VISIBLE);
                tvEmptyTip.setVisibility(View.GONE);

                DeviceItemRecyclerViewAdapter adapter = new DeviceItemRecyclerViewAdapter(list);
                rvDeviceList.setAdapter(adapter);

                adapter.setOnItemClickListener((itemView, position) -> {
                    if (mOnDeviceItemSelectListener != null) {
                        mOnDeviceItemSelectListener.onItemSelect(list.get(position));
                    }
                    dismiss();
                });
            }
        }
    }

    public void setOnDeviceItemSelectListener(OnDeviceItemSelectListener listener) {
        mOnDeviceItemSelectListener = listener;
    }

    public interface OnDeviceItemSelectListener {
        void onItemSelect(UsbDevice usbDevice);
    }
}
