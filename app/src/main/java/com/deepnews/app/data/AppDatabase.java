package com.deepnews.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.deepnews.app.data.entity.ArticleEntity;
import com.deepnews.app.data.entity.BookmarkEntity;
import com.deepnews.app.data.entity.EventEntity;

@Database(entities = {ArticleEntity.class, EventEntity.class, BookmarkEntity.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract NewsDao newsDao();
    public abstract EventDao eventDao();
    public abstract BookmarkDao bookmarkDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "deepnews_db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
