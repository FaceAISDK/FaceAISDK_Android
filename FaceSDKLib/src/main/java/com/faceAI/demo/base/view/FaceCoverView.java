package com.faceAI.demo.base.view;

import static java.lang.Math.min;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import com.faceAI.demo.R;
import com.faceAI.demo.base.utils.ScreenUtils;

/**
 * 人脸识别覆盖视图
 * - 半透明遮罩 + 圆形镂空（支持展开动画）
 * - 环形进度条（渐变色）
 * - 上方双行提示文本（支持淡入淡出）
 */
public class FaceCoverView extends View {

    public static  int MARGIN_SIZE = 8;
    private static final int START_ANGLE = 270;
    private static final int MAX_ANGLE = 360;

    // ==================== 可配置属性 ====================
    private int mFlashColor;
    private final int mStartColor;
    private final int mEndColor;
    private final boolean mShowProgress;
    private final int mTipTextColor;
    private final int mTipTextBgColor;
    private final float mTipTextSize;
    private int mCircleMargin = -1;
    private int mCirclePaddingBottom;

    // ==================== 几何参数 ====================
    private final PointF mCenterPoint = new PointF();
    private float mTargetRadius;
    private float mCurrentRadius = 0;
    private float mBgArcWidth;

    // ==================== 绘图对象 ====================
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTipsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSecondTipsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path mHolePath = new Path();
    private final RectF mFullRect = new RectF();
    private final RectF mArcRectF = new RectF();
    private final RectF mTextBgRect = new RectF();
    private final Matrix mGradientMatrix = new Matrix();

    // ==================== 文本与动画 ====================
    private String mTipsText = "";
    private String mSecondTipsText = "";
    private float mTipsWidth = 0;
    private float mSecondTipsWidth = 0;

    private int mSecondTipsAlpha = 0;
    private int mTargetSecondAlpha = -1; // 记录动画目标值，避免高频打断
    private ValueAnimator mSecondTipsAnimator;

    private float mTextSpacing;
    private float mTextPaddingHorizontal;
    private float mTextPaddingVertical;
    private float mTextBgRadius;

    private ValueAnimator mOpenAnimator;
    private float mCurrentProgressAngle = 0;

    public FaceCoverView(Context context) {
        this(context, null);
    }

