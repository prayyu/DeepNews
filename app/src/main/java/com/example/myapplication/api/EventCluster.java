package com.example.myapplication.api;

import java.util.List;

/** LLM 聚类后的事件簇 */
public class EventCluster {
    public String title;
    public String summary;
    public List<String> articleUrls;
    public List<String> articleTitles;
    public List<String> sources;
}
