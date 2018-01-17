package com.jp.jcanvas;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.jp.jcanvas.CanvasGestureDetector.OnCanvasGestureListener;
import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.brush.BrushImpl;
import com.jp.jcanvas.entity.HistoryData;
import com.jp.jcanvas.entity.Offset;
import com.jp.jcanvas.entity.Point;
import com.jp.jcanvas.entity.PointV;
import com.jp.jcanvas.entity.Scale;
import com.jp.jcanvas.entity.Track;
import com.jp.jcanvas.entity.Velocity;

import java.util.LinkedList;
import java.util.ListIterator;

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
    private static final int STATUS_SCALING = 2;

    /**
     * 移动状态。用户产生交互且正在移动视图。
     */
    private static final int STATUS_MOVING = 3;

    /**
     * 动画状态。无交互但正在显示动画。
     */
    private static final int STATUS_ANIMATING = 4;

    /**
     * 销毁状态。SurfaceView 已被销毁。
     */
    private static final int STATUS_DESTROYED = 5;

    private int mFrameTime;
    private float mMinScale;
    private float mMaxScale;

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Canvas mCacheCanvas;
    private Canvas mWorkingCanvas;

    private BaseBrush mBrush;
    private Paint mPaint;
    private Track mTrack;
    private Paint mPatternPaint;

    private Bitmap mCache;
    private Bitmap mWorkingSpace;
    private LinkedList<HistoryData> mCacheStack;

    private int mHeight;
    private int mWidth;
    private float mScale;
    private Offset mOffset;
    private Matrix mMatrix;

    private volatile int mStatus;
    private boolean mNeedInvalidate;
    private boolean mNeedFullInvalidate;

    private AccelerateDecelerateInterpolator mInterpolator;
    private Scroller mScroller;

    // 撤销栈与重做栈
    private LinkedList<HistoryData> mUndoStack;
    private LinkedList<HistoryData> mRedoStack;

    private OnScaleChangeListener mScaleListener;
    private RectF mOrin;
    private RectF mTrans;

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

        mFrameTime = DefaultValue.FRAME_TIME_MILLIS;

        setMinScale(DefaultValue.MIN_SCALE);
        setMaxScale(DefaultValue.MAX_SCALE);

        mBrush = new BrushImpl();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(DefaultValue.CANVAS_COLOR);
        // 缩放时对性能影响很大，暂时禁用
