package com.jp.jcanvas;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.jp.jcanvas.entity.Offset;
import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.PointV;
import com.jp.jcanvas.entity.Scale;
import com.jp.jcanvas.entity.Track;
import com.jp.jcanvas.entity.Velocity;

import java.lang.ref.WeakReference;

/**
 *
 */
class CanvasGestureDetector {

    private static final int START_DRAW = 1;
    private static final int START_MOVE = 2;

    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    private int mTouchSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    private Handler mHandler;
    private VelocityTracker mVelocityTracker;
    private final CanvasGestureListener mListener;

    private boolean mIsFirstPointerTouching = false;
    private int mFirstPointerId = -1;

    private boolean mIsDrawing = false;
    private boolean mIsScaling = false;
    private boolean mIsMoving = false;

    private Point mDown;
    private Point mLast;
    private Point mPivot;
    private Point mMoveLast;
    private float mSpanLast;
    private Velocity mPivotVelocity;
    private Track mTrack;

    CanvasGestureDetector(Context context, @NonNull CanvasGestureListener listener) {
        mHandler = new GestureHandler(this);
        mListener = listener;
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mDown = new Point();
        mLast = new Point();
        mPivot = new Point();
        mMoveLast = new Point();
        mPivotVelocity = new Velocity();
        mTrack = new Track();
    }

    static class GestureHandler extends Handler {
        WeakReference<CanvasGestureDetector> mGD;

        GestureHandler(CanvasGestureDetector gD) {
            super();
            mGD = new WeakReference<>(gD);
        }

