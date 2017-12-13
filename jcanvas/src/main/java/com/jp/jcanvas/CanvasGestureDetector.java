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

    private Handler mHandler;
    private VelocityTracker mVelocityTracker;
    private final OnCanvasGestureListener mListener;

    private boolean mIsFirstPointerTouching = false;
    private int mFirstPointerId = -1;

    private boolean mIsDrawing = false;
    private boolean mIsScaling = false;
    private boolean mIsMoving = false;

    private Point mDown;
    private Point mMoveStart;
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
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mPath = new Path();
        mDown = new Point();
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
                mDown.x = x;
                mDown.y = y;

                mPath.moveTo(x, y);

                handled = mListener.onActionDown(mDown);
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
                    if (shouldCallListener) {
                        mListener.onScaleStart(getPivot(event));
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsScaling) {
                    if (1 == event.getPointerCount()) {
                        double pointerSpan = Math.hypot(x - mMoveStart.x, y - mMoveStart.y);
                        if (pointerSpan > mTouchSlop) {
                            mHandler.removeMessages(START_MOVE);
                            mIsScaling = false;
                            mIsMoving = true;

                            // move
                        }

                    } else {
                        // scale
                    }

                } else if (mIsMoving) {
                    // move

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
                        mMoveStart = getPivot(event);

                    } else {
                        Point pivot = getPivot(event);
                    }


//                    setCurrentStatus(STATUS_MOVING);
//                    moveStartTime = event.getEventTime();
//                    move.x = actionIndex == pointer1Index ? event.getX(pointer2Index)
//                            : event.getX(pointer1Index);
//                    move.y = actionIndex == pointer1Index ? event.getY(pointer2Index)
//                            : event.getY(pointer1Index);
//
//                    // 更新触摸点坐标记录位
//                    if (actionIndex == pointer1Index) {
//                        pointer1Index = -1;
//                    } else if (actionIndex == pointer2Index) {
//                        pointer2Index = -1;
//                    }
//
//                    // 记录从 STATUS_SCALING 变为 STATUS_MOVING，延迟回调监听器
//                    moveButNotCallScaleEnd = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                mHandler.removeMessages(START_DRAW);
                mHandler.removeMessages(START_MOVE);
                // 首个触摸点抬起但尚未记录路径，则判断为点击。
                if (pointerId == mFirstPointerId
                        && (!mIsDrawing && !mIsScaling && !mIsMoving)) {
                    mPath.lineTo(event.getX(mFirstPointerId), event.getY(mFirstPointerId));
                    handled = mListener.onDrawPath(mPath);
                }

                if (mIsScaling) {
                    handled |= mListener.onScaleEnd(null);
                }

                mPath.rewind();
                mIsFirstPointerTouching = false;
                mFirstPointerId = -1;

                boolean fling = mIsScaling || mIsMoving;
                handled |= mListener.onActionUp(new Point(x, y), fling, null);

                mIsDrawing = false;
                mIsScaling = false;
                mIsMoving = false;
                break;
        }

        return handled;
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

    public interface OnCanvasGestureListener {
        boolean onActionDown(Point down);

        boolean onDrawPath(Path path);

        boolean onScaleStart(Point pivot);

        boolean onScale(Point pivot, Scale scale, Offset offset);

        boolean onScaleEnd(Point pivot);

        boolean onMove(Point focus, Offset offset);

        boolean onActionUp(Point focus, boolean fling, Velocity velocity);
    }
}
