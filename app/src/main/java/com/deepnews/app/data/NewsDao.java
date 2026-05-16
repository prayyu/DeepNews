package com.deepnews.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.deepnews.app.data.entity.ArticleEntity;

import java.util.List;

@Dao
public interface NewsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ArticleEntity> articles);

    @Query("SELECT * FROM articles ORDER BY published_at DESC")
    List<ArticleEntity> getAllArticles();

    @Query("SELECT * FROM articles WHERE event_title = :eventTitle")
    List<ArticleEntity> getArticlesByEvent(String eventTitle);

    @Query("SELECT * FROM articles WHERE title LIKE :query OR source LIKE :query")
    List<ArticleEntity> searchArticles(String query);

    @Query("DELETE FROM articles")
    void clearAll();
}
