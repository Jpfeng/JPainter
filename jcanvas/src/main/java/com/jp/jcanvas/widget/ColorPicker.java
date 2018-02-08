package com.jp.jcanvas.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jp.jcanvas.R;

/**
 *
 */
public class ColorPicker extends LinearLayout {

    private HueWheel mHueWheel;
    private SaturationValuePanel mSVPanel;
    private ColorPreview mPreview;
    private ColorInfo mInfo;

    private OnConfirmListener mListener;

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_color_picker, this);
        mHueWheel = view.findViewById(R.id.hw_hue);
        mSVPanel = view.findViewById(R.id.svp_panel);
        mPreview = view.findViewById(R.id.cpv_preview);
        mInfo = view.findViewById(R.id.ci_info);
        TextView tvConfirm = view.findViewById(R.id.tv_confirm);

        mHueWheel.setOnHueChangeListener(hue -> mSVPanel.setHue(hue));
        mSVPanel.setOnColorChangeListener(color -> {
            mPreview.setNew(getColor());
            mInfo.setColor(getColor());
        });

        tvConfirm.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onConfirm(this);
            }
            mPreview.setColor(getColor());
        });
    }

    public void setColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mHueWheel.setHue(hsv[0]);
        mSVPanel.setColor(color);
        mPreview.setColor(color);
    }

    @ColorInt
    public int getColor() {
        return mSVPanel.getColor();
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        mListener = listener;
    }

    public interface OnConfirmListener {
        void onConfirm(ColorPicker view);
    }
}
