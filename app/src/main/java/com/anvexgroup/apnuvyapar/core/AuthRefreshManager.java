package com.anvexgroup.apnuvyapar.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class AuthRefreshManager {

    private static final String TAG = "AuthRefreshManager";

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_RETRIES = 1;

    private AuthRefreshManager() {
    }

    public interface Callback {
        void onSuccess();

        void onFailure(@NonNull String message, boolean shouldLogout);
    }

    public static void ensureValidSession(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);

        if (!session.isLoggedIn()
                && !session.hasValidAccessToken()
                && !session.hasRefreshToken()) {
            session.logout();
            callback.onFailure("Login session not found.", true);
            return;
        }

        if (session.isInactiveFor30Days()) {
            session.logout();
            callback.onFailure("Logged out because account was inactive for 30 days.", true);
            return;
        }

        if (session.hasValidAccessToken()) {
            session.markActive();
            callback.onSuccess();
            return;
        }

        if (session.hasRefreshToken()) {
            refreshAccessToken(appContext, callback);
            return;
        }

        session.logout();
        callback.onFailure("Login session expired. Please login again.", true);
    }

    public static void refreshAccessToken(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);

        String refreshToken = session.getRefreshToken();

        if (TextUtils.isEmpty(refreshToken)) {
            session.logout();
            callback.onFailure("Refresh token not found.", true);
            return;
        }

        JSONObject body = new JSONObject();

        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            callback.onFailure("Could not prepare refresh request.", false);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.REFRESH_TOKEN,
                body,
                response -> handleRefreshResponse(session, response, callback),
                error -> handleRefreshError(session, error, callback)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                1.2f
        ));

        VolleySingleton.getInstance(appContext).add(request);
    }

    private static void handleRefreshResponse(
            @NonNull SessionManager session,
            @Nullable JSONObject response,
            @NonNull Callback callback
    ) {
        if (response == null) {
            callback.onFailure("Empty refresh response.", false);
            return;
        }

        boolean ok = response.optBoolean("ok", response.optBoolean("success", false));

        if (!ok) {
            String errorCode = response.optString("error_code", "");
            String error = response.optString("error", response.optString("message", "Session refresh failed."));
            boolean shouldLogout = isLogoutError(errorCode);

            if (shouldLogout) {
                session.logout();
            }

            callback.onFailure(error, shouldLogout);
            return;
        }

        String accessToken = response.optString("access_token", "").trim();
        String refreshToken = response.optString("refresh_token", "").trim();
        int expiresIn = response.optInt("expires_in", 0);
        int userId = response.optInt("user_id", session.getUserId());

        if (TextUtils.isEmpty(accessToken) || expiresIn <= 0) {
            callback.onFailure("Invalid refresh response.", false);
            return;
        }

        long accessExpiryEpoch = (System.currentTimeMillis() / 1000L) + expiresIn;

        if (!TextUtils.isEmpty(refreshToken)) {
            session.saveTokens(accessToken, refreshToken, userId, accessExpiryEpoch);
        } else {
            session.saveAccessToken(accessToken, accessExpiryEpoch);
        }

        session.setLoggedIn(true);
        session.markActive();

        JSONObject user = response.optJSONObject("user");
        saveUserCache(session, user);

        callback.onSuccess();
    }

    private static void handleRefreshError(
            @NonNull SessionManager session,
            @NonNull VolleyError error,
            @NonNull Callback callback
    ) {
        int statusCode = -1;

        if (error.networkResponse != null) {
            statusCode = error.networkResponse.statusCode;
        }

        String message = "Could not refresh session.";
        String errorCode = "";

        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);

                String apiError = json.optString("error", json.optString("message", ""));
                if (!TextUtils.isEmpty(apiError)) {
                    message = apiError;
                }

                errorCode = json.optString("error_code", "");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse refresh error body", e);
        }

        if (isLogoutError(errorCode) || statusCode == 401 || statusCode == 403) {
            session.logout();
            callback.onFailure(message, true);
            return;
        }

        callback.onFailure(message, false);
    }

    private static void saveUserCache(
            @NonNull SessionManager session,
            @Nullable JSONObject user
    ) {
        if (user == null) {
            return;
        }

        String name = user.optString("full_name", "").trim();
        if (TextUtils.isEmpty(name)) {
            name = user.optString("name", "").trim();
        }

        String phone = user.optString("phone", "").trim();

        if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(phone)) {
            session.saveUserProfile(name, phone);
        }
    }

    private static boolean isLogoutError(@Nullable String errorCode) {
        if (TextUtils.isEmpty(errorCode)) {
            return false;
        }

        return "INACTIVE_30_DAYS".equalsIgnoreCase(errorCode)
                || "SESSION_EXPIRED".equalsIgnoreCase(errorCode)
                || "SESSION_REVOKED".equalsIgnoreCase(errorCode)
                || "INVALID_REFRESH_TOKEN".equalsIgnoreCase(errorCode)
                || "REFRESH_TOKEN_REQUIRED".equalsIgnoreCase(errorCode)
                || "INVALID_TOKEN".equalsIgnoreCase(errorCode)
                || "ACCOUNT_DELETED".equalsIgnoreCase(errorCode)
                || "ACCOUNT_BLOCKED".equalsIgnoreCase(errorCode)
                || "ACCOUNT_INACTIVE".equalsIgnoreCase(errorCode)
                || "ACCOUNT_NOT_ACTIVE".equalsIgnoreCase(errorCode);
    }
}
