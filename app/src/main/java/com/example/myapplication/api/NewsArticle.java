package com.example.myapplication.api;

import com.google.gson.annotations.SerializedName;

/** 单条新闻文章 */
public class NewsArticle {
    public Source source;
    public String author;
    public String title;
    public String description;
    public String url;
    @SerializedName("urlToImage")
    public String urlToImage;
    @SerializedName("publishedAt")
    public String publishedAt;
    public String content;

    public static class Source {
        public String id;
        public String name;
    }
}
