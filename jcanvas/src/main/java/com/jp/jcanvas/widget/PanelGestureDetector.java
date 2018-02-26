package com.jp.jcanvas.widget;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;

/**
 *
 */
class PanelGestureDetector {
    private static final int MSG_SINGLE_TOUCH = 1;

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_SINGLE_TOUCH = 1;
    private static final int STATUS_MULTI_TOUCH = 2;

    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private int mTouchSlop;

    private int mMode;

    private Handler mHandler;
    private final OnPanelGestureListener mListener;

    private float mMinX;
    private float mMaxX;
    private float mMinY;
    private float mMaxY;
    private float mTotalX;
    private float mTotalY;

    PanelGestureDetector(Context context, RectF area, @NonNull OnPanelGestureListener listener) {
        mHandler = new GestureHandler(this);
        mListener = listener;
        init(context, area);
    }

    private void init(Context context, RectF area) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mHandler = new GestureHandler(this);
        mMode = STATUS_IDLE;

        updateLimit(area);
    }

    private void updateLimit(RectF area) {
        mMinX = area.left;
        mMaxX = area.right;
        mMinY = area.top;
        mMaxY = area.bottom;
        mTotalX = mMaxX - mMinX;
        mTotalY = mMaxY - mMinY;
    }

    static class GestureHandler extends Handler {
        WeakReference<PanelGestureDetector> mGD;

        GestureHandler(PanelGestureDetector gD) {
            super();
            mGD = new WeakReference<>(gD);
        }

        @Override
        public void handleMessage(Message msg) {
            PanelGestureDetector gD = mGD.get();
            switch (msg.what) {
                case MSG_SINGLE_TOUCH:
                    gD.mMode = STATUS_SINGLE_TOUCH;
                    break;

                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }

    private float mDownX;
    private float mDownY;

    boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                if (tryCapturePoint(mDownX, mDownY)) {
                    mHandler.sendEmptyMessageDelayed(MSG_SINGLE_TOUCH, TAP_TIMEOUT);
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (STATUS_IDLE == mMode || mHandler.hasMessages(MSG_SINGLE_TOUCH)) {
                    mHandler.removeMessages(MSG_SINGLE_TOUCH);
                    mMode = STATUS_MULTI_TOUCH;
                    updateValue(event);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                switch (mMode) {
                    case STATUS_IDLE:
                        float dx = event.getX() - mDownX;
                        float dy = event.getY() - mDownY;
                        if (Math.hypot(dx, dy) >= mTouchSlop) {
                            mHandler.removeMessages(MSG_SINGLE_TOUCH);
                            mMode = STATUS_SINGLE_TOUCH;
                            updateValue(event);
                        }
                        break;

                    case STATUS_SINGLE_TOUCH:
                        updateValue(event);
                        break;

                    case STATUS_MULTI_TOUCH:
                        if (!mHandler.hasMessages(MSG_SINGLE_TOUCH)) {
                            updateValue(event);
                        }
                        break;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (STATUS_SINGLE_TOUCH != mMode) {
                    if (2 == event.getPointerCount()) {
                        // 如果抬起后只剩一个触摸点
                        mHandler.sendEmptyMessageDelayed(MSG_SINGLE_TOUCH, TAP_TIMEOUT);
                    } else {
                        updateValue(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mHandler.removeMessages(MSG_SINGLE_TOUCH);
                if (STATUS_IDLE == mMode) {
                    mMode = STATUS_SINGLE_TOUCH;
                    updateValue(event);
                }

                mMode = STATUS_IDLE;
                break;

            case MotionEvent.ACTION_CANCEL:
                mHandler.removeMessages(MSG_SINGLE_TOUCH);
                mMode = STATUS_IDLE;
                break;
        }

        return handled;
    }

    private void updateValue(MotionEvent event) {
        float x;
        float y;

        switch (mMode) {
            case STATUS_MULTI_TOUCH:
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
                x = sumX / div;
                y = sumY / div;
                break;

            default:
                x = event.getX();
                y = event.getY();
                break;
        }

        if (x < mMinX) {
            x = mMinX;
        } else if (x > mMaxX) {
            x = mMaxX;
        }

        if (y < mMinY) {
            y = mMinY;
        } else if (y > mMaxY) {
            y = mMaxY;
        }

        float s = (x - mMinX) / mTotalX;
        float v = 1 - (y - mMinY) / mTotalY;
        mListener.onSVUpdate(s, v);
    }

    private boolean tryCapturePoint(float x, float y) {
        return x >= mMinX && x <= mMaxX && y >= mMinY && y <= mMaxY;
    }

    void setArea(RectF area) {
        updateLimit(area);
    }

    public interface OnPanelGestureListener {
        void onSVUpdate(float s, float v);
    }
}
