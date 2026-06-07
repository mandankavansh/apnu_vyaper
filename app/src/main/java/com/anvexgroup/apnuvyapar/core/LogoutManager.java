package com.anvexgroup.apnuvyapar.core;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.apnuvyapar.LoginActivity;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class LogoutManager {

    private static final String TAG = "LogoutManager";

    private static final int TIMEOUT_MS = 12000;
    private static final int MAX_RETRIES = 0;

    private LogoutManager() {
    }

    public interface Callback {
        void onComplete();
    }

    public static void logout(@NonNull Context context) {
        logout(context, false, null);
    }

    public static void logout(
            @NonNull Context context,
            boolean allDevices,
            @Nullable Callback callback
    ) {
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);

        String accessToken = session.getAccessToken();
        String refreshToken = session.getRefreshToken();

        /*
         * Important:
         * Clear local session immediately.
         * Backend revoke is best-effort.
         */
        session.logout();

        if (TextUtils.isEmpty(refreshToken)) {
            notifyComplete(callback);
            return;
        }

        JSONObject body = new JSONObject();

        try {
            body.put("refresh_token", refreshToken);
            body.put("all_devices", allDevices);
        } catch (JSONException e) {
            Log.w(TAG, "Could not prepare logout body", e);
            notifyComplete(callback);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.LOGOUT,
                body,
                response -> notifyComplete(callback),
                error -> {
                    /*
                     * Logout should never block user because local session
                     * is already cleared.
                     */
                    Log.w(TAG, "Logout API failed, local session already cleared", error);
                    notifyComplete(callback);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");

                if (!TextUtils.isEmpty(accessToken)) {
                    headers.put("Authorization", "Bearer " + accessToken);
                }

                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                1.0f
        ));

        VolleySingleton.getInstance(appContext).add(request);
    }

    public static void logoutAndGoToLogin(@NonNull Context context) {
        logoutAndGoToLogin(context, false);
    }

    public static void logoutAndGoToLogin(
            @NonNull Context context,
            boolean allDevices
    ) {
        logout(context, allDevices, () -> openLogin(context));
    }

    private static void openLogin(@NonNull Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private static void notifyComplete(@Nullable Callback callback) {
        if (callback != null) {
            callback.onComplete();
        }
    }
}