package com.jp.jcanvas.brushselector;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.entity.Track;

/**
 *
 */
public class BrushPreview extends View {

    private BaseBrush mBrush;
    private Track mTrack;

    public BrushPreview(Context context) {
        this(context, null);
    }

    public BrushPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
            wResult = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.AT_MOST == wMode) {
                wResult = Math.min(wResult, wSize);
            }
        }

        if (MeasureSpec.EXACTLY == hMode) {
            hResult = hSize;
        } else {
            hResult = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
            if (MeasureSpec.AT_MOST == hMode) {
                hResult = Math.min(hResult, hSize);
            }
        }

        setMeasuredDimension(wResult, hResult);

        mTrack = generateTrack();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setBrush(BaseBrush brush) {
        mBrush = brush;
        invalidate();
    }

    private Track generateTrack() {
        return null;
    }
}
