package com.jp.jcanvas;

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
import android.widget.Scroller;

import com.jp.jcanvas.CanvasGestureDetector.OnCanvasGestureListener;
import com.jp.jcanvas.entity.Offset;
import com.jp.jcanvas.entity.PathData;
import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.Scale;
import com.jp.jcanvas.entity.Velocity;

import java.util.ArrayList;

/**
 *
 */
public class JCanvas extends SurfaceView implements
        SurfaceHolder.Callback, OnCanvasGestureListener, Runnable {

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
     * 动画状态。无交互但正在显示动画。
     */
    private static final int STATUS_FLING = 5;

    /**
     * 销毁状态。SurfaceView 已被销毁。
     */
    private static final int STATUS_DESTROYED = 6;

    /**
     * 每秒帧率。
     */
    private static final int FRAME_RATE = 60;

    /**
     * 每一帧的时间。
     */
    private static final int FRAME_TIME_MILLIS = (int) (1000 / FRAME_RATE);

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

    private int mHeight;
    private int mWidth;
    private int mStatus;

    private float mScale;
    private Offset mOffset;
    private Matrix mMatrix;

    private Scroller mScroller;

    // 撤销栈与重做栈
    private ArrayList<PathData> mUndoStack;
    private ArrayList<PathData> mRedoStack;

    private OnScaleChangeListener mScaleListener;

    public JCanvas(Context context) {
        this(context, null);
    }

    public JCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public JCanvas(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);

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

        mScale = 1.0f;
        mOffset = new Offset();
        mMatrix = new Matrix();

        mScroller = new Scroller(getContext());

        // 初始化撤销栈与重做栈
        mUndoStack = new ArrayList<>();
        mRedoStack = new ArrayList<>();

        setStatus(STATUS_DESTROYED);

        CanvasGestureDetector gDetector = new CanvasGestureDetector(getContext(), this);
        setOnTouchListener((v, event) -> {
            boolean handled = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (STATUS_FLING == getStatus()) {
                        mScroller.abortAnimation();
                        mLastScrX = 0;
                        mLastScrY = 0;
                        setStatus(STATUS_IDLE);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    handled = performClick();
                    break;
            }

            return handled | gDetector.onTouchEvent(event);
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(mCanvas);

        setStatus(STATUS_IDLE);
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
        setStatus(STATUS_DESTROYED);
    }

    @Override
    public boolean onActionDown(Point down) {
        return true;
    }

    @Override
    public boolean onDrawPath(Path path) {
        if (STATUS_PAINTING != getStatus()) {
            setStatus(STATUS_PAINTING);
        }

        Matrix matrix = new Matrix();
        matrix.setTranslate(-mOffset.x, -mOffset.y);
        matrix.postScale(1.0f / mScale, 1.0f / mScale);
        path.transform(matrix, mPath);

        return true;
    }

    @Override
    public void onScaleStart(Point pivot) {
        setStatus(STATUS_SCALING);
        if (null != mScaleListener) {
            mScaleListener.onScaleChangeStart(mScale);
        }
    }

    @Override
    public boolean onScale(Scale scale, Offset pivotOffset) {
        float x = coordinateScreen2Canvas(scale.pivot.x, mOffset.x);
        float y = coordinateScreen2Canvas(scale.pivot.y, mOffset.y);

        float newScale = mScale * scale.factor;
        // limit scale
        if (newScale > MAX_SCALE) {
            newScale = MAX_SCALE;
        } else if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
        }

        float f = newScale / mScale;
        mScale = newScale;

        float offsetX = (f - 1.0f) * x;
        float offsetY = (f - 1.0f) * y;

        float newOffsetX = mOffset.x - offsetX * mScale + pivotOffset.x;
        float newOffsetY = mOffset.y - offsetY * mScale + pivotOffset.y;

        // limit offset
        if (newOffsetX > 0f) {
            newOffsetX = 0f;
        } else if (newOffsetX < mWidth * (1.0f - mScale)) {
            newOffsetX = mWidth * (1.0f - mScale);
        }

        if (newOffsetY > 0f) {
            newOffsetY = 0f;
        } else if (newOffsetY < mHeight * (1.0f - mScale)) {
            newOffsetY = mHeight * (1.0f - mScale);
        }

        mOffset.x = newOffsetX;
        mOffset.y = newOffsetY;

        if (null != mScaleListener) {
            mScaleListener.onScaleChange(mScale);
        }

        return true;
    }

    @Override
    public void onScaleEnd(Point pivot) {
        if (null != mScaleListener) {
            mScaleListener.onScaleChangeEnd(mScale);
        }
    }

    @Override
    public boolean onMove(Point focus, Offset offset) {
        if (STATUS_MOVING != getStatus()) {
            setStatus(STATUS_MOVING);
        }

        float newOffsetX = mOffset.x + offset.x;
        float newOffsetY = mOffset.y + offset.y;

        // limit offset
        if (newOffsetX > 0f) {
            newOffsetX = 0f;
        } else if (newOffsetX < mWidth * (1.0f - mScale)) {
            newOffsetX = mWidth * (1.0f - mScale);
        }

        if (newOffsetY > 0f) {
            newOffsetY = 0f;
        } else if (newOffsetY < mHeight * (1.0f - mScale)) {
            newOffsetY = mHeight * (1.0f - mScale);
        }

        mOffset.x = newOffsetX;
        mOffset.y = newOffsetY;
        return true;
    }

    @Override
    public boolean onActionUp(Point focus, boolean fling, Velocity velocity) {
        if (STATUS_PAINTING == getStatus()) {
            // 将路径加入撤销栈，清空重做栈，清空路径
            mUndoStack.add(new PathData(new Paint(mDrawPaint), new Path(mPath)));
            mRedoStack.clear();
            mPath.reset();
        }

//        setStatus(fling ? STATUS_FLING : STATUS_IDLE);
//        if (fling) {
//            mScroller.fling((int) mOffset.x, (int) mOffset.y, (int) velocity.x, (int) velocity.y,
//                    (int) (mWidth * (1.0f - mScale)), 0,
//                    (int) (mHeight * (1.0f - mScale)), 0);
//        }
        return true;
    }

    /**
     * View 上的坐标转换为在 canvas 中的坐标
     *
     * @param coordinate View 上的坐标
     * @param offset     偏移量
     * @return 在 canvas 中的坐标
     */
    private float coordinateScreen2Canvas(float coordinate, float offset) {
        return (coordinate - offset) / mScale;
    }

    private int mLastScrX = 0;
    private int mLastScrY = 0;

    @Override
    public void run() {
        while (STATUS_DESTROYED != getStatus()) {
            long start = System.currentTimeMillis();

            if (STATUS_IDLE != getStatus()) {
                if (STATUS_FLING == getStatus()) {
                    if (mScroller.computeScrollOffset()) {
                        mOffset.x += mScroller.getCurrX() - mLastScrX;
                        mOffset.y += mScroller.getCurrY() - mLastScrY;

                        mLastScrX = mScroller.getCurrX();
                        mLastScrY = mScroller.getCurrY();

                    } else {
                        mLastScrX = 0;
                        mLastScrY = 0;
                        setStatus(STATUS_IDLE);
                    }
                }

                drawContent();
            }
            long end = System.currentTimeMillis();

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

    private void drawContent() {
        try {
            mCanvas = mHolder.lockCanvas();
            // 进行绘图操作
            mMatrix.setTranslate(mOffset.x, mOffset.y);
            mMatrix.postScale(mScale, mScale, mOffset.x, mOffset.y);

            drawCache();
            drawCanvasBackground(mCanvas);
            mCanvas.drawBitmap(mCache, mMatrix, mPaint);

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

    public void setStatus(int status) {
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
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
        }
    }

    /**
     * 重做撤销的操作
     */
    public void redo() {
        if (0 < mRedoStack.size()) {
            PathData data = mRedoStack.remove(mRedoStack.size() - 1);
            mUndoStack.add(data);
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
         * @param startScale 开始倍率
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
