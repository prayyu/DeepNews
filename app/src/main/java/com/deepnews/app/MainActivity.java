package com.deepnews.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.deepnews.app.ui.account.AccountActivity;
import com.deepnews.app.ui.bookmark.BookmarkActivity;
import com.deepnews.app.ui.brief.DailyBriefActivity;
import com.deepnews.app.ui.detail.DetailActivity;
import com.deepnews.app.ui.home.EventAdapter;
import com.deepnews.app.ui.home.MainViewModel;
import com.deepnews.app.ui.settings.SettingsActivity;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final Logger log = Logger.get(MainActivity.class);

    private MainViewModel viewModel;
    private EventAdapter eventAdapter;
    private TextView statusText;
    private View emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private View skeletonLayout;
    private EditText searchInput;
    private View searchClear;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    viewModel.reapplyFilter();
                    SharedPreferences prefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
                    float scale = prefs.getFloat(PrefsKeys.FONT_SCALE, 1.0f);
                    eventAdapter.setFontScale(scale);
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) log.d("通知权限已授予");
                else log.d("通知权限被拒绝");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ===== ViewModel =====
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // ===== UI 绑定 =====
        RecyclerView eventList = findViewById(R.id.event_list);
        statusText = findViewById(R.id.status_text);
        emptyState = findViewById(R.id.empty_state);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        skeletonLayout = findViewById(R.id.skeleton_layout);
        searchInput = findViewById(R.id.search_input);
        searchClear = findViewById(R.id.search_clear);
        ProgressBar loadMoreProgress = findViewById(R.id.load_more_progress);
        ChipGroup chipGroup = findViewById(R.id.chip_group);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // 来源筛选 Chip
        for (int i = 0; i < MainViewModel.SOURCE_NAMES.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(MainViewModel.SOURCE_NAMES[i]);
            chip.setTag(i);
            chip.setClickable(true);
            chip.setCheckable(true);
            chip.setTextSize(13);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1f);
            chip.setChipCornerRadius(32f);
            chipGroup.addView(chip);

            if (i == 0) chip.setChecked(true);
        }
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip selected = findViewById(checkedIds.get(0));
            int index = (int) selected.getTag();
            viewModel.switchSource(index);
            searchInput.setText("");
        });

        // 搜索
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.length() >= 2) viewModel.search(q);
                else if (q.isEmpty()) viewModel.cancelSearch();
            }
        });
        searchInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                String q = searchInput.getText().toString().trim();
                if (q.length() >= 2) viewModel.search(q);
                return true;
            }
            return false;
        });
        searchClear.setOnClickListener(v -> {
            searchInput.setText("");
            viewModel.cancelSearch();
        });

        // 列表
        eventAdapter = new EventAdapter();
        SharedPreferences sharedPrefs = getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE);
        eventAdapter.setPrefs(sharedPrefs);
        eventAdapter.setFontScale(sharedPrefs.getFloat(PrefsKeys.FONT_SCALE, 1.0f));
        eventAdapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_TITLE, event.title);
            intent.putExtra(DetailActivity.EXTRA_SUMMARY, event.summary);
            intent.putStringArrayListExtra(DetailActivity.EXTRA_ARTICLE_URLS,
                    event.articleUrls != null ? new ArrayList<>(event.articleUrls) : new ArrayList<>());
            intent.putStringArrayListExtra(DetailActivity.EXTRA_ARTICLE_TITLES,
                    event.articleTitles != null ? new ArrayList<>(event.articleTitles) : new ArrayList<>());
            intent.putExtra(DetailActivity.EXTRA_SOURCES,
                    event.sources != null ? String.join(" · ", event.sources) : "");
            startActivity(intent);
        });
        eventList.setAdapter(eventAdapter);
        eventList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fade_in));

        // 滚动加载更多
        eventList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int lastVisiblePosition = lm.findLastVisibleItemPosition();
                int totalItemCount = lm.getItemCount();
                if (totalItemCount > 0 && lastVisiblePosition >= totalItemCount - 2) {
                    Boolean hasMore = viewModel.getHasMore().getValue();
                    Boolean isLoading = viewModel.getIsLoading().getValue();
                    if (hasMore != null && hasMore && (isLoading == null || !isLoading)) {
                        viewModel.loadMore();
                    }
                }
            }
        });

        // ===== 观察 ViewModel =====
        viewModel.getEvents().observe(this, clusters -> {
            eventAdapter.setEvents(clusters);
            updateEmptyState();
        });

        viewModel.getIsLoading().observe(this, loading -> {
            boolean l = loading != null && loading;
            swipeRefresh.setRefreshing(false); // SwipeRefresh 只用于刷新指示
            skeletonLayout.setVisibility(l && eventAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });

        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg == null || msg.isEmpty()) {
                statusText.setVisibility(View.GONE);
            } else {
                statusText.setText(msg);
                Boolean isError = viewModel.getIsError().getValue();
                statusText.setTextColor(isError != null && isError
                        ? getColor(R.color.status_error)
                        : getColor(R.color.status_normal));
                statusText.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoadingMore().observe(this, loadingMore -> {
            loadMoreProgress.setVisibility(loadingMore != null && loadingMore ? View.VISIBLE : View.GONE);
        });

        // ===== 事件 =====
        swipeRefresh.setOnRefreshListener(() -> viewModel.fetchNews());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_brief) {
                startActivity(new Intent(this, DailyBriefActivity.class));
                return true;
            } else if (id == R.id.nav_bookmark) {
                startActivity(new Intent(this, BookmarkActivity.class));
                return true;
            } else if (id == R.id.nav_person) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                settingsLauncher.launch(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        // ===== 通知权限（Android 13+） =====
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (PermissionChecker.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PermissionChecker.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // ===== 数据 =====
        viewModel.loadCacheThenFetch();
        log.d("MainActivity 创建完成");
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventAdapter.refreshReadStatus();
    }

    private void updateEmptyState() {
        Boolean loading = viewModel.getIsLoading().getValue();
        boolean show = eventAdapter.getItemCount() == 0
                && (loading == null || !loading)
                && skeletonLayout.getVisibility() != View.VISIBLE;
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
