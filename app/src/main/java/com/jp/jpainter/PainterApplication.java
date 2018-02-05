package com.jp.jpainter;

import android.app.Application;

import com.jp.jpainter.utils.PainterCrashHandler;

/**
 *
 */
public class PainterApplication extends Application {

    public static final String TAG = "JPainter";

    @Override
    public void onCreate() {
        super.onCreate();

        // 捕捉异常
        PainterCrashHandler.getInstance().init();
    }
}
