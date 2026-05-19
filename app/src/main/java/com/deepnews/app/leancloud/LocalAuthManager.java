package com.deepnews.app.leancloud;

import android.content.Context;
import android.content.SharedPreferences;

import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

import org.json.JSONObject;

import java.security.MessageDigest;

public class LocalAuthManager {

    private static final Logger log = Logger.get(LocalAuthManager.class);
    private static LocalAuthManager instance;
    private SharedPreferences prefs;
    private boolean initialized = false;

    public interface AuthCallback {
        void onSuccess(String username);
        void onError(String error);
    }

    private LocalAuthManager() {}

    public static synchronized LocalAuthManager getInstance() {
        if (instance == null) {
            instance = new LocalAuthManager();
        }
        return instance;
    }

    public void init(Context context) {
        prefs = context.getSharedPreferences(PrefsKeys.FILE, Context.MODE_PRIVATE);
        initialized = true;
        log.d("LocalAuthManager 初始化完成");
    }

    public boolean isAvailable() {
        return initialized;
    }

    public boolean isLoggedIn() {
        return initialized && !getCurrentUsername().isEmpty();
    }

    public String getCurrentUsername() {
        return prefs.getString(PrefsKeys.LOGGED_IN_USER, "");
    }

    public void register(String username, String password, AuthCallback callback) {
        try {
            JSONObject users = loadUsers();
            if (users.has(username)) {
                callback.onError("用户名已被注册");
                return;
            }
            users.put(username, hashPassword(password));
            saveUsers(users);
            prefs.edit().putString(PrefsKeys.LOGGED_IN_USER, username).apply();
            log.d("注册成功: " + username);
            callback.onSuccess(username);
        } catch (Exception e) {
            log.w("注册失败: " + e.getMessage());
            callback.onError("注册失败，请重试");
        }
    }

    public void login(String username, String password, AuthCallback callback) {
        try {
            JSONObject users = loadUsers();
            if (!users.has(username)) {
                callback.onError("用户不存在");
                return;
            }
            if (!users.getString(username).equals(hashPassword(password))) {
                callback.onError("用户名或密码错误");
                return;
            }
            prefs.edit().putString(PrefsKeys.LOGGED_IN_USER, username).apply();
            log.d("登录成功: " + username);
            callback.onSuccess(username);
        } catch (Exception e) {
            log.w("登录失败: " + e.getMessage());
            callback.onError("登录失败，请重试");
        }
    }

    public void logout() {
        String username = getCurrentUsername();
        prefs.edit().remove(PrefsKeys.LOGGED_IN_USER).apply();
        log.d("已登出: " + username);
    }

    private JSONObject loadUsers() throws Exception {
        String json = prefs.getString(PrefsKeys.USERS, "{}");
        return new JSONObject(json);
    }

    private void saveUsers(JSONObject users) {
        prefs.edit().putString(PrefsKeys.USERS, users.toString()).apply();
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
