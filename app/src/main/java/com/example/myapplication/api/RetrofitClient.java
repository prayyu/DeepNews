package com.example.myapplication.api;

import com.example.myapplication.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String NEWS_API_BASE_URL = "https://newsapi.org/";
    private static NewsApi newsApi;

    public static NewsApi getNewsApi() {
        if (newsApi == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.HEADERS
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(NEWS_API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            newsApi = retrofit.create(NewsApi.class);
        }
        return newsApi;
    }

    public static String getNewsApiKey() {
        return BuildConfig.NEWS_API_KEY;
    }

    /** 检查 API Key 格式是否合法 */
    public static boolean isNewsApiKeyValid() {
        String key = getNewsApiKey();
        return key != null && !key.isEmpty() && !key.contains("YOUR_") && key.length() >= 20;
    }
}
