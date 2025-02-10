package com.ai.face.search;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.ai.face.base.view.CameraXFragment;
import com.ai.face.faceSearch.utils.FaceSearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 仅供参考，UI样式可以自行设计。甚至SurfaceView 改造
 */
public class GraphicOverlay extends View {
    private static final String TAG = "GraphicOverlay";
    private final Paint rectPaint = new Paint();
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private final Paint textPaint = new Paint();
    private List<FaceSearchResult> rectFList = new ArrayList<>();

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs) {
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
//            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        }
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (FaceSearchResult rectLabel : rectFList) {
            rectPaint.setColor(Color.WHITE);
            if (!TextUtils.isEmpty(rectLabel.getFaceName())) {
                rectPaint.setColor(Color.GREEN);
                textPaint.setTextSize(45.0f);
                textPaint.setTypeface(Typeface.DEFAULT);
                textPaint.setColor(Color.GREEN);
                String faceId = rectLabel.getFaceName().replace(".jpg", "");
                canvas.drawText(faceId + " ≈ " + rectLabel.getFaceScore(), rectLabel.getRect().left + 22.0f, rectLabel.getRect().top + 55.0f, textPaint);
            }
            rectPaint.setStrokeWidth(4.0f);
            rectPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rectLabel.getRect(), rectPaint);
        }
    }

    public void drawRect(List<FaceSearchResult> rectLabels, CameraXFragment cameraXFragment) {
        this.rectFList = adjustBoundingRect(rectLabels);
        this.scaleX = cameraXFragment.getScaleX();
        this.scaleY = cameraXFragment.getScaleY();
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
        int padding = 20;

        // 画框处理后期再优化
        for (FaceSearchResult rectLabel : rectLabels) {
            Rect rect = new Rect(
                    translateX(rectLabel.getRect().left) - padding,
                    translateY(rectLabel.getRect().top) - padding,
                    translateX(rectLabel.getRect().right) + padding,
                    translateY(rectLabel.getRect().bottom) + padding
            );

            labels.add(new FaceSearchResult(rect, rectLabel.getFaceName(), rectLabel.getFaceScore()));
        }

        return labels;
    }

}
