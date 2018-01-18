package com.jp.jcanvas.entity;

/**
 *
 */
public class Offset {
    public float x;
    public float y;

    public Offset() {
        this(0f, 0f);
    }

    public Offset(Offset src) {
        this(src.x, src.y);
    }

    public Offset(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(Offset offset) {
        this.x = offset.x;
        this.y = offset.y;
    }

    public final boolean equals(float x, float y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        Offset target = (Offset) obj;
        return equals(target.x, target.y);
    }

    @Override
    public String toString() {
        return "Offset(" + x + ", " + y + ")";
    }
}
