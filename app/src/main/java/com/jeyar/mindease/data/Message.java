package com.jeyar.mindease.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages", indices = {@Index("conversationId")})
public class Message {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long conversationId;
    public String role;
    public String text;
    public long timestamp;

    public Message(long conversationId, String role, String text, long timestamp) {
        this.conversationId = conversationId;
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
    }
}
