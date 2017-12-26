package com.jp.jcanvas.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 *
 */
class SaturationValuePanel extends View {

    private static final int COLOR_POINTER_STROKE = Color.GRAY;
    private static final int DEFAULT_POINTER_SIZE_DP = 24;
    private static final int DEFAULT_POINTER_EDGE_DP = 4;

    private Paint mPanelPaint;
    private Paint mPointPaint;
    private RectF mPanelRect;
    private PanelGestureDetector mGDetector;

    private float[] mHSV;

    private OnColorChangeListener mSVListener;

    public SaturationValuePanel(Context context) {
        this(context, null);
    }

    public SaturationValuePanel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SaturationValuePanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public SaturationValuePanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mPanelPaint = new Paint();
        mPanelPaint.setAntiAlias(true);

        mPointPaint = new Paint();
        mPointPaint.setAntiAlias(true);
        mPointPaint.setStyle(Paint.Style.FILL);

        mPanelRect = new RectF();

        mHSV = new float[]{0f, 1f, 1f};

        mGDetector = new PanelGestureDetector(context, mPanelRect, (s, v) -> {
            mHSV[1] = s;
            mHSV[2] = v;
            invalidate();

            if (null != mSVListener) {
                mSVListener.onColorChange(Color.HSVToColor(mHSV));
            }
        });

        setOnTouchListener((v, event) -> {
            boolean handled = false;

            if (MotionEvent.ACTION_UP == event.getAction()) {
                handled = performClick();
            }

            return handled | mGDetector.onTouchEvent(event);
        });
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

        if (MeasureSpec.EXACTLY == wMode && MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;
            wResult = wSize;

        } else if (MeasureSpec.EXACTLY == wMode) {
            wResult = wSize;
            hResult = wSize;

        } else if (MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;
            wResult = hSize;

        } else {
            hResult = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
            if (MeasureSpec.AT_MOST == hMode) {
                hResult = Math.min(hResult, hSize);
            }

            wResult = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.AT_MOST == wMode) {
                wResult = Math.min(wResult, wSize);
            }
        }

        setMeasuredDimension(wResult, hResult);

        int wLength = wResult - getPaddingLeft() - getPaddingRight();
        int hLength = hResult - getPaddingTop() - getPaddingBottom();
        int side = Math.min(wLength, hLength);

        float l = getPaddingLeft() + (wLength - side) / 2f;
        float t = getPaddingTop() + (hLength - side) / 2f;
        float r = l + side;
        float b = t + side;
        mPanelRect.set(l, t, r, b);

        mGDetector.setArea(mPanelRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPanelPaint.setShader(generateSVShader());
        canvas.clipRect(mPanelRect);

        int layer = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(),
                null, Canvas.ALL_SAVE_FLAG);
        // https://stackoverflow.com/questions/12445583/issue-with-composeshader-on-android-4-1-1
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        canvas.drawRect(mPanelRect, mPanelPaint);
        canvas.restoreToCount(layer);

        float cX = mPanelRect.left + mHSV[1] * (mPanelRect.right - mPanelRect.left);
        float cY = mPanelRect.top + (1 - mHSV[2]) * (mPanelRect.bottom - mPanelRect.top);

        mPointPaint.setColor(COLOR_POINTER_STROKE);
        canvas.drawCircle(cX, cY,
                dp2px(DEFAULT_POINTER_SIZE_DP / 2 + DEFAULT_POINTER_EDGE_DP), mPointPaint);

        int color = Color.HSVToColor(mHSV);
        mPointPaint.setColor(color);
        canvas.drawCircle(cX, cY, dp2px(DEFAULT_POINTER_SIZE_DP / 2), mPointPaint);
    }

    private ComposeShader generateSVShader() {
        Shader vShader = new LinearGradient(mPanelRect.left, mPanelRect.top,
                mPanelRect.left, mPanelRect.bottom,
                Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);

        float[] hsv = {mHSV[0], 1f, 1f};
        int color = Color.HSVToColor(hsv);
        Shader sShader = new LinearGradient(mPanelRect.left, mPanelRect.top,
                mPanelRect.right, mPanelRect.top,
                Color.WHITE, color, Shader.TileMode.CLAMP);

        return new ComposeShader(vShader, sShader, PorterDuff.Mode.MULTIPLY);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    public void setHue(@FloatRange(from = 0f, to = 360f) float hue) {
        mHSV[0] = hue;
        invalidate();

        if (null != mSVListener) {
            mSVListener.onColorChange(Color.HSVToColor(mHSV));
        }
    }

    public void setColor(@ColorInt int color) {
        Color.colorToHSV(color, mHSV);
        invalidate();

        if (null != mSVListener) {
            mSVListener.onColorChange(Color.HSVToColor(mHSV));
        }
    }

    @ColorInt
    public int getColor() {
        return Color.HSVToColor(mHSV);
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mSVListener = listener;
    }

    public interface OnColorChangeListener {
        void onColorChange(@ColorInt int color);
    }
}
