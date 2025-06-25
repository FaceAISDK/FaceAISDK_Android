package com.ai.face.base.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.SweepGradient;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.ai.face.R;
import com.ai.face.base.utils.ScreenUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UI 自由修改，Demo UI仅供简单参考
 *
 */
public class DemoFaceCoverView extends View {
    private final String TAG = "FaceVerifyView";

    // 是否可以开始绘制了
    private boolean mStartOpenCircle = false;

    //默认中间圆的半径从0开始
    private float currentRadius = 0;

    //控件的宽度（默认）
    private int mViewWidth = 400;

    //控件高度
    private int mViewHeight = 400;

    //中心圆屏幕边距
    private int margin = 4;

    //提示文本
    private String tipText;

    //提示文本颜色
    private int mTipTextColor;

    //提示文本颜色
    private int mTipTextSize;

    private int mBackGroundColor;

    //内圆半径
    private int mRadius;

    //背景弧宽度
    private float mBgArcWidth;

    //圆心点坐标
    private Point mCenterPoint = new Point();

    //圆弧边界
    private RectF mBgRectF = new RectF();

    //开始角度
    private int mStartAngle = 270;

    //结束角度
    private int mEndAngle = 360;

    //设置默认转动角度0
    float currentAngle = 0;

    //圆弧背景画笔
    private Paint mBgArcPaint;

    //提示语画笔
    private Paint mTextPaint;

    //圆弧画笔
    private Paint mArcPaint;

    //渐变器
    private SweepGradient mSweepGradient;

    private int mStartColor;
    private int mEndColor;

    private boolean showProgress;
    private ScheduledExecutorService executor;

    public DemoFaceCoverView(Context context) {
        this(context, null);
    }

