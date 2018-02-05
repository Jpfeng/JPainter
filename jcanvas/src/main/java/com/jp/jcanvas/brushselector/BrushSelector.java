package com.jp.jcanvas.brushselector;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.jp.jcanvas.R;

/**
 *
 */
public class BrushSelector extends LinearLayout {
    public BrushSelector(Context context) {
        this(context, null);
    }

    public BrushSelector(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.layout_brush_selector, this);
        BrushList mBrushList = findViewById(R.id.bl_list);
    }
}
