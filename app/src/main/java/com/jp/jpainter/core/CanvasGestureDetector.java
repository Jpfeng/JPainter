package com.jp.jpainter.core;

import android.content.Context;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

import com.jp.jpainter.core.entity.Offset;
import com.jp.jpainter.core.entity.Point;
import com.jp.jpainter.core.entity.Scale;
import com.jp.jpainter.core.entity.Velocity;

/**
 *
 */
public class CanvasGestureDetector {

    private static final int TAP = 1;
    private static final int SCALE_END = 3;

    private OnCanvasGestureListener mListener;

    public CanvasGestureDetector(Context context, OnCanvasGestureListener listener) {
        mListener = listener;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return false;
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

                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }

    public interface OnCanvasGestureListener {
        boolean onDown(Point down);

        boolean onTapUp(Point tap);

        boolean onDrawPath(Point focus, Path path);

        boolean onScaleStart(Scale scale);

        boolean onScale(Scale scale);

        boolean onScaleEnd(Point focus, Scale scale);

        boolean onMove(Point focus, Offset offset);

        boolean onFling(Point start, Velocity velocity);
    }
}
