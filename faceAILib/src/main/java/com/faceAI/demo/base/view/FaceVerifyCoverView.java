package com.faceAI.demo.base.view;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.faceAI.demo.R;
import com.faceAI.demo.base.utils.ScreenUtils;

/**
 * [性能优化版] 人脸识别覆盖视图
 * 优化点：移除 saveLayer 离屏渲染，改用 Path 奇偶填充规则实现挖孔
 */
public class FaceVerifyCoverView extends View {

    // --- 核心属性 ---
    private int mFlashColor;
    private int mStartColor;
    private int mEndColor;
    private boolean mShowProgress;
    private int mCircleMargin;
    private int mCirclePaddingBottom;

    // --- 尺寸相关 ---
    private final Point mCenterPoint = new Point();
    private float mTargetRadius;
    private float mCurrentRadius = 0;
    private float mBgArcWidth;

    // --- 绘制对象 ---
    private Paint mBackgroundPaint;   // 背景画笔
    private Paint mBgArcPaint;        // 进度条底色画笔
    private Paint mProgressPaint;     // 进度条画笔

    // 优化：使用 Path 实现挖孔，代替 Xfermode
    private final Path mHolePath = new Path();
    private final RectF mFullRect = new RectF(); // 视图全屏区域
    private final RectF mArcRectF = new RectF(); // 进度条区域

    private SweepGradient mSweepGradient;
    private final Matrix mGradientMatrix = new Matrix(); // 用于旋转渐变

    // --- 动画 ---
    private ValueAnimator mOpenAnimator;
    private float mCurrentProgressAngle = 0;

    // 调整起始角度：270度代表从12点钟方向开始
    private static final int START_ANGLE = 270;
    private static final int MAX_ANGLE = 360;

    public FaceVerifyCoverView(Context context) {
        this(context, null);
    }

