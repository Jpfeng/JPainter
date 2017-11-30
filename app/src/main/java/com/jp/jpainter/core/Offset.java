package com.jp.jpainter.core;

import java.io.Serializable;

/**
 *
 */
public class Offset implements Serializable {
    private float x;
    private float y;

    public Offset() {
        this(0f, 0f);
    }

    public Offset(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
