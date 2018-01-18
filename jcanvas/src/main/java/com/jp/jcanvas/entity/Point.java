package com.jp.jcanvas.entity;

/**
 *
 */
public class Point {
    public float x;
    public float y;

    public Point() {
        this(0f, 0f);
    }

    public Point(Point src) {
        this(src.x, src.y);
    }

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(Point src) {
        this.x = src.x;
        this.y = src.y;
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

        Point target = (Point) obj;
        return equals(target.x, target.y);
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
}
