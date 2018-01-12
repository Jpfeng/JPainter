package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public abstract class BaseBrush {

    protected Paint mPaint;

    public BaseBrush() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public abstract void drawTrack(Canvas canvas, Track track);
}
