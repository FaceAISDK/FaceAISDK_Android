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

/**
 * Toast Bitmap 和 text。UI可根据自身业务修改
 * https://github.com/FaceAISDK/FaceAISDK_Android
 */
public class ImageToast {

    public Toast show(Context context, String tips) {
        return show(context, null, tips);
    }


    public Toast show(Context context, Bitmap bitmap, String tips) {
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