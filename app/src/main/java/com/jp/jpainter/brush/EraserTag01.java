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
public class EraserTag01 extends BaseBrush<EraserTag01> {
    @Override
    public Drawable getIcon() {
        return new ColorDrawable(Color.GREEN);
    }

    @Override
    public String getName() {
        return null;
    }

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
        brush.mSize = this.mSize;
        return brush;
    }

    @Override
    public void drawTrack(Canvas canvas, Track track) {
        Path path = track.getPath();
        canvas.drawPath(path, mPaint);
    }

    @Override
    public void drawPreview(Canvas canvas, Track track) {

        int layer = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);
        int color = mPaint.getColor();
        canvas.drawColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        Path path = track.getPath();
        canvas.drawPath(path, mPaint);
        canvas.restoreToCount(layer);
    }
}
