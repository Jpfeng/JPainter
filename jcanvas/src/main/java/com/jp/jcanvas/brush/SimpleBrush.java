package com.jp.jcanvas.brush;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.jp.jcanvas.entity.Track;

/**
 *
 */
public class SimpleBrush extends BaseBrush<SimpleBrush> {
    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void initBrush() {
    }

    @Override
    public SimpleBrush cloneBrush() {
        return new SimpleBrush();
    }

    @Override
    public void drawTrack(Canvas canvas, Track track) {
    }

    @Override
    public void drawPreview(Canvas canvas, Track track) {
    }
}
