package com.example.myapplication.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        String url = BASE_URL + source;
        String sourceName = getSourceDisplayName(source);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) DeepNews/1.0")
                .header("Accept", "application/json")
                .build();

        new Thread(() -> {
            try {
                Response response = getClient().newCall(request).execute();
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    callback.onError(sourceName + "请求失败 (HTTP " + response.code() + ")");
                    return;
                }
                String respBody = response.body() != null ? response.body().string() : "{}";
                HotListResponse hotResp = gson.fromJson(respBody, HotListResponse.class);

                if (hotResp == null || hotResp.list == null || hotResp.list.isEmpty()) {
                    callback.onError(sourceName + "数据为空");
                    return;
                }

                List<NewsArticle> articles = new ArrayList<>();
                for (HotItem item : hotResp.list) {
                    NewsArticle article = new NewsArticle();
                    article.title = item.title;
                    article.url = item.url;
                    article.source = new NewsArticle.Source();
                    article.source.name = sourceName;
                    article.source.id = source;
                    article.description = "热度: " + (item.hotValue != null ? item.hotValue : "N/A");
                    article.publishedAt = hotResp.updateTime;
                    articles.add(article);
                }

                callback.onSuccess(articles);
            } catch (IOException e) {
                callback.onError(sourceName + "请求失败: " + e.getLocalizedMessage());
            }
        }).start();
    }

    private static String getSourceDisplayName(String source) {
        switch (source) {
            case SOURCE_TOUTIAO: return "今日头条";
            case SOURCE_WEIBO:   return "微博热搜";
            case SOURCE_BAIDU:   return "百度热榜";
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
