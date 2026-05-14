package com.example.myapplication.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookmarkEntity {
    @PrimaryKey
    @NonNull
    public String url;

    public String title;
    public String source;
    public long timestamp;
}
