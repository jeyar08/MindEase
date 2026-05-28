package com.jeyar.mindease.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.jeyar.mindease.data.Conversation;
import com.jeyar.mindease.repository.MindEaseRepository;
import com.jeyar.mindease.repository.RepoCallback;

import java.util.List;

public class ConversationListViewModel extends AndroidViewModel {

    private final MindEaseRepository repository;
    private LiveData<List<Conversation>> conversations;

    public ConversationListViewModel(@NonNull Application application) {
        super(application);
        repository = MindEaseRepository.getInstance(application);
    }

    public void init(long userId) {
        if (conversations == null) {
            conversations = repository.observeConversations(userId);
        }
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void deleteConversation(long conversationId, RepoCallback<Void> cb) {
        repository.deleteConversation(conversationId, cb);
    }
}
