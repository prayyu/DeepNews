package com.deepnews.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

/**
 * 全局 Application，负责：
 * 1. 启动时恢复深色模式设置
 */
public class DeepNewsApp extends Application {

    private static final Logger log = Logger.get(DeepNewsApp.class);

    @Override
    public void onCreate() {
        super.onCreate();

        // 恢复深色模式设置
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(PrefsKeys.DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        log.d("深色模式: " + (isDark ? "开启" : "关闭"));
    }
}
