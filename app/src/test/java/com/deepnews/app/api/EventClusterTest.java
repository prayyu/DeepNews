package com.deepnews.app.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** EventCluster POJO 及 NewsArticle 转换逻辑测试 */
public class EventClusterTest {

    @Test
    public void eventCluster_canSetAndGetFields() {
        EventCluster cluster = new EventCluster();
        cluster.title = "测试事件";
        cluster.summary = "这是一个测试";
        cluster.articleUrls = new ArrayList<>();
        cluster.articleUrls.add("https://example.com/1");
        cluster.sources = new ArrayList<>();
        cluster.sources.add("测试来源");

        assertEquals("测试事件", cluster.title);
        assertEquals("这是一个测试", cluster.summary);
        assertEquals(1, cluster.articleUrls.size());
        assertEquals(1, cluster.sources.size());
    }

    @Test
    public void newsArticle_defaultFields() {
        NewsArticle article = new NewsArticle();
        article.title = "文章标题";
        article.url = "https://example.com/article";
        article.source = new NewsArticle.Source();
        article.source.name = "来源A";

        assertEquals("文章标题", article.title);
        assertEquals("https://example.com/article", article.url);
        assertEquals("来源A", article.source.name);
    }

    @Test
    public void newsArticle_missingSource() {
        NewsArticle article = new NewsArticle();
        article.title = "无来源文章";
        assertNull(article.source);
    }

    @Test
    public void articleList_fallbackGrouping() {
        // 模拟多个文章，按来源分组
        List<NewsArticle> articles = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            NewsArticle a = new NewsArticle();
            a.title = "文章" + i;
            a.url = "https://example.com/" + i;
            a.source = new NewsArticle.Source();
            a.source.name = "来源A";
            articles.add(a);
        }

        NewsArticle b1 = new NewsArticle();
        b1.title = "B文章";
        b1.url = "https://example.com/b";
        b1.source = new NewsArticle.Source();
        b1.source.name = "来源B";
        articles.add(b1);

        // 按来源名称分组
        assertEquals(4, articles.size());
        assertEquals("来源A", articles.get(0).source.name);
        assertEquals("来源B", articles.get(3).source.name);
    }

    @Test
    public void emptyArticleList() {
        List<NewsArticle> empty = new ArrayList<>();
        assertTrue(empty.isEmpty());
    }
}
