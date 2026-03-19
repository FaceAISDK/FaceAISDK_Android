package com.faceAI.demo.SysCamera.search;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.faceAI.demo.R;
import com.faceAI.demo.base.utils.BitmapUtils;

public class ImageToast {

    /**
     * 接收 Base64 图 - (改名为 showBase64)
     */
    public Toast showBase64(Context context, String base64, String tips) {
        Bitmap bitmap = BitmapUtils.base64ToBitmap(base64);
        return showBitmap(context, bitmap, tips);
    }

    /**
     * 不需要图
     */
    public Toast show(Context context, String tips) {
        return showBitmap(context, null, tips);
    }

    /**
     * 接收 Bitmap 图文并茂 - (改名为 showBitmap)
     */
    public Toast showBitmap(Context context, Bitmap bitmap, String tips) {
        Toast toast = new Toast(context);
        View view = View.inflate(context, R.layout.face_toast_tips, null);
        ImageView image = view.findViewById(R.id.toast_image);
        TextView text = view.findViewById(R.id.toast_text);

        if (bitmap == null) {
            image.setVisibility(GONE);
        } else {
            image.setVisibility(VISIBLE);
            Glide.with(context)
                    .load(bitmap)
                    .transform(new RoundedCorners(44))
                    .into(image);
        }

        text.setText(tips);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 166);
        toast.show();
        return toast;
    }
}