package com.jp.jpainter.core;

import android.content.Context;
import android.view.MotionEvent;

/**
 *
 */
public class CanvasGestureDetector {

    public CanvasGestureDetector(Context context, OnCanvasGestureListener listener) {

    }

    public interface OnCanvasGestureListener {
        void onDown(Point down);

        void onDrawPath(Point down, Point current);

        void onScale(float scale, Offset offset, Point pivot, Point finger1, Point finger2);

        void onMove(Offset offset, Point finger);

        void shouldStartAnimate(float scale, Offset offset, Point control, Velocity velocity);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
