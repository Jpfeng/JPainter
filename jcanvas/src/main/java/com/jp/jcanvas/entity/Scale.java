package com.jp.jcanvas.entity;

/**
 *
 */
public class Scale {
    public float factor;
    private PointV pivot;

    public Scale() {
        this(1.0f, new PointV());
    }

    public Scale(Scale src) {
        this(src.factor, src.pivot);
    }

    public Scale(float factor, PointV pivot) {
        this.factor = factor;
        this.pivot = new PointV(pivot);
    }

    public void set(float scale, PointV pivot) {
        this.factor = scale;
        this.pivot.set(pivot);
    }

    public void set(Scale scale) {
        this.factor = scale.factor;
        this.pivot.set(scale.pivot);
    }

    public PointV getPivot() {
        return pivot;
    }

    public final boolean equals(float scale, PointV pivot) {
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
    public String toString() {
        return "Scale " + factor + ", Pivot(" + pivot.x + ", " + pivot.y + ")";
    }
}
