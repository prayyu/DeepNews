package com.deepnews.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 缓存的新闻文章 */
@Entity(tableName = "articles")
public class ArticleEntity {
    @PrimaryKey
    @NonNull
    public String url;  // URL 天然唯一

    public String title;
    public String source;
    public String description;
    @ColumnInfo(name = "url_to_image")
    public String urlToImage;
    @ColumnInfo(name = "published_at")
    public String publishedAt;

    /** 所属事件簇标题（用于关联） */
    @ColumnInfo(name = "event_title")
    public String eventTitle;
}
