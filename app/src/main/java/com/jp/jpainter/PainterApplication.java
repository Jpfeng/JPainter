package com.jp.jpainter;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

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
        PainterCrashHandler crashHandler = PainterCrashHandler.getInstance();

        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        crashHandler.init();
    }
}
