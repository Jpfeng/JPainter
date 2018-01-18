package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public class EraserTag01 extends BaseBrush<EraserTag01> {
    @Override
    public void initBrush() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(32f);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    @Override
    public EraserTag01 cloneBrush() {
        EraserTag01 brush = new EraserTag01();
        brush.mPaint.set(this.mPaint);
        brush.mColor = this.mColor;
        brush.mWidth = this.mWidth;
        return brush;
    }

    @Override
    public void drawTrack(Canvas canvas, Track track) {
        Path path = track.getPath();
        canvas.drawPath(path, mPaint);
    }

    @Override
    public void drawPreview() {

    }
}
