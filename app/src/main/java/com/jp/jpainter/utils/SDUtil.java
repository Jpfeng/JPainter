package com.jp.jpainter.utils;

import android.os.Environment;

import java.io.File;

/**
 *
 */
public class SDUtil {
    /**
     * sdcard
     */
    private static final String SD_ROOT = Environment.getExternalStorageDirectory().toString();

    /**
     * 根目录
     */
    private static final String APP_ROOT = SD_ROOT + "/JPainter/";

    /**
     * 日志目录
     */
    private static final String LOG_DIR = APP_ROOT + "log/";

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
}
