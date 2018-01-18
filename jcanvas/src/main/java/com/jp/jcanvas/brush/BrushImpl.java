package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.PointV;
import com.jp.jcanvas.entity.Track;

import java.util.LinkedList;

/**
 *
 */
public class BrushImpl extends BaseBrush {

    private PathMeasure mPathMeasure;
    private float[] mPos;
    private float[] mTan;
    private RectF mRect;
    private int center;
    private int middle;
    private int edge;
    private float mDegree;

    private float w;
    private float h;
    private float d;

    public BrushImpl() {
        super();

        center = Color.argb(255, 128, 128, 128);
        middle = Color.argb(128, 192, 192, 192);
        edge = Color.argb(0, 255, 255, 255);

        w = 48f;
        h = 48f;
        d = 4f;

//        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(16f);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        mPaint.setColor(Color.argb(255, 192, 192, 192));
//        mPaint.setStrokeJoin(Paint.Join.ROUND);
//        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathMeasure = new PathMeasure();
        mPos = new float[2];
        mTan = new float[2];

        mRect = new RectF();
    }

    @Override
    public void initBrush() {

    }

    @Override
    public BaseBrush cloneBrush() {
        BrushImpl brush = new BrushImpl();
        brush.mPaint.set(mPaint);
        return brush;
    }

    @Override
    public void drawTrack(Canvas canvas, Track track) {
        drawPath(canvas, track.getPath());
//        drawPoints(canvas, track.getStations());
//        drawSections(canvas, track.getSections());
    }

    @Override
    public void drawPreview() {

    }

    private float minW = 9f;
    private float maxW = 27f;

    private float minD = 16f;
    private float maxD = 160f;

    private float minL = 12f;
    private float maxL = 120f;

    private void drawSections(Canvas canvas, LinkedList<Path> sections) {
//        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        for (Path p : sections) {
            canvas.drawPath(p, mPaint);
        }

//        float lastW = 0f;
//        for (Path p : sections) {
//            mPathMeasure.setPath(p, false);
//            float length = mPathMeasure.getLength();
//
//            if (0 == length) {
//                continue;
//            }
//
//            float lengthFraction = (Math.min(Math.max(minL, length), maxL) - minL) / (maxL - minL);
//            float w1 = minW + (maxW - minW) * (1 - lengthFraction);
//            if (0 == lastW) {
//                lastW = w1;
//            }
//
//            int count = (int) (length / d);
//            if (0 == count) {
//                count = 1;
//            }
//
//            for (int i = 0; i <= count; i++) {
//                float fraction = i / ((float) count);
//                mPathMeasure.getPosTan(fraction * length, mPos, mTan);
//
//                float realW = lastW + (w1 - lastW) * fraction;
//                mRect.set(mPos[0] - realW / 2, mPos[1] - realW, mPos[0] + realW / 2, mPos[1] + realW);
//                canvas.drawOval(mRect, mPaint);
//            }
//
//            lastW = w1;
//        }
    }

    private void drawPoints(Canvas canvas, LinkedList<PointV> points) {

    }

    private void drawPath(Canvas canvas, Path path) {

//        if (eraser) {
//            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
//        } else {
//            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
//        }
        canvas.drawPath(path, mPaint);

//        mPathMeasure.setPath(path, false);
//        int count = (int) (mPathMeasure.getLength() / d);
//        for (int i = 0; i < count; i++) {
//            mPathMeasure.getPosTan(d * i, mPos, mTan);
//
//            mRect.set(mPos[0] - w, mPos[1] - h, mPos[0] + w, mPos[1] + h);
//
////            mDegree = (float) (Math.atan2(mTan[1], mTan[0]) * 180f / Math.PI);
////            mDegree = 45f;
////            canvas.save();
////            canvas.rotate(mDegree, mRect.centerX(), mRect.centerY());
//            mPaint.setShader(new RadialGradient(mRect.centerX(), mRect.centerY(), w,
//                    new int[]{center, middle, edge}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
//            canvas.drawOval(mRect, mPaint);
////            canvas.restore();
//        }
    }
}
