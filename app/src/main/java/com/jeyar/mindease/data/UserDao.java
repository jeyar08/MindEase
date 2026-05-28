package com.jeyar.mindease.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getByEmail(String email);
}
