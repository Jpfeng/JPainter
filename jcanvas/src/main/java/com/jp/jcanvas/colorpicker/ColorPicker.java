package com.jp.jcanvas.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.jp.jcanvas.R;

/**
 *
 */
public class ColorPicker extends LinearLayout {

    private HueWheel mHueWheel;
    private SaturationValuePanel mSVPanel;

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

    @TargetApi(21)
    public ColorPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_color_picker, this);
        mHueWheel = view.findViewById(R.id.hw_hue);
        mSVPanel = view.findViewById(R.id.svp_panel);

        mHueWheel.setOnHueChangeListener(hue -> mSVPanel.setHue(hue));
    }

    public void setColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mHueWheel.setHue(hsv[0]);
        mSVPanel.setColor(color);
    }

    @ColorInt
    public int getColor() {
        return mSVPanel.getColor();
    }
}
