package com.ai.face.search;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ai.face.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

/**
 * Toast Bitmap 和 text
 *
 * https://github.com/AnyLifeZLB/FaceVerificationSDK
 */
public class ImageToast {

    public Toast show(Context context, Bitmap bitmap, String tips) {
        Toast toast = new Toast(context);
        View view = View.inflate(context, R.layout.face_search_toast_tips, null);
        ImageView image = view.findViewById(R.id.toast_image);
        TextView text = view.findViewById(R.id.toast_text);

        Glide.with(context)
                .load(bitmap)
                .transform(new RoundedCorners(33))
                .into(image);

        text.setText(tips);

        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 166);
        toast.show();
        return toast;
    }

}