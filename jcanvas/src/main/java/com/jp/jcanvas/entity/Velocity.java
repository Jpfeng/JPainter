package com.jp.jcanvas.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 *
 */
public class Velocity implements Parcelable, Serializable {
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

    protected Velocity(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
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
    public int hashCode() {
        return (int) (31 * x + y);
    }

    @Override
    public String toString() {
        return "Velocity(" + x + ", " + y + ")";
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

    public static final Creator<Velocity> CREATOR = new Creator<Velocity>() {
        @Override
        public Velocity createFromParcel(Parcel in) {
            return new Velocity(in);
        }

        @Override
        public Velocity[] newArray(int size) {
            return new Velocity[size];
        }
    };
}
