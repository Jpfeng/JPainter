package com.jp.jcanvas.entity;

import android.graphics.Canvas;

import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.brush.BrushImpl;

import java.io.Serializable;

/**
 *
 */
public class HistoryData {
    private BaseBrush mBrush;
    private Track mTrack;

    public HistoryData() {
        // TODO: interface
        this.mBrush = new BrushImpl();
        this.mTrack = new Track();
    }

    public HistoryData(BaseBrush brush, Track track) {
        this.mBrush = brush.cloneBrush();
        this.mTrack = new Track(track);
    }

    public HistoryData(HistoryData data) {
        this.mBrush = data.mBrush.cloneBrush();
        this.mTrack = new Track(data.mTrack);
    }

    public void draw(Canvas canvas) {
        mBrush.drawTrack(canvas, mTrack);
    }

    public Track getTrack() {
        return mTrack;
    }

    public BaseBrush getBrush() {
        return mBrush;
    }
}
