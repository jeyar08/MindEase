package com.jeyar.mindease.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.jeyar.mindease.BuildConfig;
import com.jeyar.mindease.data.AppDatabase;
import com.jeyar.mindease.data.Conversation;
import com.jeyar.mindease.data.ConversationDao;
import com.jeyar.mindease.data.Message;
import com.jeyar.mindease.data.MessageDao;
import com.jeyar.mindease.data.User;
import com.jeyar.mindease.data.UserDao;
import com.jeyar.mindease.network.GeminiApi;
import com.jeyar.mindease.network.GeminiRequest;
import com.jeyar.mindease.network.GeminiResponse;
import com.jeyar.mindease.network.RetrofitClient;
import com.jeyar.mindease.util.PasswordHasher;
import com.jeyar.mindease.util.SessionManager;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MindEaseRepository {

    private static final String FALLBACK_REPLY =
            "I'm here with you, but I'm having a little trouble forming a response right now. "
                    + "Could you try rephrasing, or tell me a bit more about what's on your mind?";

    private static volatile MindEaseRepository INSTANCE;

    private final UserDao userDao;
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final ExecutorService io;
    private final Handler main;
    private final GeminiApi geminiApi;
    private final SessionManager session;
    private final String buildApiKey;

    private MindEaseRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        conversationDao = db.conversationDao();
        messageDao = db.messageDao();
        io = Executors.newSingleThreadExecutor();
        main = new Handler(Looper.getMainLooper());
        geminiApi = RetrofitClient.getGeminiApi();
        session = new SessionManager(context);
        buildApiKey = BuildConfig.GEMINI_API_KEY;
    }

    public static MindEaseRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MindEaseRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MindEaseRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }


    public void signUp(String email, String password, RepoCallback<Long> cb) {
        final String normalized = normalize(email);
        io.execute(() -> {
            User existing = userDao.getByEmail(normalized);
            if (existing != null) {
                main.post(() -> cb.onError("An account with this email already exists."));
                return;
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(password, salt);
            long id = userDao.insert(new User(normalized, hash, salt));
            main.post(() -> cb.onSuccess(id));
        });
    }

    public void login(String email, String password, RepoCallback<User> cb) {
        final String normalized = normalize(email);
        io.execute(() -> {
            User user = userDao.getByEmail(normalized);
            if (user == null) {
                main.post(() -> cb.onError("No account found with that email."));
                return;
            }
            String hash = PasswordHasher.hash(password, user.salt);
            if (!PasswordHasher.constantTimeEquals(hash, user.passwordHash)) {
                main.post(() -> cb.onError("Incorrect password. Please try again."));
                return;
            }
            main.post(() -> cb.onSuccess(user));
        });
    }


    public LiveData<List<Conversation>> observeConversations(long userId) {
        return conversationDao.observeForUser(userId);
    }

    public void createConversation(long userId, String title, RepoCallback<Long> cb) {
        io.execute(() -> {
            long now = System.currentTimeMillis();
            long id = conversationDao.insert(new Conversation(userId, title, now, now));
            main.post(() -> cb.onSuccess(id));
        });
    }

    public void getConversation(long id, RepoCallback<Conversation> cb) {
        io.execute(() -> {
            Conversation c = conversationDao.getById(id);
            main.post(() -> cb.onSuccess(c));
        });
    }

    public void touchConversation(long id, long timestamp) {
        io.execute(() -> conversationDao.updateTimestamp(id, timestamp));
    }

    public void deleteConversation(long conversationId, RepoCallback<Void> cb) {
        io.execute(() -> {
            messageDao.deleteForConversation(conversationId);
            conversationDao.deleteById(conversationId);
            main.post(() -> cb.onSuccess(null));
        });
    }


    public void getMessages(long conversationId, RepoCallback<List<Message>> cb) {
        io.execute(() -> {
            List<Message> list = messageDao.getForConversation(conversationId);
            main.post(() -> cb.onSuccess(list));
        });
    }

    public void addMessage(Message message, RepoCallback<Message> cb) {
        io.execute(() -> {
            long id = messageDao.insert(message);
            message.id = id;
            main.post(() -> cb.onSuccess(message));
        });
    }

    public void updateMessage(long messageId, String newText, RepoCallback<Void> cb) {
        io.execute(() -> {
            messageDao.updateText(messageId, newText);
            main.post(() -> cb.onSuccess(null));
        });
    }

    public void deleteMessagesAfter(long conversationId, long afterMessageId, RepoCallback<Void> cb) {
        io.execute(() -> {
            messageDao.deleteAfterMessageId(conversationId, afterMessageId);
            main.post(() -> cb.onSuccess(null));
        });
    }


    public void sendToGemini(String systemInstruction, List<Message> history, GeminiCallback cb) {
        String customKey = session.getCustomApiKey();
        String resolvedKey = (customKey != null && !customKey.isEmpty()) ? customKey : buildApiKey;

        if (resolvedKey == null || resolvedKey.isEmpty()) {
            cb.onError("No Gemini API key found. Add one in Settings or add GEMINI_API_KEY to local.properties and rebuild.");
            return;
        }

        String model = session.getGeminiModel();

        GeminiRequest request = new GeminiRequest();
        request.systemInstruction = new GeminiRequest.Content();
        request.systemInstruction.parts =
                Collections.singletonList(new GeminiRequest.Part(systemInstruction));

        request.contents = new ArrayList<>();
        for (Message m : history) {
            GeminiRequest.Content content = new GeminiRequest.Content();
            content.role = m.role;
            content.parts = Collections.singletonList(new GeminiRequest.Part(m.text));
            request.contents.add(content);
        }

        geminiApi.generateContent(model, resolvedKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(extractText(response.body()));
                    return;
                }
                int code = response.code();
                switch (code) {
                    case 429:
                        cb.onError("MindEase is getting a lot of requests right now. "
                                + "Please wait a moment and try again.");
                        break;
                    case 400:
                        cb.onError("There was a problem with the request. "
                                + "Please check your Gemini API key in Settings.");
                        break;
                    case 401:
                    case 403:
                        cb.onError("Access was denied. Please check that your Gemini API key is valid.");
                        break;
                    default:
                        cb.onError("Something went wrong (error " + code + "). Please try again.");
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    cb.onError("That took longer than expected. Please try again.");
                } else if (t instanceof UnknownHostException || t instanceof ConnectException) {
                    cb.onError("You appear to be offline. Check your connection and try again.");
                } else {
                    cb.onError("Couldn't reach MindEase right now. Please try again.");
                }
            }
        });
    }

    private String extractText(GeminiResponse body) {
        if (body.promptFeedback != null && body.promptFeedback.blockReason != null) {
            return FALLBACK_REPLY;
        }
        if (body.candidates == null || body.candidates.isEmpty()) {
            return FALLBACK_REPLY;
        }
        GeminiResponse.Candidate candidate = body.candidates.get(0);
        if (candidate.content == null
                || candidate.content.parts == null
                || candidate.content.parts.isEmpty()) {
            return FALLBACK_REPLY;
        }
        StringBuilder sb = new StringBuilder();
        for (GeminiResponse.Part part : candidate.content.parts) {
            if (part.text != null) {
                sb.append(part.text);
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? FALLBACK_REPLY : text;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
