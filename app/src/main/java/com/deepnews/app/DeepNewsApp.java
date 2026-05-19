package com.deepnews.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.deepnews.app.leancloud.LocalAuthManager;
import com.deepnews.app.notification.NewsNotificationWorker;
import com.deepnews.app.notification.NotificationHelper;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 全局 Application，负责：
 * 1. 启动时恢复深色模式设置
 * 2. 初始化本地认证
 * 3. 定时检查关键词推送
 */
@HiltAndroidApp
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

        // 初始化本地认证
        LocalAuthManager.getInstance().init(this);

        // 创建通知渠道
        NotificationHelper.createChannel(this);

        // 定时检查关键词推送（每小时）
        scheduleKeywordNotification();
    }

    private void scheduleKeywordNotification() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NewsNotificationWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("news_keyword_check",
                        ExistingPeriodicWorkPolicy.KEEP, request);

        log.d("关键词推送定时任务已调度（每小时）");
    }
}
