package com.deepnews.app.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.deepnews.app.leancloud.LocalAuthManager;
import com.deepnews.app.util.AppExecutors;
import com.deepnews.app.util.PrefsKeys;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(PrefsKeys.FILE, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public static AppExecutors provideAppExecutors() {
        return AppExecutors.getInstance();
    }

    @Provides
    @Singleton
    public static LocalAuthManager provideLocalAuthManager() {
        return LocalAuthManager.getInstance();
    }

    @Provides
    @Singleton
    public static Gson provideGson() {
        return new Gson();
    }
}
