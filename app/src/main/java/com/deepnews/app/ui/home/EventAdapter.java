package com.deepnews.app.ui.home;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deepnews.app.R;
import com.deepnews.app.api.EventCluster;
import com.deepnews.app.util.BadgeUtils;
import com.deepnews.app.util.PrefsKeys;
import com.deepnews.app.util.ReadTracker;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<EventCluster> events = new ArrayList<>();
    private OnEventClickListener listener;
    private SharedPreferences prefs;
    private Set<String> readUrls = new HashSet<>();
    private float fontScale = 1.0f;
    private final Set<Integer> animatedPositions = new HashSet<>();

    public interface OnEventClickListener {
        void onEventClick(EventCluster event);
    }

    public void setEvents(List<EventCluster> events) {
        this.events = events;
        animatedPositions.clear();
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setPrefs(SharedPreferences prefs) {
        this.prefs = prefs;
        if (prefs != null) {
            this.readUrls = ReadTracker.getReadUrls(prefs);
            this.fontScale = prefs.getFloat(PrefsKeys.FONT_SCALE, 1.0f);
        }
    }

    public void setFontScale(float scale) {
        this.fontScale = scale;
        notifyDataSetChanged();
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
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventCluster event = events.get(position);
        holder.titleText.setText(event.title != null ? event.title : "");
        holder.titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16 * fontScale);
        holder.summaryText.setText(event.summary != null ? event.summary : "");
        holder.summaryText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14 * fontScale);

        // 源标识圆形图标
        String badgeKey = event.sources != null && !event.sources.isEmpty()
                ? event.sources.get(0) : "新闻";
        BadgeUtils.setBadge(holder.badgeText, badgeKey);

        // 显示信源标签
        StringBuilder sources = new StringBuilder();
        if (event.sources != null) {
            for (String s : event.sources) {
                if (sources.length() > 0) sources.append(" · ");
                sources.append(s);
            }
        }
        holder.sourcesText.setText(sources.toString());

        // 显示文章数量
        int count = event.articleUrls != null ? event.articleUrls.size() : 0;
        holder.countText.setText(count + " 篇报道");

        // 已读状态
        boolean anyRead = event.articleUrls != null
                && event.articleUrls.stream().anyMatch(u -> readUrls.contains(u));
        if (anyRead) {
            holder.readBadge.setVisibility(View.VISIBLE);
            holder.titleText.setAlpha(0.6f);
        } else {
            holder.readBadge.setVisibility(View.GONE);
            holder.titleText.setAlpha(1.0f);
        }

        // 你可能感兴趣：匹配关键词时显示
        if (prefs != null && matchesKeyword(event)) {
            holder.interestText.setVisibility(View.VISIBLE);
        } else {
            holder.interestText.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

        // 条目滑入动画（仅播放一次）
        if (!animatedPositions.contains(position)) {
            holder.itemView.setTranslationX(50);
            holder.itemView.setAlpha(0f);
            holder.itemView.postDelayed(() -> {
                holder.itemView.animate()
                        .translationX(0)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.BounceInterpolator())
                        .withEndAction(() -> animatedPositions.add(position))
                        .start();
            }, position * 50L);
        }
    }

    private boolean matchesKeyword(EventCluster event) {
        Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, new HashSet<>());
        if (keywords == null || keywords.isEmpty()) return false;
        String title = event.title != null ? event.title.toLowerCase() : "";
        String summary = event.summary != null ? event.summary.toLowerCase() : "";
        for (String kw : keywords) {
            String lowerKw = kw.toLowerCase();
            if (title.contains(lowerKw) || summary.contains(lowerKw)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int pos = holder.getAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) {
            animatedPositions.remove(pos);
        }
        // 重置视图状态以便下次绑定
        holder.itemView.setTranslationX(0);
        holder.itemView.setAlpha(1f);
        holder.itemView.animate().cancel();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView summaryText;
        TextView sourcesText;
        TextView countText;
        TextView badgeText;
        TextView readBadge;
        TextView interestText;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.event_title);
            summaryText = itemView.findViewById(R.id.event_summary);
            sourcesText = itemView.findViewById(R.id.event_sources);
            countText = itemView.findViewById(R.id.event_count);
            badgeText = itemView.findViewById(R.id.event_badge);
            readBadge = itemView.findViewById(R.id.event_read_badge);
            interestText = itemView.findViewById(R.id.event_interest);
        }
    }
}
