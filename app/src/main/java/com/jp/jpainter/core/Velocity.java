package com.jp.jpainter.core;

import java.io.Serializable;

/**
 *
 */
public class Velocity implements Serializable {
    public float x;
    public float y;

    public Velocity() {
        this(0f, 0f);
    }

    public Velocity(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
