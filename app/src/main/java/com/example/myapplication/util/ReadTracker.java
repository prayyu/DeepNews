package com.example.myapplication.util;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ReadTracker {

    private static final String PREF_KEY = "read_articles";

    public static boolean isRead(SharedPreferences prefs, String url) {
        if (url == null || prefs == null) return false;
        Set<String> readUrls = prefs.getStringSet(PREF_KEY, new HashSet<>());
        return readUrls.contains(url);
    }

    public static void markAsRead(SharedPreferences prefs, String url) {
        if (url == null || prefs == null) return;
        Set<String> readUrls = new HashSet<>(prefs.getStringSet(PREF_KEY, new HashSet<>()));
        if (readUrls.add(url)) {
            prefs.edit().putStringSet(PREF_KEY, readUrls).apply();
        }
    }

    public static Set<String> getReadUrls(SharedPreferences prefs) {
        return new HashSet<>(prefs.getStringSet(PREF_KEY, new HashSet<>()));
    }
}
