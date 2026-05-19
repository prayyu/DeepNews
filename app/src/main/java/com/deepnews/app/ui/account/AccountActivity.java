package com.deepnews.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.deepnews.app.R;
import com.deepnews.app.leancloud.LocalAuthManager;

public class AccountActivity extends AppCompatActivity {

    private TextView usernameText;
    private TextView statusText;
    private TextView profileStatusText;
    private Button logoutBtn;
    private View loginSection;
    private View profileSection;

    private final LocalAuthManager auth = LocalAuthManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.account_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usernameText = findViewById(R.id.account_username);
        statusText = findViewById(R.id.account_status);
        profileStatusText = findViewById(R.id.account_profile_status);
        logoutBtn = findViewById(R.id.btn_logout);
        loginSection = findViewById(R.id.account_login_section);
        profileSection = findViewById(R.id.account_profile_section);
        findViewById(R.id.account_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_go_login).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_LOGIN);
        });

        logoutBtn.setOnClickListener(v -> doLogout());
    }

    private static final int REQUEST_LOGIN = 1001;

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            updateUi();
        }
    }

    private void updateUi() {
        if (auth.isLoggedIn()) {
            loginSection.setVisibility(View.GONE);
            profileSection.setVisibility(View.VISIBLE);
            String name = auth.getCurrentUsername();
            usernameText.setText(name.isEmpty() ? "用户" : name);
            profileStatusText.setText("已登录");
        } else {
            loginSection.setVisibility(View.VISIBLE);
            profileSection.setVisibility(View.GONE);
            statusText.setText("");
        }
    }

    private void doLogout() {
        auth.logout();
        updateUi();
        statusText.setText("已登出");
    }
}
