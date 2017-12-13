package com.jp.jcanvas.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 */
public class Offset implements Parcelable {
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

    protected Offset(Parcel in) {
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

        Offset target = (Offset) obj;
        return equals(target.x, target.y);
    }

    @Override
    public int hashCode() {
        return (int) (31 * x + y);
    }

    @Override
    public String toString() {
        return "Offset(" + x + ", " + y + ")";
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

    public static final Creator<Offset> CREATOR = new Creator<Offset>() {
        @Override
        public Offset createFromParcel(Parcel in) {
            return new Offset(in);
        }

        @Override
        public Offset[] newArray(int size) {
            return new Offset[size];
        }
    };
}
