package com.jp.jcanvas.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
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

    private ImageView mIvOld;
    private ImageView mIvNew;

    private ColorDrawable mDrawableOld;
    private ColorDrawable mDrawableNew;

    private OnColorClickListener mListener;

    public ColorPreview(Context context) {
        this(context, null);
    }

    public ColorPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.ColorPreviewStyle);
    }

    public ColorPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ColorPreview, defStyleAttr, 0);

        int colorOld = ta.getColor(R.styleable.ColorPreview_cp_oldColor, 0);
        int colorNew = ta.getColor(R.styleable.ColorPreview_cp_newColor, 0);

        mDrawableOld = new ColorDrawable(colorOld);
        mDrawableNew = new ColorDrawable(colorNew);

        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_color_preview, this);
        mIvOld = view.findViewById(R.id.iv_color_old);
        mIvNew = view.findViewById(R.id.iv_color_new);

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

        mIvOld.setImageDrawable(mDrawableOld);
        mIvNew.setImageDrawable(mDrawableNew);

        mIvOld.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onOldColorClicked(mDrawableOld.getColor());
            }
        });
        mIvNew.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onNewColorClicked(mDrawableNew.getColor());
            }
        });
    }

    public void setOld(@ColorInt int color) {
        ((ColorDrawable) mDrawableOld.mutate()).setColor(color);
        mIvOld.setImageDrawable(mDrawableOld);
    }

    public void setNew(@ColorInt int color) {
        ((ColorDrawable) mDrawableNew.mutate()).setColor(color);
        mIvNew.setImageDrawable(mDrawableNew);
    }

    public void setColor(@ColorInt int color) {
        setOld(mDrawableNew.getColor());
        setNew(color);
    }

    public void setOnColorClickListener(OnColorClickListener listener) {
        mListener = listener;
    }

    public interface OnColorClickListener {
        void onOldColorClicked(@ColorInt int oldColor);

        void onNewColorClicked(@ColorInt int newColor);
    }
}
