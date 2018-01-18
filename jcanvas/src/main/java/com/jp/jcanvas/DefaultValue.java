package com.jp.jcanvas;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
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
     * 最小缩放倍率
     */
    public static final float MIN_SCALE = 1.0f;

    /**
     * 最大缩放倍率
     */
    public static final float MAX_SCALE = 6.0f;

    /**
     * 默认画布颜色
     */
    @ColorInt
    public static final int CANVAS_COLOR = Color.WHITE;
}
