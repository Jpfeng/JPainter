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

import com.jp.jpainter.R;
import com.jp.jpainter.utils.LogUtil;

/**
 *
 */
public class SurfacePainter extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    /**
     * 闲置状态。无触摸交互且无需更新视图。
     */
    private static final int STATUS_IDLE = 0;

    /**
     * 绘制状态。用户产生交互且正在绘制。
     */
    private static final int STATUS_PAINTING = 1;

    /**
     * 缩放状态。用户产生交互且正在缩放视图。
     */
    private static final int STATUS_SCALING = 3;

    /**
     * 移动状态。用户产生交互且正在移动视图。
     */
    private static final int STATUS_MOVING = 4;

    /**
     * 销毁状态。SurfaceView 已被销毁。
     */
    private static final int STATUS_DESTROYED = 5;

    /**
     * 每秒帧率。
     */
    private static final int FRAME_RATE = 60;

    /**
     * 每一帧的时间。
     */
    private static final int FRAME_TIME_MILLIS = (int) (1000 / FRAME_RATE);

    /**
     * 动画时间，单位毫秒。
     */
    private static final int ANIMATE_TIME_MILLIS = 160;

    /**
     * 动画持续的帧数。
     */
    private static final int ANIMATE_FRAME =
            (int) (((float) ANIMATE_TIME_MILLIS) / 1000.0f * FRAME_RATE);

    /**
     * 默认笔迹圆滑半径
     */
    private static final int DEFAULT_CORNER_RADIUS = 30;

    /**
     * 最小缩放倍率
     */
    private static final float MIN_SCALE = 1.0f;

    /**
     * 最大缩放倍率
     */
    private static final float MAX_SCALE = 4.0f;

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Paint mPaint;
    private Path mPath;

    private int mHeight;
    private int mWidth;
    private int mCurrentStatus;

    private Offset mCurrentOffset = new Offset(0f, 0f);
    private float mCurrentScale = 1.0f;

    private OnScaleChangeListener mScaleListener;

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

    /**
     * 初始化
     */
    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(4.0f);

        CornerPathEffect effect = new CornerPathEffect(DEFAULT_CORNER_RADIUS);
        mPaint.setPathEffect(effect);

        mPath = new Path();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(mCanvas);

        setCurrentStatus(STATUS_IDLE);
        new Thread(this).start();
    }

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

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setCurrentStatus(STATUS_DESTROYED);
    }

    private boolean actionEnd = true;
    private boolean shouldAdjustCanvas;
    private int currentAnimateFrame = 0;

    @Override
    public void run() {
        while (STATUS_DESTROYED != getCurrentStatus()) {
            long start = System.currentTimeMillis();

            // 缩放完成后需要调整画布大小或倍率
            if (shouldAdjustCanvas) {
                // 当调整未开始时计算关键值
                if (0 == currentAnimateFrame) {
                    evaluateAnimateKeyFrameValue();
                }

                // 计算当前倍数和偏移
                evaluateCurrentScaleAndOffset(((float) currentAnimateFrame) / ANIMATE_FRAME);

                // 调整结束
                if (currentAnimateFrame == ANIMATE_FRAME) {
                    currentAnimateFrame = 0;
                    shouldAdjustCanvas = false;

                    // 监听回调保证主线程执行
                    post(() -> {
                        if (null != mScaleListener) {
                            mScaleListener.onScaleChangeEnd(endScale);
                        }
                    });

                } else {
                    currentAnimateFrame++;
                }
            }

            // 绘制内容
            drawContent();

            // 若无任务则将状态置为闲置
            if (STATUS_IDLE != getCurrentStatus() && actionEnd && !shouldAdjustCanvas) {
                setCurrentStatus(STATUS_IDLE);
            }

            long end = System.currentTimeMillis();

            // 降低资源消耗
            long time = end - start;
            if (time < FRAME_TIME_MILLIS) {
                try {
                    Thread.sleep(FRAME_TIME_MILLIS - time);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 绘制内容
     */
    private void drawContent() {
        if (STATUS_IDLE == getCurrentStatus() || STATUS_DESTROYED == getCurrentStatus()) {
            return;
        }

        try {
            mCanvas = mHolder.lockCanvas();

            mCanvas.translate(mCurrentOffset.x, mCurrentOffset.y);
            mCanvas.scale(mCurrentScale, mCurrentScale);

            drawCanvasBackground();
            mCanvas.drawPath(mPath, mPaint);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    /**
     * 绘制画布背景。
     */
    private void drawCanvasBackground() {
        // 平铺灰白格子
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader shader = new BitmapShader(
                bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(shader);
        mCanvas.drawPaint(p);

        // 绘制白色画板
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8, 0, 0, 0xFF666666);
        mCanvas.drawRect(0, 0, mWidth, mHeight, paint);
    }

    private float startScale;
    private float endScale;
    private Offset startOffset = new Offset(0f, 0f);
    private Offset endOffset = new Offset(0f, 0f);

    /**
     * 计算关键值。包括调整开始的倍率，偏移 以及调整后的倍率，偏移。
     */
    private void evaluateAnimateKeyFrameValue() {
        startScale = mCurrentScale;
        startOffset.x = mCurrentOffset.x;
        startOffset.y = mCurrentOffset.y;

        if (MIN_SCALE > startScale) {
            // 当前倍率 < MIN ，调整后倍率为MIN，偏移一定均为0
            endScale = MIN_SCALE;
            endOffset.x = 0f;
            endOffset.y = 0f;

        } else if (MAX_SCALE < startScale) {
            // 当前倍率 > MAX ，调整后倍率为MAX，偏移需要根据缩放控制点计算
            endScale = MAX_SCALE;
            endOffset.x = scaleLastPivot.x - canvasPivot.x * endScale;
            endOffset.y = scaleLastPivot.y - canvasPivot.y * endScale;

            // 如果计算的偏移过度，还需要调整回来
            if (mWidth < ((mWidth - endOffset.x) / endScale)) {
                endOffset.x = (1.0f - endScale) * mWidth;
            }
            if (mHeight < ((mHeight - endOffset.y) / endScale)) {
                endOffset.y = (1.0f - endScale) * mHeight;
            }

        } else {
            // 其他情况，倍率不变。偏移根据情况计算
            endScale = startScale;
            if (mWidth < coordinateScreen2Canvas(mWidth, startOffset.x)) {
                endOffset.x = (1.0f - endScale) * mWidth;
            } else {
                endOffset.x = startOffset.x;
            }
            if (mHeight < coordinateScreen2Canvas(mHeight, startOffset.y)) {
                endOffset.y = (1.0f - endScale) * mHeight;
            } else {
                endOffset.y = startOffset.y;
            }
        }

        // 无论何种情况，偏移值不会 >0
        if (endOffset.x > 0f) {
            endOffset.x = 0f;
        }
        if (endOffset.y > 0f) {
            endOffset.y = 0f;
        }
    }

    /**
     * 计算调整过程中的倍率及偏移
     *
     * @param fraction 在整体调整进程中的比率，取值 0.0 ~ 1.0
     */
    private void evaluateCurrentScaleAndOffset(float fraction) {
        mCurrentScale = startScale + fraction * (endScale - startScale);
        mCurrentOffset.x = startOffset.x + fraction * (endOffset.x - startOffset.x);
        mCurrentOffset.y = startOffset.y + fraction * (endOffset.y - startOffset.y);

        // 保证监听回调在主线程执行
        post(() -> {
            if (null != mScaleListener) {
                mScaleListener.onScaleChange(mCurrentScale);
            }
        });
    }

    // 目前适配两个手指
    private int pointer1Index = -1;
    private int pointer2Index = -1;

    private Point down = new Point(0f, 0f);
    private Point last = new Point(0f, 0f);
    private Point moveStart = new Point(0f, 0f);
    private Point scaleStart1 = new Point(0f, 0f);
    private Point scaleStart2 = new Point(0f, 0f);
    // 缩放开始时控制点在 canvas 上的坐标
    private Point canvasPivot = new Point(0f, 0f);
    private Point scaleLastPivot = new Point(0f, 0f);

    // 开始缩放时的倍数
    private float scaleStartScale;

    private long moveStartTime;

    private boolean isFirstFingerTouching = false;
    private boolean startRecordPath = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isFirstFingerTouching = true;
                pointer1Index = event.getActionIndex();
                setCurrentStatus(STATUS_PAINTING);
                actionEnd = false;
                startRecordPath = false;
                performClick();

                down.x = x;
                down.y = y;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (2 == event.getPointerCount()) {
                    if (-1 == pointer2Index) {
                        pointer2Index = event.getActionIndex();

                    } else {
                        pointer1Index = event.getActionIndex();
                    }

                    if (shouldScaling(event)) {
                        boolean shouldCallScaleListener = !(STATUS_MOVING == getCurrentStatus());
                        setCurrentStatus(STATUS_SCALING);

                        scaleStart1.x = event.getX(pointer1Index);
                        scaleStart1.y = event.getY(pointer1Index);
                        scaleStart2.x = event.getX(pointer2Index);
                        scaleStart2.y = event.getY(pointer2Index);
                        float scaleStartPivotX = Math.min(scaleStart1.x, scaleStart2.x)
                                + (Math.abs(scaleStart1.x - scaleStart2.x) / 2);
                        float scaleStartPivotY = Math.min(scaleStart1.y, scaleStart2.y)
                                + (Math.abs(scaleStart1.y - scaleStart2.y) / 2);

                        canvasPivot.x = coordinateScreen2Canvas(scaleStartPivotX, mCurrentOffset.x);
                        canvasPivot.y = coordinateScreen2Canvas(scaleStartPivotY, mCurrentOffset.y);

                        scaleStartScale = mCurrentScale;

                        if (null != mScaleListener && shouldCallScaleListener) {
                            mScaleListener.onScaleChangeStart(scaleStartScale);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                switch (getCurrentStatus()) {
                    case STATUS_PAINTING:
                        if (isFirstFingerTouching) {
                            if (!startRecordPath) {
                                mPath.moveTo(coordinateScreen2Canvas(down.x, mCurrentOffset.x),
                                        coordinateScreen2Canvas(down.y, mCurrentOffset.y));
                                startRecordPath = true;
                            }

                            mPath.lineTo(coordinateScreen2Canvas(x, mCurrentOffset.x),
                                    coordinateScreen2Canvas(y, mCurrentOffset.y));
                        }
                        break;

                    case STATUS_SCALING:
                        Point scaleCurrent1 = new Point(
                                event.getX(pointer1Index), event.getY(pointer1Index));
                        Point scaleCurrent2 = new Point(
                                event.getX(pointer2Index), event.getY(pointer2Index));

                        double beforeSpan = Math.hypot(
                                scaleStart1.x - scaleStart2.x, scaleStart1.y - scaleStart2.y);
                        double afterSpan = Math.hypot(scaleCurrent1.x - scaleCurrent2.x,
                                scaleCurrent1.y - scaleCurrent2.y);

                        Point scaleCurrentPivot = new Point(
                                Math.min(scaleCurrent1.x, scaleCurrent2.x)
                                        + (Math.abs(scaleCurrent1.x - scaleCurrent2.x) / 2),
                                Math.min(scaleCurrent1.y, scaleCurrent2.y)
                                        + (Math.abs(scaleCurrent1.y - scaleCurrent2.y) / 2));

                        float scale = (float) (scaleStartScale * (afterSpan / beforeSpan));
                        if (scale > MAX_SCALE) {
                            scale = MAX_SCALE + (scale - MAX_SCALE) / 4.0f;
                        } else if (scale < MIN_SCALE) {
                            scale = MIN_SCALE - (MIN_SCALE - scale) / 4.0f;
                        }
                        mCurrentScale = scale;

                        mCurrentOffset.x = scaleCurrentPivot.x - canvasPivot.x * mCurrentScale;
                        mCurrentOffset.y = scaleCurrentPivot.y - canvasPivot.y * mCurrentScale;

                        scaleLastPivot.x = scaleCurrentPivot.x;
                        scaleLastPivot.y = scaleCurrentPivot.y;

                        if (null != mScaleListener) {
                            mScaleListener.onScaleChange(mCurrentScale);
                        }
                        break;

                    case STATUS_MOVING:
                        mCurrentOffset.x += x - moveStart.x;
                        mCurrentOffset.y += y - moveStart.y;

                        moveStart.x = x;
                        moveStart.y = y;

                        if (shouldMovePivot(event)) {
                            scaleLastPivot.x = x;
                            scaleLastPivot.y = y;
                            canvasPivot.x = coordinateScreen2Canvas(x, mCurrentOffset.x);
                            canvasPivot.y = coordinateScreen2Canvas(y, mCurrentOffset.y);
                        }
                        break;

                    case STATUS_IDLE:
                    default:
                        Log.w(this.getClass().getSimpleName(), "incorrect status");
                        break;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int actionIndex = event.getActionIndex();

                if (actionIndex == pointer1Index) {
                    isFirstFingerTouching = false;
                }

                if ((STATUS_SCALING == getCurrentStatus())
                        && (actionIndex == pointer1Index || actionIndex == pointer2Index)) {
                    setCurrentStatus(STATUS_MOVING);
                    moveStartTime = event.getEventTime();
                    moveStart.x = actionIndex == pointer1Index ? event.getX(pointer2Index)
                            : event.getX(pointer1Index);
                    moveStart.y = actionIndex == pointer1Index ? event.getY(pointer2Index)
                            : event.getY(pointer1Index);

                    if (actionIndex == pointer1Index) {
                        pointer1Index = -1;
                    } else if (actionIndex == pointer2Index) {
                        pointer2Index = -1;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                isFirstFingerTouching = false;
                pointer1Index = -1;
                pointer2Index = -1;
                actionEnd = true;
                shouldAdjustCanvas = shouldAdjustCanvas();
                if (!shouldAdjustCanvas && null != mScaleListener) {
                    mScaleListener.onScaleChangeEnd(mCurrentScale);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                LogUtil.d("ACTION_CANCEL ->", "actionIndex = " + event.getActionIndex());
                break;
        }

        last.x = x;
        last.y = y;
        return true;
    }

    /**
     * 非要执行这个方法。lint逼死强迫症。
     *
     * @return ?
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * View 上的坐标转换为在 canvas 中的坐标
     *
     * @param coordinate View 上的坐标
     * @param offset     偏移量
     * @return 在 canvas 中的坐标
     */
    private float coordinateScreen2Canvas(float coordinate, float offset) {
        return (coordinate - offset) / mCurrentScale;
    }

    /**
     * 判断是否进行缩放。当第一个手指触摸时间小于 TapTimeout 并且移动的距离小于 TouchSlop 时，第二个手指按下
     * 即触发缩放。
     *
     * @param event 传入 MotionEvent
     * @return true  触发缩放
     * false 不触发缩放
     */
    private boolean shouldScaling(MotionEvent event) {
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        float firstFingerX = event.getX(pointer1Index);
        float firstFingerY = event.getY(pointer1Index);

        double firstFingerSpan = Math.hypot(firstFingerX - down.x, firstFingerY - down.y);
        long timeInterval = eventTime - downTime;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        return STATUS_MOVING == getCurrentStatus()
                || (viewConfiguration.getScaledTouchSlop() > firstFingerSpan
                && timeInterval < ViewConfiguration.getTapTimeout());
    }

    /**
     * 判断从 STATUS_SCALING 转为 STATUS_MOVING 状态时是否需要移动控制点
     * 起到防抖动作用
     *
     * @param event 传入 MotionEvent
     * @return true  触发控制点移动
     * false 不触发控制点移动
     */
    private boolean shouldMovePivot(MotionEvent event) {
        return event.getEventTime() - moveStartTime > ViewConfiguration.getTapTimeout();
    }

    /**
     * 判断用户交互完成后是否需要调整画布。有以下情况需要调整：
     * 1. 画布倍率 < MIN_SCALE
     * 2. 画布倍率 > MAX_SCALE
     * 3. 画布背景（灰白格）显示出来了
     *
     * @return true  需要调整
     * false 不需要调整
     */
    private boolean shouldAdjustCanvas() {
        boolean scale = mCurrentScale > MAX_SCALE || mCurrentScale < MIN_SCALE;
        boolean border = mCurrentOffset.x > 0 || mCurrentOffset.y > 0
                || mWidth < coordinateScreen2Canvas(mWidth, mCurrentOffset.x)
                || mHeight < coordinateScreen2Canvas(mHeight, mCurrentOffset.y);
        return scale || border;
    }

    /**
     * 设定当前状态。
     *
     * @param currentStatus 当前状态
     */
    private void setCurrentStatus(int currentStatus) {
        mCurrentStatus = currentStatus;
    }

    /**
     * 获取当前状态。
     *
     * @return 当前状态
     */
    public int getCurrentStatus() {
        return mCurrentStatus;
    }

    /**
     * 设置倍率变化监听回调
     *
     * @param listener 监听器
     */
    public void setOnScaleChangeListener(OnScaleChangeListener listener) {
        mScaleListener = listener;
    }

    /**
     * 封装的坐标点
     */
    private class Point {
        Point(float x, float y) {
            Point.this.x = x;
            Point.this.y = y;
        }

        float x;
        float y;
    }

    /**
     * 封装的偏移量
     */
    private class Offset {
        Offset(float x, float y) {
            Offset.this.x = x;
            Offset.this.y = y;
        }

        float x;
        float y;
    }

    /**
     * 倍率变化监听器
     */
    public interface OnScaleChangeListener {
        /**
         * 开始变化倍率
         *
         * @param startScale 时的倍率
         */
        void onScaleChangeStart(float startScale);

        /**
         * 变化倍率中。只提供当前倍率，如需前一个倍率请自行记录
         *
         * @param currentScale 当期倍率
         */
        void onScaleChange(float currentScale);

        /**
         * 变化倍率结束
         *
         * @param endScale 结束倍率
         */
        void onScaleChangeEnd(float endScale);
    }
}
