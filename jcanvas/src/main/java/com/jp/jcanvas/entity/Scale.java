package com.jp.jcanvas.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 */
public class Scale implements Parcelable {
    public float factor;
    public Point pivot;

    public Scale() {
        this(1.0f, new Point());
    }

    public Scale(Scale src) {
        this(src.factor, src.pivot);
    }

    public Scale(float factor, Point pivot) {
        this.factor = factor;
        this.pivot = new Point(pivot);
    }

    protected Scale(Parcel in) {
        factor = in.readFloat();
        pivot = in.readParcelable(Point.class.getClassLoader());
    }

    public void set(float scale, Point pivot) {
        this.factor = scale;
        this.pivot.set(pivot);
    }

    public void set(Scale scale) {
        this.factor = scale.factor;
        this.pivot.set(scale.pivot);
    }

    public final boolean equals(float scale, Point pivot) {
        return this.factor == scale && this.pivot.equals(pivot);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        Scale target = (Scale) obj;
        return equals(target.factor, target.pivot);
    }

    @Override
    public int hashCode() {
        return (int) (31 * factor + pivot.hashCode());
    }

    @Override
    public String toString() {
        return "Scale " + factor + ", Pivot(" + pivot.x + ", " + pivot.y + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(factor);
        dest.writeParcelable(pivot, flags);
    }

    public static final Creator<Scale> CREATOR = new Creator<Scale>() {
        @Override
        public Scale createFromParcel(Parcel in) {
            return new Scale(in);
        }

        @Override
        public Scale[] newArray(int size) {
            return new Scale[size];
        }
    };
}
