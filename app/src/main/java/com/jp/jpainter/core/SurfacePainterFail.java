package com.jp.jpainter.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.jp.jpainter.R;
import com.jp.jpainter.utils.LogUtil;

/**
 *
 */
public class SurfacePainterFail extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_PAINTING = 1;
    private static final int STATUS_SCALING = 3;
    private static final int STATUS_MOVING = 4;
    private static final int STATUS_DESTROYED = 5;

    // time per frame. 60fps
    private static final int mFrameTime = (int) (1000 / 60);

    // SurfaceHolder
    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Paint mPaint;
    Path mPath;

    private int mCurrentStatus;
    private float mCurrentOffsetX = 0;
    private float mCurrentOffsetY = 0;
    private float mCurrentScale = 1.0f;

    public SurfacePainterFail(Context context) {
        this(context, null);
    }

    public SurfacePainterFail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfacePainterFail(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(4.0f);

        CornerPathEffect effect = new CornerPathEffect(30);
        mPaint.setPathEffect(effect);

        mPath = new Path();
    }

    private void drawContent() {
        if (STATUS_IDLE == getCurrentStatus() || STATUS_DESTROYED == getCurrentStatus()) {
            return;
        }

        try {
            mCanvas = mHolder.lockCanvas();

            mCanvas.scale(mCurrentScale, mCurrentScale, scalePivotX, scalePivotY);
            mCanvas.translate(mCurrentOffsetX, mCurrentOffsetY);
            // 进行绘图操作
            drawCanvasBackground();
//            drawReferencePoint();
            mCanvas.drawPath(mPath, mPaint);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    // TODO: debug
    private void drawReferencePoint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8.0f);
        mCanvas.drawPoint(mWidth / 2, mHeight / 2, paint);
        paint.setStrokeWidth(4.0f);
        mCanvas.drawRect(0, 0, mWidth, mHeight, paint);
    }

    private int pointer1Index = -1;
    private int pointer2Index = -1;
    private int currentFingerCount;
    private boolean isFirstFingerTouching = false;

    private float downX;
    private float downY;

    private float moveStartX;
    private float moveStartY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isFirstFingerTouching = true;
                pointer1Index = event.getActionIndex();
                currentFingerCount = event.getPointerCount();
                setCurrentStatus(STATUS_PAINTING);

                mPath.moveTo(x / mCurrentScale + mCurrentOffsetX, y / mCurrentScale + mCurrentOffsetY);
                downX = x;
                downY = y;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                currentFingerCount = event.getPointerCount();

                if (2 == currentFingerCount) {
                    if (-1 == pointer2Index) {
                        pointer2Index = event.getActionIndex();

                    } else {
                        pointer1Index = event.getActionIndex();
                    }

                    if (shouldScaling(event)) {
                        setCurrentStatus(STATUS_SCALING);
                        computeScaleControlPoints(event);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                switch (getCurrentStatus()) {
                    case STATUS_PAINTING:
                        if (isFirstFingerTouching) {
                            mPath.lineTo(x / mCurrentScale + mCurrentOffsetX, y / mCurrentScale + mCurrentOffsetY);
                        }
                        break;

                    case STATUS_SCALING:
                        float afterX1 = event.getX(pointer1Index);
                        float afterY1 = event.getY(pointer1Index);
                        float afterX2 = event.getX(pointer2Index);
                        float afterY2 = event.getY(pointer2Index);

                        double beforeSpan = Math.hypot(
                                scaleStartX1 - scaleStartX2, scaleStartY1 - scaleStartY2);
                        double afterSpan = Math.hypot(afterX1 - afterX2, afterY1 - afterY2);
                        mCurrentScale = (float) (mCurrentScale * (afterSpan / beforeSpan));

                        float afterPivotX = Math.min(afterX1, afterX2)
                                + (Math.abs(afterX1 - afterX2) / 2);
                        float afterPivotY = Math.min(afterY1, afterY2)
                                + (Math.abs(afterY1 - afterY2) / 2);
                        mCurrentOffsetX += afterPivotX - scalePivotX;
                        mCurrentOffsetY += afterPivotY - scalePivotY;

                        computeScaleControlPoints(event);
                        break;

                    case STATUS_MOVING:
                        mCurrentOffsetX += (x - moveStartX) / mCurrentScale;
                        mCurrentOffsetY += (y - moveStartY) / mCurrentScale;

                        moveStartX = x;
                        moveStartY = y;
                        break;

                    case STATUS_IDLE:
                    default:
                        Log.w(this.getClass().getSimpleName(), "incorrect status");
                        break;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                currentFingerCount = event.getPointerCount() - 1;
                int actionIndex = event.getActionIndex();

                if (actionIndex == pointer1Index) {
                    isFirstFingerTouching = false;
                }

                if ((STATUS_SCALING == getCurrentStatus())
                        && (actionIndex == pointer1Index || actionIndex == pointer2Index)) {
                    setCurrentStatus(STATUS_MOVING);
                    moveStartX = actionIndex == pointer1Index ? event.getX(pointer2Index)
                            : event.getX(pointer1Index);
                    moveStartY = actionIndex == pointer1Index ? event.getY(pointer2Index)
                            : event.getY(pointer1Index);

                    if (actionIndex == pointer1Index) {
                        pointer1Index = -1;
                    } else {
                        pointer2Index = -1;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                currentFingerCount = 0;
                isFirstFingerTouching = false;
                pointer1Index = -1;
                pointer2Index = -1;
                setCurrentStatus(STATUS_IDLE);
                break;

            case MotionEvent.ACTION_CANCEL:
                LogUtil.d("ACTION_CANCEL ->", "actionIndex = " + event.getActionIndex());
                break;
        }

        // TODO: debug
        if (null != debugTV) {
            debugTV.setText("Scale = " + mCurrentScale + ", OffsetX = " + mCurrentOffsetX + ", OffsetY = " + mCurrentOffsetY);
        }
        return true;
    }

    private float scaleStartX1;
    private float scaleStartY1;
    private float scaleStartX2;
    private float scaleStartY2;
    private float scalePivotX;
    private float scalePivotY;

    private void computeScaleControlPoints(MotionEvent event) {
        scaleStartX1 = event.getX(pointer1Index);
        scaleStartY1 = event.getY(pointer1Index);
        scaleStartX2 = event.getX(pointer2Index);
        scaleStartY2 = event.getY(pointer2Index);
        scalePivotX = Math.min(scaleStartX1, scaleStartX2)
                + (Math.abs(scaleStartX1 - scaleStartX2) / 2);
        scalePivotY = Math.min(scaleStartY1, scaleStartY2)
                + (Math.abs(scaleStartY1 - scaleStartY2) / 2);
    }

    private boolean shouldScaling(MotionEvent event) {
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        float firstFingerX = event.getX(pointer1Index);
        float firstFingerY = event.getY(pointer1Index);

        double firstFingerSpan = Math.hypot(firstFingerX - downX, firstFingerY - downY);
        long timeInterval = eventTime - downTime;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        return STATUS_MOVING == getCurrentStatus()
                || (viewConfiguration.getScaledTouchSlop() > firstFingerSpan
                && timeInterval < ViewConfiguration.getTapTimeout());
    }

    private void setCurrentStatus(int currentStatus) {
        mCurrentStatus = currentStatus;
    }

    public int getCurrentStatus() {
        return mCurrentStatus;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        setZOrderOnTop(true);
//        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(mCanvas);

        setCurrentStatus(STATUS_IDLE);
        new Thread(this).start();
    }

    private int mHeight;
    private int mWidth;

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHeight = height;
        mWidth = width;

        mCanvas = mHolder.lockCanvas();
        drawCanvasBackground();
        mHolder.unlockCanvasAndPost(mCanvas);

        Log.i(this.getClass().getSimpleName(),
                "surfaceChanged: width = " + width + ", height = " + height);
    }

    private void drawCanvasBackground() {


        Bitmap t = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);

        BitmapShader bs = new BitmapShader(t, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(bs);

        mCanvas.drawPaint(p);

//        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8, 0, 0, 0xFF666666);

        mCanvas.drawRect(0, 0, mWidth, mHeight, paint);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setCurrentStatus(STATUS_DESTROYED);
    }

    @Override
    public void run() {
        while (STATUS_DESTROYED != getCurrentStatus()) {
            long start = System.currentTimeMillis();
            drawContent();
            long end = System.currentTimeMillis();

            long time = end - start;
            if (time < mFrameTime) {
                try {
                    Thread.sleep(mFrameTime - time);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // TODO: debug
    private TextView debugTV;

    public void setDebugTV(TextView tv) {
        debugTV = tv;
    }
}
