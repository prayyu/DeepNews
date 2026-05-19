package com.deepnews.app.di;

import com.deepnews.app.data.AppDatabase;
import com.deepnews.app.data.NewsRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public static NewsRepository provideNewsRepository(AppDatabase database) {
        return new NewsRepository(database);
    }
}
