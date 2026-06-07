package com.anvexgroup.apnuvyapar.core;

import android.content.Context;
import android.content.SharedPreferences;

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
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /* ----------------------------- Auth tokens ----------------------------- */

    public void saveTokens(String access, String refresh, int userId) {
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, access)
                .putString(KEY_REFRESH_TOKEN, refresh)
                .putInt(KEY_USER_ID, userId)
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    public void saveTokens(String access, String refresh, int userId, long accessExpiryEpoch) {
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, access)
                .putString(KEY_REFRESH_TOKEN, refresh)
                .putInt(KEY_USER_ID, userId)
                .putLong(KEY_ACCESS_EXPIRY_EPOCH, accessExpiryEpoch)
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    public void saveAccessToken(String access, long accessExpiryEpoch) {
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, access)
                .putLong(KEY_ACCESS_EXPIRY_EPOCH, accessExpiryEpoch)
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
        return sp.getLong(KEY_ACCESS_EXPIRY_EPOCH, 0L);
    }

    public int getUserId() {
        return sp.getInt(KEY_USER_ID, -1);
    }

    public boolean hasValidAccessToken() {
        String token = getAccessToken();
        long expAt = getAccessExpiryEpoch();
        long now = nowEpochSeconds();

        return token != null
                && !token.trim().isEmpty()
                && expAt > now + 15L;
    }

    public boolean hasRefreshToken() {
        String refresh = getRefreshToken();
        return refresh != null && !refresh.trim().isEmpty();
    }

    public void clearTokens() {
        sp.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRY_EPOCH)
                .remove(KEY_USER_ID)
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

    /* ------------------------- 30 days inactivity -------------------------- */

    public void markActive() {
        sp.edit()
                .putLong(KEY_LAST_ACTIVE_EPOCH, nowEpochSeconds())
                .apply();
    }

    public long getLastActiveEpoch() {
        return sp.getLong(KEY_LAST_ACTIVE_EPOCH, 0L);
    }

    public boolean isInactiveFor30Days() {
        if (!isLoggedIn()) {
            return false;
        }

        long lastActive = getLastActiveEpoch();

        /*
         * Existing installed users may not have this value yet.
         * Do not logout them instantly. Start tracking from current open.
         */
        if (lastActive <= 0L) {
            markActive();
            return false;
        }

        long now = nowEpochSeconds();
        return now - lastActive >= INACTIVITY_LOGOUT_SECONDS;
    }

    public long getInactiveDays() {
        long lastActive = getLastActiveEpoch();

        if (lastActive <= 0L) {
            return 0L;
        }

        long diff = nowEpochSeconds() - lastActive;

        if (diff <= 0L) {
            return 0L;
        }

        return diff / (24L * 60L * 60L);
    }

    private long nowEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    /* --------------------------- User profile cache ------------------------- */

    public void saveUserProfile(String name, String phone) {
        sp.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, phone)
                .apply();
    }

    public String getCachedUserName() {
        return sp.getString(KEY_USER_NAME, "User");
    }

    public String getCachedUserPhone() {
        return sp.getString(KEY_USER_PHONE, "");
    }

    /* ------------------------------ Onboarding ----------------------------- */

    public void setOnboarded(boolean v) {
        sp.edit().putBoolean(KEY_ONBOARDED, v).apply();
    }

    public boolean isOnboarded() {
        return sp.getBoolean(KEY_ONBOARDED, false);
    }

    public void setLoggedIn(boolean v) {
        sp.edit().putBoolean(KEY_LOGGED_IN, v).apply();
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }

    /* -------------------------------- Language ----------------------------- */

    public void setLang(String code, String name) {
        sp.edit()
                .putString(KEY_LANG_CODE, code)
                .putString(KEY_LANG_NAME, name)
                .apply();
    }

    public String getLangName() {
        return sp.getString(KEY_LANG_NAME, "English");
    }

    public String getLangCode() {
        return sp.getString(KEY_LANG_CODE, null);
    }
}