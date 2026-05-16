package com.deepnews.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 缓存的事件簇（LLM 聚类结果） */
@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String summary;

    /** 关联文章的 URL 列表（JSON 数组字符串） */
    public String articleUrls;

    /** 关联文章标题列表（JSON 数组字符串） */
    public String articleTitles;

    /** 信源名称列表（JSON 数组字符串） */
    public String sources;

    /** 创建时间戳 */
    public long timestamp;
}
