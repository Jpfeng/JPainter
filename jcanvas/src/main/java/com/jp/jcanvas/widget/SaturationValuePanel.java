package com.jp.jcanvas.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
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
class SaturationValuePanel extends View {

    private Paint mPanelPaint;
    private Paint mPointerPaint;
    private RectF mPanelRect;
    private PanelGestureDetector mGDetector;

    private float[] mHSV;

    private int mPointerSize;
    private int mPointerStrokeWidth;
    private Drawable mPointerDrawable;

    private OnColorChangeListener mSVListener;

    public SaturationValuePanel(Context context) {
        this(context, null);
    }

    public SaturationValuePanel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.SVPanelStyle);
    }

    public SaturationValuePanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.SaturationValuePanel, defStyleAttr, R.style.DefaultSVPanelStyle);

        mPointerSize = ta.getDimensionPixelSize(
                R.styleable.SaturationValuePanel_svp_pointerSize, 0);
        mPointerStrokeWidth = ta.getDimensionPixelSize(
                R.styleable.SaturationValuePanel_svp_pointerStrokeWidth, 0);
        mPointerDrawable = ta.getDrawable(R.styleable.SaturationValuePanel_svp_pointerStroke);

        float hue = ta.getFloat(R.styleable.SaturationValuePanel_svp_hue, 0f);
        float saturation = ta.getFloat(R.styleable.SaturationValuePanel_svp_saturation, 1f);
        float value = ta.getFloat(R.styleable.SaturationValuePanel_svp_value, 1f);

        mHSV = new float[]{0f, 1f, 1f};
        mHSV[0] = hue % 360f;
        mHSV[1] = Math.max(Math.min(saturation, 1f), 0f);
        mHSV[2] = Math.max(Math.min(value, 1f), 0f);

        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        mPanelPaint = new Paint();
        mPanelPaint.setAntiAlias(true);

        mPointerPaint = new Paint();
        mPointerPaint.setAntiAlias(true);
        mPointerPaint.setStyle(Paint.Style.FILL);

        mPanelRect = new RectF();

        // 需要关闭硬件加速
        // https://stackoverflow.com/questions/12445583/issue-with-composeshader-on-android-4-1-1
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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
        canvas.drawRect(mPanelRect, mPanelPaint);

        float cX = mPanelRect.left + mHSV[1] * (mPanelRect.right - mPanelRect.left);
        float cY = mPanelRect.top + (1 - mHSV[2]) * (mPanelRect.bottom - mPanelRect.top);
        float r = mPointerSize / 2f;

        if (mPointerDrawable instanceof ColorDrawable) {
            mPointerPaint.setColor(((ColorDrawable) mPointerDrawable).getColor());
            canvas.drawCircle(cX, cY, mPointerSize / 2f, mPointerPaint);

        } else {
            mPointerDrawable.mutate().setBounds((int) (cX - r), (int) (cY - r),
                    (int) (cX + r), (int) (cY + r));
            mPointerDrawable.draw(canvas);
        }

        int color = Color.HSVToColor(mHSV);
        mPointerPaint.setColor(color);
        canvas.drawCircle(cX, cY, r - mPointerStrokeWidth, mPointerPaint);
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
