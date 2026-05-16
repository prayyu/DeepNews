package com.deepnews.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.deepnews.app.data.NewsRepository;
import com.deepnews.app.ui.bookmark.BookmarkActivity;
import com.deepnews.app.ui.brief.DailyBriefActivity;
import com.deepnews.app.ui.detail.DetailActivity;
import com.deepnews.app.ui.home.EventAdapter;
import com.deepnews.app.ui.home.MainViewModel;
import com.deepnews.app.ui.home.MainViewModelFactory;
import com.deepnews.app.ui.settings.SettingsActivity;
import com.deepnews.app.util.Logger;
import com.deepnews.app.util.PrefsKeys;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

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
    private TabLayout tabLayout;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    viewModel.reapplyFilter();
                }
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
        NewsRepository repository = NewsRepository.getInstance(this);
        viewModel = new ViewModelProvider(this,
                new MainViewModelFactory(repository, getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE)))
                .get(MainViewModel.class);

        // ===== UI 绑定 =====
        RecyclerView eventList = findViewById(R.id.event_list);
        statusText = findViewById(R.id.status_text);
        emptyState = findViewById(R.id.empty_state);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        skeletonLayout = findViewById(R.id.skeleton_layout);
        searchInput = findViewById(R.id.search_input);
        searchClear = findViewById(R.id.search_clear);
        tabLayout = findViewById(R.id.tab_layout);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Tab 栏
        for (String name : MainViewModel.SOURCE_NAMES) {
            tabLayout.addTab(tabLayout.newTab().setText(name));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                viewModel.switchSource(tab.getPosition());
                searchInput.setText("");
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (viewModel.getIsLoading().getValue() != Boolean.TRUE) {
                    viewModel.switchSource(tab.getPosition());
                }
            }
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
        eventAdapter.setPrefs(getSharedPreferences(PrefsKeys.FILE, MODE_PRIVATE));
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
            } else if (id == R.id.nav_settings) {
                settingsLauncher.launch(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

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
