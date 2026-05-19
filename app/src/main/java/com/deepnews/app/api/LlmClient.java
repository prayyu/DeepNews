package com.deepnews.app.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.deepnews.app.util.AppExecutors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LlmClient {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private static OkHttpClient client;

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public interface LlmCallback {
        void onSuccess(List<EventCluster> clusters);
        void onError(String error);
    }

    public static void clusterArticles(List<NewsArticle> articles, String apiKey, LlmCallback callback) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_")) {
            callback.onError("请在 local.properties 中配置 llm.api.key");
            return;
        }

        StringBuilder articlesText = new StringBuilder();
        for (int i = 0; i < Math.min(articles.size(), 15); i++) {
            NewsArticle a = articles.get(i);
            articlesText.append(i + 1).append(". [").append(a.source != null ? a.source.name : "未知")
                    .append("] ").append(a.title).append("\n   ").append(a.url).append("\n\n");
        }

        String prompt = "你是一个新闻聚合助手。请将以下新闻文章按事件/主题进行聚类分组，" +
                "同一事件的文章归为一组。\n\n" +
                "请为每个事件簇提供：\n" +
                "1. title: 事件标题（简洁，中文）\n" +
                "2. summary: 一句话摘要（中文）\n" +
                "3. articleUrls: 该事件包含的文章URL列表\n" +
                "4. sources: 该事件的信源名称列表\n\n" +
                "直接返回 JSON 数组，不要额外说明。格式：\n" +
                "[{title: string, summary: string, articleUrls: string[], sources: string[]}]\n\n"
                + "文章列表：\n" + articlesText;

        String bodyJson = gson.toJson(new DeepSeekRequest(
                "deepseek-chat",
                4096,
                List.of(new DeepSeekMessage("user", prompt))
        ));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(bodyJson, JSON))
                .build();

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = getClient().newCall(request).execute();
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    callback.onError("LLM API 错误: " + response.code() + " " + errBody);
                    return;
                }
                String respBody = response.body() != null ? response.body().string() : "{}";
                DeepSeekResponse dsResp = gson.fromJson(respBody, DeepSeekResponse.class);

                if (dsResp.choices != null && !dsResp.choices.isEmpty()
                        && dsResp.choices.get(0).message != null) {
                    String text = dsResp.choices.get(0).message.content;
                    int start = text.indexOf('[');
                    int end = text.lastIndexOf(']');
                    if (start != -1 && end > start) {
                        String json = text.substring(start, end + 1);
                        try {
                            Type listType = new TypeToken<List<EventCluster>>() {}.getType();
                            List<EventCluster> clusters = gson.fromJson(json, listType);
                            if (clusters != null && !clusters.isEmpty()) {
                                callback.onSuccess(clusters);
                                return;
                            }
                        } catch (Exception e) {
                            callback.onError("LLM 返回格式异常: " + e.getLocalizedMessage());
                            return;
                        }
                    }
                }
                callback.onError("LLM 返回格式异常");
            } catch (IOException e) {
                callback.onError("LLM 请求失败: " + e.getLocalizedMessage());
            }
        });
    }

    // === DeepSeek (OpenAI-compatible) 请求/响应结构 ===

    private static class DeepSeekRequest {
        String model;
        int max_tokens;
        List<DeepSeekMessage> messages;

        DeepSeekRequest(String model, int maxTokens, List<DeepSeekMessage> messages) {
            this.model = model;
            this.max_tokens = maxTokens;
            this.messages = messages;
        }
    }

    private static class DeepSeekMessage {
        String role;
        String content;

        DeepSeekMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class DeepSeekResponse {
        List<Choice> choices;
    }

    private static class Choice {
        DeepSeekMessage message;
    }
}
