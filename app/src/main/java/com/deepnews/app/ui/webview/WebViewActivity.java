package com.deepnews.app.ui.webview;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.deepnews.app.R;
import com.deepnews.app.data.AppDatabase;
import com.deepnews.app.data.entity.BookmarkEntity;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;

import java.util.HashSet;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private static final Logger log = Logger.get(WebViewActivity.class);

    private String currentUrl;
    private String currentTitle;
    private ImageButton bookmarkBtn;

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
        ImageButton shareBtn = findViewById(R.id.webview_share);

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

        // WebView 设置
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(
                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        // 使用默认 User Agent（不添加自定义后缀，避免部分网站拦截）
        // 保持原生 Chrome UA 以获得最佳兼容性

        // 加载失败时显示的错误页面模板
        final String errorTemplate = "<html><body style=\"display:flex;align-items:center;justify-content:center;height:100vh;margin:0;padding:24px;font-family:sans-serif\">"
                + "<div style=\"text-align:center\">"
                + "<p style=\"font-size:18px;margin:0;color:?attr/colorOnSurface\">%s</p>"
                + "<p style=\"font-size:14px;margin:12px 0 0;color:?attr/colorOnSurfaceVariant;line-height:1.6\">%s</p>"
                + "</div></body></html>";

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                log.w("页面加载失败: " + failingUrl + " (error=" + errorCode + ": " + description + ")");
                String html = String.format(errorTemplate, "页面无法打开",
                        "错误代码: " + errorCode + "<br>" + htmlEscape(description));
                view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }

            @Override
            public void onReceivedHttpError(WebView view,
                    android.webkit.WebResourceRequest request,
                    android.webkit.WebResourceResponse errorResponse) {
                if (request.isForMainFrame()) {
                    int code = errorResponse.getStatusCode();
                    log.w("HTTP 错误: " + code + " for " + request.getUrl());
                    String html = String.format(errorTemplate, "页面无法打开",
                            "服务器返回错误 (HTTP " + code
                            + ")<br>请确认链接有效后重试");
                    view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view,
                    android.webkit.WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                // 非 http 链接尝试用外部应用打开
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    log.w("无应用可处理: " + url);
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        if (currentUrl != null && !currentUrl.isEmpty()) {
            webView.loadUrl(currentUrl);
        } else {
            progressBar.setVisibility(View.GONE);
            String html = String.format(errorTemplate, "页面无法打开", "链接地址为空");
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
    }

    /** 简单 HTML 转义，防止特殊字符破坏页面 */
    private static String htmlEscape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
        HashSet<String> readSet = new HashSet<>(prefs.getStringSet(PrefsKeys.READ_ARTICLES, new HashSet<>()));
        if (readSet.add(url)) {
            prefs.edit().putStringSet(PrefsKeys.READ_ARTICLES, readSet).apply();
            log.d("标记已读: " + url);
        }
    }

    private void updateBookmarkButton() {
        if (currentUrl == null) return;
        new Thread(() -> {
            boolean bookmarked = AppDatabase.getInstance(this).bookmarkDao().isBookmarked(currentUrl) > 0;
            runOnUiThread(() ->
                    bookmarkBtn.setImageResource(bookmarked
                            ? R.drawable.ic_bookmark
                            : R.drawable.ic_bookmark_outline));
        }).start();
    }
}
