package com.example.myapplication.api;

import java.util.List;

/** NewsAPI 返回的顶层响应 */
public class NewsResponse {
    public String status;
    public int totalResults;
    public List<NewsArticle> articles;
}
