package com.jp.jpainter.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 *
 */
public class SurfacePainter extends SurfaceView implements
        SurfaceHolder.Callback/*, Runnable, OnGestureListener, OnScaleGestureListener*/ {

    // time per frame. 60fps
//    private static final int mFrameTime = (int) ((1000 / 60f) + 0.5f);

    // SurfaceHolder
    private SurfaceHolder mHolder;
    private Canvas mCanvas;
//    private boolean mIsDrawing;

    private Paint mPaint;
    Path mPath;

    //    private GestureDetector mGDetector;
//    private ScaleGestureDetector mScaleGDetector;

    public SurfacePainter(Context context) {
        this(context, null);
    }

    public SurfacePainter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfacePainter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);
//        setLongClickable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(4.0f);

        CornerPathEffect effect = new CornerPathEffect(30);
        mPaint.setPathEffect(effect);

        mPath = new Path();

//        mGDetector = new GestureDetector(getContext(), this);
//        mGDetector.setIsLongpressEnabled(false);
//
//        mScaleGDetector = new ScaleGestureDetector(getContext(), this);

//        setOnTouchListener((v, event) -> {
//            mScaleGDetector.onTouchEvent(event);
//            mGDetector.onTouchEvent(event);
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                v.performClick();
//            }
//            return false;
//        });
    }

    private int downIndex;
    private boolean isFirstFingerTouching = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(x, y);
                downIndex = event.getActionIndex();
                isFirstFingerTouching = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isFirstFingerTouching) {
                    mPath.lineTo(x, y);
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int actionIndex = event.getActionIndex();
                if (actionIndex == downIndex) {
                    isFirstFingerTouching = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                isFirstFingerTouching = false;
                break;
        }

        drawContent();
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        mIsDrawing = true;
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.LTGRAY);
        mHolder.unlockCanvasAndPost(mCanvas);
//        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        mIsDrawing = false;
    }

//    @Override
//    public void run() {
//        long start = System.currentTimeMillis();
//
//        while (mIsDrawing) {
//            drawContent();
//        }
//
//        long end = System.currentTimeMillis();
//
//        long time = end - start;
//        if (time < mFrameTime) {
//            try {
//                Thread.sleep(mFrameTime - time);
//
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void drawContent() {
        try {
            mCanvas = mHolder.lockCanvas();
            // 进行绘图操作
//            mCanvas.scale(currentFactor, currentFactor);

            mCanvas.drawColor(Color.LTGRAY);
            mCanvas.drawPath(mPath, mPaint);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

//    @Override
//    public boolean onDown(MotionEvent e) {
//        Log.d("gesture detector -> ", "onDown: " + e.getX() + ", " + e.getY());
//        mPath.moveTo(e.getX(), e.getY());
//        mPath.lineTo(e.getX(), e.getY());
//        return true;
//    }
//
//    @Override
//    public void onShowPress(MotionEvent e) {
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        Log.d("gesture detector -> ", "onSingleTapUp: " + e.getX() + ", " + e.getY());
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        Log.d("gesture detector -> ", "onScroll: " + e2.getX() + ", " + e2.getY());
//        mPath.lineTo(e2.getX(), e2.getY());
//        return true;
//    }
//
//    @Override
//    public void onLongPress(MotionEvent e) {
//        // long press disabled
//    }
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        return false;
//    }
//
//    float currentFactor = 1.0f;
//
//    @Override
//    public boolean onScale(ScaleGestureDetector detector) {
//        currentFactor = detector.getScaleFactor();
//        float focusX = detector.getFocusX();
//        float focusY = detector.getFocusY();
//        Log.d("gesture detector -> ", "onScale: " + currentFactor + "@(" + focusX + ", " + focusY + ")");
//        return false;
//    }
//
//    @Override
//    public boolean onScaleBegin(ScaleGestureDetector detector) {
//        float focusX = detector.getFocusX();
//        float focusY = detector.getFocusY();
//        Log.d("gesture detector -> ", "onScaleBegin");
//        return true;
//    }
//
//    @Override
//    public void onScaleEnd(ScaleGestureDetector detector) {
//
//    }
}
