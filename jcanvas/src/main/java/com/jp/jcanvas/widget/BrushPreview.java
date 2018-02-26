package com.jp.jcanvas.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.jp.jcanvas.R;
import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.entity.PointV;
import com.jp.jcanvas.entity.Track;
import com.jp.jcanvas.entity.Velocity;

/**
 *
 */
class BrushPreview extends View {

    private BaseBrush mBrush;
    private Track mTrack;
    private Paint mPaint;

    public BrushPreview(Context context) {
        this(context, null);
    }

    public BrushPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader bgShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(bgShader);
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

        mTrack = generateTrack(wResult, hResult);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPaint(mPaint);
        mBrush.drawPreview(canvas, mTrack);
    }

    public void setBrush(BaseBrush brush) {
        mBrush = brush;
        invalidate();
    }

    private Track generateTrack(int w, int h) {
        int paddingX = (int) Math.min(w * 0.05f, 48);
        int paddingY = (int) Math.min(h * 0.05f, 48);

        int pathW = w - 2 * paddingX;
        int pathH = h - 2 * paddingY;

        Path path = new Path();
        path.moveTo(paddingX, h - paddingY);
        path.cubicTo(.15f * pathW, .65f * pathH, .85f * pathW, .35f * pathH, w - paddingX, paddingY);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        int length = (int) (pathMeasure.getLength() + 0.5f);

        Track track = new Track();
        track.departure(new PointV(paddingX, h - paddingY, new Velocity(0, 0)));
        float[] pos = new float[2];
        float[] tan = new float[2];
        for (int i = 1; i <= length; i++) {
            pathMeasure.getPosTan(i, pos, tan);
            track.addStation(new PointV(pos[0], pos[1], new Velocity(0, 0)));
        }

        return track;
    }
}
