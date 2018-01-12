package com.jp.jcanvas.entity;

import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 */
public class Track implements Serializable {

    private LinkedList<Point> mPoints;
    private LinkedList<Path> mSections;
    private Path mPath;

    private Point mLastPoint;
    private Point mLastControl;

    private boolean mStarted;

    public Track() {
        this.mPoints = new LinkedList<>();
        this.mSections = new LinkedList<>();
        this.mPath = new Path();
        this.mLastPoint = new Point();
        this.mLastControl = new Point();
        this.mStarted = false;
    }

    public Track(@NonNull Track track) {
        this.mPoints = new LinkedList<>(track.mPoints);
        this.mSections = new LinkedList<>(track.mSections);
        this.mPath = new Path(track.mPath);
        this.mLastPoint = new Point(track.mLastPoint);
        this.mLastControl = new Point(track.mLastControl);
        this.mStarted = track.mStarted;
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

    public void set(@NonNull LinkedList<Point> points) {
        this.mPoints.clear();
        this.mPoints.addAll(points);

        this.mSections.clear();
        this.mPath.reset();
        this.mStarted = !(0 == points.size());

        if (0 == points.size()) {
            return;
        }

        // 根据 points 还原 sections 和 path
        boolean firstNode = true;
        Point lastP = new Point();
        Point lastC = new Point();
        float cX;
        float cY;
        for (Point p : points) {
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

            lastP.set(p);
            lastC.set(cX, cY);
        }
    }

    public void reset() {
        this.mPoints.clear();
        this.mSections.clear();
        this.mPath.reset();
        this.mLastPoint.set(0f, 0f);
        this.mLastControl.set(0f, 0f);
        this.mStarted = false;
    }

    public boolean isEmpty() {
        return !mStarted || (mPoints.size() <= 1);
    }

    public void departure(@NonNull Point p) {
        if (mStarted) {
            Log.w(this.getClass().getSimpleName(), "Already Started! Do Nothing.");
            return;
        }

        mStarted = true;
        mPoints.add(p);
        mPath.moveTo(p.x, p.y);
        mLastPoint.set(p);
        mLastControl.set(p);
    }

    public void addStation(@NonNull Point p) {
        if (!mStarted) {
            // 默认从 (0, 0) 开始
            departure(new Point(0f, 0f));
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

    public LinkedList<Point> getStations() {
        return new LinkedList<>(mPoints);
    }

    public LinkedList<Path> getSections() {
        return new LinkedList<>(mSections);
    }

    public Path getPath() {
        return new Path(mPath);
    }
}
