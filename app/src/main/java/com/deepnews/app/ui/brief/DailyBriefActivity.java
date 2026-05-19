package com.deepnews.app.ui.brief;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.deepnews.app.BuildConfig;
import com.deepnews.app.R;
import com.deepnews.app.util.PrefsKeys;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import okhttp3.MediaType;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class DailyBriefActivity extends AppCompatActivity {

    private TextView briefText;
    private Button refreshBtn;
    private ProgressBar loadingBar;
    private ChipGroup chipGroup;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daily_brief);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.brief_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);

        TextView titleText = findViewById(R.id.brief_title);
        briefText = findViewById(R.id.brief_content);
        refreshBtn = findViewById(R.id.brief_refresh);
        loadingBar = findViewById(R.id.brief_loading);
        chipGroup = findViewById(R.id.brief_source_chip_group);
        ImageButton backBtn = findViewById(R.id.brief_back);

        backBtn.setOnClickListener(v -> finish());

        // 恢复已保存的来源选择状态
        restoreSourceSelection();
        // 监听芯片选择变化
        setupChipListeners();

        String today = java.text.SimpleDateFormat.getDateInstance().format(new java.util.Date());
        titleText.setText("今日深度简报 — " + today);

        // 先显示缓存的简报
        String cachedDate = prefs.getString(PrefsKeys.BRIEF_DATE, "");
        String cachedBrief = prefs.getString(PrefsKeys.DAILY_BRIEF, "");
        if (cachedDate.equals(today) && !cachedBrief.isEmpty()) {
            briefText.setText(cachedBrief);
        } else {
            briefText.setText("正在生成今日简报...");
            generateBrief();
        }

        refreshBtn.setOnClickListener(v -> generateBrief());
    }

    private void generateBrief() {
        String apiKey = BuildConfig.LLM_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_")) {
            briefText.setText("请先在 local.properties 中配置 llm.api.key");
            return;
        }

        refreshBtn.setEnabled(false);
        loadingBar.setVisibility(android.view.View.VISIBLE);
        briefText.setText("正在调用 AI 生成简报...\n\n首次生成约需 10-20 秒，请耐心等待");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("model", "deepseek-chat");
                body.put("max_tokens", 2048);

                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                String sourceInfo = getSelectedSourcesInfo();
                msg.put("content", "你是一位新闻编辑。请基于今天的新闻，生成一份「今日深度简报」，包含 5 件最重要的新闻事件。\n\n" +
                        "对每件事请提供：\n1. 事件标题（中文）\n2. 一句话概述\n3. 关键意义（为什么重要）\n\n" +
                        sourceInfo +
                        "格式要求：简洁清晰，直接呈现内容，不要额外说明。");
                messages.put(msg);
                body.put("messages", messages);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.deepseek.com/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.get("application/json; charset=utf-8")))
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "{}";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        String httpMsg = response.code() == 401 ? "API Key 无效，请检查配置"
                                : response.code() == 429 ? "API 速率超限，请稍后重试"
                                : "生成失败 (HTTP " + response.code() + ")";
                        briefText.setText(httpMsg);
                        loadingBar.setVisibility(android.view.View.GONE);
                        refreshBtn.setEnabled(true);
                    });
                    return;
                }

                JSONObject json = new JSONObject(respBody);
                String text = json.getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").getString("content");

                String today = java.text.SimpleDateFormat.getDateInstance().format(new java.util.Date());
                prefs.edit()
                        .putString(PrefsKeys.DAILY_BRIEF, text)
                        .putString(PrefsKeys.BRIEF_DATE, today)
                        .apply();

                runOnUiThread(() -> {
                    briefText.setText(text);
                    loadingBar.setVisibility(android.view.View.GONE);
                    refreshBtn.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    String errMsg = e instanceof java.net.SocketTimeoutException
                            ? "请求超时，请检查网络后重试"
                            : e instanceof java.net.UnknownHostException
                            ? "网络连接失败，请检查网络"
                            : "生成失败: " + e.getLocalizedMessage();
                    briefText.setText(errMsg);
                    loadingBar.setVisibility(android.view.View.GONE);
                    refreshBtn.setEnabled(true);
                });
            }
        }).start();
    }

    /** 从 SharedPreferences 恢复已保存的来源选择 */
    private void restoreSourceSelection() {
        Set<String> savedSources = prefs.getStringSet(PrefsKeys.BRIEF_SOURCES, null);
        if (savedSources == null) {
            // 首次使用，默认全选，保存默认值
            savedSources = new HashSet<>(Arrays.asList("今日头条", "微博热搜", "百度热榜", "抖音热榜"));
            prefs.edit().putStringSet(PrefsKeys.BRIEF_SOURCES, savedSources).apply();
        }
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(savedSources.contains(chip.getText().toString()));
            }
        }
    }

    /** 为每个 Chip 设置选择变化监听，自动保存状态 */
    private void setupChipListeners() {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setOnCheckedChangeListener((buttonView, isChecked) -> saveSelectedSources());
            }
        }
    }

    /** 将当前选中的来源保存到 SharedPreferences */
    private void saveSelectedSources() {
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    selected.add(chip.getText().toString());
                }
            }
        }
        prefs.edit().putStringSet(PrefsKeys.BRIEF_SOURCES, selected).apply();
    }

    /** 获取选中来源的描述文本，用于拼接到 AI 提示词中 */
    private String getSelectedSourcesInfo() {
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    selected.add(chip.getText().toString());
                }
            }
        }
        if (selected.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("重点关注以下平台的新闻：");
        boolean first = true;
        for (String s : selected) {
            if (!first) sb.append("、");
            sb.append(s);
            first = false;
        }
        sb.append("\n\n");
        return sb.toString();
    }
}