    public FaceVerifyCoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceVerifyCoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initPaints(context);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FaceVerifyCoverView);
            mCircleMargin = array.getDimensionPixelSize(R.styleable.FaceVerifyCoverView_circle_margin, 30);
            mCirclePaddingBottom = array.getDimensionPixelSize(R.styleable.FaceVerifyCoverView_circle_padding_bottom, 0);
            mFlashColor = array.getColor(R.styleable.FaceVerifyCoverView_flash_color, Color.WHITE);
            mStartColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_start_color, Color.LTGRAY);
            mEndColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_end_color, Color.LTGRAY);
            mShowProgress = array.getBoolean(R.styleable.FaceVerifyCoverView_show_progress, true);
            array.recycle();
        } else {
            mCircleMargin = 33;
            mCirclePaddingBottom = 0;
            mFlashColor = Color.WHITE;
            mStartColor = Color.LTGRAY;
            mEndColor = Color.LTGRAY;
            mShowProgress = true;
        }


    }

    private void initPaints(Context context) {
        mBgArcWidth = ScreenUtils.dp2px(context, 2);

        // 1. 背景画笔 (直接画出带孔的背景)
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setColor(mFlashColor);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        // 2. 进度条底色
        mBgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgArcPaint.setColor(ContextCompat.getColor(context, R.color.half_grey));
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 3. 进度条
        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mBgArcWidth);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mCircleMargin=mCircleMargin+getScreenAspectRatioDx();

        // 记录全屏范围
        mFullRect.set(0, 0, w, h);

        // 计算圆心 X Y
        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2 - mCirclePaddingBottom;


        // 最大半径
        mTargetRadius = min(w/2f, h / 2f) - mCircleMargin;

        // 预计算进度条区域
        float halfStroke = mBgArcWidth / 2f;
        mArcRectF.set(
                mCenterPoint.x - mTargetRadius - halfStroke,
                mCenterPoint.y - mTargetRadius - halfStroke,
                mCenterPoint.x + mTargetRadius + halfStroke,
                mCenterPoint.y + mTargetRadius + halfStroke
        );

        // 初始化渐变 & 矩阵旋转
        updateGradient();
    }


    /**
     * 根据屏幕宽高比计算 0-10 的评分
     * 10分 = 16:9 (传统屏幕，较短)
     * 0分  = 20:9 (全面屏，很长)
     * * @param width  屏幕宽 (px)
     * @return 0 到 10 之间的整数
     */
    public  int getScreenAspectRatioDx() {
        float score;
        int height = max(getHeight(),getWidth());
        int width = min(getHeight(),getWidth());

        // 1. 计算当前宽高比 (注意转为 float)
        float currentRatio = (float) width / height;

        // 2. 定义阈值
        final float RATIO_9_16 = 9f / 16f; // 0.5625 (较宽/短) -> 对应 10 分
        final float RATIO_9_20 = 9f / 20f; // 0.4500 (较窄/长) -> 对应 0 分

        // 3. 处理边界情况
        if (currentRatio >= RATIO_9_16) {
            score= 12;
        }else if (currentRatio <= RATIO_9_20) {
            score = 0;
        }else{
            // 4. 线性插值计算
            // 公式：(当前值 - 最小值) / (最大值 - 最小值) * 总分
            float range = RATIO_9_16 - RATIO_9_20;
            float offset = currentRatio - RATIO_9_20;
            score = (offset / range) * 12;
        }
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (score * scale + 0.5f);
    }


    private void updateGradient() {
        mSweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mStartColor, mEndColor);
        // 旋转渐变，使其起始颜色对齐到 START_ANGLE (270度)
        mGradientMatrix.setRotate(START_ANGLE, mCenterPoint.x, mCenterPoint.y);
        mSweepGradient.setLocalMatrix(mGradientMatrix);
        mProgressPaint.setShader(mSweepGradient);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // 1. 绘制带“挖孔”的背景 (高性能方式)
        drawHollowBackgroundPath(canvas);

        // 2. 绘制进度条
        if (mShowProgress) {
            // 绘制底色圆环
            canvas.drawArc(mArcRectF, START_ANGLE, MAX_ANGLE, false, mBgArcPaint);
            // 绘制彩色进度 (Shader已在onSizeChanged处理)
            canvas.drawArc(mArcRectF, START_ANGLE, mCurrentProgressAngle, false, mProgressPaint);
        }
    }

    /**
     * 使用 Path.FillType.EVEN_ODD 实现挖孔
     * 原理：路径包含一个大矩形和一个小圆。奇偶规则下，重叠区域不填充。
     */
    private void drawHollowBackgroundPath(Canvas canvas) {
        mHolePath.reset();
        // 添加全屏矩形
        mHolePath.addRect(mFullRect, Path.Direction.CW);
        // 添加中间圆 (半径为动画值)
        if (mCurrentRadius > 0) {
            mHolePath.addCircle(mCenterPoint.x, mCenterPoint.y, mCurrentRadius, Path.Direction.CW);
        }
        // 关键：设置填充模式为奇偶填充
        mHolePath.setFillType(Path.FillType.EVEN_ODD);

        canvas.drawPath(mHolePath, mBackgroundPaint);
    }

    // --- 动画与控制 ---

    public void setProgress(float percent) {
        if (!mShowProgress) return;
        float targetAngle = MAX_ANGLE * percent;
        this.mCurrentProgressAngle = min(targetAngle, MAX_ANGLE);
        invalidate();
    }

    /**
     * 炫彩活体更新屏幕颜色，人脸要离屏幕近一点
     * @param color
     */
    public void setFlashColor(int color) {
        mFlashColor = color;
        mBackgroundPaint.setColor(mFlashColor); // 记得更新画笔
        invalidate();
    }

    // 更新底部间距
    public void setCirclePaddingBottom(int paddingBottom) {
        if (this.mCirclePaddingBottom != paddingBottom) {
            this.mCirclePaddingBottom = paddingBottom;
            requestLayout();
        }
    }

    public void setMargin(int newMargin) {
        if (this.mCircleMargin != newMargin) {
            mCircleMargin = newMargin;
            requestLayout();
        }
    }

    private void startOpenAnimation() {
        if (mOpenAnimator != null && mOpenAnimator.isRunning()) {
            mOpenAnimator.cancel();
        }
        mOpenAnimator = ValueAnimator.ofFloat(0, mTargetRadius);
        mOpenAnimator.setDuration(400);
        mOpenAnimator.setInterpolator(new DecelerateInterpolator());
        mOpenAnimator.addUpdateListener(animation -> {
            mCurrentRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        mOpenAnimator.start();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            post(this::startOpenAnimation);
        } else {
            if (mOpenAnimator != null) {
                mOpenAnimator.cancel();
            }
            mCurrentRadius = 0;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mOpenAnimator != null) {
            mOpenAnimator.cancel();
            mOpenAnimator = null;
        }
    }
}