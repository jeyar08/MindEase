package com.jeyar.mindease.repository;

public interface GeminiCallback {
    void onSuccess(String reply);

    void onError(String message);
}
