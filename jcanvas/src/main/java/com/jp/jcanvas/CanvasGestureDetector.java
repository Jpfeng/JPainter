package com.jp.jcanvas;

import android.content.Context;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.jp.jcanvas.entity.Offset;
import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.Scale;
import com.jp.jcanvas.entity.Velocity;

/**
 *
 */
public class CanvasGestureDetector {

    private static final int START_DRAW = 1;
    private static final int START_MOVE = 2;

    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    private int mTouchSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    private Handler mHandler;
    private VelocityTracker mVelocityTracker;
    private final OnCanvasGestureListener mListener;

    private boolean mIsFirstPointerTouching = false;
    private int mFirstPointerId = -1;

    private boolean mIsDrawing = false;
    private boolean mIsScaling = false;
    private boolean mIsMoving = false;

    private Point mDown;
    private Point mPivot;
    private Point mMoveLast;
    private float mSpanLast;
    private Velocity mPivotVelocity;
    private Path mPath;

    public CanvasGestureDetector(Context context, @NonNull OnCanvasGestureListener listener) {
        this(context, listener, null);
    }

    public CanvasGestureDetector(Context context, @NonNull OnCanvasGestureListener listener,
                                 Handler handler) {
        if (null != handler) {
            mHandler = new GestureHandler(handler);
        } else {
            mHandler = new GestureHandler();
        }

        mListener = listener;

        init(context);
    }

