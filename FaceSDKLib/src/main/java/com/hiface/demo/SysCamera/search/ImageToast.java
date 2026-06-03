package com.hiface.demo.SysCamera.search;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.hiface.demo.R;
import com.hiface.demo.base.utils.BitmapUtils;

public class ImageToast {

    /**
     * 接收 Base64 图 - (改名为 showBase64)
     */
    public Toast showBase64(Context context, String base64, String tips) {
        Bitmap bitmap = BitmapUtils.base64ToBitmap(base64);
        return showBitmap(context, bitmap, tips);
    }

    public Toast show(Context context, String tips) {
        return showBitmap(context, null, tips);
    }

    public Toast showBitmap(Context context, Bitmap bitmap, String tips) {
        return showBitmap(context, bitmap, tips, true);
    }

    public Toast showBitmap(Context context, Bitmap bitmap, String tips, boolean isSuccess) {
        final Toast toast = new Toast(context);
        final View view = View.inflate(context, R.layout.face_toast_tips, null);
        if(!isSuccess){
            view.setBackgroundResource(R.drawable.circle_bar_bg_red);
        }

        ImageView image = view.findViewById(R.id.toast_image);
        TextView text = view.findViewById(R.id.toast_text);

        if (bitmap == null) {
            image.setVisibility(GONE);
        } else {
            image.setVisibility(VISIBLE);
            Glide.with(context)
                    .load(bitmap)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(new CenterCrop(), new RoundedCorners(22))
                    .into(image);
        }

        text.setText(tips);
        toast.setView(view);

        // 保持 LENGTH_SHORT（约 2 秒），确保有足够的时间留给我们的自定义动画
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 166);
        toast.show();

        // ================= 核心优化：1秒展示 + 500毫秒渐隐 =================
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 使用 ViewPropertyAnimator 执行 500ms 渐隐动画
                view.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                // 动画结束（此时 View 已完全透明），彻底释放并取消 Toast 窗口
                                try {
                                    toast.cancel();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .start();
            }
        }, 1000); // 1000 毫秒（1秒）后开始执行渐隐
        // =================================================================

        return toast;
    }
}