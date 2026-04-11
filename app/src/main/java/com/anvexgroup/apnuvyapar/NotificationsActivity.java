package com.anvexgroup.apnuvyapar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the user's in-app notification inbox.
 *
 * Supports:
 * - Pull-to-refresh
 * - Mark single notification as read (on tap)
 * - Mark all notifications as read
 * - Deep-link routing when target_screen / target_id exist
 * - Empty state
 *
 * Navigation contract:
 * - Can be started normally from the nav drawer.
 * - Can be started from FCM push tap via Intent extras:
 *     EXTRA_TARGET_SCREEN and EXTRA_TARGET_ID
 */
public class NotificationsActivity extends AppCompatActivity
        implements NotificationsAdapter.OnItemClickListener {

    private static final String TAG = "NotificationsActivity";

    public static final String EXTRA_TARGET_SCREEN = "target_screen";
    public static final String EXTRA_TARGET_ID     = "target_id";

    // Views
    private RecyclerView          rvNotifications;
    private SwipeRefreshLayout    swipeRefresh;
    private View                  layoutEmptyState;
    private TextView              tvUnreadCount;
    private MaterialButton        btnMarkAllRead;

    // Data
    private final List<NotificationItem> notifications = new ArrayList<>();
    private NotificationsAdapter adapter;

    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Apply locale (mirrors MainActivity pattern)
        applySavedLocale();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat wic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (wic != null) {
            wic.setAppearanceLightStatusBars(false);
            wic.setAppearanceLightNavigationBars(false);
        }

        setContentView(R.layout.activity_notifications);
        LanguageManager.enforceLtr(this);
        // System-bar insets (mirrors MainActivity/MyAdsActivity pattern)
        View viewStatusBar = findViewById(R.id.viewStatusBarBackground);
        View viewNavBar    = findViewById(R.id.viewNavBarBackground);
        View root          = findViewById(R.id.rootContainer);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (viewStatusBar != null) {
                    viewStatusBar.getLayoutParams().height = bars.top;
                    viewStatusBar.requestLayout();
                }
                if (viewNavBar != null) {
                    viewNavBar.getLayoutParams().height = bars.bottom;
                    viewNavBar.requestLayout();
                }
                return insets;
            });
        }

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(I18n.t(this, "Notifications"));
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Views
        rvNotifications  = findViewById(R.id.rvNotifications);
        swipeRefresh     = findViewById(R.id.swipeRefresh);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvUnreadCount    = findViewById(R.id.tvUnreadCount);
        btnMarkAllRead   = findViewById(R.id.btnMarkAllRead);

        // Empty state text
        TextView tvEmptyTitle    = findViewById(R.id.tvEmptyTitle);
        TextView tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        if (tvEmptyTitle    != null) tvEmptyTitle.setText(I18n.t(this, "No notifications yet"));
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText(I18n.t(this, "You'll see updates and alerts here"));

        // RecyclerView
        adapter = new NotificationsAdapter(notifications, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        // Swipe to refresh
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
            swipeRefresh.setOnRefreshListener(this::fetchNotifications);
        }

        // Mark all read
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> markAllNotificationsRead());
        }

        // Check if launched from a push tap with deep-link extras
        handlePushTapIntent(getIntent());

        // Load
        fetchNotifications();
    }

    // ─── Intent handling ─────────────────────────────────────────────────

    /**
     * If this Activity was started from a push tap carrying target extras,
     * route immediately to the correct destination then let the stack settle.
     * We do NOT finish() here so the user can press Back and see their inbox.
     */
    private void handlePushTapIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String targetScreen = intent.getStringExtra(EXTRA_TARGET_SCREEN);
        String targetId     = intent.getStringExtra(EXTRA_TARGET_ID);
        if (!TextUtils.isEmpty(targetScreen) && !TextUtils.isEmpty(targetId)) {
            routeToTarget(targetScreen, targetId, false);
        }
    }

    // ─── Fetch ────────────────────────────────────────────────────────────

    private void fetchNotifications() {
        String accessToken = session.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            showEmpty();
            stopRefreshing();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                ApiRoutes.LIST_NOTIFICATIONS,
                null,
                response -> {
                    stopRefreshing();
                    parseAndShowNotifications(response);
                },
                error -> {
                    stopRefreshing();
                    Log.e(TAG, "fetchNotifications error: " + error);
                    Toast.makeText(this,
                            I18n.t(this, "Could not load notifications"),
                            Toast.LENGTH_SHORT).show();
                    if (notifications.isEmpty()) showEmpty();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + accessToken);
                h.put("Accept",        "application/json");
                return h;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                12000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(this).add(req);
    }

    private void parseAndShowNotifications(@NonNull JSONObject response) {
        try {
            if (!response.optBoolean("success", false)) {
                showEmpty();
                return;
            }

            int unreadCount = response.optInt("unread_count", 0);
            JSONArray arr   = response.optJSONArray("notifications");

            notifications.clear();

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    notifications.add(new NotificationItem(
                            o.optLong("notification_id", 0),
                            o.optString("title",         ""),
                            o.optString("body",          ""),
                            o.optString("image_url",     ""),
                            o.optString("target_screen", ""),
                            o.optString("target_id",     ""),
                            o.optString("source",        "admin_push"),
                            o.optBoolean("is_read",      false),
                            o.optString("read_at",       ""),
                            o.optString("created_at",    "")
                    ));
                }
            }

            adapter.notifyDataSetChanged();
            updateUnreadUi(unreadCount);

            if (notifications.isEmpty()) {
                showEmpty();
            } else {
                hideEmpty();
            }
        } catch (Exception e) {
            Log.e(TAG, "parseAndShowNotifications failed", e);
            showEmpty();
        }
    }

    // ─── Mark single read ─────────────────────────────────────────────────

    @Override
    public void onNotificationClick(int position, NotificationItem item) {
        if (!item.isRead()) {
            markSingleNotificationRead(position, item);
        } else {
            // Already read – just navigate
            if (item.hasTarget()) {
                routeToTarget(item.getTargetScreen(), item.getTargetId(), true);
            }
        }
    }

    private void markSingleNotificationRead(int position, @NonNull NotificationItem item) {
        String accessToken = session.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) return;

        // Optimistic update – update immediately for responsive feel
        item.markRead();
        adapter.notifyItemChanged(position);
        decrementUnreadCount();

        JSONObject body = new JSONObject();
        try {
            body.put("notification_id", item.getNotificationId());
        } catch (Exception e) {
            Log.e(TAG, "markSingleRead build body failed", e);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.MARK_NOTIFICATION_READ,
                body,
                response -> {
                    Log.d(TAG, "markSingleRead success");
                    // After mark-read, navigate if there's a target
                    if (item.hasTarget()) {
                        routeToTarget(item.getTargetScreen(), item.getTargetId(), true);
                    }
                },
                error -> Log.e(TAG, "markSingleRead error: " + error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + accessToken);
                h.put("Content-Type",  "application/json");
                h.put("Accept",        "application/json");
                return h;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(this).add(req);
    }

    // ─── Mark all read ────────────────────────────────────────────────────

    private void markAllNotificationsRead() {
        String accessToken = session.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) return;

        // Optimistic update
        for (NotificationItem item : notifications) {
            item.markRead();
        }
        adapter.notifyDataSetChanged();
        updateUnreadUi(0);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.MARK_ALL_NOTIF_READ,
                new JSONObject(),
                response -> Log.d(TAG, "markAllRead success"),
                error    -> Log.e(TAG, "markAllRead error: " + error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + accessToken);
                h.put("Content-Type",  "application/json");
                h.put("Accept",        "application/json");
                return h;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(this).add(req);
    }

    // ─── Deep-link routing ────────────────────────────────────────────────

    /**
     * Routes the user to the correct screen based on target_screen and target_id.
     * Fails safely – never crashes on unknown screen names.
     *
     * @param startActivity whether to startActivity immediately
     */
    private void routeToTarget(@NonNull String targetScreen, @NonNull String targetId,
                               boolean startActivity) {
        Intent intent = buildTargetIntent(targetScreen, targetId);
        if (intent == null) {
            // Unknown screen – stay on inbox, do nothing
            return;
        }
        if (startActivity) {
            startActivity(intent);
        }
    }

    @Nullable
    private Intent buildTargetIntent(@NonNull String screen, @NonNull String id) {
        switch (screen.toLowerCase(java.util.Locale.ROOT)) {
            case "product_detail":
            case "listing_detail":
            case "listing": {
                int listingId = parseIntSafe(id);
                if (listingId <= 0) return null;
                Intent i = new Intent(this, ProductDetail.class);
                i.putExtra("listing_id", listingId);
                return i;
            }
            case "main":
            case "home": {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return i;
            }
            default:
                Log.w(TAG, "Unknown target_screen: " + screen);
                return null;
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────

    private void showEmpty() {
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
        if (rvNotifications   != null) rvNotifications.setVisibility(View.GONE);
    }

    private void hideEmpty() {
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (rvNotifications   != null) rvNotifications.setVisibility(View.VISIBLE);
    }

    private void stopRefreshing() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    private void updateUnreadUi(int count) {
        if (tvUnreadCount != null) {
            if (count > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(I18n.t(this,
                        count + " unread"));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
        }
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void decrementUnreadCount() {
        if (tvUnreadCount == null) return;
        String text = tvUnreadCount.getText() != null ? tvUnreadCount.getText().toString() : "";
        try {
            int current = Integer.parseInt(text.replaceAll("[^0-9]", "").trim());
            int next    = Math.max(0, current - 1);
            updateUnreadUi(next);
        } catch (NumberFormatException ignored) {}
    }

    // ─── Misc ─────────────────────────────────────────────────────────────

    private int parseIntSafe(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void applySavedLocale() {
        android.content.SharedPreferences sp =
                getSharedPreferences(com.anvexgroup.apnuvyapar.LanguageSelection.PREFS, MODE_PRIVATE);
        String lang = sp.getString(com.anvexgroup.apnuvyapar.LanguageSelection.KEY_LANG_CODE, "en");
        com.anvexgroup.apnuvyapar.Adapter.LanguageManager.apply(this, lang);
    }
}
