package com.jp.jcanvas.widget;

import android.content.Context;
import android.content.res.TypedArray;
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
class ColorInfo extends LinearLayout {

    private TextView mTvRGB;
    private TextView mTvHEX;

    private int mColor;
    private StringBuilder mSBuilder;

    public ColorInfo(Context context) {
        this(context, null);
    }

    public ColorInfo(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.ColorInfoStyle);
    }

    public ColorInfo(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ColorInfo, defStyleAttr, 0);

        mColor = ta.getColor(R.styleable.ColorInfo_ci_color, Color.BLACK);
        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_color_info, this);
        mTvRGB = view.findViewById(R.id.value_rgb);
        mTvHEX = view.findViewById(R.id.value_hex);

        mSBuilder = new StringBuilder();
        showValue();
    }

    public void setColor(@ColorInt int color) {
        mColor = color;
        showValue();
    }

    private void showValue() {
        int r = Color.red(mColor);
        int g = Color.green(mColor);
        int b = Color.blue(mColor);

        mSBuilder.append(r)
                .append(", ")
                .append(g)
                .append(", ")
                .append(b);

        mTvRGB.setText(mSBuilder.toString());
        mTvHEX.setText(Integer.toHexString(mColor).toUpperCase().substring(2));

        mSBuilder.delete(0, mSBuilder.length());
    }
}
