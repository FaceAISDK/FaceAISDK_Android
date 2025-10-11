package com.faceAI.demo.SysCamera.search;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 仅供参考，UI样式可以自行设计。横屏还没适配，优先级放低
 *
 * <a href="https://github.com/FaceAISDK/FaceAISDK_Android">...</a>
 */
public class FaceSearchGraphicOverlay extends View {
    private static final String TAG = "GraphicOverlay";
    private final Paint rectPaint = new Paint();
    private float scaleX = 1.0f, scaleY = 1.0f;
    private final Paint textPaint = new Paint();
    private List<FaceSearchResult> rectFList = new ArrayList<>();

    public FaceSearchGraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width < height) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }


    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (FaceSearchResult rectLabel : rectFList) {
            rectPaint.setColor(Color.GREEN);
            if (!TextUtils.isEmpty(rectLabel.getFaceName())) {
                rectPaint.setColor(Color.GREEN);
                textPaint.setTextSize(45.0f);
                textPaint.setTypeface(Typeface.DEFAULT);
                textPaint.setColor(Color.RED);
                String faceId = rectLabel.getFaceName().replace(".jpg", "");
                canvas.drawText(faceId + " ≈ " + rectLabel.getFaceScore(), rectLabel.getRect().left + 22.0f, rectLabel.getRect().top + 55.0f, textPaint);
            }
            rectPaint.setStrokeWidth(4.0f);
            rectPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rectLabel.getRect(), rectPaint);
        }
    }



    public void clearRect() {
        this.rectFList.clear();
        postInvalidate();
        requestLayout();
    }



    public void drawRect(List<FaceSearchResult> rectLabels, float scaleX,float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rectFList = adjustBoundingRect(rectLabels);
        postInvalidate();
        requestLayout();
    }

    private int translateX(int x) {
        return (int) (scaleX * x);
    }

    private int translateY(int y) {
        return (int) (scaleY * y);
    }

    private List<FaceSearchResult> adjustBoundingRect(List<FaceSearchResult> rectLabels) {
        List<FaceSearchResult> labels = new ArrayList<>();
        // 画框处理后期再优化
        for (FaceSearchResult rectLabel : rectLabels) {
            Rect rect = new Rect(
                    translateX(rectLabel.getRect().left),
                    translateY(rectLabel.getRect().top),
                    translateX(rectLabel.getRect().right),
                    translateY(rectLabel.getRect().bottom)
            );
            labels.add(new FaceSearchResult(rect, rectLabel.getFaceName(), rectLabel.getFaceScore()));
        }
        return labels;
    }


}
