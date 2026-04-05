package com.anvexgroup.sheharsetu;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.sheharsetu.core.SessionManager;
import com.anvexgroup.sheharsetu.net.ApiRoutes;
import com.anvexgroup.sheharsetu.net.VolleySingleton;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";

    private static final String CHANNEL_ID = "sheharsetu_general_notifications";
    private static final String CHANNEL_NAME = "General Notifications";
    private static final String CHANNEL_DESC = "App alerts and updates";

    private static final String PREFS_FCM = "fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_LAST_UPLOADED_TOKEN = "last_uploaded_fcm_token";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            syncSavedTokenIfPossible();
        } catch (Exception e) {
            Log.e(TAG, "syncSavedTokenIfPossible() failed in onCreate", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d(TAG, "NEW_FCM_TOKEN = " + token);

        try {
            saveTokenLocally(token);
            uploadTokenToServerIfPossible(token);
        } catch (Exception e) {
            Log.e(TAG, "onNewToken handling failed", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        try {
            Log.d(TAG, "FROM = " + remoteMessage.getFrom());

            String title = null;
            String body = null;

            if (remoteMessage.getNotification() != null) {
                title = remoteMessage.getNotification().getTitle();
                body = remoteMessage.getNotification().getBody();
            }

            Map<String, String> data = remoteMessage.getData();
            if (data != null && !data.isEmpty()) {
                Log.d(TAG, "DATA = " + data.toString());

                if (TextUtils.isEmpty(title)) {
                    title = data.get("title");
                }
                if (TextUtils.isEmpty(body)) {
                    body = data.get("body");
                }
            }

            if (TextUtils.isEmpty(title)) {
                title = getString(R.string.app_name);
            }

            if (TextUtils.isEmpty(body)) {
                body = "You have a new notification";
            }

            showNotification(title, body, data);

        } catch (Exception e) {
            Log.e(TAG, "onMessageReceived failed", e);
        }
    }

    private void syncSavedTokenIfPossible() {
        String savedToken = getSavedToken();
        if (!TextUtils.isEmpty(savedToken)) {
            Log.d(TAG, "Found saved FCM token, trying sync");
            uploadTokenToServerIfPossible(savedToken);
        } else {
            Log.d(TAG, "No saved FCM token found for sync");
        }
    }

    private void saveTokenLocally(@NonNull String token) {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        sp.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    private String getSavedToken() {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        return sp.getString(KEY_FCM_TOKEN, null);
    }

    private String getLastUploadedToken() {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        return sp.getString(KEY_LAST_UPLOADED_TOKEN, null);
    }

    private void setLastUploadedToken(@NonNull String token) {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_UPLOADED_TOKEN, token).apply();
    }

    private void uploadTokenToServerIfPossible(@NonNull String token) {
        if (TextUtils.isEmpty(token)) {
            Log.w(TAG, "uploadTokenToServerIfPossible: token empty");
            return;
        }

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        if (TextUtils.isEmpty(accessToken)) {
            Log.d(TAG, "Access token missing, FCM token saved locally for later upload");
            return;
        }

        String alreadyUploaded = getLastUploadedToken();
        if (token.equals(alreadyUploaded)) {
            Log.d(TAG, "FCM token already uploaded, skipping");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("token", token);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiRoutes.FCM_TOKEN,
                    body,
                    response -> {
                        Log.d(TAG, "FCM token upload success: " + response);
                        setLastUploadedToken(token);
                    },
                    error -> Log.e(TAG, "FCM token upload failed: " + buildVolleyError(error), error)
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + accessToken);
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };

            req.setShouldCache(false);
            req.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(this).add(req);

        } catch (Exception e) {
            Log.e(TAG, "Failed to build/save FCM upload request", e);
        }
    }

    private void showNotification(@NonNull String title, @NonNull String body, Map<String, String> data) {
        try {
            createNotificationChannel();

            // Resolve tap destination
            // If data carries target_screen + target_id → route to correct screen.
            // Otherwise → open NotificationsActivity so user can see their inbox.
            Intent tapIntent = buildTapIntent(data);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    (int) System.currentTimeMillis(),
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                    return;
                }
            }

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        } catch (Exception e) {
            Log.e(TAG, "showNotification failed", e);
        }
    }

    /**
     * Builds the Intent to open when a push notification is tapped.
     * Routes to the specific target screen when target_screen and target_id are present.
     * Falls back to NotificationsActivity otherwise.
     */
    private Intent buildTapIntent(Map<String, String> data) {
        String targetScreen = data != null ? data.get("target_screen") : null;
        String targetId     = data != null ? data.get("target_id")     : null;

        if (!TextUtils.isEmpty(targetScreen) && !TextUtils.isEmpty(targetId)) {
            switch (targetScreen.toLowerCase(java.util.Locale.ROOT)) {
                case "product_detail":
                case "listing_detail":
                case "listing": {
                    try {
                        int listingId = Integer.parseInt(targetId.trim());
                        if (listingId > 0) {
                            Intent i = new Intent(this, ProductDetail.class);
                            i.putExtra("listing_id", listingId);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            return i;
                        }
                    } catch (NumberFormatException ignored) {}
                    break;
                }
                default:
                    break;
            }
        }

        // Default: open notifications inbox
        Intent i = new Intent(this, NotificationsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Pass through any target extras so NotificationsActivity can act on them
        if (!TextUtils.isEmpty(targetScreen)) {
            i.putExtra(NotificationsActivity.EXTRA_TARGET_SCREEN, targetScreen);
        }
        if (!TextUtils.isEmpty(targetId)) {
            i.putExtra(NotificationsActivity.EXTRA_TARGET_ID, targetId);
        }
        return i;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.w(TAG, "NotificationManager is null");
            return;
        }

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESC);

        manager.createNotificationChannel(channel);
    }

    private String buildVolleyError(VolleyError err) {
        if (err == null) return "VolleyError=null";

        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(err.getClass().getSimpleName());

        if (err.getCause() != null) {
            sb.append(" cause=")
                    .append(err.getCause().getClass().getSimpleName())
                    .append(":")
                    .append(err.getCause().getMessage());
        }

        try {
            if (err.networkResponse != null) {
                sb.append(" status=").append(err.networkResponse.statusCode);
                if (err.networkResponse.data != null) {
                    String body = new String(err.networkResponse.data).trim();
                    if (body.length() > 500) {
                        body = body.substring(0, 500) + "...";
                    }
                    sb.append(" body=").append(body);
                }
            } else {
                sb.append(" networkResponse=null");
            }
        } catch (Exception ignored) {
        }

        return sb.toString();
    }
}