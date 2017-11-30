package com.jp.jpainter.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.io.Serializable;

/**
 *
 */
public class PathData implements Serializable {
    private Paint paint;
    private Path path;

    public PathData(Paint paint, Path path) {
        this.paint = paint;
        this.path = path;
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
