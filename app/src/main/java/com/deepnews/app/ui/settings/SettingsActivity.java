package com.deepnews.app.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;

import com.deepnews.app.R;
import com.deepnews.app.util.PrefsKeys;

import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView keywordList;
    private EditText keywordInput;
    private MaterialButton fontSmall, fontMedium, fontLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
        findViewById(R.id.settings_back).setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        keywordInput = findViewById(R.id.keyword_input);
        keywordList = findViewById(R.id.keyword_list);
        Button addBtn = findViewById(R.id.btn_add_keyword);
        Button clearBtn = findViewById(R.id.btn_clear_keywords);

        SwitchCompat darkModeSwitch = findViewById(R.id.dark_mode_switch);
        boolean isDark = prefs.getBoolean(PrefsKeys.DARK_MODE, false);
        darkModeSwitch.setChecked(isDark);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PrefsKeys.DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        updateKeywordDisplay();

        addBtn.setOnClickListener(v -> {
            String kw = keywordInput.getText().toString().trim();
            if (!kw.isEmpty()) {
                Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, new HashSet<>());
                keywords.add(kw);
                prefs.edit().putStringSet(PrefsKeys.KEYWORDS, keywords).apply();
                keywordInput.setText("");
                updateKeywordDisplay();
                setResult(RESULT_OK);
            }
        });

        clearBtn.setOnClickListener(v -> {
            prefs.edit().remove(PrefsKeys.KEYWORDS).apply();
            updateKeywordDisplay();
            setResult(RESULT_OK);
        });

        // 字号设置
        fontSmall = (MaterialButton) findViewById(R.id.btn_font_small);
        fontMedium = (MaterialButton) findViewById(R.id.btn_font_medium);
        fontLarge = (MaterialButton) findViewById(R.id.btn_font_large);
        updateFontButtons();

        fontSmall.setOnClickListener(v -> saveFontScale(0.85f));
        fontMedium.setOnClickListener(v -> saveFontScale(1.0f));
        fontLarge.setOnClickListener(v -> saveFontScale(1.2f));

        // 清除已读记录
        findViewById(R.id.btn_clear_read).setOnClickListener(v -> {
            prefs.edit().remove(PrefsKeys.READ_ARTICLES).apply();
            ((TextView) findViewById(R.id.btn_clear_read)).setText("已清除已读记录");
        });

        findViewById(R.id.btn_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }

    private void saveFontScale(float scale) {
        prefs.edit().putFloat(PrefsKeys.FONT_SCALE, scale).apply();
        updateFontButtons();
        setResult(RESULT_OK);
    }

    private void updateFontButtons() {
        float current = prefs.getFloat(PrefsKeys.FONT_SCALE, 1.0f);
        int selected = (int) (getResources().getDisplayMetrics().density * 3);
        int normal = (int) (getResources().getDisplayMetrics().density * 1);
        fontSmall.setStrokeWidth(current == 0.85f ? selected : normal);
        fontMedium.setStrokeWidth(current == 1.0f ? selected : normal);
        fontLarge.setStrokeWidth(current == 1.2f ? selected : normal);
    }

    private void updateKeywordDisplay() {
        Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, new HashSet<>());
        if (keywords.isEmpty()) {
            keywordList.setText("暂无订阅关键词\n\n添加关键词后，首页将优先展示相关新闻");
        } else {
            StringBuilder sb = new StringBuilder("已订阅关键词:\n");
            for (String kw : keywords) {
                sb.append("• ").append(kw).append("\n");
            }
            keywordList.setText(sb.toString());
        }
    }
}
