package com.deepnews.app.ui.detail;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.deepnews.app.R;
import com.deepnews.app.ui.webview.WebViewActivity;
import com.deepnews.app.util.PrefsKeys;
import com.deepnews.app.util.ReadTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "event_title";
    public static final String EXTRA_SUMMARY = "event_summary";
    public static final String EXTRA_ARTICLE_URLS = "article_urls";
    public static final String EXTRA_ARTICLE_TITLES = "article_titles";
    public static final String EXTRA_SOURCES = "sources";

    private SharedPreferences prefs;
    private ArticleListAdapter adapter;
    private ArrayList<String> articleUrls;
    private ArrayList<String> articleTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);

        findViewById(R.id.detail_back).setOnClickListener(v -> finish());

        ImageButton shareBtn = findViewById(R.id.detail_share);
        TextView titleText = findViewById(R.id.detail_title);
        TextView summaryText = findViewById(R.id.detail_summary);
        TextView sourcesText = findViewById(R.id.detail_sources);
        RecyclerView articleList = findViewById(R.id.detail_article_list);
        TextView emptyText = findViewById(R.id.detail_empty);

        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String summary = intent.getStringExtra(EXTRA_SUMMARY);
        String sources = intent.getStringExtra(EXTRA_SOURCES);
        articleUrls = intent.getStringArrayListExtra(EXTRA_ARTICLE_URLS);
        articleTitles = intent.getStringArrayListExtra(EXTRA_ARTICLE_TITLES);

        titleText.setText(title != null && !title.isEmpty() ? title : "事件详情");
        summaryText.setText(summary != null && !summary.isEmpty() ? summary : "暂无摘要信息");
        sourcesText.setText(sources != null && !sources.isEmpty() ? "信源: " + sources : "");

        // 分享按钮
        boolean hasShareContent = title != null && !title.isEmpty();
        if (hasShareContent) {
            shareBtn.setVisibility(android.view.View.VISIBLE);
            shareBtn.setOnClickListener(v -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                String shareText = title;
                if (summary != null && !summary.isEmpty()) shareText += "\n\n" + summary;
                shareText += "\n\n—— 来自 DeepNews";
                share.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(share, "分享到"));
            });
        }

        boolean hasArticles = articleUrls != null && !articleUrls.isEmpty()
                && articleTitles != null && !articleTitles.isEmpty();

        if (hasArticles) {
            Set<String> readUrls = ReadTracker.getReadUrls(prefs);

            adapter = new ArticleListAdapter(articleTitles, articleUrls);
            adapter.setReadUrls(readUrls);
            adapter.setOnItemClickListener((url, articleTitle) -> {
                // 标记已读
                ReadTracker.markAsRead(prefs, url);
                adapter.setReadUrls(ReadTracker.getReadUrls(prefs));

                Intent webIntent = new Intent(this, WebViewActivity.class);
                webIntent.putExtra(WebViewActivity.EXTRA_URL, url);
                webIntent.putExtra(WebViewActivity.EXTRA_TITLE, articleTitle);
                startActivity(webIntent);
            });
            articleList.setAdapter(adapter);
            articleList.setVisibility(android.view.View.VISIBLE);
            if (emptyText != null) emptyText.setVisibility(android.view.View.GONE);
        } else {
            articleList.setVisibility(android.view.View.GONE);
            if (emptyText != null) emptyText.setVisibility(android.view.View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.setReadUrls(ReadTracker.getReadUrls(prefs));
        }
    }
}