    private void init(Context context) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mDown = new Point();
        mPivot = new Point();
        mMoveLast = new Point();
        mPivotVelocity = new Velocity();
        mPath = new Path();
    }

    private class GestureHandler extends Handler {
        GestureHandler() {
            super();
        }

        GestureHandler(Handler handler) {
            super(handler.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_DRAW:
                    mIsDrawing = true;
                    break;

                case START_MOVE:
                    mIsMoving = true;
                    break;

                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int pointerId = event.getPointerId(event.getActionIndex());

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        boolean handled = false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mFirstPointerId = pointerId;
                mIsFirstPointerTouching = true;

                // 记录首个触摸点按下的坐标
                mDown.set(x, y);

                mPath.moveTo(x, y);

                handled = mListener.onActionDown(new Point(mDown));
                mHandler.sendEmptyMessageDelayed(START_DRAW, TAP_TIMEOUT);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 如果当前状态为非绘制
                if (!mIsDrawing) {
                    if (!mIsMoving && !mIsScaling) {
                        mHandler.removeMessages(START_DRAW);
                    }

                    boolean shouldCallListener = !mIsScaling;
                    mIsMoving = false;
                    mIsScaling = true;
                    mPivot.set(getPivot(event));
                    mSpanLast = getSpan(event);
                    if (shouldCallListener) {
                        mListener.onScaleStart(mPivot);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsScaling) {
                    if (1 == event.getPointerCount()) {
                        double pointerSpan = Math.hypot(x - mMoveLast.x, y - mMoveLast.y);
                        if (pointerSpan > mTouchSlop) {
                            mHandler.removeMessages(START_MOVE);
                            mIsScaling = false;
                            mIsMoving = true;

                            // scale end and move start
                            mListener.onScaleEnd(new Point(mPivot));
                            handled = mListener.onMove(new Point(x, y),
                                    new Offset(x - mMoveLast.x, y - mMoveLast.y));
                            mMoveLast.set(x, y);
                        }

                    } else {
                        // scale
                        final Point currentPivot = getPivot(event);
                        final float span = getSpan(event);
                        final Offset offset = new Offset(currentPivot.x - mPivot.x,
                                currentPivot.y - mPivot.y);
                        final Scale scale = new Scale(span / mSpanLast,
                                new Point(currentPivot));
                        handled = mListener.onScale(scale, offset);

                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);

                        final int count = event.getPointerCount();
                        float sumVX = 0f;
                        float sumVY = 0f;
                        for (int i = 0; i < count; i++) {
                            sumVX += velocityTracker.getXVelocity(i);
                            sumVY += velocityTracker.getYVelocity(i);
                        }

                        final float vX = sumVX / count;
                        final float vY = sumVY / count;

                        mPivot.set(currentPivot);
                        mSpanLast = span;
                        mPivotVelocity.set(vX, vY);
                    }

                } else if (mIsMoving) {
                    handled = mListener.onMove(new Point(x, y),
                            new Offset(x - mMoveLast.x, y - mMoveLast.y));
                    mMoveLast.set(x, y);

                } else {
                    if (mIsFirstPointerTouching) {
                        mPath.lineTo(event.getX(mFirstPointerId), event.getY(mFirstPointerId));
                    }

                    if (mIsDrawing) {
                        handled = mListener.onDrawPath(mPath);

                    } else {
                        float pointerX = event.getX(mFirstPointerId);
                        float pointerY = event.getY(mFirstPointerId);
                        double fingerSpan = Math.hypot(pointerX - mDown.x, pointerY - mDown.y);

                        if (mTouchSlop < fingerSpan) {
                            mHandler.removeMessages(START_DRAW);
                            mIsDrawing = true;
                            handled = mListener.onDrawPath(mPath);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // 判断首个触摸点是否还存在
                mIsFirstPointerTouching &= pointerId != mFirstPointerId;

                if (mIsScaling) {
                    if (2 == event.getPointerCount()) {
                        mHandler.sendEmptyMessageDelayed(START_MOVE, TAP_TIMEOUT);
                        mMoveLast.set(getPivot(event));

                    } else {
                        mPivot.set(getPivot(event));
                        mSpanLast = getSpan(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mHandler.removeMessages(START_DRAW);
                mHandler.removeMessages(START_MOVE);
                // 首个触摸点抬起但尚未记录路径，则判断为点击。
                if (pointerId == mFirstPointerId
                        && (!mIsDrawing && !mIsScaling && !mIsMoving)) {
                    mIsDrawing = true;
                    mPath.lineTo(event.getX(mFirstPointerId), event.getY(mFirstPointerId));
                    handled = mListener.onDrawPath(mPath);
                }

                mPath.reset();
                mIsFirstPointerTouching = false;
                mFirstPointerId = -1;

                if (mIsScaling) {
                    mListener.onScaleEnd(new Point(mPivot));
                }

                float vX = 0f;
                float vY = 0f;
                if (mIsMoving) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                    vX = velocityTracker.getXVelocity();
                    vY = velocityTracker.getYVelocity();

                } else if (mIsScaling) {
                    vX = mPivotVelocity.x;
                    vY = mPivotVelocity.y;
                }

                boolean fling = (Math.abs(vX) > mMinFlingVelocity)
                        || (Math.abs(vY) > mMinFlingVelocity);
                fling &= mIsScaling || mIsMoving;
                handled |= mListener.onActionUp(new Point(x, y), fling, new Velocity(vX, vY));

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                mIsDrawing = false;
                mIsScaling = false;
                mIsMoving = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;
        }

        return handled;
    }

    private void cancel() {
        mHandler.removeMessages(START_DRAW);
        mHandler.removeMessages(START_MOVE);
        mPath.reset();
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mIsFirstPointerTouching = false;
        mFirstPointerId = -1;
        mIsDrawing = false;
        mIsScaling = false;
        mIsMoving = false;
    }

    private Point getPivot(MotionEvent event) {
        final boolean pointerUp = MotionEvent.ACTION_POINTER_UP == event.getActionMasked();
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        float sumX = 0f;
        float sumY = 0f;
        final int count = event.getPointerCount();

        for (int i = 0; i < count; i++) {
            if (skipIndex == i) {
                continue;
            }
            sumX += event.getX(i);
            sumY += event.getY(i);
        }

        final int div = pointerUp ? count - 1 : count;
        final float pivotX = sumX / div;
        final float pivotY = sumY / div;

        return new Point(pivotX, pivotY);
    }

    private float getSpan(MotionEvent event) {
        final boolean pointerUp = MotionEvent.ACTION_POINTER_UP == event.getActionMasked();
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        float sumX = 0f;
        float sumY = 0f;
        final int count = event.getPointerCount();

        for (int i = 0; i < count; i++) {
            if (skipIndex == i) {
                continue;
            }
            sumX += event.getX(i);
            sumY += event.getY(i);
        }

        final int div = pointerUp ? count - 1 : count;
        final float pivotX = sumX / div;
        final float pivotY = sumY / div;

        float devSumX = 0f;
        float devSumY = 0f;

        for (int i = 0; i < count; i++) {
            if (skipIndex == i) {
                continue;
            }

            devSumX += Math.abs(event.getX(i) - pivotX);
            devSumY += Math.abs(event.getY(i) - pivotY);
        }

        final float devX = devSumX / div;
        final float devY = devSumY / div;

        final float spanX = devX * 2;
        final float spanY = devY * 2;

        return (float) Math.hypot(spanX, spanY);
    }

    public interface OnCanvasGestureListener {
        boolean onActionDown(Point down);

        boolean onDrawPath(final Path path);

        void onScaleStart(Point pivot);

        boolean onScale(Scale scale, Offset pivotOffset);

        void onScaleEnd(Point pivot);

        boolean onMove(Point focus, Offset offset);

        boolean onActionUp(Point focus, boolean fling, Velocity velocity);
    }
}
