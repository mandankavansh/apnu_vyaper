package com.anvexgroup.apnuvyapar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.AuthRefreshManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SplashScreen extends AppCompatActivity {

    /*
     * Keep this for old files like DynamicFormActivity
     * where SplashScreen.PREFS is used.
     */
    public static final String PREFS = SessionManager.PREFS;

    private static final long SPLASH_DELAY = 1800L;
    private static final int PROFILE_CHECK_TIMEOUT_MS = 10000;

    private SessionManager session;
    private boolean routed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        session = new SessionManager(this);

        LanguageManager.enforceLtr(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        View brandContainer = findViewById(R.id.brandContainer);
        ProgressBar progress = findViewById(R.id.progress);
        TextView tvLoading = findViewById(R.id.tvLoading);

        if (brandContainer != null) {
            brandContainer.setScaleX(0.85f);
            brandContainer.setScaleY(0.85f);
            brandContainer.setAlpha(0f);
            brandContainer.setTranslationY(20f);

            brandContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(600L)
                    .setStartDelay(200L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (progress != null) {
            progress.setAlpha(0f);

            progress.animate()
                    .alpha(1f)
                    .setDuration(400L)
                    .setStartDelay(900L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        if (tvLoading != null) {
            tvLoading.setAlpha(0f);

            tvLoading.animate()
                    .alpha(1f)
                    .setDuration(400L)
                    .setStartDelay(900L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, SPLASH_DELAY);
    }

    private void routeNext() {
        if (routed || isFinishing() || isDestroyed()) return;

        SharedPreferences prefs = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE);

        String lang = prefs.getString(SessionManager.KEY_LANG_CODE, null);

        if (TextUtils.isEmpty(lang) || lang.trim().isEmpty()) {
            go(LanguageSelection.class);
            return;
        }

        LanguageManager.apply(this, lang);

        if (!session.isLoggedIn() && !session.hasRefreshToken() && !session.hasValidAccessToken()) {
            if (session.isOnboarded()) {
                go(LoginActivity.class);
            } else {
                go(UserInfoActivity.class);
            }
            return;
        }

        AuthRefreshManager.ensureValidSession(this, new AuthRefreshManager.Callback() {
            @Override
            public void onSuccess() {
                checkProfileCompletionAndRoute();
            }

            @Override
            public void onFailure(String message, boolean shouldLogout) {
                if (shouldLogout) {
                    Toast.makeText(SplashScreen.this, message, Toast.LENGTH_SHORT).show();
                    go(LoginActivity.class);
                    return;
                }

                go(MainActivity.class);
            }
        });
    }

    private void checkProfileCompletionAndRoute() {
        if (routed || isFinishing() || isDestroyed()) return;

        String accessToken = session.getAccessToken();

        if (TextUtils.isEmpty(accessToken)) {
            session.logout();
            go(LoginActivity.class);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiRoutes.GET_USER_PROFILE,
                null,
                response -> handleProfileCheckResponse(response),
                this::handleProfileCheckError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                PROFILE_CHECK_TIMEOUT_MS,
                0,
                1.0f
        ));

        VolleySingleton.getInstance(this).add(request);
    }

    private void handleProfileCheckResponse(JSONObject response) {
        if (routed || isFinishing() || isDestroyed()) return;

        if (response == null) {
            go(MainActivity.class);
            return;
        }

        boolean ok = response.optBoolean("success", response.optBoolean("ok", false));

        if (!ok) {
            String errorCode = response.optString("error_code", "");

            if (isAuthBlockingError(errorCode)) {
                session.logout();
                go(LoginActivity.class);
                return;
            }

            go(MainActivity.class);
            return;
        }

        JSONObject user = response.optJSONObject("user");
        saveUserCacheIfAvailable(user);

        boolean profileComplete;
        if (response.has("profile_complete")) {
            profileComplete = response.optBoolean("profile_complete", false);
        } else {
            profileComplete = isProfileComplete(user);
        }

        String next = response.optString("next", "");
        boolean isNew = response.optBoolean("is_new", false);

        if (!profileComplete || isNew || "complete_profile".equalsIgnoreCase(next)) {
            goToUserInfo(user);
            return;
        }

        go(MainActivity.class);
    }

    private void handleProfileCheckError(VolleyError error) {
        if (routed || isFinishing() || isDestroyed()) return;

        int statusCode = -1;
        if (error != null && error.networkResponse != null) {
            statusCode = error.networkResponse.statusCode;
        }

        if (statusCode == 401 || statusCode == 403) {
            session.logout();
            go(LoginActivity.class);
            return;
        }

        go(MainActivity.class);
    }

    private boolean isProfileComplete(JSONObject user) {
        if (user == null) return false;

        return !isBlank(user.optString("full_name", ""))
                && !isBlank(user.optString("phone", ""))
                && !isBlank(user.optString("state", ""))
                && !isBlank(user.optString("district", ""))
                && !isBlank(user.optString("village_name", ""))
                && !isBlank(user.optString("address", ""))
                && !isBlank(user.optString("pincode", ""))
                && !isBlank(user.optString("place_type", ""));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void saveUserCacheIfAvailable(JSONObject user) {
        if (user == null) return;

        String name = user.optString("full_name", "");
        String phone = user.optString("phone", "");

        if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(phone)) {
            session.saveUserProfile(name, phone);
        }
    }

    private boolean isAuthBlockingError(String errorCode) {
        if (TextUtils.isEmpty(errorCode)) return false;

        return "SESSION_EXPIRED".equalsIgnoreCase(errorCode)
                || "SESSION_REVOKED".equalsIgnoreCase(errorCode)
                || "INVALID_TOKEN".equalsIgnoreCase(errorCode)
                || "INVALID_REFRESH_TOKEN".equalsIgnoreCase(errorCode)
                || "ACCOUNT_DELETED".equalsIgnoreCase(errorCode)
                || "ACCOUNT_BLOCKED".equalsIgnoreCase(errorCode)
                || "ACCOUNT_INACTIVE".equalsIgnoreCase(errorCode);
    }

    private void goToUserInfo(JSONObject user) {
        if (routed || isFinishing() || isDestroyed()) return;

        Intent intent = new Intent(this, UserInfoActivity.class);

        if (user != null) {
            String phone = user.optString("phone", "");
            if (!TextUtils.isEmpty(phone)) {
                intent.putExtra("prefill_phone", phone);
            }
        }

        startRoute(intent);
    }

    private void go(Class<?> cls) {
        if (routed || isFinishing() || isDestroyed()) return;
        startRoute(new Intent(this, cls));
    }

    private void startRoute(Intent intent) {
        if (routed || isFinishing() || isDestroyed()) return;

        routed = true;
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
