package com.jp.jcanvas.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 */
public class Point implements Parcelable {
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

    protected Point(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
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
    public int hashCode() {
        return (int) (31 * x + y);
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(x);
        dest.writeFloat(y);
    }

    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };
}
