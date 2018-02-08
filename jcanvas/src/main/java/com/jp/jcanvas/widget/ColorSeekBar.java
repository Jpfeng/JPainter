package com.jp.jcanvas.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.jp.jcanvas.R;

/**
 *
 */
class ColorSeekBar extends View {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private Paint mBarPaint;
    private Paint mFinderPaint;

    private RectF mRectBar;
    private RectF mRectFinder;
    private RectF mRectFinderFill;

    private BitmapShader mBGShader;
    private PorterDuffXfermode mXferSrcOver;
    private PorterDuffXfermode mXferClear;

    @ColorInt
    private int mStartColor;
    @ColorInt
    private int mEndColor;

    private int mOrientation;
    private int mBarSize;
    private int mFinderSize;
    private int mFinderStrokeWidth;
    private int mFinderCornerR;

    private Drawable mFinderDrawable;

    @FloatRange(from = 0f, to = 1f)
    private float mProgress;

    private OnProgressChangeListener mListener;

    public ColorSeekBar(Context context) {
        this(context, null);
    }

    public ColorSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.ColorSeekBarStyle);
    }

    public ColorSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ColorSeekBar, defStyleAttr, R.style.DefaultColorSeekBarStyle);

        mOrientation = ta.getInt(R.styleable.ColorSeekBar_csb_orientation, HORIZONTAL);
        mBarSize = ta.getDimensionPixelSize(R.styleable.ColorSeekBar_csb_barSize, 0);
        mFinderSize = ta.getDimensionPixelSize(R.styleable.ColorSeekBar_csb_finderSize, 0);
        mFinderDrawable = ta.getDrawable(R.styleable.ColorSeekBar_csb_finderStroke);
        mFinderStrokeWidth = ta.getDimensionPixelSize(
                R.styleable.ColorSeekBar_csb_finderStrokeWidth, 0);
        mFinderCornerR = ta.getDimensionPixelSize(
                R.styleable.ColorSeekBar_csb_finderCornerRadius, 0);

        mStartColor = ta.getColor(R.styleable.ColorSeekBar_csb_colorStart, 0);
        mEndColor = ta.getColor(R.styleable.ColorSeekBar_csb_colorEnd, 0);

        ta.recycle();
        init();
    }

    private void init() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        mBGShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mXferSrcOver = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        mXferClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);

        mFinderPaint = new Paint();
        mFinderPaint.setAntiAlias(true);
        mFinderPaint.setStyle(Paint.Style.FILL);

        mRectBar = new RectF();
        mRectFinder = new RectF();
        mRectFinderFill = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int wResult = 0;
        int hResult = 0;

        if (MeasureSpec.EXACTLY == wMode) {
            wResult = wSize;

        } else {
            switch (mOrientation) {
                case HORIZONTAL:
                    wResult = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
                    break;

                case VERTICAL:
                    wResult = Math.max(getSuggestedMinimumWidth(),
                            mBarSize + getPaddingLeft() + getPaddingRight());
                    break;
            }

            if (MeasureSpec.AT_MOST == wMode) {
                wResult = Math.min(wResult, wSize);
            }
        }

        if (MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;

        } else {
            switch (mOrientation) {
                case HORIZONTAL:
                    hResult = Math.max(getSuggestedMinimumHeight(),
                            mBarSize + getPaddingTop() + getPaddingBottom());
                    break;

                case VERTICAL:
                    hResult = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
                    break;
            }

            if (MeasureSpec.AT_MOST == hMode) {
                hResult = Math.min(hResult, hSize);
            }
        }

        setMeasuredDimension(wResult, hResult);

        float l = 0f;
        float r = 0f;
        float t = 0f;
        float b = 0f;
        switch (mOrientation) {
            case HORIZONTAL:
                l = getPaddingLeft();
                t = Math.max((hResult - mBarSize) / 2f, getPaddingTop());
                r = wResult - getPaddingRight();
                b = Math.min(t + mBarSize, hResult - getPaddingBottom());
                break;

            case VERTICAL:
                l = Math.max((wResult - mBarSize) / 2f, getPaddingLeft());
                t = getPaddingTop();
                r = Math.min(l + mBarSize, wResult - getPaddingRight());
                b = hResult - getPaddingBottom();
                break;
        }
        mRectBar.set(l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mBarPaint.setShader(mBGShader);
        mBarPaint.setXfermode(null);
        canvas.drawPaint(mBarPaint);

        mBarPaint.setShader(generateShader());
        mBarPaint.setXfermode(mXferSrcOver);
        canvas.drawRect(mRectBar, mBarPaint);

        float l = 0f;
        float t = 0f;
        float r = 0f;
        float b = 0f;
        switch (mOrientation) {
            case HORIZONTAL:
                l = mRectBar.left + (mRectBar.right - mRectBar.left - mFinderSize) * mProgress;
                t = mRectBar.top;
                r = l + mFinderSize;
                b = mRectBar.bottom;
                break;

            case VERTICAL:
                l = mRectBar.left;
                t = mRectBar.top + (mRectBar.bottom - mRectBar.top - mFinderSize) * mProgress;
                r = mRectBar.right;
                b = t + mFinderSize;
                break;
        }

        mRectFinder.set(l, t, r, b);
        mRectFinderFill.set(l + mFinderStrokeWidth, t + mFinderStrokeWidth,
                r - mFinderStrokeWidth, b - mFinderStrokeWidth);

        int layerBar = canvas.saveLayer(mRectBar, null, Canvas.ALL_SAVE_FLAG);
        mFinderPaint.setXfermode(mXferSrcOver);

        if (mFinderDrawable instanceof ColorDrawable) {
            mFinderPaint.setColor(((ColorDrawable) mFinderDrawable).getColor());
            float cornerStroke = mFinderCornerR + mFinderStrokeWidth;
            canvas.drawRoundRect(mRectFinder, cornerStroke, cornerStroke, mFinderPaint);

        } else {
            mFinderDrawable.mutate().setBounds((int) (mRectFinder.left), (int) (mRectFinder.top),
                    (int) (mRectFinder.right), (int) (mRectFinder.bottom));
            mFinderDrawable.draw(canvas);
        }

        mFinderPaint.setXfermode(mXferClear);
        canvas.drawRoundRect(mRectFinderFill, mFinderCornerR, mFinderCornerR, mFinderPaint);
        canvas.restoreToCount(layerBar);
    }

    private Shader generateShader() {
        switch (mOrientation) {
            case HORIZONTAL:
                return new LinearGradient(mRectBar.left, mRectBar.top, mRectBar.right, mRectBar.top,
                        mStartColor, mEndColor, Shader.TileMode.CLAMP);

            case VERTICAL:
                return new LinearGradient(mRectBar.left, mRectBar.top, mRectBar.left, mRectBar.bottom,
                        mStartColor, mEndColor, Shader.TileMode.CLAMP);
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float progress = 0f;
        boolean handled = false;

        int action = event.getAction();
        if (MotionEvent.ACTION_DOWN == action && !tryCapturePoint(event)) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
                performClick();
                // case 穿透

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                switch (mOrientation) {
                    case HORIZONTAL:
                        x = Math.max(mRectBar.left, Math.min(mRectBar.right, x));
                        progress = (x - mRectBar.left) / (mRectBar.right - mRectBar.left);
                        break;

                    case VERTICAL:
                        y = Math.max(mRectBar.top, Math.min(mRectBar.bottom, y));
                        progress = (y - mRectBar.top) / (mRectBar.bottom - mRectBar.top);
                        break;
                }

                setProgress(progress);
                handled = true;
                break;
        }

        return handled;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean tryCapturePoint(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        return mRectBar.left <= x && mRectBar.right >= x
                && mRectBar.top <= y && mRectBar.bottom >= y;
    }

    public void setStartColor(@ColorInt int color) {
        mStartColor = color;
        invalidate();
    }

    public void setEndColor(@ColorInt int color) {
        mEndColor = color;
        invalidate();
    }

    public void setColors(@ColorInt int startColor, @ColorInt int endColor) {
        mStartColor = startColor;
        mEndColor = endColor;
        invalidate();
    }

    public void setOrientation(int orientation) {
        if (!(HORIZONTAL == orientation || VERTICAL == orientation)) {
            throw new IllegalArgumentException("illegal argument: orientation = " + orientation);
        }

        mOrientation = orientation;
        invalidate();
    }

    public void setProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        mProgress = progress;
        invalidate();

        if (null != mListener) {
            mListener.onProgressChange(mProgress);
        }
    }

    public float getProgress() {
        return mProgress;
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        mListener = listener;
    }

    public interface OnProgressChangeListener {
        void onProgressChange(@FloatRange(from = 0f, to = 1f) float progress);
    }
}
