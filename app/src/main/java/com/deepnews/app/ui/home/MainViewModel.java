package com.deepnews.app.ui.home;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.deepnews.app.api.EventCluster;
import com.deepnews.app.api.NewsArticle;
import com.deepnews.app.data.NewsRepository;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private static final Logger log = Logger.get(MainViewModel.class);

    public static final String[] SOURCES = {"all", "toutiao", "weibo", "baidu", "douyin"};
    public static final String[] SOURCE_NAMES = {"综合", "今日头条", "微博热搜", "百度热榜", "抖音热榜"};

    private final MutableLiveData<List<EventCluster>> events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isError = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentSource = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isSearching = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);

    private final NewsRepository repository;
    private final SharedPreferences prefs;
    private List<EventCluster> latestClusters = new ArrayList<>();
    private List<NewsArticle> latestArticles = new ArrayList<>();
    private boolean isFirstLoadDone = false;
    private int loadMoreCount = 0;
    private static final int MAX_LOAD_MORE = 3;

    @Inject
    public MainViewModel(NewsRepository repository, SharedPreferences prefs) {
        this.repository = repository;
        this.prefs = prefs;
    }

    public LiveData<List<EventCluster>> getEvents() { return events; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsError() { return isError; }
    public LiveData<Integer> getCurrentSource() { return currentSource; }
    public LiveData<Boolean> getIsSearching() { return isSearching; }
    public LiveData<Boolean> getHasMore() { return hasMore; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public boolean isFirstLoadDone() { return isFirstLoadDone; }

    public String getCurrentSourceName() {
        Integer idx = currentSource.getValue();
        return idx != null && idx >= 0 && idx < SOURCE_NAMES.length ? SOURCE_NAMES[idx] : "综合";
    }

    public void switchSource(int index) {
        if (index < 0 || index >= SOURCES.length) return;
        currentSource.setValue(index);
        fetchNews();
    }

    public void loadCacheThenFetch() {
        loadCache();
        fetchNews();
    }

    public void loadCache() {
        if (repository == null) return;
        repository.loadCache(clusters -> {
            if (!clusters.isEmpty()) {
                latestClusters = clusters;
                events.postValue(applyKeywordFilter(clusters));
                statusMessage.postValue("已加载缓存");
                isError.postValue(false);
                log.d("缓存加载完成");
            }
        });
    }

    public void fetchNews() {
        if (repository == null) {
            statusMessage.setValue("Repository 未初始化");
            isError.setValue(true);
            return;
        }

        Boolean loading = isLoading.getValue();
        if (loading != null && loading) return;

        Integer sourceIdx = currentSource.getValue();
        String source = SOURCES[sourceIdx != null ? sourceIdx : 0];
        String sourceName = SOURCE_NAMES[sourceIdx != null ? sourceIdx : 0];

        isLoading.setValue(true);
        isError.setValue(false);
        isSearching.setValue(false);
        loadMoreCount = 0;
        hasMore.setValue(true);
        isLoadingMore.setValue(false);
        statusMessage.setValue("正在获取" + sourceName + "...");

        repository.fetchNews(source, new NewsRepository.OnResult() {
            @Override
            public void onSuccess(List<EventCluster> clusters, List<NewsArticle> articles) {
                latestClusters = clusters;
                latestArticles = articles;
                List<EventCluster> filtered = applyKeywordFilter(clusters);
                events.postValue(filtered);
                isLoading.postValue(false);
                statusMessage.postValue(sourceName + "：共 " + articles.size() + " 条新闻，"
                        + clusters.size() + " 个事件"
                        + (filtered.size() < clusters.size() ? "（筛选后 " + filtered.size() + "）" : ""));
                isError.postValue(false);
                isFirstLoadDone = true;
                log.d(sourceName + "获取完成");
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                statusMessage.postValue(error);
                isError.postValue(true);
                log.w("获取失败: " + error);
            }
        });
    }

    /** 搜索（本地缓存 + 在线补充） */
    public void search(String query) {
        if (repository == null || query == null || query.trim().isEmpty()) {
            if (query == null || query.trim().isEmpty()) cancelSearch();
            return;
        }
        isSearching.setValue(true);
        isLoading.setValue(true);
        statusMessage.setValue("正在在线搜索: " + query);

        repository.searchOnline(query.trim(), clusters -> {
            events.postValue(clusters);
            isLoading.postValue(false);
            if (clusters.isEmpty()) {
                statusMessage.postValue("未找到匹配结果");
                isError.postValue(true);
            } else {
                statusMessage.postValue("找到 " + clusters.size() + " 个相关事件");
                isError.postValue(false);
            }
        });
    }

    /** 加载更多内容 */
    public void loadMore() {
        if (repository == null) return;
        Boolean loading = isLoading.getValue();
        if (loading != null && loading) return;
        Boolean more = hasMore.getValue();
        if (more == null || !more) return;

        // 保存当前已加载的 URL 用于去重
        final List<EventCluster> existingClusters = new ArrayList<>(latestClusters);
        final List<NewsArticle> existingArticles = new ArrayList<>(latestArticles);
        final Set<String> existingUrls = new HashSet<>();
        for (NewsArticle a : existingArticles) {
            if (a.url != null) existingUrls.add(a.url);
        }

        Integer sourceIdx = currentSource.getValue();
        String source = SOURCES[sourceIdx != null ? sourceIdx : 0];

        isLoading.setValue(true);
        isLoadingMore.setValue(true);
        statusMessage.setValue("正在加载更多...");

        repository.fetchNews(source, new NewsRepository.OnResult() {
            @Override
            public void onSuccess(List<EventCluster> clusters, List<NewsArticle> articles) {
                List<NewsArticle> newArticles = new ArrayList<>();
                for (NewsArticle a : articles) {
                    if (a.url != null && !existingUrls.contains(a.url)) {
                        newArticles.add(a);
                    }
                }

                if (newArticles.isEmpty()) {
                    isLoading.postValue(false);
                    isLoadingMore.postValue(false);
                    statusMessage.postValue("暂无更多内容");
                    hasMore.postValue(false);
                    return;
                }

                Set<String> newUrls = new HashSet<>();
                for (NewsArticle a : newArticles) {
                    if (a.url != null) newUrls.add(a.url);
                }

                List<EventCluster> newClusters = new ArrayList<>();
                for (EventCluster c : clusters) {
                    EventCluster nc = new EventCluster();
                    nc.title = c.title;
                    nc.summary = c.summary;
                    nc.articleUrls = new ArrayList<>();
                    nc.articleTitles = new ArrayList<>();
                    nc.sources = new ArrayList<>();
                    if (c.sources != null) nc.sources.addAll(c.sources);

                    boolean hasNew = false;
                    if (c.articleUrls != null) {
                        for (int i = 0; i < c.articleUrls.size(); i++) {
                            String url = c.articleUrls.get(i);
                            if (newUrls.contains(url)) {
                                nc.articleUrls.add(url);
                                if (c.articleTitles != null && i < c.articleTitles.size()) {
                                    nc.articleTitles.add(c.articleTitles.get(i));
                                }
                                hasNew = true;
                            }
                        }
                    }
                    if (hasNew && !nc.articleUrls.isEmpty()) {
                        newClusters.add(nc);
                    }
                }

                latestArticles = new ArrayList<>(existingArticles);
                latestArticles.addAll(newArticles);
                latestClusters = new ArrayList<>(existingClusters);
                latestClusters.addAll(newClusters);

                events.postValue(applyKeywordFilter(latestClusters));
                isLoading.postValue(false);
                isLoadingMore.postValue(false);
                statusMessage.postValue("已加载 " + newArticles.size() + " 条新内容");
                isError.postValue(false);

                loadMoreCount++;
                hasMore.postValue(loadMoreCount < MAX_LOAD_MORE);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                isLoadingMore.postValue(false);
                statusMessage.postValue("加载更多失败: " + error);
                isError.postValue(true);
                hasMore.postValue(false);
            }
        });
    }

    public void cancelSearch() {
        isSearching.setValue(false);
        events.setValue(applyKeywordFilter(latestClusters));
        if (latestClusters.isEmpty()) {
            statusMessage.setValue("");
        }
    }

    public void reapplyFilter() {
        if (Boolean.TRUE.equals(isSearching.getValue())) return;
        List<EventCluster> filtered = applyKeywordFilter(latestClusters);
        events.setValue(filtered);
        int total = latestClusters.size();
        if (filtered.size() < total) {
            statusMessage.setValue("关键词筛选：" + filtered.size() + "/" + total + " 个事件");
        }
    }

    private List<EventCluster> applyKeywordFilter(List<EventCluster> clusters) {
        if (clusters == null || clusters.isEmpty()) return clusters;
        if (prefs == null) return clusters;

        Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, new HashSet<>());
        if (keywords == null || keywords.isEmpty()) return clusters;

        List<EventCluster> result = new ArrayList<>(clusters);
        Collections.sort(result, (a, b) -> {
            boolean aMatch = matchesAnyKeyword(a, keywords);
            boolean bMatch = matchesAnyKeyword(b, keywords);
            if (aMatch && !bMatch) return -1;
            if (!aMatch && bMatch) return 1;
            return 0;
        });
        return result;
    }

    private boolean matchesAnyKeyword(EventCluster event, Set<String> keywords) {
        if (event.title == null && event.summary == null) return false;
        for (String kw : keywords) {
            String lowerKw = kw.toLowerCase();
            if ((event.title != null && event.title.toLowerCase().contains(lowerKw))
                    || (event.summary != null && event.summary.toLowerCase().contains(lowerKw))) {
                return true;
            }
        }
        return false;
    }

    public void clearStatus() {
        Boolean loading = isLoading.getValue();
        if (loading != null && !loading) {
            statusMessage.setValue("");
        }
    }
}
