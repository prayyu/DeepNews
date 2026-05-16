package com.deepnews.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.deepnews.app.data.entity.EventEntity;

import java.util.List;

@Dao
public interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventEntity> events);

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    List<EventEntity> getAllEvents();

    @Query("DELETE FROM events")
    void clearAll();
}
