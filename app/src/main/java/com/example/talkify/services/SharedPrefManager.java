package com.example.talkify.services;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "TalkifyPrefs";
    private static SharedPrefManager instance;
    private SharedPreferences prefs;


    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    // =======================================
    // ==== THÊM CÁC KEY MỚI ====
    // =======================================
    private static final String KEY_FULL_NAME = "user_full_name";
    private static final String KEY_AVATAR_URL = "user_avatar_url";
    private static final String KEY_USER_NAME = "user_user_name";
    private static final String KEY_BIO = "user_bio";
    private static final String KEY_DOB = "user_dob";
    private static final String KEY_GENDER = "user_gender";

    private SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Dùng hàm này khi Login thành công
     */
    public void saveUserSession(String userId, String email, String token, String refreshToken, String fullName, String avatarUrl) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken); // Lưu Refresh Token
        editor.putString(KEY_FULL_NAME, fullName); // <-- Lưu FullName
        editor.putString(KEY_AVATAR_URL, avatarUrl); // <-- Lưu Avatar
        editor.apply();
    }


    // User ID
    public void saveUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    // Email
    public void saveEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    // Token
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // RefreshToken
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    // Hàm lưu Refresh Token mới khi được cấp lại
    public void saveRefreshToken(String refreshToken) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    // Login status
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    // ===========================================
    // ==== CÁC HÀM MỚI CHO SETTINGSFRAGMENT ====
    // ===========================================

    // Full Name
    public void saveUserFullName(String fullName) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply();
    }
    public String getUserFullName() {
        return prefs.getString(KEY_FULL_NAME, null); // (File SettingsFragment đã gọi hàm này)
    }

    // Avatar URL
    public void saveUserAvatarUrl(String avatarUrl) {
        prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
    }
    public String getUserAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, null); // (File SettingsFragment đã gọi hàm này)
    }

    public void saveUserName(String userName) {
        prefs.edit().putString(KEY_USER_NAME, userName).apply();
    }
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    // Bio
    public void saveUserBio(String bio) {
        prefs.edit().putString(KEY_BIO, bio).apply();
    }

    public void clearUserSession() {
        prefs.edit().clear().apply();
    }
}