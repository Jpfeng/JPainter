package com.jp.jpainter.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 */
public class ToolDrawer extends ViewGroup {

    private static final int DURATION = 250;
    private static final int BASE_ALPHA = 128;

    private static final int CLOSED = 0;
    private static final int OPENED = 1;

    private static final int R = 0;
    private static final int G = 0;
    private static final int B = 0;

    private Paint mPaint;
    private ObjectAnimator mAnimator;

    private int mStatus;
    private float mAnimProgress;
    private ToolDrawerListener mListener;

    public ToolDrawer(Context context) {
        this(context, null);
    }

    public ToolDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToolDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        mStatus = CLOSED;
        mAnimProgress = 0f;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.argb(0, R, G, B));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (0 == getChildCount()) {
            return;
        }

        View tool = getChildAt(0);
        int toolX = -tool.getMeasuredWidth();
        tool.layout(toolX, 0, 0, tool.getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (0 == getChildCount()) {
            return;
        }

        View tool = getChildAt(0);
        int toolR = tool.getRight();
        int alpha = (int) (BASE_ALPHA * mAnimProgress);
        mPaint.setColor(Color.argb(alpha, R, G, B));
        canvas.drawRect(toolR, 0, getWidth(), getHeight(), mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (CLOSED == mStatus) {
            return false;
        }

        boolean handled = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handled |= isPointerInScrim(event);
                break;

            case MotionEvent.ACTION_UP:
                if (handled |= isPointerInScrim(event)) {
                    close();
                }
                break;
        }

        return handled;
    }

    private boolean isPointerInScrim(MotionEvent event) {
        if (0 == getChildCount()) {
            return false;
        }

        View tool = getChildAt(0);
        int right = (int) (tool.getX() + tool.getWidth());
        return event.getX() > right;
    }

    public void open(@NonNull View tool) {
        if (OPENED == mStatus) {
            return;
        }

        mStatus = OPENED;
        mAnimProgress = 0f;

        removeAllViews();
        addView(tool, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

        tool.post(() -> {
            float startX = tool.getX();

            mAnimator = ObjectAnimator.ofFloat(tool, "X", 0)
                    .setDuration(DURATION);
            mAnimator.addUpdateListener(animation -> {
                mAnimProgress = animation.getAnimatedFraction();
                invalidate();

                if (null != mListener) {
                    float offset = tool.getX() - startX;
                    mListener.onDrawerSlide(ToolDrawer.this, offset);
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (null != mListener) {
                        mListener.onDrawerOpened(ToolDrawer.this);
                    }
                }
            });
            mAnimator.start();
        });
    }

    public void close() {
        if (CLOSED == mStatus) {
            return;
        }

        mStatus = CLOSED;
        if (mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        View tool = getChildAt(0);
        int toolW = tool.getWidth();
        int duration = (int) (DURATION * mAnimProgress);
        float startAnimProgress = mAnimProgress;
        mAnimator = ObjectAnimator.ofFloat(tool, "X", -toolW)
                .setDuration(duration);
        mAnimator.addUpdateListener(animation -> {
            mAnimProgress = startAnimProgress * (1 - animation.getAnimatedFraction());
            invalidate();
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                removeAllViews();
                if (null != mListener) {
                    mListener.onDrawerClosed(ToolDrawer.this);
                }
            }
        });
        mAnimator.start();
    }

    public void setToolDrawerListener(ToolDrawerListener listener) {
        mListener = listener;
    }

    public interface ToolDrawerListener {
        void onDrawerSlide(View toolDrawer, float slideOffset);

        void onDrawerOpened(View toolDrawer);

        void onDrawerClosed(View toolDrawer);
    }
}
