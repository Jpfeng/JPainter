package com.jp.jcanvas.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 *
 */
public class PointV implements Parcelable, Serializable {
    public float x;
    public float y;
    private Velocity v;

    public PointV() {
        this(0f, 0f, new Velocity(0f, 0f));
    }

    public PointV(PointV src) {
        this(src.x, src.y, src.v);
    }

    public PointV(float x, float y, Velocity v) {
        this.x = x;
        this.y = y;
        this.v = new Velocity(v);
    }

    protected PointV(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
        v = in.readParcelable(Velocity.class.getClassLoader());
    }

    public Velocity getVelocity() {
        return v;
    }

    public void set(float x, float y, Velocity v) {
        set(x, y);
        this.v = new Velocity(v);
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(PointV p) {
        this.x = p.x;
        this.y = p.y;
        this.v = new Velocity(p.v);
    }

    public final boolean equals(float x, float y) {
        return this.x == x && this.y == y;
    }

    public final boolean equals(float x, float y, Velocity v) {
        return this.equals(x, y) && this.v.equals(v);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        PointV target = (PointV) obj;
        return equals(target.x, target.y, target.v);
    }

    @Override
    public int hashCode() {
        return (int) (31 * x + y + v.x + v.y);
    }

    @Override
    public String toString() {
        return "Point@(" + x + ", " + y + "), v = (" + v.x + ", " + v.y + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeParcelable(v, flags);
    }

    public static final Creator<PointV> CREATOR = new Creator<PointV>() {
        @Override
        public PointV createFromParcel(Parcel in) {
            return new PointV(in);
        }

        @Override
        public PointV[] newArray(int size) {
            return new PointV[size];
        }
    };
}
