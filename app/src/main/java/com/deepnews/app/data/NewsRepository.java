package com.deepnews.app.data;

import com.deepnews.app.BuildConfig;
import com.deepnews.app.api.EventCluster;
import com.deepnews.app.api.HotListClient;
import com.deepnews.app.api.LlmClient;
import com.deepnews.app.api.NewsApi;
import com.deepnews.app.api.NewsArticle;
import com.deepnews.app.api.NewsResponse;
import com.deepnews.app.api.RetrofitClient;
import com.deepnews.app.data.entity.ArticleEntity;
import com.deepnews.app.data.entity.EventEntity;
import com.deepnews.app.util.AppExecutors;
import com.deepnews.app.util.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据仓库，统一管理网络数据获取、LLM 聚类和本地缓存。
 * 支持多平台热榜数据源切换。
 */
public class NewsRepository {

    private static final Logger log = Logger.get(NewsRepository.class);

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

    public NewsRepository(AppDatabase database) {
        this.database = database;
        log.d("Repository 初始化完成");
    }

    // ==================== 缓存 ====================

    public void loadCache(OnCacheResult callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
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
        });
    }

    // ==================== 搜索 ====================

    /** 在缓存的文章中搜索关键词 */
    public void searchArticles(String query, OnSearchResult callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
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
        });
    }

    /** 搜索（本地缓存 + 在线补充），本地结果不足 3 个时触发在线搜索 */
    public void searchOnline(String query, OnSearchResult callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // 1. 搜索本地缓存
            List<ArticleEntity> localMatches = database.newsDao().searchArticles("%" + query + "%");
            Map<String, EventCluster> clusterMap = new LinkedHashMap<>();
            for (ArticleEntity a : localMatches) {
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

            int localCount = clusterMap.size();
            if (localCount >= 3) {
                callback.onResult(new ArrayList<>(clusterMap.values()));
                return;
            }

            // 2. 本地结果不足，触发在线搜索
            log.d("本地搜索结果不足，触发在线搜索: " + query);
            final Object lock = new Object();
            final List<NewsArticle>[] onlineArticles = new List[1];
            final boolean[] onlineDone = {false};

            HotListClient.searchOnline(query, new HotListClient.HotListCallback() {
                @Override
                public void onSuccess(List<NewsArticle> articles) {
                    synchronized (lock) {
                        onlineArticles[0] = articles;
                        onlineDone[0] = true;
                        lock.notify();
                    }
                }

                @Override
                public void onError(String error) {
                    log.w("在线搜索失败: " + error);
                    synchronized (lock) {
                        onlineDone[0] = true;
                        lock.notify();
                    }
                }
            });

            synchronized (lock) {
                try {
                    if (!onlineDone[0]) lock.wait(30000);
                } catch (InterruptedException ignored) {}
            }

            // 3. 合并结果（按 URL 去重）
            if (onlineArticles[0] != null && !onlineArticles[0].isEmpty()) {
                Set<String> existingUrls = new HashSet<>();
                for (EventCluster c : clusterMap.values()) {
                    if (c.articleUrls != null) existingUrls.addAll(c.articleUrls);
                }

                for (NewsArticle a : onlineArticles[0]) {
                    if (a.url != null && existingUrls.contains(a.url)) continue;
                    String sourceName = a.source != null ? a.source.name : "在线搜索";
                    EventCluster cluster = clusterMap.get(sourceName);
                    if (cluster == null) {
                        cluster = new EventCluster();
                        cluster.title = sourceName + " 搜索结果";
                        cluster.summary = a.title;
                        cluster.articleUrls = new ArrayList<>();
                        cluster.articleTitles = new ArrayList<>();
                        cluster.sources = new ArrayList<>();
                        clusterMap.put(sourceName, cluster);
                    }
                    if (a.url != null && !cluster.articleUrls.contains(a.url)) {
                        cluster.articleUrls.add(a.url);
                        if (a.url != null) existingUrls.add(a.url);
                    }
                    if (a.title != null && !cluster.articleTitles.contains(a.title)) {
                        cluster.articleTitles.add(a.title);
                    }
                    if (sourceName != null && !cluster.sources.contains(sourceName)) {
                        cluster.sources.add(sourceName);
                    }
                }
            }

            callback.onResult(new ArrayList<>(clusterMap.values()));
        });
    }

    // ==================== 网络获取 + 聚类 + 缓存 ====================

    public void fetchNews(String source, OnResult callback) {
        if ("all".equals(source)) {
            fetchWithNewsApi(callback);
            return;
        }

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
                processAndCallback(articles, hasLlmKey, llmKey, callback);
            }

            @Override
            public void onError(String error) {
                log.e("热榜获取失败: " + error);
                callback.onError(error);
            }
        });
    }

    /** 综合来源：并行抓取热榜（3平台）+ NewsAPI 通用新闻 */
    private void fetchWithNewsApi(OnResult callback) {
        String llmKey = BuildConfig.LLM_API_KEY;
        boolean hasLlmKey = llmKey != null && !llmKey.isEmpty() && !llmKey.contains("YOUR_");
        boolean hasNewsKey = RetrofitClient.isNewsApiKeyValid();

        List<NewsArticle> allArticles = Collections.synchronizedList(new ArrayList<>());
        int[] remaining = {hasNewsKey ? 2 : 1};
        StringBuilder errorMsg = new StringBuilder();

        Runnable onAllDone = () -> {
            List<NewsArticle> deduped = deduplicate(allArticles);
            log.d("综合-合并后共 " + deduped.size() + " 条（去重前 " + allArticles.size() + "）");
            if (deduped.isEmpty()) {
                String msg = errorMsg.length() > 0 ? errorMsg.toString().trim() : "全部来源无数据";
                callback.onError(msg);
                return;
            }
            processAndCallback(deduped, hasLlmKey, llmKey, callback);
        };

        // 1) 抓取热榜（3平台合并）
        HotListClient.fetchHotList("all", new HotListClient.HotListCallback() {
            @Override
            public void onSuccess(List<NewsArticle> articles) {
                log.d("综合-热榜获取成功: " + articles.size() + " 条");
                allArticles.addAll(articles);
                checkDone();
            }

            @Override
            public void onError(String error) {
                log.w("综合-热榜失败: " + error);
                synchronized (errorMsg) {
                    errorMsg.append("热榜: ").append(error).append(" ");
                }
                checkDone();
            }

            private void checkDone() {
                synchronized (remaining) {
                    remaining[0]--;
                    if (remaining[0] == 0) onAllDone.run();
                }
            }
        });

        // 2) 抓取 NewsAPI 通用新闻
        if (hasNewsKey) {
            AppExecutors.getInstance().networkIO().execute(() -> {
                try {
                    NewsApi api = RetrofitClient.getNewsApi();
                    String apiKey = RetrofitClient.getNewsApiKey();
                    retrofit2.Response<NewsResponse> response =
                            api.getTopHeadlines("us", 50, apiKey).execute();
                    if (response.isSuccessful() && response.body() != null
                            && response.body().articles != null) {
                        for (NewsArticle a : response.body().articles) {
                            if (a.source == null) a.source = new NewsArticle.Source();
                            if (a.source.name == null) a.source.name = "NewsAPI";
                            a.source.id = "newsapi";
                        }
                        allArticles.addAll(response.body().articles);
                        log.d("综合-NewsAPI 获取成功: " + response.body().articles.size() + " 条");
                    } else {
                        log.w("综合-NewsAPI 失败: HTTP " + response.code());
                        synchronized (errorMsg) {
                            errorMsg.append("NewsAPI: HTTP ").append(response.code()).append(" ");
                        }
                    }
                } catch (Exception e) {
                    log.w("综合-NewsAPI 异常: " + e.getMessage());
                    synchronized (errorMsg) {
                        errorMsg.append("NewsAPI: ").append(e.getMessage()).append(" ");
                    }
                }
                synchronized (remaining) {
                    remaining[0]--;
                    if (remaining[0] == 0) onAllDone.run();
                }
            });
        }
    }

    /** 聚类 / 降级 + 缓存 + 回调 */
    private void processAndCallback(List<NewsArticle> articles, boolean hasLlmKey,
                                    String llmKey, OnResult callback) {
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

    /** 按 URL 去重 */
    private static List<NewsArticle> deduplicate(List<NewsArticle> articles) {
        LinkedHashMap<String, NewsArticle> map = new LinkedHashMap<>();
        for (NewsArticle a : articles) {
            if (a.url != null && !a.url.isEmpty()) {
                if (!map.containsKey(a.url)) {
                    map.put(a.url, a);
                }
            } else {
                // 无 URL 的条目直接保留（极少情况）
                map.put(System.identityHashCode(a) + "", a);
            }
        }
        return new ArrayList<>(map.values());
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
        AppExecutors.getInstance().diskIO().execute(() -> {
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
        });
    }
}
