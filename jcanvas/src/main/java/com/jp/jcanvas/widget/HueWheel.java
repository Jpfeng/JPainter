package com.jp.jcanvas.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.jp.jcanvas.R;

/**
 *
 */
class HueWheel extends View {

    @ColorInt
    private static final int[] mWheelColors = new int[]{
            //              Color   (H,   S,   V)
            0xFFFF0000, //  red     (0,   100, 100)
            0xFFFFFF00, //  yellow  (60,  100, 100)
            0xFF00FF00, //  green   (120, 100, 100)
            0xFF00FFFF, //  cyan    (180, 100, 100)
            0xFF0000FF, //  blue    (240, 100, 100)
            0xFFFF00FF, //  magenta (300, 100, 100)
            0xFFFF0000}; // red     (360, 100, 100)

    private static final float MIN_DEGREE = 0f;
    private static final float MAX_DEGREE = 360f;
    private static final float ARC_SWEEP_ANGEL = 90f;

    private Paint mWheelPaint;
    private Paint mFinderPaint;
    private SweepGradient mShader;
    private PorterDuffXfermode mClearXfermode;

    private RectF mRectIn;
    private RectF mRectOut;
    private RectF mRectFinder;
    private RectF mRectFinderFill;

    private float mWheelWidth;
    private float mFinderHeight;
    private float mFinderLedge;
    private float mFinderStrokeWidth;
    private float mFinderCornerR;

    private Drawable mFinderDrawable;

    private float mHue;
    private float[] mHSV;

    private float mOneDegreePx;

    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    private OnHueChangeListener mHueListener;

    public HueWheel(Context context) {
        this(context, null);
    }

