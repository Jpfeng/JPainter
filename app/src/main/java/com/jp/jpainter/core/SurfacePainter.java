package com.jp.jpainter.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
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
import com.jp.jpainter.core.entity.Offset;
import com.jp.jpainter.core.entity.PathData;
import com.jp.jpainter.core.entity.Point;
import com.jp.jpainter.utils.LogUtil;

import java.util.ArrayList;

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
    private Canvas mDrawCanvas;
    private Paint mPaint;
    private Paint mDrawPaint;
    private Path mPath;
    private Bitmap mCache;

    // 撤销栈与重做栈
    private ArrayList<PathData> mUndoStack;
    private ArrayList<PathData> mRedoStack;

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

    @TargetApi(21)
    public SurfacePainter(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        // 缩放时对性能影响很大，暂时禁用
//        mPaint.setFilterBitmap(true);

        mDrawPaint = new Paint();
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setColor(Color.BLACK);
        mDrawPaint.setStrokeWidth(8.0f);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);

        CornerPathEffect effect = new CornerPathEffect(DEFAULT_CORNER_RADIUS);
        mDrawPaint.setPathEffect(effect);

        mPath = new Path();

        // 初始化撤销栈与重做栈
        mUndoStack = new ArrayList<>();
        mRedoStack = new ArrayList<>();
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

        mCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mDrawCanvas = new Canvas(mCache);
        drawPaintBoardBackground(mDrawCanvas);

        mCanvas = mHolder.lockCanvas();
        drawCanvasBackground(mCanvas);
        mCanvas.drawBitmap(mCache, 0, 0, mPaint);
        mHolder.unlockCanvasAndPost(mCanvas);

        Log.i(this.getClass().getSimpleName(),
                "surfaceChanged: width = " + width + ", height = " + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setCurrentStatus(STATUS_DESTROYED);
    }

    private boolean actionEnd = true;
    private boolean shouldAdjustCanvas = false;
    private boolean adjustScale = false;
    private int currentAnimateFrame = 0;
    private boolean needOneMoreFrame = false;

    @Override
    public void run() {
        while (STATUS_DESTROYED != getCurrentStatus()) {
            long start = System.currentTimeMillis();

            // 缩放完成后需要调整画布大小或倍率
            if (shouldAdjustCanvas) {
                // 当调整未开始时计算关键值并回调监听
                if (0 == currentAnimateFrame) {
                    evaluateAnimateKeyFrameValue();
                    adjustScale = shouldAdjustScale();
                    post(() -> {
                        if (adjustScale
                                && STATUS_MOVING == getCurrentStatus()
                                && null != mScaleListener) {
                            mScaleListener.onScaleChangeStart(mCurrentScale);
                        }
                    });
                }

                // 计算当前倍数和偏移
                evaluateCurrentScaleAndOffset(((float) currentAnimateFrame) / ANIMATE_FRAME);

                // 调整结束
                if (currentAnimateFrame == ANIMATE_FRAME) {
                    currentAnimateFrame = 0;
                    shouldAdjustCanvas = false;

                    // 监听回调保证主线程执行
                    post(() -> {
                        if (adjustScale && null != mScaleListener) {
                            mScaleListener.onScaleChangeEnd(endScale);
                        }
                        adjustScale = false;
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
                // 多绘制一帧，避免出现未完整绘制就结束的情况
                needOneMoreFrame = true;
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
            if (needOneMoreFrame) {
                needOneMoreFrame = false;
            } else {
                return;
            }
        }

        try {
            mCanvas = mHolder.lockCanvas();

            Matrix matrix = new Matrix();
            matrix.setTranslate(mCurrentOffset.x, mCurrentOffset.y);
            matrix.postScale(mCurrentScale, mCurrentScale, mCurrentOffset.x, mCurrentOffset.y);

            drawCache();
            drawCanvasBackground(mCanvas);
            mCanvas.drawBitmap(mCache, matrix, mPaint);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    /**
     * 在缓存上绘制路径。
     * <p>
     * 直接在 Bitmap 上重复 drawPath 会产生锯齿。每次绘制路径首先清空 Bitmap ，然后再绘制。
     * 参考：
     * https://medium.com/@ali.muzaffar/android-why-your-canvas-shapes-arent-smooth-aa2a3f450eb5
     */
    private void drawCache() {
        drawPaintBoardBackground(mDrawCanvas);
//        mDrawCanvas.drawPath(mPath, mDrawPaint);
        for (PathData path : mUndoStack) {
            path.draw(mDrawCanvas);
        }
        if (!mPath.isEmpty()) {
            mDrawCanvas.drawPath(mPath, mDrawPaint);
        }
    }

    /**
     * 绘制画布背景。
     */
    private void drawCanvasBackground(Canvas canvas) {
        // 平铺灰白格子
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader shader = new BitmapShader(
                bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(shader);
        canvas.drawPaint(p);
    }

    /**
     * 绘制画板背景。
     */
    private void drawPaintBoardBackground(Canvas canvas) {
        // 绘制白色画板
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
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
            if (adjustScale && null != mScaleListener) {
                mScaleListener.onScaleChange(mCurrentScale);
            }
        });
    }

    // 触摸点坐标记录位。 -1 表示没有记录。目前适配两个手指
    private int pointer1Index = -1;
    private int pointer2Index = -1;

    private Point down = new Point(0f, 0f);
    private Point last = new Point(0f, 0f);
    private Point move = new Point(0f, 0f);
    private Point scaleStart1 = new Point(0f, 0f);
    private Point scaleStart2 = new Point(0f, 0f);
    // 缩放开始时控制点在 canvas 上的坐标
    private Point canvasPivot = new Point(0f, 0f);
    // 缩放过程中上一个控制点的坐标
    private Point scaleLastPivot = new Point(0f, 0f);

    // 开始缩放时的倍数
    private float scaleStartScale;

    private long moveStartTime;

    // 首个触摸点存在的标记位。在 STATUS_PAINTING 状态下，只会对首个触摸点进行轨迹记录和绘制。其他触摸点则忽略
    private boolean isFirstFingerTouching = false;
    private boolean startRecordPath = false;
    private boolean moveButNotCallScaleEnd = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isFirstFingerTouching = true;
                startRecordPath = false;
                actionEnd = false;
                pointer1Index = event.getActionIndex();
                setCurrentStatus(STATUS_PAINTING);
                performClick();

                // 记录首个触摸点按下的坐标
                down.x = x;
                down.y = y;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 如果当前是两个触摸点
                if (2 == event.getPointerCount()) {
                    // 寻找没有记录的记录位进行记录
                    if (-1 == pointer2Index) {
                        pointer2Index = event.getActionIndex();
                    } else {
                        pointer1Index = event.getActionIndex();
                    }

                    // 判断是否触发缩放
                    if (shouldScaling(event)) {
                        setCurrentStatus(STATUS_SCALING);

                        // 记录缩放开始时两个手指和控制点的坐标
                        scaleStart1.x = event.getX(pointer1Index);
                        scaleStart1.y = event.getY(pointer1Index);
                        scaleStart2.x = event.getX(pointer2Index);
                        scaleStart2.y = event.getY(pointer2Index);
                        float scaleStartPivotX = Math.min(scaleStart1.x, scaleStart2.x)
                                + (Math.abs(scaleStart1.x - scaleStart2.x) / 2);
                        float scaleStartPivotY = Math.min(scaleStart1.y, scaleStart2.y)
                                + (Math.abs(scaleStart1.y - scaleStart2.y) / 2);
                        // 将控制点坐标转换为 canvas 上的坐标
                        canvasPivot.x = coordinateScreen2Canvas(scaleStartPivotX, mCurrentOffset.x);
                        canvasPivot.y = coordinateScreen2Canvas(scaleStartPivotY, mCurrentOffset.y);

                        // 记录缩放开始时的倍率
                        scaleStartScale = mCurrentScale;

                        if (null != mScaleListener) {
                            mScaleListener.onScaleChangeStart(scaleStartScale);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // 根据当前状态执行操作
                switch (getCurrentStatus()) {
                    case STATUS_PAINTING:
                        // 只记录首个触摸点的轨迹。判断首个触摸点仍然存在
                        if (isFirstFingerTouching) {
                            // 判断是否开始记录。如果将 moveTo(x,y) 方法放在 ACTION_DOWN 中，
                            // 在缩放时偶尔会出现额外的直线
                            if (!startRecordPath) {
                                startRecordPath = shouldStartRecordPath(event);
                                if (startRecordPath) {
                                    mPath.moveTo(coordinateScreen2Canvas(down.x, mCurrentOffset.x),
                                            coordinateScreen2Canvas(down.y, mCurrentOffset.y));
                                }

                            } else {
                                // 记录轨迹
                                mPath.lineTo(coordinateScreen2Canvas(x, mCurrentOffset.x),
                                        coordinateScreen2Canvas(y, mCurrentOffset.y));
                            }
                        }
                        break;

                    case STATUS_SCALING:
                        // 当前两个有效触摸点和控制点的坐标
                        Point scaleCurrent1 = new Point(
                                event.getX(pointer1Index), event.getY(pointer1Index));
                        Point scaleCurrent2 = new Point(
                                event.getX(pointer2Index), event.getY(pointer2Index));
                        Point scaleCurrentPivot = new Point(
                                Math.min(scaleCurrent1.x, scaleCurrent2.x)
                                        + (Math.abs(scaleCurrent1.x - scaleCurrent2.x) / 2),
                                Math.min(scaleCurrent1.y, scaleCurrent2.y)
                                        + (Math.abs(scaleCurrent1.y - scaleCurrent2.y) / 2));

                        // 计算触摸点间的距离
                        double beforeSpan = Math.hypot(
                                scaleStart1.x - scaleStart2.x, scaleStart1.y - scaleStart2.y);
                        double afterSpan = Math.hypot(scaleCurrent1.x - scaleCurrent2.x,
                                scaleCurrent1.y - scaleCurrent2.y);

                        // 计算缩放倍率
                        float scale = (float) (scaleStartScale * (afterSpan / beforeSpan));
                        // 添加缩放越界阻尼效果
                        if (scale > MAX_SCALE) {
                            scale = MAX_SCALE + (scale - MAX_SCALE) / 4.0f;
                        } else if (scale < MIN_SCALE) {
                            scale = MIN_SCALE - (MIN_SCALE - scale) / 4.0f;
                        }
                        mCurrentScale = scale;

                        // 计算当前偏移量
                        mCurrentOffset.x = scaleCurrentPivot.x - canvasPivot.x * mCurrentScale;
                        mCurrentOffset.y = scaleCurrentPivot.y - canvasPivot.y * mCurrentScale;

                        // 记录当前控制点
                        scaleLastPivot.x = scaleCurrentPivot.x;
                        scaleLastPivot.y = scaleCurrentPivot.y;

                        if (null != mScaleListener) {
                            mScaleListener.onScaleChange(mCurrentScale);
                        }
                        break;

                    case STATUS_MOVING:
                        // 用户结束缩放交互时，无法保证两个手指同时抬起。当第一个手指抬起而第二个手指
                        // 尚未抬起时，会进入 STATUS_MOVING 状态。此时第二个手指移动会触发画布的移动。
                        // 在此处加入判断，防止此现象发生，提高用户体验。
                        if (shouldMove(event)) {
                            if (moveButNotCallScaleEnd && null != mScaleListener) {
                                mScaleListener.onScaleChangeEnd(mCurrentScale);
                                moveButNotCallScaleEnd = false;
                            }

                            // 计算当前偏移量
                            mCurrentOffset.x += x - move.x;
                            mCurrentOffset.y += y - move.y;

                            // 更新移动坐标
                            move.x = x;
                            move.y = y;

                            // 移动需要同时移动缩放控制点，以应对缩放倍率越界回弹的情况
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

                // 判断首个触摸点是否还存在
                isFirstFingerTouching = !(actionIndex == pointer1Index);

                // 判断是否进入 STATUS_MOVING 状态。
                if ((STATUS_SCALING == getCurrentStatus())
                        && (actionIndex == pointer1Index || actionIndex == pointer2Index)) {
                    setCurrentStatus(STATUS_MOVING);
                    moveStartTime = event.getEventTime();
                    move.x = actionIndex == pointer1Index ? event.getX(pointer2Index)
                            : event.getX(pointer1Index);
                    move.y = actionIndex == pointer1Index ? event.getY(pointer2Index)
                            : event.getY(pointer1Index);

                    // 更新触摸点坐标记录位
                    if (actionIndex == pointer1Index) {
                        pointer1Index = -1;
                    } else if (actionIndex == pointer2Index) {
                        pointer2Index = -1;
                    }

                    // 记录从 STATUS_SCALING 变为 STATUS_MOVING，延迟回调监听器
                    moveButNotCallScaleEnd = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                // 首个触摸点抬起但尚未记录路径，则判断为点击。
                if (event.getActionIndex() == pointer1Index && !shouldStartRecordPath(event)) {
                    mPath.moveTo(coordinateScreen2Canvas(down.x, mCurrentOffset.x),
                            coordinateScreen2Canvas(down.y, mCurrentOffset.y));
                    mPath.lineTo(coordinateScreen2Canvas(x, mCurrentOffset.x),
                            coordinateScreen2Canvas(y, mCurrentOffset.y));
                }

                // 将路径加入撤销栈，清空重做栈，清空路径
                mUndoStack.add(new PathData(new Paint(mDrawPaint), new Path(mPath)));
                mRedoStack.clear();
                mPath.rewind();

                isFirstFingerTouching = false;
                actionEnd = true;
                pointer1Index = -1;
                pointer2Index = -1;

                // 判断是否需要调整画布
                shouldAdjustCanvas = shouldAdjustCanvas();
                if (!shouldAdjustScale()
                        && (STATUS_SCALING == getCurrentStatus()
                        || (STATUS_MOVING == getCurrentStatus() && !shouldMove(event)))
                        && null != mScaleListener) {
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
     * 判断是否开始记录路径。当手指触摸时间大于 TapTimeout 或者移动的距离大于 TouchSlop 时
     *
     * @param event 传入 MotionEvent
     * @return true  开始记录
     * false 不开始记录
     */
    private boolean shouldStartRecordPath(MotionEvent event) {
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        float pointerX = event.getX(pointer1Index);
        float pointerY = event.getY(pointer1Index);

        double fingerSpan = Math.hypot(pointerX - down.x, pointerY - down.y);
        long timeInterval = eventTime - downTime;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        return viewConfiguration.getScaledTouchSlop() < fingerSpan
                || ViewConfiguration.getTapTimeout() < timeInterval;
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
                && ViewConfiguration.getTapTimeout() > timeInterval);
    }

    /**
     * 判断从 STATUS_SCALING 转为 STATUS_MOVING 状态时是否触发移动
     * 起到防抖动作用
     *
     * @param event 传入 MotionEvent
     * @return true  触发移动
     * false 不触发移动
     */
    private boolean shouldMove(MotionEvent event) {
        return event.getEventTime() - moveStartTime > ViewConfiguration.getTapTimeout();
    }

    /**
     * 判断用户交互完成后是否需要调整倍率。有以下情况需要调整：
     * 1. 画布倍率 < MIN_SCALE
     * 2. 画布倍率 > MAX_SCALE
     *
     * @return true  需要调整
     * false 不需要调整
     */
    private boolean shouldAdjustScale() {
        return mCurrentScale > MAX_SCALE || mCurrentScale < MIN_SCALE;
    }

    /**
     * 判断用户交互完成后是否需要调整画布。有以下情况需要调整：
     * 1. 需要调整倍率
     * 3. 画布背景（灰白格）显示出来了
     *
     * @return true  需要调整
     * false 不需要调整
     */
    private boolean shouldAdjustCanvas() {
        boolean scale = shouldAdjustScale();
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
     * 当前是否可撤销
     *
     * @return true 可撤销
     * false 不可撤销
     */
    public boolean canUndo() {
        return mUndoStack.size() > 0;
    }

    /**
     * 当前是否可重做
     *
     * @return true 可重做
     * false 不可重做
     */
    public boolean canRedo() {
        return mRedoStack.size() > 0;
    }

    /**
     * 撤销上一步操作
     */
    public void undo() {
        if (0 < mUndoStack.size()) {
            PathData data = mUndoStack.remove(mUndoStack.size() - 1);
            mRedoStack.add(data);
            needOneMoreFrame = true;
        }
    }

    /**
     * 重做撤销的操作
     */
    public void redo() {
        if (0 < mRedoStack.size()) {
            PathData data = mRedoStack.remove(mRedoStack.size() - 1);
            mUndoStack.add(data);
            needOneMoreFrame = true;
        }
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