        @Override
        public void handleMessage(Message msg) {
            CanvasGestureDetector gD = mGD.get();
            switch (msg.what) {
                case START_DRAW:
                    gD.mIsDrawing = true;
                    break;

                case START_MOVE:
                    gD.mIsMoving = true;
                    break;

                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }

    boolean onTouchEvent(MotionEvent event) {
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

            {
                final VelocityTracker velocityTracker = mVelocityTracker;
                Velocity v = getVelocity(velocityTracker, event);
                mTrack.departure(new PointV(x, y, v));
                mLast.set(x, y);
            }

            handled = mListener.onActionDown(new Point(mDown));
            mHandler.sendEmptyMessageDelayed(START_DRAW, TAP_TIMEOUT);
            break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 如果当前状态为非绘制，此时应进入缩放状态
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
                    // 当前只有一个触摸点且状态为 mIsScaling 。此时应当进入移动状态
                    // 判断是否应该进入 mIsMoving 状态
                    if (1 == event.getPointerCount()) {
                        double pointerSpan = Math.hypot(x - mMoveLast.x, y - mMoveLast.y);
                        if (pointerSpan > mTouchSlop) {
                            mHandler.removeMessages(START_MOVE);
                            mIsScaling = false;
                            mIsMoving = true;

                            // scale end and move start
                            final VelocityTracker velocityTracker = mVelocityTracker;
                            Velocity v = getVelocity(velocityTracker, event);
                            mListener.onScaleEnd(new Point(mPivot));
                            handled = mListener.onMove(new PointV(x, y, v),
                                    new Offset(x - mMoveLast.x, y - mMoveLast.y));
                            mMoveLast.set(x, y);
                        }

                    } else {
                        // scale
                        final Point currentPivot = getPivot(event);
                        final float span = getSpan(event);
                        final Offset offset = new Offset(currentPivot.x - mPivot.x,
                                currentPivot.y - mPivot.y);
                        // 计算控制点的速率
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        Velocity vP = getVelocity(velocityTracker, event);

                        final Scale scale = new Scale(span / mSpanLast,
                                new PointV(currentPivot.x, currentPivot.y, vP));
                        handled = mListener.onScale(scale, offset);

                        mPivot.set(currentPivot);
                        mSpanLast = span;
                        mPivotVelocity.set(vP);
                    }

                } else if (mIsMoving) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    Velocity v = getVelocity(velocityTracker, event);
                    handled = mListener.onMove(new PointV(x, y, v),
                            new Offset(x - mMoveLast.x, y - mMoveLast.y));
                    mMoveLast.set(x, y);

                } else if (mIsFirstPointerTouching) {
                    // 其他情况，包括 mIsDrawing = true ，和三个状态均为 false 两种情况
                    // 只有在首个触摸点还存在时才会触发绘制
                    int pointerIndex = event.findPointerIndex(mFirstPointerId);
                    float x1 = event.getX(pointerIndex);
                    float y1 = event.getY(pointerIndex);
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    Velocity v = getVelocity(velocityTracker, event);
                    mTrack.addStation(new PointV(x1, y1, v));

                    if (mIsDrawing) {
                        handled = mListener.onDrawPath(new PointV(x1, y1, v), new Track(mTrack));

                    } else {
                        // 判断是否应该进入 mIsDrawing 状态
                        float pointerX = event.getX(pointerIndex);
                        float pointerY = event.getY(pointerIndex);
                        double fingerSpan = Math.hypot(pointerX - mDown.x, pointerY - mDown.y);

                        if (mTouchSlop < fingerSpan) {
                            mHandler.removeMessages(START_DRAW);
                            mIsDrawing = true;
                            handled = mListener.onDrawPath(
                                    new PointV(x1, y1, v), new Track(mTrack));
                        }
                    }

                    mLast.set(x, y);
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // 判断首个触摸点是否还存在
                mIsFirstPointerTouching &= pointerId != mFirstPointerId;

                if (mIsScaling) {
                    if (2 == event.getPointerCount()) {
                        // 如果是缩放状态且抬起后只剩一个触摸点
                        // 准备进入移动状态
                        mHandler.sendEmptyMessageDelayed(START_MOVE, TAP_TIMEOUT);
                        mMoveLast.set(getPivot(event));

                    } else {
                        // 更新控制点
                        mPivot.set(getPivot(event));
                        mSpanLast = getSpan(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mHandler.removeMessages(START_DRAW);
                mHandler.removeMessages(START_MOVE);
                // 首个触摸点抬起但尚未进入任何状态，则判断为点击。
                if (pointerId == mFirstPointerId
                        && (!mIsDrawing && !mIsScaling && !mIsMoving)) {
                    mIsDrawing = true;
                    int pointerIndex = event.findPointerIndex(mFirstPointerId);
                    handled = mListener.onSingleTapUp(
                            new Point(event.getX(pointerIndex), event.getY(pointerIndex)));
                }

                mTrack.reset();
                mIsFirstPointerTouching = false;
                mFirstPointerId = -1;

                if (mIsScaling) {
                    mListener.onScaleEnd(new Point(mPivot));
                }

                // 计算速度，判断是否需要惯性滑动
                Velocity v = new Velocity();
                if (mIsMoving) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    v.set(getVelocity(velocityTracker, event));

                } else if (mIsScaling) {
                    v.set(mPivotVelocity);
                }

                boolean fling = (Math.abs(v.x) > mMinFlingVelocity)
                        || (Math.abs(v.y) > mMinFlingVelocity);
                fling &= mIsScaling || mIsMoving;
                handled |= mListener.onActionUp(new PointV(x, y, v), fling);

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
        mTrack.reset();
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

        // 计算控制点。
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

        // 计算各触摸点到控制点的平均距离
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

        // 得出最终的span值
        final float spanX = devX * 2;
        final float spanY = devY * 2;

        return (float) Math.hypot(spanX, spanY);
    }

    private Velocity getVelocity(VelocityTracker tracker, MotionEvent event) {
        final boolean pointerUp = MotionEvent.ACTION_POINTER_UP == event.getActionMasked();
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);

        float sumVX = 0f;
        float sumVY = 0f;
        final int count = event.getPointerCount();

        for (int i = 0; i < count; i++) {
            if (skipIndex == i) {
                continue;
            }
            sumVX += tracker.getXVelocity(i);
            sumVY += tracker.getYVelocity(i);
        }

        final int div = pointerUp ? count - 1 : count;
        final float vX = sumVX / div;
        final float vY = sumVY / div;

        return new Velocity(vX, vY);
    }

    public interface CanvasGestureListener {
        boolean onActionDown(Point down);

        boolean onSingleTapUp(Point focus);

        boolean onDrawPath(PointV focus, final Track track);

        void onScaleStart(Point pivot);

        boolean onScale(Scale scale, Offset pivotOffset);

        void onScaleEnd(Point pivot);

        boolean onMove(PointV focus, Offset offset);

        boolean onActionUp(PointV focus, boolean fling);
    }
}
