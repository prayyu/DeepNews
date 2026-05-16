package com.deepnews.app.ui.home;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deepnews.app.R;
import com.deepnews.app.api.NewsArticle;
import com.deepnews.app.util.BadgeUtils;
import com.deepnews.app.util.ReadTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private List<NewsArticle> articles = new ArrayList<>();
    private OnItemClickListener listener;
    private SharedPreferences prefs;
    private Set<String> readUrls = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(NewsArticle article);
    }

    public void setArticles(List<NewsArticle> articles) {
        this.articles = articles;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setPrefs(SharedPreferences prefs) {
        this.prefs = prefs;
        if (prefs != null) {
            this.readUrls = ReadTracker.getReadUrls(prefs);
        }
    }

    public void refreshReadStatus() {
        if (prefs != null) {
            this.readUrls = ReadTracker.getReadUrls(prefs);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsArticle article = articles.get(position);
        holder.titleText.setText(article.title != null ? article.title : "");

        String sourceName = article.source != null ? article.source.name : "";
        holder.sourceText.setText(sourceName);

        // 源标识圆形图标
        BadgeUtils.setBadge(holder.badgeText, sourceName);

        // 已读状态
        boolean isRead = readUrls.contains(article.url);
        holder.titleText.setAlpha(isRead ? 0.6f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            // 标记为已读
            if (prefs != null && article.url != null) {
                ReadTracker.markAsRead(prefs, article.url);
                readUrls = ReadTracker.getReadUrls(prefs);
                notifyItemChanged(position);
            }
            if (listener != null) listener.onItemClick(article);
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView sourceText;
        TextView badgeText;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.news_title);
            sourceText = itemView.findViewById(R.id.news_source);
            badgeText = itemView.findViewById(R.id.news_badge);
        }
    }
}
