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
 * 仅供参考，UI样式可以自行设计。建议使用SDK自带的GraphicOverlay
 *
 * <a href="https://github.com/FaceAISDK/FaceAISDK_Android">...</a>
 */
@Deprecated
public class FaceSearchGraphicOverlay extends View {
    private static final String TAG = "GraphicOverlay";
    private final Paint rectPaint = new Paint();
    private float scaleX = 1.0f, scaleY = 1.0f;
    private final Paint textPaint = new Paint();
    private List<FaceSearchResult> rectFList = new ArrayList<>();


    // 新增变量
    private boolean isFrontCamera = true; // 标记是否为前置摄像头，默认为是
    private int imageWidth;  // 摄像头用于人脸检测图像的宽度
    private int imageHeight; // 摄像头用于人脸检测图像的高度


    /**
     * 设置渲染所需的上下文信息,UVC Camera 也要设置对应分辨率
     *
     * @param imageWidth  摄像头预览的宽度 (例如 640)
     * @param imageHeight 摄像头预览的高度 (例如 480)
     * @param isFrontCamera 是否是前置摄像头
     */
    public void setCameraInfo(int imageWidth, int imageHeight, boolean isFrontCamera) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.isFrontCamera = isFrontCamera;
        // 在这里计算缩放比例
        // 假设预览内容是 fitCenter/fitStart, 以View的宽度为基准计算缩放
        // 注意：这里的计算方式取决于你的预览View的scaleType，可能需要调整
        scaleX = (float) getWidth() / (float) this.imageHeight;
        scaleY = (float) getHeight() / (float) this.imageWidth;

        // 如果你的预览是横向的，计算方式可能是下面这样
        // scaleX = (float) getWidth() / (float) this.previewWidth;
        // scaleY = (float) getHeight() / (float) this.previewHeight;
    }

    public FaceSearchGraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // 用下面的方法替换掉你原来的 onMeasure 方法
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (imageWidth == 0 || imageHeight == 0) {
            return;
        }

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        // 原始预览是横向的，在竖屏UI上，其宽高比需要颠倒
        float cameraRatio = (float) imageHeight / (float) imageWidth;

        int newWidth = measuredWidth;
        int newHeight = (int) (newWidth * cameraRatio);

        if (newHeight > measuredHeight) {
            newHeight = measuredHeight;
            newWidth = (int) (newHeight / cameraRatio);
        }

        setMeasuredDimension(newWidth, newHeight);
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
                canvas.drawText(faceId + " ≈ " + rectLabel.getFaceScore(), rectLabel.getRect().left + 20.0f, rectLabel.getRect().top + 50.0f, textPaint);
            }
            rectPaint.setStrokeWidth(4.0f);
            rectPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rectLabel.getRect(), rectPaint);
        }
    }


    /**
     * 外部不要调用了
     */
    public void clearRect() {
        this.rectFList.clear();
        postInvalidate();
    }


    public void drawRect(List<FaceSearchResult> rectLabels) {
        if(rectLabels.isEmpty()){
            clearRect();
        }

        this.rectFList = adjustBoundingRect(rectLabels);
        postInvalidate();
    }


    /**
     * 将原始X坐标转换为View中的X坐标
     * 如果是前置摄像头，需要进行水平镜像翻转
     */
    private int translateX(int x) {
        if (isFrontCamera) {
            // 镜像翻转公式：View宽度 - (原始坐标 * X轴缩放比例)
            return (int) (getWidth() - (x * scaleX));
        } else {
            return (int) (x * scaleX);
        }
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
