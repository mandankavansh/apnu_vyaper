package com.anvexgroup.apnuvyapar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.AuthRefreshManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;

public class SplashScreen extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1800L; // 1.8s

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        session = new SessionManager(this);

        LanguageManager.enforceLtr(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Set entirely transparent status bar so gradient shows edge-to-edge
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false); // Make icons white on dark background

        // Root view padding
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ---------- Views ----------
        View brandContainer = findViewById(R.id.brandContainer);
        ProgressBar progress = findViewById(R.id.progress);
        TextView tvLoading = findViewById(R.id.tvLoading);

        brandContainer.setScaleX(0.85f);
        brandContainer.setScaleY(0.85f);
        brandContainer.setAlpha(0f);
        brandContainer.setTranslationY(20f);

        progress.setAlpha(0f);
        tvLoading.setAlpha(0f);

        // 1) Brand Container slide-up + fade-in + scale-up
        brandContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(600L)
                .setStartDelay(200L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 2) Bottom loading + text fade-in
        progress.animate()
                .alpha(1f)
                .setDuration(400L)
                .setStartDelay(900L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        tvLoading.animate()
                .alpha(1f)
                .setDuration(400L)
                .setStartDelay(900L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, SPLASH_DELAY);
    }

    private void routeNext() {
        /*
         * Do not use session.getLangCode() here because it returns "en" as default.
         * On fresh install we must check real saved value, otherwise LanguageSelection
         * will be skipped.
         */
        SharedPreferences sp = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE);
        String lang = sp.getString(SessionManager.KEY_LANG_CODE, null);

        if (lang == null || lang.trim().isEmpty()) {
            go(LanguageSelection.class);
            return;
        }

        /*
         * No login/session found.
         */
        if (!session.isLoggedIn()
                && !session.hasRefreshToken()
                && !session.hasValidAccessToken()) {

            if (session.isOnboarded()) {
                go(LoginActivity.class);
            } else {
                go(UserInfoActivity.class);
            }

            return;
        }

        /*
         * Shaher Setu style session check:
         * - valid access token: go MainActivity
         * - expired access token + refresh token: silently refresh
         * - inactive 30 days: logout and go LoginActivity
         */
        AuthRefreshManager.ensureValidSession(this, new AuthRefreshManager.Callback() {
            @Override
            public void onSuccess() {
                go(MainActivity.class);
            }

            @Override
            public void onFailure(String message, boolean shouldLogout) {
                if (shouldLogout) {
                    Toast.makeText(SplashScreen.this, message, Toast.LENGTH_SHORT).show();
                    go(LoginActivity.class);
                    return;
                }

                /*
                 * Network/server issue while refreshing token.
                 * Do not force logout active user.
                 * Let MainActivity open; protected APIs can retry/handle later.
                 */
                go(MainActivity.class);
            }
        });
    }

    private void go(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}