package com.deepnews.app.util;

/** SharedPreferences 键名集中管理 */
public final class PrefsKeys {

    /** SharedPreferences 文件名 */
    public static final String FILE = "deepnews_prefs";

    /** 深色模式开关 (boolean) */
    public static final String DARK_MODE = "dark_mode";

    /** 订阅关键词 (Set<String>) */
    public static final String KEYWORDS = "keywords";

    /** 已读文章 URL 集合 (Set<String>) */
    public static final String READ_ARTICLES = "read_articles";

    /** 缓存简报内容 (String) */
    public static final String DAILY_BRIEF = "daily_brief";

    /** 简报日期 (String) */
    public static final String BRIEF_DATE = "brief_date";

    /** 注册用户 JSON (String): { "username": "sha256hash", ... } */
    public static final String USERS = "users";

    /** 当前登录用户 (String): 空串表示未登录 */
    public static final String LOGGED_IN_USER = "logged_in_user";

    private PrefsKeys() {}
}
