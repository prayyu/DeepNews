package com.deepnews.app.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.deepnews.app.data.AppDatabase;
import com.deepnews.app.data.NewsDao;
import com.deepnews.app.data.entity.ArticleEntity;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsNotificationWorker extends Worker {

    private static final Logger log = Logger.get(NewsNotificationWorker.class);

    public NewsNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PrefsKeys.FILE, Context.MODE_PRIVATE);

        Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, new HashSet<>());
        if (keywords == null || keywords.isEmpty()) {
            log.d("无订阅关键词，跳过通知检查");
            return Result.success();
        }

        AppDatabase db = AppDatabase.getInstance(context);
        NewsDao newsDao = db.newsDao();
        List<ArticleEntity> articles = newsDao.getAllArticles();

        if (articles.isEmpty()) {
            log.d("无缓存文章，跳过通知检查");
            return Result.success();
        }

        List<String> matchedTitles = new ArrayList<>();
        for (ArticleEntity article : articles) {
            if (article.title == null) continue;
            String lowerTitle = article.title.toLowerCase();
            for (String keyword : keywords) {
                if (lowerTitle.contains(keyword.toLowerCase())) {
                    matchedTitles.add(article.title);
                    break;
                }
            }
        }

        if (!matchedTitles.isEmpty()) {
            log.d("关键词匹配到 " + matchedTitles.size() + " 条新闻");
            NotificationHelper.showKeywordMatchNotification(
                    context, matchedTitles, keywords.size());
        } else {
            log.d("本轮未匹配到关键词新闻");
        }

        return Result.success();
    }
}
