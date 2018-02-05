package com.jp.jpainter.brush;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.entity.Track;

/**
 *
 */
public class BrushTag01 extends BaseBrush<BrushTag01> {
    @Override
    public Drawable getIcon() {
        return new ColorDrawable(Color.CYAN);
    }

    @Override
    public String getName() {
        return "Impl01";
    }

    @Override
    public void initBrush() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GRAY);
        mPaint.setStrokeWidth(16f);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
    }

    @Override
    public BrushTag01 cloneBrush() {
        BrushTag01 brush = new BrushTag01();
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
    public void drawPreview(Canvas canvas, Track track) {

    }
}
