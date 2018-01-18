package com.jp.jcanvas.entity;

/**
 *
 */
public class Velocity {
    public float x;
    public float y;

    public Velocity() {
        this(0f, 0f);
    }

    public Velocity(Velocity src) {
        this(src.x, src.y);
    }

    public Velocity(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(Velocity v) {
        this.x = v.x;
        this.y = v.y;
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

        Velocity target = (Velocity) obj;
        return equals(target.x, target.y);
    }

    @Override
    public String toString() {
        return "Velocity(" + x + ", " + y + ")";
    }
}
