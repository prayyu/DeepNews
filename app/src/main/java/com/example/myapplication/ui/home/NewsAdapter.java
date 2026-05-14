package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.api.NewsArticle;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private List<NewsArticle> articles = new ArrayList<>();
    private OnItemClickListener listener;

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
        holder.sourceText.setText(article.source != null ? article.source.name : "");
        holder.itemView.setOnClickListener(v -> {
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

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.news_title);
            sourceText = itemView.findViewById(R.id.news_source);
        }
    }
}
