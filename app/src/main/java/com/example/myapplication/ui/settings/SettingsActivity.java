package com.example.myapplication.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;

import com.example.myapplication.R;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView keywordList;
    private EditText keywordInput;

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

        prefs = getSharedPreferences("deepnews_prefs", MODE_PRIVATE);
        findViewById(R.id.settings_back).setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        keywordInput = findViewById(R.id.keyword_input);
        keywordList = findViewById(R.id.keyword_list);
        Button addBtn = findViewById(R.id.btn_add_keyword);
        Button clearBtn = findViewById(R.id.btn_clear_keywords);

        SwitchCompat darkModeSwitch = findViewById(R.id.dark_mode_switch);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDark);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        updateKeywordDisplay();

        addBtn.setOnClickListener(v -> {
            String kw = keywordInput.getText().toString().trim();
            if (!kw.isEmpty()) {
                Set<String> keywords = prefs.getStringSet("keywords", new HashSet<>());
                keywords.add(kw);
                prefs.edit().putStringSet("keywords", keywords).apply();
                keywordInput.setText("");
                updateKeywordDisplay();
            }
        });

        clearBtn.setOnClickListener(v -> {
            prefs.edit().remove("keywords").apply();
            updateKeywordDisplay();
        });

        findViewById(R.id.btn_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }

    private void updateKeywordDisplay() {
        Set<String> keywords = prefs.getStringSet("keywords", new HashSet<>());
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
