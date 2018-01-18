package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public abstract class BaseBrush<T extends BaseBrush> {

    protected Paint mPaint;
    @ColorInt
    protected int mColor;
    protected float mWidth;

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

    public void setWidth(float width) {
        mWidth = width;
        mPaint.setStrokeWidth(width);
    }

    public abstract void initBrush();

    public abstract T cloneBrush();

    public abstract void drawTrack(Canvas canvas, Track track);

    public abstract void drawPreview();
}
