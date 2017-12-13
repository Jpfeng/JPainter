package com.jp.jpainter;

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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.jp.jcanvas.CanvasGestureDetector;
import com.jp.jcanvas.CanvasGestureDetector.OnCanvasGestureListener;
import com.jp.jcanvas.entity.Offset;
import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.Scale;
import com.jp.jcanvas.entity.Velocity;
import com.jp.jpainter.core.entity.PathData;
import com.jp.jpainter.utils.LogUtil;

import java.util.ArrayList;

/**
 *
 */
public class SurfaceCanvasTest extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // time per frame. 60fps
    private static final int mFrameTime = (int) ((1000 / 60f) + 0.5f);

    private ArrayList<PathData> mUndoStack;
    private ArrayList<PathData> mRedoStack;

    private Canvas mCanvas;
    private Canvas mDrawCanvas;
    private Paint mPaint;
    private Paint mDrawPaint;
    private Bitmap mCache;

    private int mHeight;
    private int mWidth;

    // SurfaceHolder
    private SurfaceHolder mHolder;
    private boolean mIsDrawing;
    private CanvasGestureDetector gestureDetector;

    public SurfaceCanvasTest(Context context) {
        this(context, null);
    }

    public SurfaceCanvasTest(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfaceCanvasTest(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(8.0f);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        CornerPathEffect effect = new CornerPathEffect(30);
        mPaint.setPathEffect(effect);

        mPath = new Path();

        mUndoStack = new ArrayList<>();
        mRedoStack = new ArrayList<>();

        gestureDetector = new CanvasGestureDetector(getContext(), new OnCanvasGestureListener() {
            @Override
            public boolean onActionDown(Point down) {
                LogUtil.d("gestureDetector -> ", "onActionDown(" + down.x + ", " + down.y + ")");
                return true;
            }

            @Override
            public boolean onDrawPath(Path path) {
                mPath.set(path);
                return true;
            }

            @Override
            public boolean onScaleStart(Point pivot) {
                LogUtil.d("gestureDetector -> ", "onScaleStart(" + pivot.x + ", " + pivot.y + ")");
                return false;
            }

            @Override
            public boolean onScale(Point pivot, Scale scale, Offset offset) {
                return false;
            }

            @Override
            public boolean onScaleEnd(Point pivot) {
                return false;
            }

            @Override
            public boolean onMove(Point focus, Offset offset) {
                return false;
            }

            @Override
            public boolean onActionUp(Point focus, boolean fling, Velocity velocity) {
                LogUtil.d("gestureDetector -> ", "onActionUp()");
                return true;
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    private Path mPath;

    private VelocityTracker vTracker;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (null == vTracker) {
            vTracker = VelocityTracker.obtain();
        }

        vTracker.addMovement(event);

        vTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity());
        float velocityX = vTracker.getXVelocity();
        float velocityY = vTracker.getYVelocity();
        float v = (float) Math.hypot(velocityX, velocityY);

        LogUtil.d("vTracker -> ", "V(" + velocityX + ", " + velocityY + ")" + v);


//        vTracker.

        final int count = event.getPointerCount();
//        vTracker.computeCurrentVelocity(1000);
//        final int upIndex = event.getActionIndex();
//        final int id1 = event.getPointerId(upIndex);
//        final float x1 = vTracker.getXVelocity(id1);
//        final float y1 = vTracker.getYVelocity(id1);
//        for (int i = 0; i < count; i++) {
//            if (i == upIndex) continue;
//
//            final int id2 = event.getPointerId(i);
//            final float x = x1 * vTracker.getXVelocity(id2);
//            final float y = y1 * vTracker.getYVelocity(id2);

//                final float dot = x + y;
//                if (dot < 0) {
//                    vTracker.clear();
//                    break;
//                }
//        }

        return true;
    }

    //    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        float x = event.getX();
//        float y = event.getY();
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                mPath.moveTo(x, y);
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                mPath.lineTo(x, y);
//                break;
//
//            case MotionEvent.ACTION_UP:
//                mUndoStack.add(new PathData(mPaint, new Path(mPath)));
//                mRedoStack.clear();
//                mPath.reset();
//                break;
//        }
//
//        return true;
//    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsDrawing = true;
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.WHITE);
        mHolder.unlockCanvasAndPost(mCanvas);
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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsDrawing = false;
    }

    private void drawCanvasBackground(Canvas canvas) {
        // 平铺灰白格子
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader shader = new BitmapShader(
                bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(shader);
        canvas.drawPaint(p);
    }

    private void drawPaintBoardBackground(Canvas canvas) {
        // 绘制白色画板
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
    }

    @Override
    public void run() {
        while (mIsDrawing) {
            long start = System.currentTimeMillis();

            drawContent();

            long end = System.currentTimeMillis();

            long time = end - start;
            if (time < mFrameTime) {
                try {
                    Thread.sleep(mFrameTime - time);

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

            drawPaintBoardBackground(mDrawCanvas);

            for (PathData path : mUndoStack) {
                path.draw(mDrawCanvas);
            }
            if (!mPath.isEmpty()) {
                mDrawCanvas.drawPath(mPath, mPaint);
            }

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

    public boolean canUndo() {
        return mUndoStack.size() > 0;
    }

    public boolean canRedo() {
        return mRedoStack.size() > 0;
    }

    public void undo() {
        if (0 < mUndoStack.size()) {
            PathData data = mUndoStack.remove(mUndoStack.size() - 1);
            mRedoStack.add(data);
        }
    }

    public void redo() {
        if (0 < mRedoStack.size()) {
            PathData data = mRedoStack.remove(mRedoStack.size() - 1);
            mUndoStack.add(data);
        }
    }
}
