package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.api.EventCluster;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<EventCluster> events = new ArrayList<>();
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(EventCluster event);
    }

    public void setEvents(List<EventCluster> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
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
        holder.summaryText.setText(event.summary != null ? event.summary : "");

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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView summaryText;
        TextView sourcesText;
        TextView countText;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.event_title);
            summaryText = itemView.findViewById(R.id.event_summary);
            sourcesText = itemView.findViewById(R.id.event_sources);
            countText = itemView.findViewById(R.id.event_count);
        }
    }
}
