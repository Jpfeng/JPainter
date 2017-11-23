package com.jp.jpainter.utils;

import android.content.Context;
import android.widget.Toast;

/**
 *
 */

public class ToastUtil {
    private static Toast toast;

    public static void show(Context context, String content) {
        if (null == toast) {
            toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);

        } else {
            toast.setText(content);
        }

        toast.show();
    }

    public static void cancel() {
        if (null != toast) {
            toast.cancel();
        }
    }
}
