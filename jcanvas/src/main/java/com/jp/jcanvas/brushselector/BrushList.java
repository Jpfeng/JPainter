package com.jp.jcanvas.brushselector;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.jp.jcanvas.R;
import com.jp.jcanvas.brush.BaseBrush;

import java.util.ArrayList;

/**
 *
 */
class BrushList extends RecyclerView {

    private ArrayList<BaseBrush> mBrushes;
    private BrushAdapter mAdapter;

    private BaseBrush mSelected;
    private OnBrushSelectListener mListener;

    public BrushList(Context context) {
        this(context, null);
    }

    public BrushList(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushList(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mBrushes = new ArrayList<>();

        mAdapter = new BrushAdapter();
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        setAdapter(mAdapter);
    }

    public BrushList addBrush(BaseBrush brush) {
        mBrushes.add(brush);
        mAdapter.notifyItemInserted(mBrushes.size() - 1);
        return this;
    }

    public BrushList addBrushes(ArrayList<BaseBrush> brushes) {
        mBrushes.addAll(brushes);
        mAdapter.notifyItemRangeInserted(
                mBrushes.size() - brushes.size(), mBrushes.size() - 1);
        return this;
    }

    public BrushList setOnBrushSelectListener(OnBrushSelectListener listener) {
        mListener = listener;
        return this;
    }

    private class BrushAdapter extends Adapter<BrushViewHolder> {

        @Override
        public BrushViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BrushViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_brush_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(BrushViewHolder holder, int position) {
            if (mBrushes.size() > position && null != mBrushes.get(position)) {
                final BaseBrush brush = mBrushes.get(position);
                holder.ivIcon.setImageDrawable(brush.getIcon());
                holder.ivIcon.setOnClickListener(v -> {
                    mSelected = brush;
                    if (null != mListener) {
                        mListener.onBrushSelected(mSelected);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mBrushes.size();
        }
    }

    private class BrushViewHolder extends ViewHolder {

        private ImageView ivIcon;

        BrushViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_brush_icon);
        }
    }

    public interface OnBrushSelectListener {
        void onBrushSelected(BaseBrush brush);
    }
}
