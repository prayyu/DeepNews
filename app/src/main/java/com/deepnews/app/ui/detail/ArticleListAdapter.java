package com.deepnews.app.ui.detail;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deepnews.app.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

    private final List<String> titles;
    private final List<String> urls;
    private OnItemClickListener listener;
    private Set<String> readUrls = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(String url, String title);
    }

    public ArticleListAdapter(List<String> titles, List<String> urls) {
        this.titles = titles;
        this.urls = urls;
    }

    /** 设置已读 URL 集合，用于显示已读标记 */
    public void setReadUrls(Set<String> readUrls) {
        this.readUrls = readUrls != null ? readUrls : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detail_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String title = titles.get(position);
        String url = urls.get(position);
        holder.titleText.setText(title != null ? title : "查看原文");

        // 已读标记
        boolean isRead = readUrls.contains(url);
        holder.readStatus.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.titleText.setAlpha(isRead ? 0.6f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(url, title);
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView readStatus;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.detail_article_title);
            readStatus = itemView.findViewById(R.id.read_status);
        }
    }
}
