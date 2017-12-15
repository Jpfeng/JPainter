package com.jp.jpainter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.jp.jcanvas.JCanvas;

public class PainterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        TextView tvUndo = findViewById(R.id.tv_undo);
        TextView tvRedo = findViewById(R.id.tv_redo);
        TextView tvScale = findViewById(R.id.tv_scale);
        tvScale.setText("1.0x");

        JCanvas painter = findViewById(R.id.sp_painter);

        tvUndo.setOnClickListener(v -> painter.undo());
        tvRedo.setOnClickListener(v -> painter.redo());

        painter.setOnScaleChangeListener(new JCanvas.OnScaleChangeListener() {
            @Override
            public void onScaleChangeStart(float startScale) {
                tvScale.setText(
                        String.valueOf((float) (Math.round(startScale * 10)) / 10).concat("x"));
            }

            @Override
            public void onScaleChange(float currentScale) {
                tvScale.setText(
                        String.valueOf((float) (Math.round(currentScale * 10)) / 10).concat("x"));
            }

            @Override
            public void onScaleChangeEnd(float endScale) {
            }
        });
    }
}