    public FaceCoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceCoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FaceVerifyCoverView);

        // 读取通用属性
        mFlashColor = array.getColor(R.styleable.FaceVerifyCoverView_flash_color, Color.WHITE);
        mStartColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_start_color, Color.LTGRAY);
        mEndColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_end_color, Color.LTGRAY);
        mShowProgress = array.getBoolean(R.styleable.FaceVerifyCoverView_show_progress, true);

        // 读取与 attrs.xml 关联的文字属性
        mTipTextColor = array.getColor(R.styleable.FaceVerifyCoverView_tip_text_color, Color.WHITE);
        mTipTextBgColor = array.getColor(R.styleable.FaceVerifyCoverView_tip_text_bg_color, ContextCompat.getColor(context, R.color.face_main_color));

        // 默认 18sp
        float defaultTextSize = 19 * context.getResources().getDisplayMetrics().scaledDensity;
        mTipTextSize = array.getDimension(R.styleable.FaceVerifyCoverView_tip_text_size, defaultTextSize);

        array.recycle();

        initPaints(context);
    }

    private void initPaints(Context context) {
        mBgArcWidth = ScreenUtils.dp2px(context, 2);

        mBackgroundPaint.setColor(mFlashColor);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mBgArcPaint.setColor(ContextCompat.getColor(context, R.color.half_grey));
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mBgArcWidth);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);

        // 使用从 XML 中获取的文字属性
        mTipsPaint.setColor(mTipTextColor);
        mTipsPaint.setTextSize(mTipTextSize);
        mTipsPaint.setTextAlign(Paint.Align.CENTER);
        mTipsPaint.setFakeBoldText(true);

        mSecondTipsPaint.setColor(mTipTextColor);
        // 副标题稍微小一点，或者也可以保持一致
        mSecondTipsPaint.setTextSize(mTipTextSize * 0.9f);
        mSecondTipsPaint.setTextAlign(Paint.Align.CENTER);
        mSecondTipsPaint.setFakeBoldText(true);

        // 使用从 XML 获取的背景颜色
        mTextBgPaint.setColor(mTipTextBgColor);
        mTextBgPaint.setStyle(Paint.Style.FILL);

        mTextSpacing = ScreenUtils.dp2px(context, 6);
        mTextPaddingHorizontal = ScreenUtils.dp2px(context, 17);
        mTextPaddingVertical = ScreenUtils.dp2px(context, 6);
        mTextBgRadius = ScreenUtils.dp2px(context, 20);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateLayoutParameters(w, h);
    }

    private void updateLayoutParameters(int w, int h) {
        if (w <= 0 || h <= 0) return;

        mFullRect.set(0, 0, w, h);
        int shorterSide = min(w, h);
        if(w>h) {
            MARGIN_SIZE = 5;
        }else {
            MARGIN_SIZE = 8;
        }

        if (mCircleMargin < 0) mCircleMargin = shorterSide / MARGIN_SIZE;

        int basePadding = shorterSide / MARGIN_SIZE;
        mCirclePaddingBottom = (w > h) ? -basePadding/3 : basePadding;

        mCenterPoint.set(w / 2f, h / 2f - mCirclePaddingBottom);
        mTargetRadius = (shorterSide / 2f) - mCircleMargin;

        float halfStroke = mBgArcWidth / 2f;
        mArcRectF.set(
                mCenterPoint.x - mTargetRadius - halfStroke,
                mCenterPoint.y - mTargetRadius - halfStroke,
                mCenterPoint.x + mTargetRadius + halfStroke,
                mCenterPoint.y + mTargetRadius + halfStroke
        );

        SweepGradient sweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mStartColor, mEndColor);
        mGradientMatrix.setRotate(START_ANGLE, mCenterPoint.x, mCenterPoint.y);
        sweepGradient.setLocalMatrix(mGradientMatrix);
        mProgressPaint.setShader(sweepGradient);

        if (mOpenAnimator == null || !mOpenAnimator.isRunning()) {
            mCurrentRadius = mTargetRadius;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mHolePath.reset();
        mHolePath.addRect(mFullRect, Path.Direction.CW);
        if (mCurrentRadius > 0) {
            mHolePath.addCircle(mCenterPoint.x, mCenterPoint.y, mCurrentRadius, Path.Direction.CW);
        }
        mHolePath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(mHolePath, mBackgroundPaint);

        if (mShowProgress) {
            canvas.drawArc(mArcRectF, START_ANGLE, MAX_ANGLE, false, mBgArcPaint);
            canvas.drawArc(mArcRectF, START_ANGLE, mCurrentProgressAngle, false, mProgressPaint);
        }

        drawTipsText(canvas);
    }

    private void drawTipsText(Canvas canvas) {
        boolean hasFirst = !TextUtils.isEmpty(mTipsText);
        boolean hasSecond = mSecondTipsAlpha > 0 && !TextUtils.isEmpty(mSecondTipsText);
        if (!hasFirst && !hasSecond) return;

        float secondTipsY = mCenterPoint.y - mTargetRadius - mTextSpacing - mTextPaddingVertical - mSecondTipsPaint.descent();
        float secondBgTop = secondTipsY + mSecondTipsPaint.ascent() - mTextPaddingVertical;
        float firstTipsY = secondBgTop - mTextSpacing - mTextPaddingVertical - mTipsPaint.descent();

        if (hasSecond) {
            int savedAlpha = mTextBgPaint.getAlpha();
            mSecondTipsPaint.setAlpha(mSecondTipsAlpha);
            mTextBgPaint.setAlpha(mSecondTipsAlpha);
            drawTextWithBackground(canvas, mSecondTipsText, mSecondTipsWidth, mCenterPoint.x, secondTipsY, mSecondTipsPaint);
            mTextBgPaint.setAlpha(savedAlpha);
        }

        if (hasFirst) {
            drawTextWithBackground(canvas, mTipsText, mTipsWidth, mCenterPoint.x, firstTipsY, mTipsPaint);
        }
    }

    private void drawTextWithBackground(Canvas canvas, String text, float textWidth, float x, float y, Paint paint) {
        float bgWidth = textWidth + mTextPaddingHorizontal * 2;
        mTextBgRect.set(
                x - bgWidth / 2,
                y + paint.ascent() - mTextPaddingVertical,
                x + bgWidth / 2,
                y + paint.descent() + mTextPaddingVertical
        );
        canvas.drawRoundRect(mTextBgRect, mTextBgRadius, mTextBgRadius, mTextBgPaint);
        canvas.drawText(text, x, y, paint);
    }

    // ==================== 公开 API ====================

    public void setTipsText(String tips) {
        String newTips = tips != null ? tips : "";
        if (TextUtils.equals(this.mTipsText, newTips)) {
            return;
        }
        this.mTipsText = newTips;
        this.mTipsWidth = mTipsPaint.measureText(mTipsText);
        if (!TextUtils.isEmpty(mTipsText)) {
            if (mSecondTipsAnimator != null) mSecondTipsAnimator.cancel();
            mSecondTipsAlpha = 0;
            mTargetSecondAlpha = 0;
        }
        invalidate();
    }

    public void setTipsText(@StringRes int resId) {
        setTipsText(resId == 0 ? "" : getContext().getString(resId));
    }

    public void setSecondTipsText(String tips) {
        if (TextUtils.isEmpty(tips)) {
            animateSecondTipsAlpha(0);
            return;
        }
        if (TextUtils.equals(tips, mSecondTipsText)) {
            animateSecondTipsAlpha(255);
            return;
        }
        this.mSecondTipsText = tips;
        this.mSecondTipsWidth = mSecondTipsPaint.measureText(mSecondTipsText);
        animateSecondTipsAlpha(255);
    }

    public void setSecondTipsText(@StringRes int resId) {
        setSecondTipsText(resId == 0 ? "" : getContext().getString(resId));
    }

    public void setProgress(float percent) {
        if (!mShowProgress) return;
        mCurrentProgressAngle = min(MAX_ANGLE * percent, MAX_ANGLE);
        invalidate();
    }

    public void setFlashColor(@ColorInt int color) {
        if (mFlashColor != color) {
            mFlashColor = color;
            mBackgroundPaint.setColor(mFlashColor);
            invalidate();
        }
    }

    public void setCirclePaddingBottom(int paddingBottom) {
        if (this.mCirclePaddingBottom != paddingBottom) {
            this.mCirclePaddingBottom = paddingBottom;
            updateLayoutParameters(getWidth(), getHeight());
            invalidate();
        }
    }

    public void setMargin(int newMargin) {
        if (this.mCircleMargin != newMargin) {
            this.mCircleMargin = newMargin;
            updateLayoutParameters(getWidth(), getHeight());
            invalidate();
        }
    }

    // ==================== 动画处理 ====================

    private void animateSecondTipsAlpha(int targetAlpha) {
        if (mSecondTipsAlpha == targetAlpha || mTargetSecondAlpha == targetAlpha) {
            return;
        }
        mTargetSecondAlpha = targetAlpha;
        if (mSecondTipsAnimator != null) {
            mSecondTipsAnimator.cancel();
        }
        int baseDuration = targetAlpha == 255 ? 200 : 400;
        int duration = (int) (Math.abs(targetAlpha - mSecondTipsAlpha) / 255f * baseDuration);

        mSecondTipsAnimator = ValueAnimator.ofInt(mSecondTipsAlpha, targetAlpha);
        mSecondTipsAnimator.setDuration(Math.max(duration, 50));
        mSecondTipsAnimator.addUpdateListener(animation -> {
            mSecondTipsAlpha = (int) animation.getAnimatedValue();
            invalidate();
        });
        mSecondTipsAnimator.start();
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

    // ==================== 生命周期 ====================

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            post(this::startOpenAnimation);
        } else {
            if (mOpenAnimator != null) mOpenAnimator.cancel();
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
        if (mSecondTipsAnimator != null) {
            mSecondTipsAnimator.cancel();
            mSecondTipsAnimator = null;
        }
    }
}