    public HueWheel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.HueWheelStyle);
    }

    public HueWheel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.HueWheel, defStyleAttr, R.style.DefaultHueWheelStyle);

        mWheelWidth = ta.getDimensionPixelSize(R.styleable.HueWheel_hw_wheelWidth, 0);
        mFinderHeight = ta.getDimensionPixelSize(R.styleable.HueWheel_hw_finderHeight, 0);
        mFinderLedge = ta.getDimensionPixelSize(R.styleable.HueWheel_hw_finderLedge, 0);
        mFinderStrokeWidth = ta.getDimensionPixelSize(
                R.styleable.HueWheel_hw_finderStrokeWidth, 0);
        mFinderCornerR = ta.getDimensionPixelSize(
                R.styleable.HueWheel_hw_finderCornerRadius, 0);
        mFinderDrawable = ta.getDrawable(R.styleable.HueWheel_hw_finderStroke);

        float hue = ta.getFloat(R.styleable.HueWheel_hw_hue, 0);
        mHue = hue % 360f;

        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        mWheelPaint = new Paint();
        mWheelPaint.setAntiAlias(true);
        mClearXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        mFinderPaint = new Paint();
        mFinderPaint.setAntiAlias(true);
        mFinderPaint.setStyle(Paint.Style.FILL);

        mRectIn = new RectF();
        mRectOut = new RectF();
        mRectFinder = new RectF();
        mRectFinderFill = new RectF();

        mHSV = new float[]{mHue, 1f, 1f};

        mScroller = new Scroller(getContext(), new AccelerateDecelerateInterpolator());
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int wResult;
        int hResult;

        if (MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;

        } else {
            hResult = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
            if (MeasureSpec.AT_MOST == hMode) {
                hResult = Math.min(hResult, hSize);
            }
        }

        float wheelH = hResult - getPaddingTop() - getPaddingBottom();
        float radius = (float) (wheelH / Math.sqrt(2));
        float halfWW = mWheelWidth / 2;
        float finderLedge = Math.min(mFinderLedge, radius - wheelH / 2 - halfWW);

        if (MeasureSpec.EXACTLY == wMode) {
            wResult = wSize;

        } else {
            int w = (int) (radius - wheelH / 2 + halfWW + finderLedge
                    + getPaddingLeft() + getPaddingRight() + 0.5f);
            wResult = Math.max(w, getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight());
            if (MeasureSpec.AT_MOST == wMode) {
                wResult = Math.min(wResult, wSize);
            }
        }

        setMeasuredDimension(wResult, hResult);

        float l = -(radius + wheelH / 2) + getPaddingLeft();
        float t = -(radius - wheelH / 2) + getPaddingTop();
        float r = l + radius * 2;
        float b = t + radius * 2;

        mRectIn.set(l + halfWW, t + halfWW, r - halfWW, b - halfWW);
        mRectOut.set(l - halfWW, t - halfWW, r + halfWW, b + halfWW);

        updateShader();
        mOneDegreePx = wheelH / ARC_SWEEP_ANGEL;

        float finderH = Math.min(mFinderHeight, hResult);
        float finderL = getPaddingLeft() + radius - wheelH / 2 - halfWW - finderLedge;
        float finderT = getPaddingTop() + wheelH / 2 - finderH / 2;
        float finderR = finderL + mWheelWidth + finderLedge * 2;
        float finderB = finderT + finderH;

        mRectFinder.set(finderL, finderT, finderR, finderB);
        mRectFinderFill.set(finderL + mFinderStrokeWidth, finderT + mFinderStrokeWidth,
                finderR - mFinderStrokeWidth, finderB - mFinderStrokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int layer = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(),
                null, Canvas.ALL_SAVE_FLAG);

        float wheelDegree = mHue - ARC_SWEEP_ANGEL / 2;
        float canvasDegree = -mHue;
        mWheelPaint.setShader(mShader);

        canvas.clipRect(getPaddingLeft(), getPaddingTop(),
                getRight() - getPaddingRight(), getBottom() - getPaddingBottom());

        canvas.save();
        canvas.rotate(canvasDegree, mRectOut.centerX(), mRectOut.centerY());
        canvas.drawArc(mRectOut, wheelDegree, ARC_SWEEP_ANGEL, true, mWheelPaint);
        mWheelPaint.setXfermode(mClearXfermode);
        canvas.drawArc(mRectIn, MIN_DEGREE, MAX_DEGREE, true, mWheelPaint);
        mWheelPaint.setXfermode(null);

        canvas.restore();
        canvas.restoreToCount(layer);

        if (mFinderDrawable instanceof ColorDrawable) {
            mFinderPaint.setColor(((ColorDrawable) mFinderDrawable).getColor());
            float cornerStroke = mFinderCornerR + mFinderStrokeWidth;
            canvas.drawRoundRect(mRectFinder, cornerStroke, cornerStroke, mFinderPaint);

        } else {
            mFinderDrawable.mutate().setBounds((int) (mRectFinder.left), (int) (mRectFinder.top),
                    (int) (mRectFinder.right), (int) (mRectFinder.bottom));
            mFinderDrawable.draw(canvas);
        }

        mHSV[0] = mHue;
        mFinderPaint.setColor(Color.HSVToColor(mHSV));
        canvas.drawRoundRect(mRectFinderFill, mFinderCornerR, mFinderCornerR, mFinderPaint);
    }

    float mMoveLastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        boolean handled = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (tryCapturePoint(event)) {
                    mScroller.abortAnimation();
                    mMoveLastY = y;
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float dY = y - mMoveLastY;
                mHue = (mHue - dY / mOneDegreePx) % MAX_DEGREE;
                if (mHue < MIN_DEGREE) {
                    mHue = MAX_DEGREE + mHue;
                }
                if (null != mHueListener) {
                    mHueListener.onHueChange(mHue);
                }
                mMoveLastY = y;
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float vY = velocityTracker.getYVelocity();

                if (Math.abs(vY) > mMinFlingVelocity) {
                    mScroller.fling(0, 0, 0, (int) -vY,
                            0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    mScrollLastY = 0f;
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                long time = event.getEventTime() - event.getDownTime();
                if (time < ViewConfiguration.getTapTimeout()) {
                    performClick();
                }
                handled = true;
                break;
        }

        invalidate();
        return handled;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    float mScrollLastY;

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();
            mHue = (mHue + (currY - mScrollLastY) / mOneDegreePx) % MAX_DEGREE;
            if (mHue < MIN_DEGREE) {
                mHue = MAX_DEGREE + mHue;
            }
            if (null != mHueListener) {
                mHueListener.onHueChange(mHue);
            }
            mScrollLastY = currY;
            invalidate();
        }
    }

    private void updateShader() {
        mShader = new SweepGradient(
                mRectOut.centerX(), mRectOut.centerY(), mWheelColors, null);
    }

    private boolean tryCapturePoint(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (x < getPaddingLeft() || getRight() - x < getPaddingRight()
                || y < getPaddingTop() || getBottom() - y < getPaddingBottom()) {
            return false;
        }

        int h = getMeasuredHeight();
        int halfH = h / 2;
        float minR = (float) (h / Math.sqrt(2) - mWheelWidth / 2);
        float maxR = (float) (h / Math.sqrt(2) + mWheelWidth / 2);

        float dx = x + halfH;
        float dy = halfH - y;
        float r = (float) Math.hypot(dx, dy);

        return r >= minR && r <= maxR;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    public float getHue() {
        return mHue;
    }

    public void setHue(@FloatRange(from = MIN_DEGREE, to = MAX_DEGREE) float hue) {
        mHue = hue;
        invalidate();
        if (null != mHueListener) {
            mHueListener.onHueChange(mHue);
        }
    }

    @Px
    public int getWheelWidth() {
        return (int) mWheelWidth;
    }

    public void setWheelWidth(int wheelWidth) {
        mWheelWidth = wheelWidth;
        requestLayout();
    }

    public void setWheelWidthDp(int wheelWidthDp) {
        mWheelWidth = dp2px(wheelWidthDp);
        requestLayout();
        invalidate();
    }

    public void setOnHueChangeListener(OnHueChangeListener listener) {
        mHueListener = listener;
    }

    public interface OnHueChangeListener {
        void onHueChange(float hue);
    }
}
