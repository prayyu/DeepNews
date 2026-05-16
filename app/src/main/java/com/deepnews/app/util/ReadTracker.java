package com.deepnews.app.util;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ReadTracker {

    public static boolean isRead(SharedPreferences prefs, String url) {
        if (url == null || prefs == null) return false;
        Set<String> readUrls = prefs.getStringSet(PrefsKeys.READ_ARTICLES, new HashSet<>());
        return readUrls.contains(url);
    }

    public static void markAsRead(SharedPreferences prefs, String url) {
        if (url == null || prefs == null) return;
        Set<String> readUrls = new HashSet<>(prefs.getStringSet(PrefsKeys.READ_ARTICLES, new HashSet<>()));
        if (readUrls.add(url)) {
            prefs.edit().putStringSet(PrefsKeys.READ_ARTICLES, readUrls).apply();
        }
    }

    public static Set<String> getReadUrls(SharedPreferences prefs) {
        return new HashSet<>(prefs.getStringSet(PrefsKeys.READ_ARTICLES, new HashSet<>()));
    }
}
