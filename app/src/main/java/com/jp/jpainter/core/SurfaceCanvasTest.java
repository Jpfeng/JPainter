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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jp.jpainter.R;

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

        CornerPathEffect effect = new CornerPathEffect(30);
        mPaint.setPathEffect(effect);

        mPath = new Path();

        mUndoStack = new ArrayList<>();
        mRedoStack = new ArrayList<>();
    }

    private Path mPath;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(x, y);
                break;

            case MotionEvent.ACTION_UP:
                mUndoStack.add(new PathData(mPaint, new Path(mPath)));
                mRedoStack.clear();
                mPath.reset();
                break;
        }

        return true;
    }

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
