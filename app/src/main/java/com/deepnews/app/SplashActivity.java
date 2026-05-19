package com.deepnews.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.deepnews.app.util.PrefsKeys;

import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
        TextView subtitle = findViewById(R.id.splash_subtitle);

        Set<String> keywords = prefs.getStringSet(PrefsKeys.KEYWORDS, null);
        String loggedInUser = prefs.getString(PrefsKeys.LOGGED_IN_USER, "");
        StringBuilder status = new StringBuilder("AI 驱动的热点聚合");
        if (!loggedInUser.isEmpty()) {
            status.append(" · ").append(loggedInUser);
        }
        if (keywords != null && !keywords.isEmpty()) {
            status.append("\n已订阅 ").append(keywords.size()).append(" 个关键词");
        }
        subtitle.setText(status.toString());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1200);
    }
}
