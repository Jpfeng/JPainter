package com.jp.jpainter.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jp.jpainter.R;

import java.util.ArrayList;

/**
 *
 */
public class ToolMenu extends FrameLayout {

    private TextView mBtnMenu;
    private ArrayList<View> mTools;

    private boolean opened;
    private OnToolClickListener mClickListener;

    private AnimatorSet mCurrentAnim;

    private int mToolMargin = 48;
    private ToolMenuListener mListener;

    public ToolMenu(Context context) {
        this(context, null);
    }

    public ToolMenu(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToolMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View v = LayoutInflater.from(context).inflate(R.layout.layout_widget_menu, this);
        mBtnMenu = v.findViewById(R.id.btn_menu_menu);
        TextView mBtnColor = v.findViewById(R.id.btn_menu_color);
        TextView mBtnBrush = v.findViewById(R.id.btn_menu_brush);
        TextView mBtnGallery = v.findViewById(R.id.btn_menu_gallery);
        TextView mBtnSave = v.findViewById(R.id.btn_menu_save);
        TextView mBtnClear = v.findViewById(R.id.btn_menu_clear);
        TextView mBtnSetting = v.findViewById(R.id.btn_menu_settings);
        TextView mBtnImport = v.findViewById(R.id.btn_menu_import);
        TextView mBtnLayer = v.findViewById(R.id.btn_menu_layer);
        TextView mBtnUndo = v.findViewById(R.id.btn_menu_undo);

        mTools = new ArrayList<>();
        opened = false;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (!(child instanceof ViewGroup)) {
                mTools.add(child);
            }
        }

        mCurrentAnim = new AnimatorSet();

        mBtnColor.setOnClickListener(view -> {
            if (null != mClickListener) mClickListener.onColorClicked();
        });
        mBtnMenu.setOnClickListener(view -> {
            toggle();
        });
        mBtnBrush.setOnClickListener(view -> {
            if (null != mClickListener) mClickListener.onBrushClicked();
        });
        mBtnGallery.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onGalleryClicked();
        });
        mBtnSave.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onSaveClicked();
        });
        mBtnClear.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onClearClicked();
        });
        mBtnSetting.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onSettingsClicked();
        });
        mBtnImport.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onImportClicked();
        });
        mBtnLayer.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onLayerClicked();
        });
        mBtnUndo.setOnClickListener(view -> {
            if (null != mClickListener && opened) mClickListener.onUndoClicked();
        });
    }

    private void toggle() {
        if (!opened) {
            open();
        } else {
            close();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (opened) {
                    return true;
                }

            case MotionEvent.ACTION_UP:
                close();
                break;
        }
        return super.onTouchEvent(event);
    }

    public void open() {
        if (opened) {
            return;
        }

        opened = true;
        if (null != mListener) {
            mListener.onMenuOpened(this);
        }
        mCurrentAnim.cancel();

        int h = mToolMargin * (mTools.size() - 1);
        for (View v : mTools) {
            h += v.getHeight();
            v.setAlpha(0);
            v.setVisibility(VISIBLE);
        }

        int[] position = new int[2];
        mBtnMenu.getLocationOnScreen(position);

        float x = position[0] + mBtnMenu.getWidth() + mToolMargin;
        float y = (getHeight() - h) / 2;

        mCurrentAnim = new AnimatorSet();
        mCurrentAnim.setDuration(250);
        mCurrentAnim.removeAllListeners();
        ArrayList<Animator> anims = new ArrayList<>();
        for (View v : mTools) {
            ObjectAnimator a1 = ObjectAnimator.ofFloat(v, "x", x);
            ObjectAnimator a2 = ObjectAnimator.ofFloat(v, "y", y);
            ObjectAnimator a3 = ObjectAnimator.ofFloat(v, "alpha", 1);
            anims.add(a1);
            anims.add(a2);
            anims.add(a3);

            y += v.getHeight() + mToolMargin;
        }
        mCurrentAnim.playTogether(anims);
        mCurrentAnim.start();
    }

    public void close() {
        if (!opened) {
            return;
        }

        opened = false;
        if (null != mListener) {
            mListener.onMenuClosed(this);
        }
        mCurrentAnim.cancel();

        int[] position = new int[2];
        mBtnMenu.getLocationOnScreen(position);

        float x = position[0];
        float y = position[1];

        mCurrentAnim = new AnimatorSet();
        mCurrentAnim.setDuration(250);
        mCurrentAnim.removeAllListeners();
        mCurrentAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                for (View v : mTools) {
                    v.setVisibility(INVISIBLE);
                }
            }
        });

        ArrayList<Animator> anims = new ArrayList<>();
        for (View v : mTools) {
            ObjectAnimator a1 = ObjectAnimator.ofFloat(v, "x", x);
            ObjectAnimator a2 = ObjectAnimator.ofFloat(v, "y", y);
            ObjectAnimator a3 = ObjectAnimator.ofFloat(v, "alpha", 0);
            anims.add(a1);
            anims.add(a2);
            anims.add(a3);
        }
        mCurrentAnim.playTogether(anims);
        mCurrentAnim.start();
    }

    public void setOnToolClickListener(OnToolClickListener listener) {
        mClickListener = listener;
    }

    public void setToolMenuListener(ToolMenuListener listener) {
        mListener = listener;
    }

    public interface OnToolClickListener {
        void onColorClicked();

        void onBrushClicked();

        void onGalleryClicked();

        void onSaveClicked();

        void onClearClicked();

        void onSettingsClicked();

        void onImportClicked();

        void onLayerClicked();

        void onUndoClicked();
    }

    public interface ToolMenuListener {
        void onMenuOpened(ToolMenu view);

        void onMenuClosed(ToolMenu view);
    }
}
