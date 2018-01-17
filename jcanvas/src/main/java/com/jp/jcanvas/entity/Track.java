package com.jp.jcanvas.entity;

import android.graphics.Matrix;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 */
public class Track implements Parcelable, Serializable {

    private LinkedList<PointV> mPoints;
    private transient LinkedList<Path> mSections;
    private transient Path mPath;

    private PointV mLastPoint;
    private Point mLastControl;

    private boolean mStarted;

    public Track() {
        this.mPoints = new LinkedList<>();
        this.mSections = new LinkedList<>();
        this.mPath = new Path();
        this.mLastPoint = new PointV();
        this.mLastControl = new Point();
        this.mStarted = false;
    }

    public Track(@NonNull Track track) {
        this.mPoints = new LinkedList<>(track.mPoints);
        this.mSections = new LinkedList<>(track.mSections);
        this.mPath = new Path(track.mPath);
        this.mLastPoint = new PointV(track.mLastPoint);
        this.mLastControl = new Point(track.mLastControl);
        this.mStarted = track.mStarted;
    }

    protected Track(Parcel in) {
        in.readTypedList(this.mPoints, PointV.CREATOR);
        this.mLastPoint = in.readParcelable(PointV.class.getClassLoader());
        this.mLastControl = in.readParcelable(Point.class.getClassLoader());
        this.mStarted = in.readByte() != 0;
        generatePathViaPoints(this.mPoints);
    }

    public void set(@NonNull Track track) {
        if (this == track) {
            return;
        }
        this.mPoints.clear();
        this.mPoints.addAll(track.mPoints);
        this.mSections.clear();
        this.mSections.addAll(track.mSections);
        this.mPath.set(track.mPath);
        this.mLastPoint.set(track.mLastPoint);
        this.mLastControl.set(track.mLastControl);
        this.mStarted = track.mStarted;
    }

    public void set(@NonNull LinkedList<PointV> points) {
        this.mPoints.clear();
        this.mPoints.addAll(points);
        this.mStarted = !(0 == points.size());

        generatePathViaPoints(mPoints);
    }

    private void generatePathViaPoints(LinkedList<PointV> points) {
        this.mSections.clear();
        this.mPath.reset();

        if (0 == points.size()) {
            return;
        }

        // 根据 points 还原 sections 和 path
        boolean firstNode = true;
        Point lastP = new Point();
        Point lastC = new Point();
        float cX;
        float cY;
        for (PointV p : points) {
            if (firstNode) {
                mPath.moveTo(p.x, p.y);
                cX = p.x;
                cY = p.y;
                firstNode = false;

            } else {
                // 贝塞尔曲线的控制点为起点和终点的中点
                cX = (p.x + lastP.x) / 2f;
                cY = (p.y + lastP.y) / 2f;
                mPath.quadTo(lastP.x, lastP.y, cX, cY);

                Path path = new Path();
                path.moveTo(mLastControl.x, mLastControl.y);
                path.quadTo(mLastPoint.x, mLastPoint.y, cX, cY);
                mSections.add(path);
            }

            lastP.set(p.x, p.y);
            lastC.set(cX, cY);
        }
    }

    public void reset() {
        this.mPoints.clear();
        this.mSections.clear();
        this.mPath.reset();
        this.mLastPoint.set(0f, 0f, new Velocity());
        this.mLastControl.set(0f, 0f);
        this.mStarted = false;
    }

    public boolean isEmpty() {
        return !mStarted || (mPoints.size() <= 1);
    }

    public void departure(@NonNull PointV p) {
        if (mStarted) {
            Log.w(this.getClass().getSimpleName(), "Already Started! Do Nothing.");
            return;
        }

        mStarted = true;
        mPoints.add(p);
        mPath.moveTo(p.x, p.y);
        mLastPoint.set(p);
        mLastControl.set(p.x, p.y);
    }

    public void addStation(@NonNull PointV p) {
        if (!mStarted) {
            // 默认从 (0, 0) 开始
            departure(new PointV(0f, 0f, new Velocity()));
        }

        mPoints.add(p);
        float cX = (p.x + mLastPoint.x) / 2f;
        float cY = (p.y + mLastPoint.y) / 2f;

        Path path = new Path();
        path.moveTo(mLastControl.x, mLastControl.y);
        path.quadTo(mLastPoint.x, mLastPoint.y, cX, cY);
        mSections.add(path);

        mPath.quadTo(mLastPoint.x, mLastPoint.y, cX, cY);

        mLastPoint.set(p);
        mLastControl.set(cX, cY);
    }

    public Track transform(Matrix matrix) {
        float[] pts = new float[2];
        for (PointV p : mPoints) {
            pts[0] = p.x;
            pts[1] = p.y;
            matrix.mapPoints(pts);
            p.set(pts[0], pts[1]);
        }

        for (Path p : mSections) {
            p.transform(matrix);
        }

        mPath.transform(matrix);

        return this;
    }

    public LinkedList<PointV> getStations() {
        return mPoints;
    }

    public LinkedList<Path> getSections() {
        return mSections;
    }

    public Path getPath() {
        return new Path(mPath);
    }

    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        generatePathViaPoints(mPoints);
    }

    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        s.defaultWriteObject();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.mPoints);
        dest.writeParcelable(this.mLastPoint, flags);
        dest.writeParcelable(this.mLastControl, flags);
        dest.writeByte(this.mStarted ? (byte) 1 : (byte) 0);
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel source) {
            return new Track(source);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
}
