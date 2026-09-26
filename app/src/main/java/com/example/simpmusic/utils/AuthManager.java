package com.example.simpmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "AUTH";
    private static final String KEY_TOKEN = "TOKEN";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_EMAIL = "USER_EMAIL";
    private static final String KEY_USERNAME = "USER_NAME";
    private static final String KEY_ROLE = "USER_ROLE";

    private final SharedPreferences prefs;
    private static AuthManager instance;

    private AuthManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public void saveAuthData(String token, int userId, String email, String username, String role) {
        // Sử dụng commit() để lưu đồng bộ, đảm bảo Token có hiệu lực ngay lập tức
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .commit(); 
    }

    public void setRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).commit();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, null); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, null); }
    public String getRole() { return prefs.getString(KEY_ROLE, ""); }
    public boolean isLoggedIn() { return getToken() != null; }

    public void clear() {
        prefs.edit().clear().commit();
    }
}
