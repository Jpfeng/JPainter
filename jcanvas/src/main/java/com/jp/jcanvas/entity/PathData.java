package com.jp.jcanvas.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 *
 */
public class PathData {
    private Paint paint;
    private Path path;

    public PathData() {
        this.paint = new Paint();
        this.path = new Path();
    }

    public PathData(Paint paint, Path path) {
        this.paint = new Paint(paint);
        this.path = new Path(path);
    }

    public PathData(PathData data) {
        this.paint = new Paint(data.getPaint());
        this.path = new Path(data.getPath());
    }

    public void draw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    public Path getPath() {
        return path;
    }

    public Paint getPaint() {
        return paint;
    }
}
