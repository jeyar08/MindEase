package com.jeyar.mindease.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(Message message);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    List<Message> getForConversation(long conversationId);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteForConversation(long conversationId);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND id > :afterId")
    void deleteAfterMessageId(long conversationId, long afterId);

    @Query("UPDATE messages SET text = :newText WHERE id = :messageId")
    void updateText(long messageId, String newText);
}
