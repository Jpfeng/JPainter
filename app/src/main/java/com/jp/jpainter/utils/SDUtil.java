package com.jp.jpainter.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 */
public class SDUtil {
    /**
     * sdcard
     */
    private static final String SD_ROOT = Environment.getExternalStorageDirectory().toString();

    /**
     * app根目录
     */
    private static final String APP_ROOT = SD_ROOT + "/JPainter/";

    /**
     * 日志目录
     */
    private static final String LOG_DIR = APP_ROOT + "log/";

    /**
     * 图片根目录
     */
    private static final String PIC_ROOT =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

    /**
     * 图片目录
     */
    private static final String PIC_DIR = PIC_ROOT + "/JPainter/";

    private SDUtil() {
    }

    /**
     * 初始化日志目录
     *
     * @return 初始化情况
     */
    public static boolean initLogDir() {
        if (!sdExist()) {
            return false;
        }

        File logFile = new File(LOG_DIR);
        boolean exists = logFile.exists();
        boolean mkdirs = logFile.mkdirs();
        return exists || mkdirs;
    }

    /**
     * 初始化图片目录
     *
     * @return 初始化情况
     */
    public static boolean initBitmapDir() {
        if (!sdExist()) {
            return false;
        }

        File logFile = new File(PIC_DIR);
        boolean exists = logFile.exists();
        boolean mkdirs = logFile.mkdirs();
        return exists || mkdirs;
    }

    /**
     * 是否存在 SDCard
     *
     * @return 是否存在
     */
    public static boolean sdExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取日志目录
     *
     * @return 日志目录
     */
    public static File getLogDir() {
        return new File(LOG_DIR);
    }

    public static File getBitmapDir() {
        return new File(PIC_DIR);
    }

    public static boolean saveBitmap(String fileName, Bitmap bitmap) {
        File file = new File(getBitmapDir(), fileName);
        if (file.exists()) {
            return false;
        }

        FileOutputStream out;
        try {
            out = new FileOutputStream(file);

            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
