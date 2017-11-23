package com.jp.jpainter.utils;

import android.util.Log;

/**
 * Logging util
 */
public class LogUtil {

    private static final boolean SHOW_LOG = true;

    private LogUtil() {
    }

    public static void v(String tag, String msg) {
        if (SHOW_LOG) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (SHOW_LOG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (SHOW_LOG) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (SHOW_LOG) {
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (SHOW_LOG) {
            Log.w(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (SHOW_LOG) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (SHOW_LOG) {
            Log.e(tag, msg, tr);
        }
    }
}