    public DemoFaceCoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DemoFaceCoverView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取xml里面的属性值
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FaceViewDemo);

        margin = array.getDimensionPixelSize(R.styleable.FaceViewDemo_circle_margin, 10);
        mTipTextColor = array.getColor(R.styleable.FaceViewDemo_tip_text_color, Color.BLACK);
        mTipTextSize = array.getDimensionPixelSize(R.styleable.FaceViewDemo_tip_text_size, ScreenUtils.sp2px(context, 22));
        tipText = array.getString(R.styleable.FaceViewDemo_tip_text);
        mBackGroundColor  = array.getColor(R.styleable.FaceViewDemo_background_color, getResources().getColor(R.color.white));

        mStartColor = array.getColor(R.styleable.FaceViewDemo_progress_start_color, getResources().getColor(R.color.half_grey));
        mEndColor = array.getColor(R.styleable.FaceViewDemo_progress_end_color, getResources().getColor(R.color.half_grey));
        showProgress = array.getBoolean(R.styleable.FaceViewDemo_show_progress, true);

        array.recycle();

        initPaint(context);
    }

    /**
     * 初始化控件View
     */
    private void initPaint(Context context) {

        //保持屏幕长亮
        setKeepScreenOn(true);

        mBgArcWidth = ScreenUtils.dp2px(context, 3);

        //绘制文字画笔
        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth(9);
        mTextPaint.setColor(mTipTextColor);
        mTextPaint.setTextSize(mTipTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        // 圆弧背景
        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(true);
        mBgArcPaint.setColor(getResources().getColor(R.color.half_grey));
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 圆弧
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mBgArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        openCameraCircle();
    }


    /**
     *
     */
    private void openCameraCircle() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            if (mStartOpenCircle) {
                currentRadius = currentRadius + 13;
                if (currentRadius > mRadius && mRadius != 0) {
                    currentRadius = mRadius;
                    mStartOpenCircle = false;
                }
            }

            postInvalidate();

        }, 100, 20, TimeUnit.MILLISECONDS);
    }


    /**
     * 开始倒计时
     *
     * @param percent
     */
    public void startCountDown(float percent) {
        currentAngle = 360f * percent ;
        //外部圈的动画效果
        if (showProgress) {
//            Log.e("Time count","currentAngle: "+currentAngle);
            if (currentAngle >= mEndAngle) {
                currentAngle = mEndAngle;
            }
            postInvalidate();
        }
    }




    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //测量view的宽度
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        //测量view的高度
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(mViewWidth, mViewHeight);

        //获取圆的相关参数
        mCenterPoint.x = mViewWidth / 2;
        mCenterPoint.y = mViewHeight / 2;

        //外环圆的半径
        mRadius = Math.min(mCenterPoint.x, mCenterPoint.y) - margin;

        //绘制背景圆弧的边界
        mBgRectF.left = mCenterPoint.x - mRadius - mBgArcWidth / 2;
        mBgRectF.top = mCenterPoint.y - mRadius - mBgArcWidth / 2;
        mBgRectF.right = mCenterPoint.x + mRadius + mBgArcWidth / 2;
        mBgRectF.bottom = mCenterPoint.y + mRadius + mBgArcWidth / 2;

        //进度条颜色 -mStartAngle/2将位置到原处
        mSweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mStartColor, mEndColor);
    }


    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        mStartOpenCircle = (visibility == VISIBLE);
    }


    /**
     * 获取圆形两边的间距
     *
     * @return
     */
    public int getMargin() {
        return margin/2;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        executor.shutdownNow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //绘制画布内容
        drawContent(canvas);
    }

    /**
     * 跟新提示信息
     *
     * @param title
     */
    public void setTipText(String title) {
        tipText = title;

        //重绘view
        postInvalidate();
    }

    /**
     * 动态绘制内容
     *
     * @param canvas
     */
    private void drawContent(Canvas canvas) {
        //防止save()和restore()方法代码之后对Canvas执行的操作，继续对后续的绘制会产生影响
        canvas.save();

        //绘制人脸识别部分
        drawFaceCircle(canvas);

        //先画提示语
        drawHintText(canvas);

        //画外边进度条
        if (showProgress) {
            drawRoundProgress(canvas);
        }

        canvas.restore();
    }

    /**
     * 人脸框的背景色允许变化
     *
     * @param canvas
     */
    private void drawFaceCircle(Canvas canvas) {
        //设置画板样式
        Path path = new Path();

        //以（400,200）为圆心，半径为100绘制圆 指创建顺时针方向的矩形路径
        path.addCircle(mCenterPoint.x, mCenterPoint.y, currentRadius, Path.Direction.CW);

        // 是A形状中不同于B的部分显示出来
        canvas.clipPath(path, Region.Op.DIFFERENCE);

        // 半透明背景效果
        canvas.clipRect(0, 0, mViewWidth, mViewHeight);

        //绘制背景颜色
        canvas.drawColor(mBackGroundColor);

//        canvas.drawColor(colors[new Random().nextInt(6)]);
    }


    /**
     * 绘制人脸识别界面进度条
     *
     * @param canvas canvas
     */
    private void drawRoundProgress(Canvas canvas) {
        // 逆时针旋转105度
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        // 设置圆环背景
        canvas.drawArc(mBgRectF, 0, mEndAngle, false, mBgArcPaint);

        // 设置渐变颜色
        mArcPaint.setShader(mSweepGradient);
        canvas.drawArc(mBgRectF, 0, currentAngle, false, mArcPaint);
    }


    /**
     * 绘制人脸识别提示
     *
     * @param canvas canvas
     */
    private void drawHintText(Canvas canvas) {
        if (TextUtils.isEmpty(tipText)) return;

        //圆视图宽度 （屏幕减去两边距离）
        int cameraWidth = mViewWidth - 2 * margin;
        //x轴起点（文字背景起点）
        int x = margin;
        //宽度（提示框背景宽度）
        int width = cameraWidth;
        //y轴起点
        int y = (int) (mCenterPoint.y - mRadius);
        //提示框背景高度 ？？？？
//        int height = cameraWidth / 4;

        //计算baseline
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float distance = (fontMetrics.bottom - fontMetrics.top);
        Rect rect = new Rect(x, (int) (y - distance), x + width, y);

        canvas.drawText(tipText, rect.centerX(), mCenterPoint.y - mRadius - distance/2, mTextPaint);
    }


}