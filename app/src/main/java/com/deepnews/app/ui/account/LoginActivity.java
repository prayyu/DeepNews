package com.deepnews.app.ui.account;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.deepnews.app.R;
import com.deepnews.app.leancloud.LocalAuthManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private TextView statusText;
    private ProgressBar loadingBar;
    private Button loginBtn;
    private Button registerBtn;
    private TextView resetPwdBtn;

    private final LocalAuthManager auth = LocalAuthManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usernameInput = findViewById(R.id.login_username);
        passwordInput = findViewById(R.id.login_password);
        statusText = findViewById(R.id.login_status);
        loadingBar = findViewById(R.id.login_loading);
        loginBtn = findViewById(R.id.btn_login);
        registerBtn = findViewById(R.id.btn_register);
        resetPwdBtn = findViewById(R.id.btn_reset_password);
        findViewById(R.id.login_back).setOnClickListener(v -> finish());

        loginBtn.setOnClickListener(v -> doLogin());
        registerBtn.setOnClickListener(v -> doRegister());
        resetPwdBtn.setOnClickListener(v -> statusText.setText("本地账号不支持密码重置功能"));
    }

    private void setLoading(boolean loading) {
        loadingBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginBtn.setEnabled(!loading);
        registerBtn.setEnabled(!loading);
    }

    private void doLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            statusText.setText("请输入用户名和密码");
            return;
        }
        setLoading(true);
        statusText.setText("登录中...");
        auth.login(username, password, new LocalAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String username) {
                setLoading(false);
                statusText.setText("登录成功");
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onError(String error) {
                setLoading(false);
                statusText.setText(error);
            }
        });
    }

    private void doRegister() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            statusText.setText("请输入用户名和密码");
            return;
        }
        if (password.length() < 6) {
            statusText.setText("密码至少 6 位");
            return;
        }
        setLoading(true);
        statusText.setText("注册中...");
        auth.register(username, password, new LocalAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String username) {
                setLoading(false);
                statusText.setText("注册成功");
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onError(String error) {
                setLoading(false);
                statusText.setText(error);
            }
        });
    }
}
