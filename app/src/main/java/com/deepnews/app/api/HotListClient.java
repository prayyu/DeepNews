package com.deepnews.app.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.deepnews.app.util.AppExecutors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 多平台热榜客户端，支持切换数据源。
 * 通过 uapis.cn 聚合 API 获取不同平台的热榜数据。
 */
public class HotListClient {

    private static final String BASE_URL = "https://uapis.cn/api/v1/misc/hotboard?type=";
    private static final Gson gson = new Gson();
    private static OkHttpClient client;

    public static final String SOURCE_TOUTIAO = "toutiao";
    public static final String SOURCE_WEIBO = "weibo";
    public static final String SOURCE_BAIDU = "baidu";
    public static final String SOURCE_DOUYIN = "douyin";

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public interface HotListCallback {
        void onSuccess(List<NewsArticle> articles);
        void onError(String error);
    }

    /** 获取指定平台的热榜 */
    public static void fetchHotList(String source, HotListCallback callback) {
        if ("all".equals(source)) {
            fetchAllSources(callback);
            return;
        }

        String url = BASE_URL + source;
        String sourceName = getSourceDisplayName(source);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) DeepNews/1.0")
                .header("Accept", "application/json")
                .build();

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = getClient().newCall(request).execute();
                if (!response.isSuccessful()) {
                    callback.onError(sourceName + "请求失败 (HTTP " + response.code() + ")");
                    return;
                }
                String respBody = response.body() != null ? response.body().string() : "{}";
                List<NewsArticle> articles = parseResponse(respBody, sourceName, source);
                if (articles.isEmpty()) {
                    callback.onError(sourceName + "数据为空");
                    return;
                }
                callback.onSuccess(articles);
            } catch (IOException e) {
                callback.onError(sourceName + "请求失败: " + e.getLocalizedMessage());
            }
        });
    }

    /** 同时获取全部平台的热榜并合并 */
    private static void fetchAllSources(HotListCallback callback) {
        String[] sources = {SOURCE_TOUTIAO, SOURCE_WEIBO, SOURCE_BAIDU, SOURCE_DOUYIN};
        String[] names = {"今日头条", "微博热搜", "百度热榜", "抖音热榜"};
        List<NewsArticle> allArticles = Collections.synchronizedList(new ArrayList<>());
        int[] remaining = {sources.length};
        StringBuilder errorMsg = new StringBuilder();

        for (int i = 0; i < sources.length; i++) {
            final int idx = i;
            String url = BASE_URL + sources[idx];

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) DeepNews/1.0")
                    .header("Accept", "application/json")
                    .build();

            AppExecutors.getInstance().networkIO().execute(() -> {
                try {
                    Response response = getClient().newCall(request).execute();
                    if (response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "{}";
                        List<NewsArticle> articles = parseResponse(respBody, names[idx], sources[idx]);
                        allArticles.addAll(articles);
                    } else {
                        synchronized (errorMsg) {
                            errorMsg.append(names[idx]).append("失败 ");
                        }
                    }
                } catch (IOException e) {
                    synchronized (errorMsg) {
                        errorMsg.append(names[idx]).append("失败 ");
                    }
                }

                synchronized (remaining) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        if (allArticles.isEmpty()) {
                            callback.onError(errorMsg.length() > 0 ? errorMsg.toString().trim() : "全部来源无数据");
                        } else {
                            callback.onSuccess(new ArrayList<>(allArticles));
                        }
                    }
                }
            });
        }
    }

    /** 在线搜索：获取所有平台数据并在本地按标题过滤 */
    public static void searchOnline(String query, HotListCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("搜索关键词为空");
            return;
        }
        String finalQuery = query.trim().toLowerCase();
        String[] sources = {SOURCE_TOUTIAO, SOURCE_WEIBO, SOURCE_BAIDU, SOURCE_DOUYIN};
        String[] names = {"今日头条", "微博热搜", "百度热榜", "抖音热榜"};
        List<NewsArticle> allArticles = Collections.synchronizedList(new ArrayList<>());
        int[] remaining = {sources.length};
        StringBuilder errorMsg = new StringBuilder();

        for (int i = 0; i < sources.length; i++) {
            final int idx = i;
            String url = BASE_URL + sources[idx];

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) DeepNews/1.0")
                    .header("Accept", "application/json")
                    .build();

            AppExecutors.getInstance().networkIO().execute(() -> {
                try {
                    Response response = getClient().newCall(request).execute();
                    if (response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "{}";
                        List<NewsArticle> articles = parseResponse(respBody, names[idx], sources[idx]);
                        for (NewsArticle a : articles) {
                            if (a.title != null && a.title.toLowerCase().contains(finalQuery)) {
                                allArticles.add(a);
                            }
                        }
                    } else {
                        synchronized (errorMsg) {
                            errorMsg.append(names[idx]).append("失败 ");
                        }
                    }
                } catch (IOException e) {
                    synchronized (errorMsg) {
                        errorMsg.append(names[idx]).append("失败 ");
                    }
                }

                synchronized (remaining) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        if (allArticles.isEmpty()) {
                            callback.onError("在线未找到匹配结果");
                        } else {
                            callback.onSuccess(new ArrayList<>(allArticles));
                        }
                    }
                }
            });
        }
    }

    /** 解析 API 响应为文章列表 */
    private static List<NewsArticle> parseResponse(String respBody, String displayName, String sourceId) {
        HotListResponse hotResp = gson.fromJson(respBody, HotListResponse.class);
        List<NewsArticle> articles = new ArrayList<>();
        if (hotResp == null || hotResp.list == null) return articles;

        for (HotItem item : hotResp.list) {
            NewsArticle article = new NewsArticle();
            article.title = item.title;
            article.url = item.url;
            article.source = new NewsArticle.Source();
            article.source.name = displayName;
            article.source.id = sourceId;
            article.description = "热度: " + (item.hotValue != null ? item.hotValue : "N/A");
            article.publishedAt = hotResp.updateTime;
            articles.add(article);
        }
        return articles;
    }

    private static String getSourceDisplayName(String source) {
        switch (source) {
            case SOURCE_TOUTIAO: return "今日头条";
            case SOURCE_WEIBO:   return "微博热搜";
            case SOURCE_BAIDU:   return "百度热榜";
            case SOURCE_DOUYIN:  return "抖音热榜";
            default:             return source;
        }
    }

    // === 响应结构 ===

    private static class HotListResponse {
        @SerializedName("type")
        String type;
        @SerializedName("update_time")
        String updateTime;
        @SerializedName("list")
        List<HotItem> list;
    }

    private static class HotItem {
        @SerializedName("title")
        String title;
        @SerializedName("url")
        String url;
        @SerializedName("hot_value")
        String hotValue;
        @SerializedName("index")
        int index;
    }
}
