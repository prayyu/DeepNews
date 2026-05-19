package com.deepnews.app.di;

import android.content.Context;

import com.deepnews.app.data.AppDatabase;
import com.deepnews.app.data.BookmarkDao;
import com.deepnews.app.data.EventDao;
import com.deepnews.app.data.NewsDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    public static NewsDao provideNewsDao(AppDatabase db) {
        return db.newsDao();
    }

    @Provides
    public static EventDao provideEventDao(AppDatabase db) {
        return db.eventDao();
    }

    @Provides
    public static BookmarkDao provideBookmarkDao(AppDatabase db) {
        return db.bookmarkDao();
    }
}
