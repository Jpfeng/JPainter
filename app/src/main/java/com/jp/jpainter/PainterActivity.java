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
import android.view.View;
import android.widget.TextView;

import com.jp.jcanvas.CanvasInterface;
import com.jp.jcanvas.JCanvas;
import com.jp.jcanvas.brush.BaseBrush;
import com.jp.jcanvas.brushselector.BrushSelector;
import com.jp.jcanvas.colorpicker.ColorPicker;
import com.jp.jpainter.brush.BrushTag01;
import com.jp.jpainter.brush.EraserTag01;
import com.jp.jpainter.utils.SDUtil;
import com.jp.jpainter.widgets.ToolDrawer;
import com.jp.jpainter.widgets.ToolMenu;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PainterActivity extends AppCompatActivity {

    private BaseBrush mBrush;

    @ColorInt
    private int mColor;
    private int mPaintWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        ToolDrawer cPicker = findViewById(R.id.cv_color_picker);
        JCanvas painter = findViewById(R.id.sp_painter);
        ToolMenu menu = findViewById(R.id.tm_tool_menu);
        TextView tvScale = findViewById(R.id.tv_scale);

        ColorPicker cp = new ColorPicker(getApplicationContext());
        BrushSelector bs = new BrushSelector(getApplicationContext());

        menu.setToolMenuListener(new ToolMenu.ToolMenuListener() {
            @Override
            public void onMenuOpened(ToolMenu view) {
                painter.stopInteract(true);
            }

            @Override
            public void onMenuClosed(ToolMenu view) {
                painter.stopInteract(false);
            }
        });

        menu.setOnToolClickListener(new ToolMenu.OnToolClickListener() {
            @Override
            public void onColorClicked() {
                cp.setColor(mColor);
                cPicker.open(cp);
            }

            @Override
            public void onBrushClicked() {
                cPicker.open(bs);
            }

            @Override
            public void onGalleryClicked() {

            }

            @Override
            public void onSaveClicked() {
                if (!SDUtil.initBitmapDir()) {
                    new AlertDialog.Builder(PainterActivity.this)
                            .setTitle("保存失败0")
                            .setPositiveButton("ok", null)
                            .show();
                    return;
                }

                DateFormat formatter
                        = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
                String fileName = formatter.format(new Date()) + ".png";

                if (SDUtil.saveBitmap(fileName, painter.getBitmap())) {
                    new AlertDialog.Builder(PainterActivity.this)
                            .setTitle("保存成功")
                            .setPositiveButton("new", (dialog, which) -> {
                                painter.resetCanvas();
                            })
                            .setNegativeButton("ok", null)
                            .show();

                } else {
                    new AlertDialog.Builder(PainterActivity.this)
                            .setTitle("保存失败1")
                            .setPositiveButton("ok", null)
                            .show();
                }
            }

            @Override
            public void onClearClicked() {
                new AlertDialog.Builder(PainterActivity.this)
                        .setTitle("清空？")
                        .setPositiveButton("ok", (dialog, which) -> {
                            painter.resetCanvas();
                        })
                        .setNegativeButton("no", null)
                        .show();
            }

            @Override
            public void onSettingsClicked() {

            }

            @Override
            public void onImportClicked() {

            }

            @Override
            public void onLayerClicked() {

            }

            @Override
            public void onUndoClicked() {
                painter.undo();
            }
        });

        cPicker.setToolDrawerListener(new ToolDrawer.ToolDrawerListener() {
            @Override
            public void onDrawerSlide(View toolDrawer, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View toolDrawer) {
                painter.stopInteract(true);
            }

            @Override
            public void onDrawerClosed(View toolDrawer) {
                painter.stopInteract(false);
            }
        });

        mBrush = new BrushTag01();
        mPaintWidth = 16;
        mBrush.setWidth(mPaintWidth);

        tvScale.setText("1.0x");
        cp.setColor(Color.RED);
        cp.setOnConfirmListener(view -> {
            mColor = cp.getColor();
            mBrush.setColor(mColor);
            cPicker.close();
            painter.stopInteract(false);
        });
        mBrush.setColor(cp.getColor());

        painter.setBrush(mBrush);

        painter.setOnScaleChangeListener(new CanvasInterface.OnScaleChangeListener() {
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
