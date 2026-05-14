package com.example.myapplication.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApi {

    @GET("v2/top-headlines")
    Call<NewsResponse> getTopHeadlines(
            @Query("country") String country,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );

    @GET("v2/everything")
    Call<NewsResponse> getEverything(
            @Query("q") String keyword,
            @Query("pageSize") int pageSize,
            @Query("language") String language,
            @Query("apiKey") String apiKey
    );
}
