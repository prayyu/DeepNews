package com.example.myapplication.ui.webview;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.entity.BookmarkEntity;
import com.example.myapplication.util.Logger;

import java.util.HashSet;

public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private static final Logger log = Logger.get(WebViewActivity.class);

    private String currentUrl;
    private String currentTitle;
    private TextView bookmarkBtn;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_webview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webview_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ProgressBar progressBar = findViewById(R.id.webview_progress);
        WebView webView = findViewById(R.id.webview);
        TextView titleText = findViewById(R.id.webview_title);
        bookmarkBtn = findViewById(R.id.webview_bookmark);
        TextView shareBtn = findViewById(R.id.webview_share);

        currentUrl = getIntent().getStringExtra(EXTRA_URL);
        currentTitle = getIntent().getStringExtra(EXTRA_TITLE);
        titleText.setText(currentTitle != null ? currentTitle : "阅读原文");

        findViewById(R.id.webview_back).setOnClickListener(v -> finish());

        // 标记为已读
        markAsRead(currentUrl);

        // 书签
        updateBookmarkButton();
        bookmarkBtn.setOnClickListener(v -> {
            if (currentUrl == null) return;
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                if (db.bookmarkDao().isBookmarked(currentUrl) > 0) {
                    db.bookmarkDao().delete(currentUrl);
                } else {
                    BookmarkEntity entity = new BookmarkEntity();
                    entity.url = currentUrl;
                    entity.title = currentTitle != null ? currentTitle : "";
                    entity.source = "";
                    entity.timestamp = System.currentTimeMillis();
                    db.bookmarkDao().insert(entity);
                }
                runOnUiThread(this::updateBookmarkButton);
            }).start();
        });

        // 分享
        shareBtn.setOnClickListener(v -> shareArticle());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setMixedContentMode(
                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        String ua = webView.getSettings().getUserAgentString();
        if (!ua.contains("DeepNews")) {
            webView.getSettings().setUserAgentString(ua + " DeepNews/1.0");
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view,
                    android.webkit.WebResourceRequest request) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        if (currentUrl != null) {
            webView.loadUrl(currentUrl);
        }
    }

    private void shareArticle() {
        if (currentUrl == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentTitle != null ? currentTitle : "");
        shareIntent.putExtra(Intent.EXTRA_TEXT, (currentTitle != null ? currentTitle + "\n" : "") + currentUrl);
        startActivity(Intent.createChooser(shareIntent, "分享到"));
        log.d("分享文章: " + currentTitle);
    }

    /** 将文章 URL 记录为已读 */
    private void markAsRead(String url) {
        if (url == null) return;
        SharedPreferences prefs = getSharedPreferences("deepnews_prefs", MODE_PRIVATE);
        HashSet<String> readSet = new HashSet<>(prefs.getStringSet("read_articles", new HashSet<>()));
        if (readSet.add(url)) {
            prefs.edit().putStringSet("read_articles", readSet).apply();
            log.d("标记已读: " + url);
        }
    }

    private void updateBookmarkButton() {
        if (currentUrl == null) return;
        new Thread(() -> {
            boolean bookmarked = AppDatabase.getInstance(this).bookmarkDao().isBookmarked(currentUrl) > 0;
            runOnUiThread(() ->
                    bookmarkBtn.setText(bookmarked ? "★" : "☆"));
        }).start();
    }
}
