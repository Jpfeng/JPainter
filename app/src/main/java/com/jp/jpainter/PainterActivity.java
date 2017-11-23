package com.jp.jpainter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.jp.jpainter.core.SurfacePainter;

public class PainterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        FrameLayout painterContainer = findViewById(R.id.fl_painter_container);
        SurfacePainter painter = new SurfacePainter(this);
        painterContainer.addView(painter);
    }
}
