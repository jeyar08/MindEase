package com.jeyar.mindease.repository;

public interface RepoCallback<T> {
    void onSuccess(T result);

    void onError(String message);
}
