package com.jp.jcanvas.widget;

import android.content.Context;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.jp.jcanvas.R;
import com.jp.jcanvas.brush.BaseBrush;

import java.util.ArrayList;

/**
 *
 */
public class BrushSelector extends LinearLayout {

    private BrushList mBrushList;
    private OnBrushSelectListener mListener;

    private BaseBrush mCurrentBrush;
    @FloatRange(from = 0f, to = 1f)
    private float mBrushSize;
    @FloatRange(from = 0f, to = 1f)
    private float mBrushAlpha;

    public BrushSelector(Context context) {
        this(context, null);
    }

    public BrushSelector(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.layout_brush_selector, this);
        mBrushList = findViewById(R.id.bl_list);
        BrushPreview mPreview = findViewById(R.id.bp_brush_preview);
        ColorSeekBar mSizeSeek = findViewById(R.id.csb_size);
        ColorSeekBar mAlphaSeek = findViewById(R.id.csb_alpha);

        mBrushSize = 0.1f;
        mBrushAlpha = 1f;

        mSizeSeek.setProgress(mBrushSize);
        mAlphaSeek.setProgress(mBrushAlpha);

        mBrushList.setOnBrushSelectListener(brush -> {
            mCurrentBrush = brush;
            mCurrentBrush.setSize(mBrushSize);
            mCurrentBrush.setAlpha(mBrushAlpha);
            mPreview.setBrush(mCurrentBrush);
            if (null != mListener) {
                mListener.onBrushSelected(brush);
            }
        });

        mSizeSeek.setOnProgressChangeListener(progress -> {
            mBrushSize = progress;
            mCurrentBrush.setSize(mBrushSize);
            mPreview.setBrush(mCurrentBrush);
        });

        mAlphaSeek.setOnProgressChangeListener(progress -> {
            mBrushAlpha = progress;
            mCurrentBrush.setAlpha(mBrushAlpha);
            mPreview.setBrush(mCurrentBrush);
        });
    }

    public void addBrush(BaseBrush brush) {
        mBrushList.addBrush(brush);
    }

    public void addBrushes(ArrayList<BaseBrush> brushes) {
        mBrushList.addBrushes(brushes);
    }

    public BaseBrush getBrush() {
        mCurrentBrush.setSize(mBrushSize);
        mCurrentBrush.setAlpha(mBrushAlpha);
        return mCurrentBrush;
    }

    public void setOnBrushSelectListener(OnBrushSelectListener listener) {
        mListener = listener;
    }

    public interface OnBrushSelectListener {
        void onBrushSelected(BaseBrush brush);
    }
}
