package com.example.myapplication.util;

import android.util.Log;

/**
 * 统一日志工具，自动添加 TAG 前缀。
 * 线上版本会自动关闭 DEBUG 级别日志。
 */
public class Logger {
    private static final String PREFIX = "DeepNews";
    private static final boolean DEBUG = true; // BuildConfig.DEBUG

    private final String tag;

    private Logger(String tag) {
        this.tag = PREFIX + "/" + tag;
    }

    public static Logger get(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }

    public void d(String message) {
        if (DEBUG) Log.d(tag, message);
    }

    public void i(String message) {
        Log.i(tag, message);
    }

    public void w(String message) {
        Log.w(tag, message);
    }

    public void e(String message) {
        Log.e(tag, message);
    }

    public void e(String message, Throwable tr) {
        Log.e(tag, message, tr);
    }
}
