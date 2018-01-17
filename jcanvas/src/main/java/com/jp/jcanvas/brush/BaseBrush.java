package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public abstract class BaseBrush {

    protected Paint mPaint;
    protected boolean eraser;

    public BaseBrush() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eraser = false;
    }

    public BaseBrush(BaseBrush src) {
        this.mPaint = new Paint(src.mPaint);
    }

    public void painter() {
        eraser = false;
    }

    public void eraser() {
        eraser = true;
    }

    public abstract BaseBrush cloneBrush();

    public abstract void drawTrack(Canvas canvas, Track track);
}
