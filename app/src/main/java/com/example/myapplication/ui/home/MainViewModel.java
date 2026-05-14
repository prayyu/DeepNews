package com.example.myapplication.ui.home;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.api.EventCluster;
import com.example.myapplication.api.NewsArticle;
import com.example.myapplication.data.NewsRepository;
import com.example.myapplication.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainViewModel extends ViewModel {

    private static final Logger log = Logger.get(MainViewModel.class);

    public static final String[] SOURCES = {"toutiao", "weibo", "baidu"};
    public static final String[] SOURCE_NAMES = {"今日头条", "微博热搜", "百度热榜"};

    private final MutableLiveData<List<EventCluster>> events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isError = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentSource = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isSearching = new MutableLiveData<>(false);

    private NewsRepository repository;
    private SharedPreferences prefs;
    private List<EventCluster> latestClusters = new ArrayList<>();
    private List<NewsArticle> latestArticles = new ArrayList<>();
    private boolean isFirstLoadDone = false;

    public void init(NewsRepository repository, SharedPreferences prefs) {
        if (this.repository == null) {
            this.repository = repository;
            this.prefs = prefs;
        }
    }

    public LiveData<List<EventCluster>> getEvents() { return events; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsError() { return isError; }
    public LiveData<Integer> getCurrentSource() { return currentSource; }
    public LiveData<Boolean> getIsSearching() { return isSearching; }
    public boolean isFirstLoadDone() { return isFirstLoadDone; }

    public String getCurrentSourceName() {
        Integer idx = currentSource.getValue();
        return idx != null && idx >= 0 && idx < SOURCE_NAMES.length ? SOURCE_NAMES[idx] : "今日头条";
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
        statusMessage.setValue("正在获取" + sourceName + "...");

        repository.fetchNews(source, new NewsRepository.OnResult() {
            @Override
            public void onSuccess(List<EventCluster> clusters, List<NewsArticle> articles) {
                latestClusters = clusters;
                latestArticles = articles;
                List<EventCluster> filtered = applyKeywordFilter(clusters);
                events.postValue(filtered);
                isLoading.postValue(false);
                statusMessage.postValue(sourceName + "：共 " + articles.size() + " 条热点，"
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

    /** 搜索缓存文章 */
    public void search(String query) {
        if (repository == null || query == null || query.trim().isEmpty()) {
            if (query == null || query.trim().isEmpty()) cancelSearch();
            return;
        }
        isSearching.setValue(true);
        isLoading.setValue(true);
        statusMessage.setValue("搜索: " + query);

        repository.searchArticles(query.trim(), clusters -> {
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

        Set<String> keywords = prefs.getStringSet("keywords", new HashSet<>());
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
