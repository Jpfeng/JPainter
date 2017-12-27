package com.jp.jcanvas.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.jp.jcanvas.R;

/**
 *
 */
class AlphaSeekBar extends View {

    public static final int DEFAULT_BAR_HEIGHT_DP = 32;
    private static final int DEFAULT_FINDER_WIDTH_DP = 16;
    private static final int DEFAULT_FINDER_EDGE_DP = 4;
    private static final int DEFAULT_FINDER_CORNER_RADIUS = 4;

    private Paint mBarPaint;
    private Paint mFinderPaint;

    private RectF mRectBar;
    private RectF mRectFinder;

    private BitmapShader mBGShader;
    private PorterDuffXfermode xfermode;

    @FloatRange(from = 0f, to = 1f)
    private float mAlpha;
    private int mColor;

    private OnAlphaChangeListener mListener;

    public AlphaSeekBar(Context context) {
        this(context, null);
    }

    public AlphaSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphaSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public AlphaSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                        int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        mBGShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);

        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);

        mFinderPaint = new Paint();
        mFinderPaint.setAntiAlias(true);
        mFinderPaint.setStrokeWidth(dp2px(DEFAULT_FINDER_EDGE_DP));

        mRectBar = new RectF();
        mRectFinder = new RectF();

        mColor = Color.RED;
        mAlpha = 1f;
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

        if (MeasureSpec.EXACTLY == wMode) {
            wResult = wSize;
        } else {
            wResult = getSuggestedMinimumHeight();
            if (MeasureSpec.AT_MOST == wMode) {
                wResult = Math.min(wResult, wSize);
            }
        }

        int defaultH = getPaddingTop() + getPaddingBottom() + dp2px(DEFAULT_BAR_HEIGHT_DP);

        if (MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;
        } else {
            hResult = defaultH;
            if (MeasureSpec.AT_MOST == hMode) {
                hResult = Math.min(hResult, hSize);
            }
        }

        setMeasuredDimension(wResult, hResult);

        float l = getPaddingLeft();
        float r = wResult - getPaddingRight();
        float t = getPaddingTop();
        if (hResult > defaultH) {
            t += (hResult - defaultH - getPaddingTop() - getPaddingBottom()) / 2f;
        }
        float b = t + dp2px(DEFAULT_BAR_HEIGHT_DP);
        mRectBar.set(l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int layerBar = canvas.saveLayer(mRectBar, null, Canvas.ALL_SAVE_FLAG);
        mBarPaint.setShader(mBGShader);
        mBarPaint.setXfermode(null);
        canvas.drawPaint(mBarPaint);

        mBarPaint.setShader(generateShader());
        mBarPaint.setXfermode(xfermode);
        canvas.drawRect(mRectBar, mBarPaint);
        canvas.restoreToCount(layerBar);

        float halfEW = dp2px(DEFAULT_FINDER_EDGE_DP) / 2;
        float l = mRectBar.left + halfEW
                + (mRectBar.right - mRectBar.left - dp2px(DEFAULT_FINDER_WIDTH_DP)) * mAlpha;
        float t = mRectBar.top + halfEW;
        float r = l + dp2px(DEFAULT_FINDER_WIDTH_DP) - halfEW * 2;
        float b = mRectBar.bottom - halfEW;
        mRectFinder.set(l, t, r, b);

        float cornerR = dp2px(DEFAULT_FINDER_CORNER_RADIUS);
        mFinderPaint.setStyle(Paint.Style.STROKE);
        mFinderPaint.setColor(Color.GRAY);
        canvas.drawRoundRect(mRectFinder, cornerR, cornerR, mFinderPaint);
    }

    private Shader generateShader() {
        return new LinearGradient(mRectBar.right, mRectBar.top, mRectBar.left, mRectBar.top,
                mColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();

        boolean handled = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (tryCapturePoint(event)) {
                    if (x < mRectBar.left) {
                        x = mRectBar.left;
                    } else if (x > mRectBar.right) {
                        x = mRectBar.right;
                    }
                    mAlpha = (x - mRectBar.left) / (mRectBar.right - mRectBar.left);
                    handled = true;
                    performClick();

                    if (null != mListener) {
                        mListener.onAlphaChange(mAlpha);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (x < mRectBar.left) {
                    x = mRectBar.left;
                } else if (x > mRectBar.right) {
                    x = mRectBar.right;
                }
                mAlpha = (x - mRectBar.left) / (mRectBar.right - mRectBar.left);
                handled = true;

                if (null != mListener) {
                    mListener.onAlphaChange(mAlpha);
                }
                break;
        }

        invalidate();
        return handled;
    }

    private boolean tryCapturePoint(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        return mRectBar.left - dp2px(DEFAULT_FINDER_WIDTH_DP / 2) <= x
                && mRectBar.right + dp2px(DEFAULT_FINDER_WIDTH_DP / 2) >= x
                && mRectBar.top <= y && mRectBar.bottom >= y;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    public void setColor(@ColorInt int color) {
        mColor = color;
        invalidate();
    }

    public void setAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
        mAlpha = alpha;
        invalidate();

        if (null != mListener) {
            mListener.onAlphaChange(mAlpha);
        }
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setOnColorChangeListener(OnAlphaChangeListener listener) {
        mListener = listener;
    }

    public interface OnAlphaChangeListener {
        void onAlphaChange(@FloatRange(from = 0f, to = 1f) float alpha);
    }
}
