package com.jp.jcanvas.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jp.jcanvas.R;

/**
 *
 */
class ColorPreview extends LinearLayout {

    @ColorInt
    private int mColorOld;
    @ColorInt
    private int mColorNew;

    private ImageView mIvOld;
    private ImageView mIvNew;

    public ColorPreview(Context context) {
        this(context, null);
    }

    public ColorPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public ColorPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_color_preview, this);
        mIvOld = view.findViewById(R.id.iv_color_old);
        mIvNew = view.findViewById(R.id.iv_color_new);

        mColorOld = Color.TRANSPARENT;
        mColorNew = Color.TRANSPARENT;

        ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
                return new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            }
        };

        PaintDrawable drawable = new PaintDrawable();
        drawable.setShape(new RectShape());
        drawable.setShaderFactory(sf);
        this.setBackgroundDrawable(drawable);

        ColorDrawable drawableOld = new ColorDrawable(mColorOld);
        mIvOld.setImageDrawable(drawableOld);

        ColorDrawable drawableNew = new ColorDrawable(mColorNew);
        mIvNew.setImageDrawable(drawableNew);
    }

    public void setOld(@ColorInt int color) {
        mColorOld = color;
        ColorDrawable drawable = new ColorDrawable(color);
        mIvOld.setImageDrawable(drawable);
    }

    public void setNew(@ColorInt int color) {
        mColorNew = color;
        ColorDrawable drawable = new ColorDrawable(color);
        mIvNew.setImageDrawable(drawable);
    }

    public void setColor(@ColorInt int color) {
        mColorOld = mColorNew;
        setOld(mColorOld);
        mColorNew = color;
        setNew(mColorNew);
    }
}
