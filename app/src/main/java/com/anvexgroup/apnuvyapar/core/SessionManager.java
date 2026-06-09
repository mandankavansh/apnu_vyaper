package com.anvexgroup.apnuvyapar.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class SessionManager {

    public static final String PREFS = "apnuvyapar_prefs";

    public static final String KEY_LANG_CODE = "app_lang_code";
    public static final String KEY_LANG_NAME = "app_lang_name";
    public static final String KEY_ONBOARDED = "onboarding_done";
    public static final String KEY_LOGGED_IN = "logged_in";

    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_ACCESS_EXPIRY_EPOCH = "access_expiry_epoch";

    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_PHONE = "user_phone";

    public static final String KEY_LAST_ACTIVE_EPOCH = "last_active_epoch";

    public static final long INACTIVITY_LOGOUT_DAYS = 30L;
    public static final long INACTIVITY_LOGOUT_SECONDS =
            INACTIVITY_LOGOUT_DAYS * 24L * 60L * 60L;

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveTokens(String access, String refresh, int userId) {
        saveTokens(access, refresh, userId, 0L);
    }

    public void saveTokens(String access, String refresh, int userId, long accessExpiryEpoch) {
        SharedPreferences.Editor editor = sp.edit()
                .putString(KEY_ACCESS_TOKEN, safe(access))
                .putString(KEY_REFRESH_TOKEN, safe(refresh))
                .putLong(KEY_USER_ID, userId)
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds());

        if (accessExpiryEpoch > 0L) {
            editor.putLong(KEY_ACCESS_EXPIRY_EPOCH, accessExpiryEpoch);
        } else {
            editor.remove(KEY_ACCESS_EXPIRY_EPOCH);
        }

        editor.apply();
    }

    public void saveAccessToken(String access, long accessExpiryEpoch) {
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, safe(access))
                .putLong(KEY_ACCESS_EXPIRY_EPOCH, Math.max(0L, accessExpiryEpoch))
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    public void saveRefreshToken(String refresh) {
        sp.edit()
                .putString(KEY_REFRESH_TOKEN, safe(refresh))
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    @Nullable
    public String getAccessToken() {
        return sp.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        return sp.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getAccessExpiryEpoch() {
        return readLong(KEY_ACCESS_EXPIRY_EPOCH, 0L);
    }

    public int getUserId() {
        long userId = readLong(KEY_USER_ID, -1L);

        if (userId > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (userId < Integer.MIN_VALUE) {
            return -1;
        }

        return (int) userId;
    }

    public boolean hasValidAccessToken() {
        String token = getAccessToken();
        long expAt = getAccessExpiryEpoch();
        long now = nowEpochSeconds();

        return !TextUtils.isEmpty(token)
                && expAt > now + 15L;
    }

    public boolean hasRefreshToken() {
        return !TextUtils.isEmpty(getRefreshToken());
    }

    public boolean hasAnyAuthToken() {
        return !TextUtils.isEmpty(getAccessToken()) || !TextUtils.isEmpty(getRefreshToken());
    }

    public void clearTokens() {
        sp.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRY_EPOCH)
                .remove(KEY_USER_ID)
                .putBoolean(KEY_LOGGED_IN, false)
                .apply();
    }

    public void logout() {
        sp.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRY_EPOCH)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_PHONE)
                .remove(KEY_LAST_ACTIVE_EPOCH)
                .putBoolean(KEY_LOGGED_IN, false)
                .apply();
    }

    public void markActive() {
        sp.edit()
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    public long getLastActiveEpoch() {
        return readLong(KEY_LAST_ACTIVE_EPOCH, 0L);
    }

    public boolean isInactiveFor30Days() {
        if (!isLoggedIn() && !hasAnyAuthToken()) {
            return false;
        }

        long lastActive = getLastActiveEpoch();

        if (lastActive <= 0L) {
            markActive();
            return false;
        }

        long now = nowEpochSeconds();
        return now - lastActive >= INACTIVITY_LOGOUT_SECONDS;
    }

    public long getInactiveDays() {
        long lastActive = getLastActiveEpoch();
        if (lastActive <= 0L) return 0L;

        long diff = nowEpochSeconds() - lastActive;
        if (diff <= 0L) return 0L;

        return diff / (24L * 60L * 60L);
    }

    public void saveUserProfile(String name, String phone) {
        sp.edit()
                .putString(KEY_USER_NAME, safe(name))
                .putString(KEY_USER_PHONE, safe(phone))
                .apply();
    }

    public String getCachedUserName() {
        String name = sp.getString(KEY_USER_NAME, "User");
        if (TextUtils.isEmpty(name)) return "User";
        return name;
    }

    public String getCachedUserPhone() {
        String phone = sp.getString(KEY_USER_PHONE, "");
        if (phone == null) return "";
        return phone;
    }

    public void setOnboarded(boolean v) {
        sp.edit().putBoolean(KEY_ONBOARDED, v).apply();
    }

    public boolean isOnboarded() {
        return sp.getBoolean(KEY_ONBOARDED, false);
    }

    public void setLoggedIn(boolean v) {
        SharedPreferences.Editor editor = sp.edit().putBoolean(KEY_LOGGED_IN, v);

        if (v) {
            editor.putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds());
        }

        editor.apply();
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLang(String code, String name) {
        sp.edit()
                .putString(KEY_LANG_CODE, TextUtils.isEmpty(code) ? "en" : code.trim())
                .putString(KEY_LANG_NAME, TextUtils.isEmpty(name) ? "English" : name.trim())
                .apply();
    }

    public String getLangName() {
        String langName = sp.getString(KEY_LANG_NAME, "English");
        if (TextUtils.isEmpty(langName)) return "English";
        return langName;
    }

    public String getLangCode() {
        String langCode = sp.getString(KEY_LANG_CODE, "en");
        if (TextUtils.isEmpty(langCode)) return "en";
        return langCode;
    }

    public boolean hasSelectedLanguage() {
        String langCode = sp.getString(KEY_LANG_CODE, null);
        return !TextUtils.isEmpty(langCode);
    }

    private long nowEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private long readLong(String key, long defaultValue) {
        Object value = sp.getAll().get(key);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (Exception ignored) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.trim();
    }
}
