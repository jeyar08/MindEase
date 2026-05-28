package com.jeyar.mindease.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jeyar.mindease.data.Conversation;
import com.jeyar.mindease.data.Message;
import com.jeyar.mindease.repository.GeminiCallback;
import com.jeyar.mindease.repository.MindEaseRepository;
import com.jeyar.mindease.repository.RepoCallback;
import com.jeyar.mindease.util.SafetyConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private static final long NO_CONVERSATION = -1L;

    private final MindEaseRepository repository;

    private boolean initialized;
    private long userId;
    private long conversationId = NO_CONVERSATION;

    private final List<Message> history = new ArrayList<>();

    private final Deque<String> messageQueue = new ArrayDeque<>();

    private final MutableLiveData<List<Message>> messages  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>        loading   = new MutableLiveData<>(false);
    private final MutableLiveData<String>         title     = new MutableLiveData<>();
    private final MutableLiveData<String>         error     = new MutableLiveData<>();
    private final MutableLiveData<Integer>        queueCount = new MutableLiveData<>(0);

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = MindEaseRepository.getInstance(application);
    }

    public void init(long userId, long conversationId) {
        if (initialized) return;
        initialized = true;
        this.userId = userId;
        this.conversationId = conversationId;

        if (conversationId != NO_CONVERSATION) {
            repository.getConversation(conversationId, new RepoCallback<Conversation>() {
                @Override public void onSuccess(Conversation result) {
                    if (result != null) title.setValue(result.title);
                }
                @Override public void onError(String message) {  }
            });
            repository.getMessages(conversationId, new RepoCallback<List<Message>>() {
                @Override public void onSuccess(List<Message> result) {
                    history.clear();
                    history.addAll(result);
                    messages.setValue(new ArrayList<>(history));
                }
                @Override public void onError(String message) { error.setValue(message); }
            });
        }
    }


    public LiveData<List<Message>>  getMessages()   { return messages; }
    public LiveData<Boolean>        getLoading()     { return loading; }
    public LiveData<String>         getTitle()       { return title; }
    public LiveData<String>         getError()       { return error; }
    public LiveData<Integer>        getQueueCount()  { return queueCount; }

    public void clearError() { error.setValue(null); }


    public void sendMessage(String raw) {
        final String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return;

        if (Boolean.TRUE.equals(loading.getValue())) {
            messageQueue.addLast(text);
            queueCount.setValue(messageQueue.size());
            return;
        }

        dispatchMessage(text);
    }

    public void clearQueue() {
        messageQueue.clear();
        queueCount.setValue(0);
    }


    public void retry() {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (history.isEmpty() || !"user".equals(history.get(history.size() - 1).role)) return;
        clearQueue();
        loading.setValue(true);
        callGemini();
    }


    public void editMessage(Message target, String newText) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        final String trimmed = newText == null ? "" : newText.trim();
        if (trimmed.isEmpty()) return;

        clearQueue();
        loading.setValue(true);

        repository.deleteMessagesAfter(conversationId, target.id, new RepoCallback<Void>() {
            @Override public void onSuccess(Void r) {
                repository.updateMessage(target.id, trimmed, new RepoCallback<Void>() {
                    @Override public void onSuccess(Void r2) {
                        int idx = indexOfMessage(target.id);
                        if (idx >= 0) {
                            while (history.size() > idx + 1) history.remove(history.size() - 1);
                            history.get(idx).text = trimmed;
                        }
                        messages.setValue(new ArrayList<>(history));
                        callGemini();
                    }
                    @Override public void onError(String msg) { loading.setValue(false); error.setValue(msg); }
                });
            }
            @Override public void onError(String msg) { loading.setValue(false); error.setValue(msg); }
        });
    }

    public void retryFromMessage(Message target) {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        Message userMessage;
        if ("user".equals(target.role)) {
            userMessage = target;
        } else {
            int idx = indexOfMessage(target.id);
            userMessage = null;
            for (int i = (idx >= 0 ? idx : history.size()) - 1; i >= 0; i--) {
                if ("user".equals(history.get(i).role)) { userMessage = history.get(i); break; }
            }
        }
        if (userMessage == null) return;

        clearQueue();
        loading.setValue(true);

        final Message finalUser = userMessage;
        repository.deleteMessagesAfter(conversationId, finalUser.id, new RepoCallback<Void>() {
            @Override public void onSuccess(Void r) {
                int idx = indexOfMessage(finalUser.id);
                if (idx >= 0) while (history.size() > idx + 1) history.remove(history.size() - 1);
                messages.setValue(new ArrayList<>(history));
                callGemini();
            }
            @Override public void onError(String msg) { loading.setValue(false); error.setValue(msg); }
        });
    }


    private void dispatchMessage(String text) {
        loading.setValue(true);

        if (conversationId == NO_CONVERSATION) {
            final String newTitle = deriveTitle(text);
            repository.createConversation(userId, newTitle, new RepoCallback<Long>() {
                @Override public void onSuccess(Long result) {
                    conversationId = result;
                    title.setValue(newTitle);
                    persistUserMessageThenSend(text);
                }
                @Override public void onError(String msg) {
                    loading.setValue(false);
                    error.setValue(msg);
                }
            });
        } else {
            persistUserMessageThenSend(text);
        }
    }

    private void persistUserMessageThenSend(String text) {
        Message userMessage = new Message(conversationId, "user", text, System.currentTimeMillis());
        repository.addMessage(userMessage, new RepoCallback<Message>() {
            @Override public void onSuccess(Message result) {
                history.add(result);
                messages.setValue(new ArrayList<>(history));
                callGemini();
            }
            @Override public void onError(String msg) {
                loading.setValue(false);
                error.setValue(msg);
            }
        });
    }

    private void callGemini() {
        repository.sendToGemini(
            SafetyConstants.SYSTEM_INSTRUCTION,
            new ArrayList<>(history),
            new GeminiCallback() {
                @Override
                public void onSuccess(String reply) {
                    Message modelMessage = new Message(
                            conversationId, "model", reply, System.currentTimeMillis());
                    repository.addMessage(modelMessage, new RepoCallback<Message>() {
                        @Override public void onSuccess(Message result) {
                            history.add(result);
                            messages.setValue(new ArrayList<>(history));
                            repository.touchConversation(conversationId, result.timestamp);

                            String next = messageQueue.pollFirst();
                            if (next != null) {
                                queueCount.setValue(messageQueue.size());
                                persistUserMessageThenSend(next);
                            } else {
                                loading.setValue(false);
                            }
                        }
                        @Override public void onError(String msg) {
                            clearQueue();
                            loading.setValue(false);
                            error.setValue(msg);
                        }
                    });
                }

                @Override
                public void onError(String msg) {
                    clearQueue();
                    loading.setValue(false);
                    error.setValue(msg);
                }
            });
    }

    private int indexOfMessage(long messageId) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).id == messageId) return i;
        }
        return -1;
    }

    private String deriveTitle(String firstMessage) {
        String oneLine = firstMessage.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 40 ? oneLine : oneLine.substring(0, 40).trim() + "…";
    }
}
