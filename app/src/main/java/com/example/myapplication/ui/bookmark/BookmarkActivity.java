package com.example.myapplication.ui.bookmark;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.entity.BookmarkEntity;
import com.example.myapplication.ui.webview.WebViewActivity;

import java.util.List;

public class BookmarkActivity extends AppCompatActivity {

    private BookmarkListAdapter adapter;
    private TextView emptyText;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookmark);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookmark_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView bookmarkList = findViewById(R.id.bookmark_list);
        emptyText = findViewById(R.id.bookmark_empty);
        swipeRefresh = findViewById(R.id.bookmark_swipe_refresh);
        findViewById(R.id.bookmark_back).setOnClickListener(v -> finish());

        adapter = new BookmarkListAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(WebViewActivity.EXTRA_URL, item.url);
            intent.putExtra(WebViewActivity.EXTRA_TITLE, item.title);
            startActivity(intent);
        });
        bookmarkList.setAdapter(adapter);

        // 左滑删除
        ColorDrawable deleteBg = new ColorDrawable(
                ContextCompat.getColor(this, R.color.status_error));
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                BookmarkEntity item = adapter.getItem(pos);
                if (item != null) {
                    new Thread(() -> {
                        AppDatabase.getInstance(BookmarkActivity.this).bookmarkDao().delete(item.url);
                        runOnUiThread(BookmarkActivity.this::loadBookmarks);
                    }).start();
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView rv, RecyclerView.ViewHolder vh,
                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                ViewCompat.setTranslationX(vh.itemView, dX);
                if (dX < 0) {
                    deleteBg.setBounds(vh.itemView.getRight() + (int) dX,
                            vh.itemView.getTop(), vh.itemView.getRight(), vh.itemView.getBottom());
                    deleteBg.draw(c);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(bookmarkList);

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(this::loadBookmarks);

        loadBookmarks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookmarks();
    }

    private void loadBookmarks() {
        new Thread(() -> {
            List<BookmarkEntity> items = AppDatabase.getInstance(this).bookmarkDao().getAllBookmarks();
            runOnUiThread(() -> {
                adapter.setItems(items);
                emptyText.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                swipeRefresh.setRefreshing(false);
            });
        }).start();
    }
}
