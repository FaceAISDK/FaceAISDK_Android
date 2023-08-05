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
import com.ai.face.faceSearch.utils.RectLabel;

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
    private List<RectLabel> rectFList = new ArrayList<>();   //List<RectF>

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width < height){
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        }else{
            super.onMeasure(heightMeasureSpec,widthMeasureSpec);
        }
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (RectLabel rectLabel : rectFList) {
            rectPaint.setColor(Color.WHITE);

            if (!TextUtils.isEmpty(rectLabel.getLabel())) {
                textPaint.setColor(Color.GREEN);
                rectPaint.setColor(Color.GREEN);
                textPaint.setTextSize(44.0f);
                textPaint.setTypeface(Typeface.DEFAULT);
                canvas.drawText(rectLabel.getLabel(), rectLabel.getRect().left + 22.0f, rectLabel.getRect().top + 55.0f, textPaint);
            }

            rectPaint.setStrokeWidth(3.0f);
            rectPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rectLabel.getRect(), rectPaint);
        }

    }

    public void drawRect(List<RectLabel> rectLabels, CameraXFragment cameraXFragment) {
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

    private List<RectLabel> adjustBoundingRect(List<RectLabel> rectLabels) {
        List<RectLabel> labels = new ArrayList<>();
        int padding = 10;

        //调整一点
        for (RectLabel rectLabel : rectLabels) {
            Rect rect = new Rect(
                    translateX(rectLabel.getRect().left)  ,
                    translateY(rectLabel.getRect().top) - padding,
                    translateX(rectLabel.getRect().right) ,
                    translateY(rectLabel.getRect().bottom) + padding
            );

            labels.add(new RectLabel(rect, rectLabel.getLabel()));
        }
        return labels;
    }




}
