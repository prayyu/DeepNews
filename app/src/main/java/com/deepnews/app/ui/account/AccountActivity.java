package com.deepnews.app.ui.account;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.deepnews.app.R;
import com.deepnews.app.leancloud.LocalAuthManager;
import com.deepnews.app.util.PrefsKeys;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AccountActivity extends AppCompatActivity {

    private TextView usernameText;
    private TextView statusText;
    private TextView profileStatusText;
    private Button logoutBtn;
    private Button editNicknameBtn;
    private View loginSection;
    private View profileSection;
    private SharedPreferences prefs;

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

        prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);

        usernameText = findViewById(R.id.account_username);
        statusText = findViewById(R.id.account_status);
        profileStatusText = findViewById(R.id.account_profile_status);
        logoutBtn = findViewById(R.id.btn_logout);
        editNicknameBtn = findViewById(R.id.btn_edit_nickname);
        loginSection = findViewById(R.id.account_login_section);
        profileSection = findViewById(R.id.account_profile_section);
        findViewById(R.id.account_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_go_login).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_LOGIN);
        });

        logoutBtn.setOnClickListener(v -> doLogout());

        editNicknameBtn.setOnClickListener(v -> showEditNicknameDialog());
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
            String nickname = prefs.getString(PrefsKeys.NICKNAME, "");
            String name = auth.getCurrentUsername();
            if (!nickname.isEmpty()) {
                usernameText.setText(nickname);
            } else {
                usernameText.setText(name.isEmpty() ? "用户" : name);
            }
            profileStatusText.setText("已登录");
        } else {
            loginSection.setVisibility(View.VISIBLE);
            profileSection.setVisibility(View.GONE);
            statusText.setText("");
        }
    }

    private void showEditNicknameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑昵称");

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("请输入新昵称");
        String currentNickname = prefs.getString(PrefsKeys.NICKNAME, auth.getCurrentUsername());
        input.setText(currentNickname);
        input.setSelection(input.getText().length());

        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newNickname = input.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                prefs.edit().putString(PrefsKeys.NICKNAME, newNickname).apply();
                updateUi();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void doLogout() {
        auth.logout();
        updateUi();
        statusText.setText("已登出");
    }
}
