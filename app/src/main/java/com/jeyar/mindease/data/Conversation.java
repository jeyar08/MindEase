package com.jeyar.mindease.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations", indices = {@Index("userId")})
public class Conversation {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String title;
    public long createdAt;
    public long updatedAt;

    public Conversation(long userId, String title, long createdAt, long updatedAt) {
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
