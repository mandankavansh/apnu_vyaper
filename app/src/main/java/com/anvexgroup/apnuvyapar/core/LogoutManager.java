package com.anvexgroup.apnuvyapar.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LogoutManager {

    private static final String TAG = "LogoutManager";
    private static final int TIMEOUT_MS = 12000;

    private LogoutManager() {
    }

    public interface Callback {
        void onComplete();
    }

    public static void logout(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        logout(context, false, callback);
    }

    public static void logoutAll(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        logout(context, true, callback);
    }

    private static void logout(
            @NonNull Context context,
            boolean logoutAll,
            @NonNull Callback callback
    ) {
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);

        String accessToken = safe(session.getAccessToken());
        String refreshToken = safe(session.getRefreshToken());

        AtomicBoolean completed = new AtomicBoolean(false);
        Callback finishOnce = () -> {
            if (completed.compareAndSet(false, true)) {
                clearLocalSession(appContext);
                callback.onComplete();
            }
        };

        if (!TextUtils.isEmpty(accessToken) || !TextUtils.isEmpty(refreshToken)) {
            callBackendLogout(appContext, accessToken, refreshToken, logoutAll, finishOnce);
            return;
        }

        finishOnce.onComplete();
    }

    private static void callBackendLogout(
            @NonNull Context context,
            @Nullable String accessToken,
            @Nullable String refreshToken,
            boolean logoutAll,
            @NonNull Callback callback
    ) {
        JSONObject body = new JSONObject();

        try {
            if (!TextUtils.isEmpty(refreshToken)) {
                body.put("refresh_token", refreshToken);
            }

            if (logoutAll) {
                body.put("logout_all", true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not prepare logout request body", e);
            callback.onComplete();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.LOGOUT,
                body,
                response -> callback.onComplete(),
                error -> {
                    Log.w(TAG, "Backend logout failed. Local session will still be cleared.", error);
                    callback.onComplete();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");

                if (!TextUtils.isEmpty(accessToken)) {
                    headers.put("Authorization", "Bearer " + accessToken);
                }

                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                0,
                1.0f
        ));

        VolleySingleton.getInstance(context).add(request);
    }

    private static void clearLocalSession(@NonNull Context context) {
        SessionManager session = new SessionManager(context);
        session.logout();

        context.getSharedPreferences("user", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_uploaded_fcm_token", "")
                .apply();
    }

    private static String safe(@Nullable String value) {
        if (value == null) return "";
        return value.trim();
    }
}
