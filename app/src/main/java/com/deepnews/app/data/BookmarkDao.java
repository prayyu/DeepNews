package com.deepnews.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.deepnews.app.data.entity.BookmarkEntity;

import java.util.List;

@Dao
public interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks WHERE url = :url")
    void delete(String url);

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    List<BookmarkEntity> getAllBookmarks();

    @Query("SELECT COUNT(*) FROM bookmarks WHERE url = :url")
    int isBookmarked(String url);
}
