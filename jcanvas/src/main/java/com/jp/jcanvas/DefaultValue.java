package com.jp.jcanvas;

import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;

/**
 * 默认值
 */
public class DefaultValue {

    /**
     * 每秒帧率。普通 60fps
     */
    public static final int NORMAL_FRAME_RATE = 60;

    /**
     * 每一帧的时间。
     */
    public static final int FRAME_TIME_MILLIS = (int) (1000 / NORMAL_FRAME_RATE);

    /**
     * 默认笔迹圆滑半径
     */
    public static final float CORNER_RADIUS = 30.0f;

    /**
     * 最小缩放倍率
     */
    public static final float MIN_SCALE = 1.0f;

    /**
     * 最大缩放倍率
     */
    public static final float MAX_SCALE = 6.0f;

    /**
     * 默认线条宽度
     */
    public static final float PAINT_WIDTH = 8.0f;

    /**
     * 默认画笔颜色
     */
    @ColorInt
    public static final int PAINT_COLOR = Color.BLACK;

    /**
     * 默认笔头形状
     */
    public static final Paint.Cap PAINT_CAP = Paint.Cap.ROUND;
}
