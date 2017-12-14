package com.jp.jcanvas;

import android.annotation.TargetApi;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

    // 撤销栈与重做栈
    private ArrayList<PathData> mUndoStack;
    private ArrayList<PathData> mRedoStack;

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

        // 初始化撤销栈与重做栈
        mUndoStack = new ArrayList<>();
        mRedoStack = new ArrayList<>();

        setStatus(STATUS_DESTROYED);

        CanvasGestureDetector gDetector = new CanvasGestureDetector(getContext(), this);
        setOnTouchListener((v, event) -> gDetector.onTouchEvent(event));
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
        mPath.set(path);
        return true;
    }

    @Override
    public void onScaleStart(Point pivot) {
    }

    @Override
    public boolean onScale(Scale scale, Offset offset) {
        return true;
    }

    @Override
    public void onScaleEnd(Point pivot) {
    }

    @Override
    public boolean onMove(Point focus, Offset offset) {
        return true;
    }

    @Override
    public boolean onActionUp(Point focus, boolean fling, Velocity velocity) {
        // 将路径加入撤销栈，清空重做栈，清空路径
        mUndoStack.add(new PathData(new Paint(mDrawPaint), new Path(mPath)));
        mRedoStack.clear();
        mPath.rewind();
        return true;
    }

    @Override
    public void run() {
        while (STATUS_DESTROYED != getStatus()) {
            long start = System.currentTimeMillis();
            drawContent();
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
            drawCache();
            drawCanvasBackground(mCanvas);
            mCanvas.drawBitmap(mCache, 0f, 0f, mPaint);

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
        return 0;
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
}
