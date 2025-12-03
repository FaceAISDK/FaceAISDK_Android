package com.faceAI.demo.base.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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
 * 人脸识别覆盖视图，动作活体进度条，炫彩活体闪光颜色
 */
public class FaceVerifyCoverView extends View {

    // --- 核心属性 ---
    private int mBackgroundColor;    // 背景遮罩颜色
    private int mStartColor;         // 进度条渐变起始色
    private int mEndColor;           // 进度条渐变结束色
    private boolean mShowProgress;   // 是否显示进度条
    private int mMargin;             // 中心圆距离屏幕边缘的边距

    // --- 尺寸相关 ---
    private int mViewWidth;
    private int mViewHeight;
    private final Point mCenterPoint = new Point();
    private float mTargetRadius;     // 最终圆半径
    private float mCurrentRadius = 0;// 当前圆半径（动画过程）
    private float mBgArcWidth;       // 圆弧宽度

    // --- 绘制对象 (避免在 onDraw 中创建) ---
    private Paint mHolePaint;        // 挖孔画笔
    private Paint mBgArcPaint;       // 进度条背景画笔
    private Paint mProgressPaint;    // 进度条画笔
    private final RectF mArcRectF = new RectF(); // 圆弧绘制区域
    private SweepGradient mSweepGradient;
    private PorterDuffXfermode mXfermode; // 混合模式，用于挖孔

    // --- 动画与状态 ---
    private ValueAnimator mOpenAnimator;
    private float mCurrentProgressAngle = 0;
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
            mMargin = array.getDimensionPixelSize(R.styleable.FaceVerifyCoverView_circle_margin, 10);
            mBackgroundColor = array.getColor(R.styleable.FaceVerifyCoverView_background_color, Color.WHITE);
            mStartColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_start_color, Color.LTGRAY);
            mEndColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_end_color, Color.LTGRAY);
            mShowProgress = array.getBoolean(R.styleable.FaceVerifyCoverView_show_progress, true);
            array.recycle();
        } else {
            // 默认值兜底
            mMargin = 10;
            mBackgroundColor = Color.WHITE;
            mStartColor = Color.LTGRAY;
            mEndColor = Color.LTGRAY;
            mShowProgress = true;
        }
    }

    // --- 公开 API ---

    /**
     * 支持动态显示不同颜色，准备拓展为炫彩活体
     * @param color
     */
    public void setBackGroundColor(int color){
        mBackgroundColor=color;
        invalidate();
    }


    /**
     * 设置倒计时/进度
     * @param percent 0.0f - 1.0f
     */
    public void setProgress(float percent) {
        if (!mShowProgress) return;

        float targetAngle = MAX_ANGLE * percent;
        if (targetAngle > MAX_ANGLE) targetAngle = MAX_ANGLE;

        this.mCurrentProgressAngle = targetAngle;
        invalidate();
    }

    public int getMargin() {
        return mMargin / 2;
    }


    private void initPaints(Context context) {
        // 关闭硬件加速可能导致的某些 Xfermode 异常（通常 API 14+ 开启是没问题的，但为了稳妥可以保留，或者只针对特定层开启）
        // setLayerType(LAYER_TYPE_SOFTWARE, null); // 只有在遇到兼容性问题时才开启软件绘制，通常不需要

        mBgArcWidth = ScreenUtils.dp2px(context, 2);

        // 1. 挖孔画笔 (用于清除背景色)
        mHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHolePaint.setStyle(Paint.Style.FILL);
        // DST_OUT 模式：保留目标像素（背景），但在源像素（圆）覆盖的地方清除掉
        mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

        // 2. 进度条背景画笔
        mBgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgArcPaint.setColor(ContextCompat.getColor(context, R.color.half_grey)); // 建议改为传入颜色或固定颜色
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 3. 进度条画笔
        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mBgArcWidth);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * 尺寸改变时重新计算参数，替代 onMeasure 中的逻辑
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;

        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2;

        // 计算最大半径
        mTargetRadius = Math.min(mCenterPoint.x, mCenterPoint.y) - mMargin;

        // 计算进度条的边界
        float halfStroke = mBgArcWidth / 2f;
        mArcRectF.set(
                mCenterPoint.x - mTargetRadius - halfStroke,
                mCenterPoint.y - mTargetRadius - halfStroke,
                mCenterPoint.x + mTargetRadius + halfStroke,
                mCenterPoint.y + mTargetRadius + halfStroke
        );

        // 初始化渐变色
        mSweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mStartColor, mEndColor);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // 1. 绘制带“挖孔”的背景
        drawHollowBackground(canvas);

        // 2. 绘制外围进度条
        if (mShowProgress) {
            drawProgressBar(canvas);
        }
    }

    /**
     * 绘制遮罩背景和中间的透明圆
     * 使用离屏缓冲(Layer) + Xfermode 实现平滑的挖孔效果
     */
    private void drawHollowBackground(Canvas canvas) {
        // 保存一个新的图层，用于处理混合模式
        int layerId = canvas.saveLayer(0, 0, mViewWidth, mViewHeight, null);

        // A. 绘制全屏半透明背景 (DST)
        canvas.drawColor(mBackgroundColor);

        // B. 绘制圆形，使用 DST_OUT 模式将该区域“擦除” (SRC)
        mHolePaint.setXfermode(mXfermode);
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mCurrentRadius, mHolePaint);
        mHolePaint.setXfermode(null); // 还原混合模式

        // 恢复图层
        canvas.restoreToCount(layerId);
    }


    /**
     * 绘制圆形进度条
     */
    private void drawProgressBar(Canvas canvas) {
        // 旋转画布，使起始点在顶部 (默认 0 度是右侧)
        canvas.save();
        canvas.rotate(START_ANGLE, mCenterPoint.x, mCenterPoint.y);

        // 绘制底部灰色圆环
        canvas.drawArc(mArcRectF, 0, MAX_ANGLE, false, mBgArcPaint);

        // 绘制彩色进度
        mProgressPaint.setShader(mSweepGradient);
        canvas.drawArc(mArcRectF, 0, mCurrentProgressAngle, false, mProgressPaint);

        canvas.restore();
    }

    /**
     * 启动扩散动画 (从中心点扩散到最大半径)
     */
    private void startOpenAnimation() {
        if (mOpenAnimator != null && mOpenAnimator.isRunning()) {
            mOpenAnimator.cancel();
        }

        // 使用 ValueAnimator，时长 400ms，插值器使动画更自然
        mOpenAnimator = ValueAnimator.ofFloat(0, mTargetRadius);
        mOpenAnimator.setDuration(400);
        mOpenAnimator.setInterpolator(new DecelerateInterpolator());
        mOpenAnimator.addUpdateListener(animation -> {
            mCurrentRadius = (float) animation.getAnimatedValue();
            invalidate(); // 触发重绘
        });
        mOpenAnimator.start();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            // View 显示且已经测量完毕后启动动画
            post(this::startOpenAnimation);
        } else {
            if (mOpenAnimator != null) {
                mOpenAnimator.cancel();
            }
            mCurrentRadius = 0; // 重置
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
