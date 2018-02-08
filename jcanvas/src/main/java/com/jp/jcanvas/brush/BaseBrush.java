package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public abstract class BaseBrush<T extends BaseBrush> {

    protected static final int MAX_SIZE = 99;

    @ColorInt
    protected int mColor;
    protected Paint mPaint;
    protected float mSize;
    protected float mAlpha;

    public BaseBrush() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        initBrush();
    }

    public BaseBrush(BaseBrush src) {
        this.mPaint = new Paint(src.mPaint);
    }

    public void setColor(@ColorInt int color) {
        mColor = color;
        mPaint.setColor(color);
    }

    public void setSize(float size) {
        mSize = size;
        mPaint.setStrokeWidth(size * MAX_SIZE);
    }

    public void setAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
        mAlpha = alpha;
        mPaint.setAlpha((int) (alpha * 255));
    }

    public abstract Drawable getIcon();

    public abstract CharSequence getName();

    public abstract void initBrush();

    public abstract T cloneBrush();

    public abstract void drawTrack(Canvas canvas, Track track);

    public abstract void drawPreview(Canvas canvas, Track track);
}
