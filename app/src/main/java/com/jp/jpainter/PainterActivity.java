package com.jp.jpainter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jp.jcanvas.JCanvas;
import com.jp.jpainter.utils.SDUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PainterActivity extends AppCompatActivity {

    private int mPaintWidth;
    @ColorInt
    private int mPaintColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        TextView tvUndo = findViewById(R.id.tv_undo);
        TextView tvRedo = findViewById(R.id.tv_redo);
        TextView tvScale = findViewById(R.id.tv_scale);
        TextView tvColor = findViewById(R.id.tv_color);
        TextView tvWidth = findViewById(R.id.tv_width);
        TextView tvPaint = findViewById(R.id.tv_paint);
        TextView tvEraser = findViewById(R.id.tv_eraser);
        TextView tvSave = findViewById(R.id.tv_save);
        JCanvas painter = findViewById(R.id.sp_painter);
        tvScale.setText("1.0x");

        tvPaint.setBackgroundColor(Color.CYAN);

        tvUndo.setOnClickListener(v -> painter.undo());
        tvRedo.setOnClickListener(v -> painter.redo());

        tvColor.setOnClickListener(v -> {
            TextView tvRed = new TextView(getApplicationContext());
            tvRed.setText("  红  ");
            tvRed.setTextSize(18.0f);
            tvRed.setOnClickListener(view -> mPaintColor = 0x88FF0000);
            TextView tvGreen = new TextView(getApplicationContext());
            tvGreen.setText("  绿  ");
            tvGreen.setTextSize(18.0f);
            tvGreen.setOnClickListener(view -> mPaintColor = 0x8800FF00);
            TextView tvBlue = new TextView(getApplicationContext());
            tvBlue.setText("  蓝  ");
            tvBlue.setTextSize(18.0f);
            tvBlue.setOnClickListener(view -> mPaintColor = 0x880000FF);
            TextView tvYellow = new TextView(getApplicationContext());
            tvYellow.setText("  黄  ");
            tvYellow.setTextSize(18.0f);
            tvYellow.setOnClickListener(view -> mPaintColor = 0x88FFFF00);
            TextView tvGray = new TextView(getApplicationContext());
            tvGray.setText("  灰  ");
            tvGray.setTextSize(18.0f);
            tvGray.setOnClickListener(view -> mPaintColor = 0x88888888);
            TextView tvBlack = new TextView(getApplicationContext());
            tvBlack.setText("  黑  ");
            tvBlack.setTextSize(18.0f);
            tvBlack.setOnClickListener(view -> mPaintColor = 0x88000000);

            LinearLayout ll = new LinearLayout(getApplicationContext());
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.addView(tvRed);
            ll.addView(tvGreen);
            ll.addView(tvBlue);
            ll.addView(tvYellow);
            ll.addView(tvGray);
            ll.addView(tvBlack);

            new AlertDialog.Builder(this)
                    .setTitle("设置画笔颜色")
                    .setView(ll)
                    .setPositiveButton("ok", (dialog, which) -> {
                        painter.setPaintColor(mPaintColor);
                    })
                    .setNegativeButton("no", null)
                    .create()
                    .show();
        });

        tvWidth.setOnClickListener(v -> {
            SeekBar seekBar = new SeekBar(getApplicationContext());
            seekBar.setMax(99);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mPaintWidth = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            seekBar.setProgress((int) painter.getPaintWidth());

            new AlertDialog.Builder(this)
                    .setTitle("设置笔尖大小")
                    .setView(seekBar)
                    .setPositiveButton("ok", (dialog, which) -> {
                        painter.setPaintWidth(mPaintWidth);
                    })
                    .setNegativeButton("no", null)
                    .create()
                    .show();
        });

        tvPaint.setOnClickListener(v -> {
            painter.usePaint();
            tvPaint.setBackgroundColor(Color.CYAN);
            tvEraser.setBackgroundColor(Color.parseColor("#FFAAAAAA"));
        });

        tvEraser.setOnClickListener(v -> {
            painter.useEraser();
            tvEraser.setBackgroundColor(Color.CYAN);
            tvPaint.setBackgroundColor(Color.parseColor("#FFAAAAAA"));
        });

        tvSave.setOnClickListener(v -> {
            if (!SDUtil.initBitmapDir()) {
                new AlertDialog.Builder(this)
                        .setTitle("保存失败0")
                        .setPositiveButton("ok", null)
                        .show();
                return;
            }

            DateFormat formatter
                    = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
            String fileName = formatter.format(new Date()) + ".png";

            if (SDUtil.saveBitmap(fileName, painter.getBitmap())) {
                new AlertDialog.Builder(this)
                        .setTitle("保存成功")
                        .setPositiveButton("new", (dialog, which) -> {
                            painter.resetCanvas();
                        })
                        .setNegativeButton("ok", null)
                        .show();

            } else {
                new AlertDialog.Builder(this)
                        .setTitle("保存失败1")
                        .setPositiveButton("ok", null)
                        .show();
            }
        });

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
                tvScale.setText(
                        String.valueOf((float) (Math.round(endScale * 10)) / 10).concat("x"));
            }
        });

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (PackageManager.PERMISSION_DENIED == permission) {
            String[] req = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, req, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
