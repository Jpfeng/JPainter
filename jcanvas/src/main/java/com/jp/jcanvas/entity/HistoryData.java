package com.jp.jcanvas.entity;

import android.graphics.Canvas;

import com.jp.jcanvas.brush.BaseBrush;

/**
 *
 */
public class HistoryData {
    private BaseBrush mBrush;
    private Track mTrack;

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
}
