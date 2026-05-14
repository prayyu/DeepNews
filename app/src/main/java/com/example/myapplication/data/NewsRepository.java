package com.example.myapplication.data;

import android.content.Context;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.api.EventCluster;
import com.example.myapplication.api.HotListClient;
import com.example.myapplication.api.LlmClient;
import com.example.myapplication.api.NewsArticle;
import com.example.myapplication.data.entity.ArticleEntity;
import com.example.myapplication.data.entity.EventEntity;
import com.example.myapplication.util.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据仓库，统一管理网络数据获取、LLM 聚类和本地缓存。
 * 支持多平台热榜数据源切换。
 */
public class NewsRepository {

    private static final Logger log = Logger.get(NewsRepository.class);
    private static NewsRepository instance;

    private final AppDatabase database;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<String>>() {}.getType();

    public interface OnResult {
        void onSuccess(List<EventCluster> clusters, List<NewsArticle> articles);
        void onError(String error);
    }

    public interface OnCacheResult {
        void onCacheLoaded(List<EventCluster> clusters);
    }

    public interface OnSearchResult {
        void onResult(List<EventCluster> clusters);
    }

    private NewsRepository(AppDatabase database) {
        this.database = database;
    }

    public static synchronized NewsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NewsRepository(AppDatabase.getInstance(context.getApplicationContext()));
            log.d("Repository 初始化完成");
        }
        return instance;
    }

    // ==================== 缓存 ====================

    public void loadCache(OnCacheResult callback) {
        new Thread(() -> {
            List<EventEntity> cached = database.eventDao().getAllEvents();
            if (!cached.isEmpty()) {
                log.d("从缓存加载了 " + cached.size() + " 个事件");
                List<EventCluster> clusters = new ArrayList<>();
                for (EventEntity e : cached) {
                    EventCluster cluster = new EventCluster();
                    cluster.title = e.title;
                    cluster.summary = e.summary;
                    cluster.articleUrls = gson.fromJson(e.articleUrls, listType);
                    cluster.articleTitles = gson.fromJson(e.articleTitles, listType);
                    cluster.sources = gson.fromJson(e.sources, listType);
                    clusters.add(cluster);
                }
                callback.onCacheLoaded(clusters);
            } else {
                log.d("缓存为空");
                callback.onCacheLoaded(new ArrayList<>());
            }
        }).start();
    }

    // ==================== 搜索 ====================

    /** 在缓存的文章中搜索关键词 */
    public void searchArticles(String query, OnSearchResult callback) {
        new Thread(() -> {
            List<ArticleEntity> matches = database.newsDao().searchArticles("%" + query + "%");
            if (matches.isEmpty()) {
                callback.onResult(new ArrayList<>());
                return;
            }
            Map<String, EventCluster> clusterMap = new LinkedHashMap<>();
            for (ArticleEntity a : matches) {
                String key = a.eventTitle != null ? a.eventTitle : "搜索结果";
                EventCluster cluster = clusterMap.get(key);
                if (cluster == null) {
                    cluster = new EventCluster();
                    cluster.title = key;
                    cluster.summary = a.title;
                    cluster.sources = new ArrayList<>();
                    cluster.articleUrls = new ArrayList<>();
                    cluster.articleTitles = new ArrayList<>();
                    clusterMap.put(key, cluster);
                }
                if (a.url != null) cluster.articleUrls.add(a.url);
                if (a.title != null) cluster.articleTitles.add(a.title);
                if (a.source != null && !cluster.sources.contains(a.source)) cluster.sources.add(a.source);
            }
            callback.onResult(new ArrayList<>(clusterMap.values()));
        }).start();
    }

    // ==================== 网络获取 + 聚类 + 缓存 ====================

    public void fetchNews(String source, OnResult callback) {
        String llmKey = BuildConfig.LLM_API_KEY;
        boolean hasLlmKey = llmKey != null && !llmKey.isEmpty() && !llmKey.contains("YOUR_");

        HotListClient.fetchHotList(source, new HotListClient.HotListCallback() {
            @Override
            public void onSuccess(List<NewsArticle> articles) {
                log.d("热榜获取成功，来源: " + source + "，共 " + articles.size() + " 条");
                if (articles.isEmpty()) {
                    callback.onError("热榜暂无数据");
                    return;
                }

                if (!hasLlmKey) {
                    log.d("无 LLM Key，使用降级分组");
                    List<EventCluster> fallback = buildFallbackClusters(articles);
                    attachArticleTitles(fallback, articles);
                    saveToCache(articles, fallback);
                    callback.onSuccess(fallback, articles);
                    return;
                }

                LlmClient.clusterArticles(articles, llmKey, new LlmClient.LlmCallback() {
                    @Override
                    public void onSuccess(List<EventCluster> clusters) {
                        log.d("LLM 聚类完成，共 " + clusters.size() + " 个事件");
                        attachArticleTitles(clusters, articles);
                        saveToCache(articles, clusters);
                        callback.onSuccess(clusters, articles);
                    }

                    @Override
                    public void onError(String error) {
                        log.w("LLM 聚类失败，降级: " + error);
                        List<EventCluster> fallback = buildFallbackClusters(articles);
                        attachArticleTitles(fallback, articles);
                        saveToCache(articles, fallback);
                        callback.onError("AI 聚类失败，已使用默认分组: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                log.e("热榜获取失败: " + error);
                callback.onError(error);
            }
        });
    }

    // ==================== 降级分组 ====================

    private List<EventCluster> buildFallbackClusters(List<NewsArticle> articles) {
        Map<String, EventCluster> map = new LinkedHashMap<>();
        for (NewsArticle a : articles) {
            String sourceName = a.source != null ? a.source.name : "未知来源";
            EventCluster cluster = map.get(sourceName);
            if (cluster == null) {
                cluster = new EventCluster();
                cluster.title = sourceName + " 最新报道";
                cluster.summary = a.title != null ? a.title : "";
                cluster.articleUrls = new ArrayList<>();
                cluster.articleTitles = new ArrayList<>();
                cluster.sources = new ArrayList<>();
                map.put(sourceName, cluster);
            }
            if (a.url != null) cluster.articleUrls.add(a.url);
            if (a.title != null) cluster.articleTitles.add(a.title);
            if (!cluster.sources.contains(sourceName)) cluster.sources.add(sourceName);
        }
        return new ArrayList<>(map.values());
    }

    private void attachArticleTitles(List<EventCluster> clusters, List<NewsArticle> articles) {
        Map<String, String> urlToTitle = new HashMap<>();
        for (NewsArticle a : articles) {
            if (a.url != null && a.title != null) {
                urlToTitle.put(a.url, a.title);
            }
        }
        for (EventCluster c : clusters) {
            if (c.articleTitles == null) c.articleTitles = new ArrayList<>();
            if (c.articleUrls != null) {
                for (String url : c.articleUrls) {
                    String title = urlToTitle.get(url);
                    c.articleTitles.add(title != null ? title : "查看原文");
                }
            }
        }
    }

    // ==================== 缓存写入 ====================

    private void saveToCache(List<NewsArticle> articles, List<EventCluster> clusters) {
        new Thread(() -> {
            database.eventDao().clearAll();
            database.newsDao().clearAll();
            log.d("已清除旧缓存");

            List<EventEntity> eventEntities = new ArrayList<>();
            for (EventCluster c : clusters) {
                EventEntity e = new EventEntity();
                e.title = c.title;
                e.summary = c.summary;
                e.articleUrls = gson.toJson(c.articleUrls);
                e.articleTitles = gson.toJson(c.articleTitles);
                e.sources = gson.toJson(c.sources);
                e.timestamp = System.currentTimeMillis();
                eventEntities.add(e);
            }
            database.eventDao().insertAll(eventEntities);
            log.d("写入 " + eventEntities.size() + " 个事件到缓存");

            for (NewsArticle a : articles) {
                ArticleEntity ae = new ArticleEntity();
                ae.url = a.url;
                ae.title = a.title;
                ae.source = a.source != null ? a.source.name : "";
                ae.description = a.description;
                ae.urlToImage = a.urlToImage;
                ae.publishedAt = a.publishedAt;
                for (EventCluster c : clusters) {
                    if (c.articleUrls != null && c.articleUrls.contains(a.url)) {
                        ae.eventTitle = c.title;
                        break;
                    }
                }
                database.newsDao().insertAll(List.of(ae));
            }
            log.d("写入 " + articles.size() + " 篇文章到缓存");
        }).start();
    }
}
