package com.jeyar.mindease.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(Conversation conversation);

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    LiveData<List<Conversation>> observeForUser(long userId);

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    Conversation getById(long id);

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    void updateTimestamp(long id, long timestamp);

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(long id);
}