//        mPaint.setFilterBitmap(true);

        mPatternPaint = new Paint();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader s = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mPatternPaint.setShader(s);

        mTrack = new Track();

        mScale = 1.0f;
        mOffset = new Offset();
        mMatrix = new Matrix();

        mOrin = new RectF();
        mTrans = new RectF();

        mInterpolator = new AccelerateDecelerateInterpolator();
        mScroller = new Scroller(getContext(), mInterpolator);

        // 初始化撤销栈与重做栈
        mUndoStack = new LinkedList<>();
        mRedoStack = new LinkedList<>();

        mCacheStack = new LinkedList<>();

        mNeedInvalidate = false;
        mNeedFullInvalidate = false;

        setStatus(STATUS_DESTROYED);

        CanvasGestureDetector gDetector = new CanvasGestureDetector(getContext(), this);
        setOnTouchListener((v, event) -> {
            boolean handled = false;

            if (MotionEvent.ACTION_UP == event.getAction()) {
                handled = performClick();
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
        requestFullInvalidate();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHeight = height;
        mWidth = width;
        mOrin.set(0, 0, mWidth, mHeight);

        mCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mWorkingSpace = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCacheCanvas = new Canvas(mCache);
        mWorkingCanvas = new Canvas(mWorkingSpace);
        requestFullInvalidate();

        Log.i(this.getClass().getSimpleName(),
                "surfaceChanged: width = " + width + ", height = " + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setStatus(STATUS_DESTROYED);
    }

    private Point mDown;
    private boolean mAbortAnimating;

    @Override
    public boolean onActionDown(Point down) {
        if (STATUS_ANIMATING == getStatus()) {
            mScroller.abortAnimation();
            mLastScrX = 0;
            mLastScrY = 0;
            mAbortAnimating = true;
            setStatus(STATUS_IDLE);
        }

        mDown = new Point(down);
        return true;
    }

    @Override
    public boolean onSingleTapUp(Point focus) {
        if (!mAbortAnimating) {
            setStatus(STATUS_PAINTING);
            Track track = new Track();
            track.departure(new PointV(mDown.x, mDown.y, new Velocity()));
            track.addStation(new PointV(focus.x, focus.y, new Velocity()));

            Matrix matrix = new Matrix();
            matrix.setTranslate(-mOffset.x, -mOffset.y);
            matrix.postScale(1.0f / mScale, 1.0f / mScale);
            mTrack.set(track.applyTransform(matrix));
        }
        return true;
    }

    @Override
    public boolean onDrawPath(PointV focus, final Track track) {
        if (STATUS_PAINTING != getStatus()) {
            setStatus(STATUS_PAINTING);
        }

        Matrix matrix = new Matrix();
        matrix.setTranslate(-mOffset.x, -mOffset.y);
        matrix.postScale(1.0f / mScale, 1.0f / mScale);
        mTrack.set(track.applyTransform(matrix));

        return true;
    }

    private float mFactor;
    private float mStartScale;
    private Point mScalePivot;

    @Override
    public void onScaleStart(Point pivot) {
        setStatus(STATUS_SCALING);
        mFactor = 1.0f;
        mStartScale = mScale;
        mScalePivot = new Point(pivot);
        if (null != mScaleListener) {
            mScaleListener.onScaleChangeStart(mScale);
        }
    }

    @Override
    public boolean onScale(Scale scale, Offset pivotOffset) {
        mFactor *= scale.factor;
        float newScale = mStartScale * mFactor;

        // limit scale
        if (newScale > mMaxScale) {
            newScale = mMaxScale + (newScale - mMaxScale) / 4.0f;
        } else if (newScale < mMinScale) {
            newScale = mMinScale - (mMinScale - newScale) / 4.0f;
        }

        float f = newScale / mScale;
        mScale = newScale;

        mOffset.x = mOffset.x - (f - 1.0f) * (scale.getPivot().x - mOffset.x) + pivotOffset.x;
        mOffset.y = mOffset.y - (f - 1.0f) * (scale.getPivot().y - mOffset.y) + pivotOffset.y;

        mScalePivot.set(scale.getPivot().x, scale.getPivot().y);

        if (null != mScaleListener) {
            mScaleListener.onScaleChange(mScale);
        }

        return true;
    }

    @Override
    public void onScaleEnd(Point pivot) {
        mScalePivot.set(pivot);
        if (null != mScaleListener) {
            mScaleListener.onScaleChangeEnd(mScale);
        }
    }

    @Override
    public boolean onMove(PointV focus, Offset offset) {
        if (STATUS_MOVING != getStatus()) {
            setStatus(STATUS_MOVING);
        }

        mOffset.x = mOffset.x + offset.x;
        mOffset.y = mOffset.y + offset.y;
        mScalePivot.set(focus.x, focus.y);
        return true;
    }

    private float mAnimStartScale;
    private float mAnimEndScale;

    @Override
    public boolean onActionUp(PointV focus, boolean fling) {
        if (STATUS_PAINTING == getStatus()) {
            // 将路径加入撤销栈，清空重做栈，清空路径
            mUndoStack.addFirst(new HistoryData(mBrush.cloneBrush(), new Track(mTrack)));
            mRedoStack.clear();
            updateCache(false);
            mTrack.reset();
        }

        // 处理动画
        mAbortAnimating = false;
        boolean animating = checkScale() || checkBonds();
        mAnimStartScale = mScale;
        mAnimEndScale = mScale;
        if (animating) {
            int targetX;
            int targetY;

            if (mMinScale > mScale) {
                // 当前倍率 < mMinScale ，调整后倍率为 mMinScale ，计算偏移
                mAnimEndScale = mMinScale;

                targetX = (int) (mWidth * (1.0f - mAnimEndScale) / 2.0f - mOffset.x);
                targetY = (int) (mHeight * (1.0f - mAnimEndScale) / 2.0f - mOffset.y);

                if (null != mScaleListener) {
                    mScaleListener.onScaleChangeStart(mScale);
                }

            } else if (mMaxScale < mScale) {
                // 当前倍率 > mMaxScale ，调整后倍率为 mMaxScale ，偏移需要根据缩放控制点计算
                mAnimEndScale = mMaxScale;

                float endOffsetX = mScalePivot.x - (mScalePivot.x - mOffset.x)
                        / mAnimStartScale * mAnimEndScale;
                float endOffsetY = mScalePivot.y - (mScalePivot.y - mOffset.y)
                        / mAnimStartScale * mAnimEndScale;

                // 如果计算的偏移过度，还需要调整回来
                if (mWidth < ((mWidth - endOffsetX) / mAnimEndScale)) {
                    endOffsetX = mWidth * (1.0f - mAnimEndScale);
                } else if (0 < endOffsetX) {
                    endOffsetX = 0f;
                }

                if (mHeight < ((mHeight - endOffsetY) / mAnimEndScale)) {
                    endOffsetY = mHeight * (1.0f - mAnimEndScale);
                } else if (0 < endOffsetY) {
                    endOffsetY = 0f;
                }

                targetX = (int) (endOffsetX - mOffset.x);
                targetY = (int) (endOffsetY - mOffset.y);

                if (null != mScaleListener) {
                    mScaleListener.onScaleChangeStart(mScale);
                }

            } else {
                // 其他情况，倍率不变。偏移根据情况计算
                float endOffsetX;
                float endOffsetY;

                if (mScale < 1.0f) {
                    endOffsetX = mWidth * (1.0f - mAnimEndScale) / 2.0f;
                    endOffsetY = mHeight * (1.0f - mAnimEndScale) / 2.0f;

                } else {
                    if (mWidth < (mWidth - mOffset.x) / mScale) {
                        endOffsetX = mWidth * (1.0f - mAnimEndScale);
                    } else if (0 < mOffset.x) {
                        endOffsetX = 0f;
                    } else {
                        endOffsetX = mOffset.x;
                    }

                    if (mHeight < (mHeight - mOffset.y) / mScale) {
                        endOffsetY = mHeight * (1.0f - mAnimEndScale);
                    } else if (0 < mOffset.y) {
                        endOffsetY = 0f;
                    } else {
                        endOffsetY = mOffset.y;
                    }
                }

                targetX = (int) (endOffsetX - mOffset.x);
                targetY = (int) (endOffsetY - mOffset.y);
            }

            // 如果出现边界或倍率越界，则不进行惯性滑动
            mScroller.startScroll(0, 0, targetX, targetY);

        } else {
            if (fling) {
                mScroller.fling(0, 0,
                        (int) focus.getVelocity().x, (int) focus.getVelocity().y,
                        (int) (mWidth * (1.0f - mScale) - mOffset.x), (int) -mOffset.x,
                        (int) (mHeight * (1.0f - mScale) - mOffset.y), (int) -mOffset.y);
                animating = true;
            }
        }

        setStatus(animating ? STATUS_ANIMATING : STATUS_IDLE);
        return true;
    }

    /**
     * 检查倍率，是否出现越界情况
     *
     * @return 越界情况
     */
    private boolean checkScale() {
        return mScale > mMaxScale || mScale < mMinScale;
    }

    /**
     * 检查边界，是否出现越界情况
     * 由于调用此方法时对于倍率越界的判断已经进行，所以此处只需判断倍率未越界时的边界情况
     *
     * @return 越界情况
     */
    private boolean checkBonds() {
        boolean xOut;
        boolean yOut;
        if (mScale > 1.0f) {
            xOut = mOffset.x > 0 || mOffset.x < mWidth * (1.0f - mScale);
            yOut = mOffset.y > 0 || mOffset.y < mHeight * (1.0f - mScale);
        } else {
            // 若缩放倍率小于 1.0f 则永远在画布中央
            xOut = mOffset.x != mWidth * (1.0f - mScale) / 2.0f;
            yOut = mOffset.y != mHeight * (1.0f - mScale) / 2.0f;
        }
        return xOut || yOut;
    }

    private int mLastScrX = 0;
    private int mLastScrY = 0;

    @Override
    public void run() {
        while (STATUS_DESTROYED != getStatus()) {
            long time = 0;

            if (STATUS_IDLE != getStatus() || mNeedInvalidate) {
                long start = System.currentTimeMillis();
                mNeedInvalidate = false;

                if (STATUS_ANIMATING == getStatus()) {
                    // 获取滑动的位移
                    if (mScroller.computeScrollOffset()) {
                        mOffset.x += mScroller.getCurrX() - mLastScrX;
                        mOffset.y += mScroller.getCurrY() - mLastScrY;

                        if (mAnimStartScale != mAnimEndScale) {
                            float fraction = (float) mScroller.timePassed()
                                    / (float) mScroller.getDuration();

                            // timePassed() 获得的值可能大于 Duration
                            fraction = Math.min(fraction, 1.0f);
                            fraction = mInterpolator.getInterpolation(fraction);
                            mScale = mAnimStartScale + fraction * (mAnimEndScale - mAnimStartScale);

                            if (null != mScaleListener) {
                                // 保证回调在主线程执行
                                post(() -> mScaleListener.onScaleChange(mScale));
                            }
                        }

                        mLastScrX = mScroller.getCurrX();
                        mLastScrY = mScroller.getCurrY();

                    } else {
                        if (mAnimStartScale != mAnimEndScale) {
                            if (null != mScaleListener) {
                                // 保证回调在主线程执行
                                post(() -> mScaleListener.onScaleChangeEnd(mScale));
                            }
                        }
                        mLastScrX = 0;
                        mLastScrY = 0;
                        setStatus(STATUS_IDLE);
                    }
                }

                drawContent(mNeedFullInvalidate);
                mNeedFullInvalidate = false;

                long end = System.currentTimeMillis();
                time = end - start;
                Log.d(this.getClass().getSimpleName(), "frame time -> " + time + "ms");
            }

            if (time < mFrameTime) {
                try {
                    Thread.sleep(mFrameTime - time);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 绘制内容
     */
    private void drawContent(boolean fullInvalidate) {
        try {
            mCanvas = mHolder.lockCanvas();
            // 进行绘图操作
            mMatrix.setTranslate(mOffset.x, mOffset.y);
            mMatrix.postScale(mScale, mScale, mOffset.x, mOffset.y);

            if (fullInvalidate) {
                updateCache(true);
            }
            drawWorkingPath();
            drawCanvasBackground(mCanvas, mMatrix);
            mCanvas.drawBitmap(mWorkingSpace, mMatrix, mPaint);

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
    private synchronized void updateCache(boolean full) {
        if (full) {
            // 清空画布
            mCacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            mCacheStack.addAll(mUndoStack);
            // 绘制撤销栈中记录的路径。需要逆序遍历
            ListIterator it = mCacheStack.listIterator(mCacheStack.size());
            while (it.hasPrevious()) {
                HistoryData path = (HistoryData) it.previous();
                path.draw(mCacheCanvas);
            }

            mCacheStack.clear();

        } else {
            HistoryData data = mUndoStack.getFirst();
            data.draw(mCacheCanvas);
        }
    }

    private void drawWorkingPath() {
        // 清空画布
        mWorkingCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // 绘制缓存
        mWorkingCanvas.drawBitmap(mCache, 0, 0, mPaint);
        // 绘制当前工作路径
        if (!mTrack.isEmpty()) {
            Track track = new Track(mTrack);
            mBrush.drawTrack(mWorkingCanvas, track);
        }
    }

    /**
     * 绘制画布背景。
     */
    private void drawCanvasBackground(Canvas canvas, Matrix matrix) {
        // 平铺灰白格子
        canvas.drawPaint(mPatternPaint);
        // 绘制背景
        matrix.mapRect(mTrans, mOrin);
        canvas.drawRect(mTrans, mPaint);
    }

    /**
     * 请求进行绘制
     */
    private void requestInvalidate() {
        mNeedInvalidate = true;
    }

    private void requestFullInvalidate() {
        mNeedFullInvalidate = true;
        requestInvalidate();
    }

    /**
     * 设置状态
     *
     * @param status 状态
     */
    private void setStatus(int status) {
        mStatus = status;
        // 当设置为 STATUS_IDLE 时，很大可能绘制线程仍在绘制上一帧。
        // 当绘制线程绘制完上一帧而开始绘制当前帧时，检测到状态为 STATUS_IDLE 就会停止绘制。
        // 所以在将状态置为 STATUS_IDLE 时调用 requestInvalidate() 强制进行当前帧的绘制，保证完整。
        if (STATUS_IDLE == status) {
            requestInvalidate();
        }
    }

    /**
     * 获取当前状态
     *
     * @return 当前状态
     */
    public int getStatus() {
        return mStatus;
    }

    public void setBrush(BaseBrush brush) {
        this.mBrush = brush;
    }

    public BaseBrush getBrush() {
        return mBrush;
    }

    /**
     * 设定画布的最小缩放倍率
     * 最小倍率应在 0f 和 1.0f 之间。
     *
     * @param scale 倍率
     */
    public void setMinScale(float scale) {
        mMinScale = Math.min(Math.max(0f, scale), 1.0f);
    }

    /**
     * 设定画布的最大缩放倍率
     * 最大倍率至少为 1.0f
     *
     * @param scale 倍率
     */
    public void setMaxScale(float scale) {
        mMaxScale = Math.max(scale, 1.0f);
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
            HistoryData data = mUndoStack.removeFirst();
            mRedoStack.addFirst(data);
            requestFullInvalidate();
        }
    }

    /**
     * 重做撤销的操作
     */
    public void redo() {
        if (0 < mRedoStack.size()) {
            HistoryData data = mRedoStack.removeFirst();
            mUndoStack.addFirst(data);
            requestFullInvalidate();
        }
    }

    /**
     * 获取当前的 bitmap
     *
     * @return bitmap
     */
    public Bitmap getBitmap() {
        return Bitmap.createBitmap(mCache);
    }

    /**
     * 重置画布。重置撤销栈
     */
    public void resetCanvas() {
        mScale = 1.0f;
        mOffset.set(0f, 0f);
        mMatrix.reset();
        mTrack.reset();
        mUndoStack.clear();
        mRedoStack.clear();
        requestInvalidate();
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
