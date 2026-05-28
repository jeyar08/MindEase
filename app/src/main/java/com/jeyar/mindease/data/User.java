package com.jeyar.mindease.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String email;
    public String passwordHash;
    public String salt;

    public User(String email, String passwordHash, String salt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }
}
