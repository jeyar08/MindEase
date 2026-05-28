package com.jeyar.mindease.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "mindease_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private static final String KEY_GEMINI_MODEL = "gemini_model";
    private static final String KEY_CUSTOM_API_KEY = "custom_api_key";

    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String email) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getUserId() != -1L;
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .apply();
    }

    public boolean isDisclaimerShown() {
        return prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
    }

    public void setDisclaimerShown() {
        prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply();
    }


    public String getGeminiModel() {
        return prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL);
    }

    public void setGeminiModel(String model) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model).apply();
    }

    public String getCustomApiKey() {
        return prefs.getString(KEY_CUSTOM_API_KEY, "");
    }

    public void setCustomApiKey(String key) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key == null ? "" : key.trim()).apply();
    }
}
