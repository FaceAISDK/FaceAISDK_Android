package com.faceAI.demo.UVCCamera.manger.select;
import com.faceAI.demo.R;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link UsbDevice}.
 */
public class DeviceItemRecyclerViewAdapter extends RecyclerView.Adapter<DeviceItemRecyclerViewAdapter.ViewHolder> {

    private final List<UsbDevice> mValues;
    private OnItemClickListener mOnItemClickListener;

    public DeviceItemRecyclerViewAdapter(List<UsbDevice> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_device_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
//        holder.rbDeviceSelected.setChecked(holder.mItem.equals(mCurrentItem));
        holder.tvProductName.setText(holder.mItem.getProductName() != null ? holder.mItem.getProductName() : holder.mItem.getManufacturerName());
        holder.tvDeviceName.setText(holder.mItem.getDeviceName());
        holder.mRootView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mRootView;
        public final RadioButton rbDeviceSelected;
        public final TextView tvProductName;
        public final TextView tvDeviceName;
        public UsbDevice mItem;

        public ViewHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            rbDeviceSelected = itemView.findViewById(R.id.rbDeviceSelected);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }
}