package com.jp.jpainter.core;

import java.io.Serializable;

/**
 *
 */
public class Point implements Serializable {
    public float x;
    public float y;

    public Point() {
        this(0f, 0f);
    }

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
