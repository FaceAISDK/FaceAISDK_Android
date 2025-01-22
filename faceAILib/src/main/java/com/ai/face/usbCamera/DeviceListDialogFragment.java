package com.ai.face.usbCamera;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.face.R;
import com.herohan.uvcapp.ICameraHelper;

import java.lang.ref.WeakReference;
import java.util.List;

public class DeviceListDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;
    private UsbDevice mUsbDevice;

    private OnDeviceItemSelectListener mOnDeviceItemSelectListener;

    private RecyclerView rvDeviceList;
    private TextView tvEmptyTip;

    private int selectIndex=-1;

    public DeviceListDialogFragment(ICameraHelper cameraHelper, UsbDevice usbDevice) {
        mCameraHelperWeak = new WeakReference<>(cameraHelper);
        mUsbDevice = usbDevice;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_device_list,null);
        rvDeviceList = view.findViewById(R.id.rvDeviceList);
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip);

        initDeviceList();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.device_list_dialog_title);
        builder.setView(view);
        builder.setNegativeButton(R.string.device_list_cancel_button, (dialog, which) -> {
            dismiss();
        });

//        builder.setPositiveButton(R.string.device_list_confirm_button, (dialog, which) -> {
//            if(selectIndex!=-1){
//                if (mOnDeviceItemSelectListener != null) {
//                    mOnDeviceItemSelectListener.onItemSelect(mCameraHelperWeak.get().getDeviceList().get(selectIndex));
//                }
//                dismiss();
//            }else{
//                Toast.makeText(getContext(),"请选择设备",Toast.LENGTH_SHORT).show();
//            }
//        });

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

                DeviceItemRecyclerViewAdapter adapter = new DeviceItemRecyclerViewAdapter(list, mUsbDevice);
                rvDeviceList.setAdapter(adapter);

                adapter.setOnItemClickListener((itemView, position) -> {
                    selectIndex=position;
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
