package com.anvexgroup.apnuvyapar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.anvexgroup.apnuvyapar.Adapter.LanguageAdapter;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageSelection extends AppCompatActivity implements LanguageAdapter.OnLanguageClick {

    public static final String PREFS = "apnuvyapar_prefs";
    public static final String KEY_LANG_CODE = "app_lang_code";
    public static final String KEY_LANG_NAME = "app_lang_name";

    private static final String TAG = "LanguageSelection";

    private RecyclerView rv;
    private ProgressBar progress;
    private Button btnContinue;

    private final List<String[]> languages = new ArrayList<>();
    private LanguageAdapter adapter;

    private String[] selectedLanguage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupSystemBars();

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = sp.getString(KEY_LANG_CODE, null);
        if (saved != null && !saved.trim().isEmpty()) {
            LanguageManager.apply(this, saved);
            goNext();
            return;
        }

        setContentView(R.layout.activity_language_selection);
        LanguageManager.enforceLtr(this);

        rv = findViewById(R.id.rvLanguages);
        progress = findViewById(R.id.progressLanguages);
        btnContinue = findViewById(R.id.btnContinue);

        setupContinueButtonBackground();
        setupRecyclerView();

        btnContinue.setEnabled(false);

        btnContinue.setOnClickListener(v -> {
            if (selectedLanguage == null) {
                return;
            }

            btnContinue.setEnabled(false);

            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_LANG_CODE, selectedLanguage[0])
                    .putString(KEY_LANG_NAME, selectedLanguage[1])
                    .apply();

            LanguageManager.apply(this, selectedLanguage[0]);
            goNext();
        });

        fetchLanguages();
    }

    private void setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void setupRecyclerView() {
        int spanCount = calculateSpanCount();

        GridLayoutManager glm = new GridLayoutManager(this, spanCount);
        rv.setLayoutManager(glm);

        rv.setHasFixedSize(false);
        rv.setClipToPadding(false);
        rv.setNestedScrollingEnabled(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        rv.setPadding(
                (int) dpToPx(2),
                (int) dpToPx(4),
                (int) dpToPx(2),
                (int) dpToPx(10)
        );

        adapter = new LanguageAdapter(languages, this);
        rv.setAdapter(adapter);
    }

    private int calculateSpanCount() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;

        if (screenWidthDp >= 720) {
            return 5;
        } else if (screenWidthDp >= 600) {
            return 4;
        } else if (screenWidthDp < 360) {
            return 2;
        } else {
            return 3;
        }
    }

    private void setupContinueButtonBackground() {
        float radius = dpToPx(24);

        GradientDrawable disabled = new GradientDrawable();
        disabled.setShape(GradientDrawable.RECTANGLE);
        disabled.setCornerRadius(radius);
        disabled.setColor(Color.parseColor("#6696A78D"));
        disabled.setStroke((int) dpToPx(1), Color.parseColor("#33FFFFFF"));

        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.RECTANGLE);
        pressed.setCornerRadius(radius);
        pressed.setColor(Color.parseColor("#7A96A78D"));
        pressed.setStroke((int) dpToPx(1), Color.parseColor("#80B6CEB4"));

        GradientDrawable enabled = new GradientDrawable();
        enabled.setShape(GradientDrawable.RECTANGLE);
        enabled.setCornerRadius(radius);
        enabled.setColor(Color.parseColor("#96A78D"));
        enabled.setStroke((int) dpToPx(1), Color.parseColor("#B6CEB4"));

        StateListDrawable stateList = new StateListDrawable();
        stateList.addState(new int[]{-android.R.attr.state_enabled}, disabled);
        stateList.addState(new int[]{android.R.attr.state_pressed}, pressed);
        stateList.addState(new int[]{}, enabled);

        btnContinue.setBackground(stateList);
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setElevation(dpToPx(4));
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void fetchLanguages() {
        showLoading(true);

        final String url = ApiRoutes.GET_LANGUAGES;
        final String origin = buildOriginFromUrl(url);
        final String referer = origin + "/";

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    String cleaned = response == null ? "" : response.trim();

                    if (cleaned.startsWith("\uFEFF")) {
                        cleaned = cleaned.substring(1).trim();
                    }

                    if (cleaned.startsWith("<!DOCTYPE") || cleaned.startsWith("<html") || cleaned.startsWith("<")) {
                        Toast.makeText(this, "Blocked/HTML response from server. Check WAF/ModSecurity.", Toast.LENGTH_LONG).show();
                        showLoading(false);
                        return;
                    }

                    try {
                        JSONObject resp = new JSONObject(cleaned);

                        languages.clear();

                        boolean ok = resp.optBoolean("ok", false);
                        if (!ok) {
                            String errMsg = resp.optString("error", "ok=false");
                            Toast.makeText(this, "Failed to load languages: " + errMsg, Toast.LENGTH_SHORT).show();
                            showLoading(false);
                            return;
                        }

                        JSONArray arr = resp.optJSONArray("data");
                        if (arr == null || arr.length() == 0) {
                            Toast.makeText(this, "No languages found", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                            return;
                        }

                        int englishIndex = -1;

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) {
                                continue;
                            }

                            int enabled = o.optInt("enabled", 1);
                            if (enabled != 1) {
                                continue;
                            }

                            String code = o.optString("code", "").trim();
                            String nativeName = o.optString("native_name", "").trim();
                            String englishName = o.optString("english_name", "").trim();

                            if (code.isEmpty() || nativeName.isEmpty()) {
                                continue;
                            }

                            languages.add(new String[]{code, nativeName, englishName});

                            if ("en".equalsIgnoreCase(code)) {
                                englishIndex = languages.size() - 1;
                            }
                        }

                        if (languages.isEmpty()) {
                            Toast.makeText(this, "No enabled languages found", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                            return;
                        }

                        if (englishIndex > 0) {
                            String[] en = languages.remove(englishIndex);
                            languages.add(0, en);
                        }

                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e(TAG, "Parse error in fetchLanguages", e);
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    showLoading(false);
                },
                err -> {
                    String message = buildVolleyErrorMessage(err);
                    Log.e(TAG, "fetchLanguages Volley error: " + message, err);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    showLoading(false);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> h = new HashMap<>();

                h.put("Accept", "application/json");

                String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getString(KEY_LANG_CODE, "en");
                h.put("Accept-Language", (current == null || current.trim().isEmpty()) ? "en" : current);

                h.put("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
                h.put("Referer", referer);
                h.put("Origin", origin);

                return h;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String charset = HttpHeaderParser.parseCharset(response.headers, "UTF-8");
                    String parsed = new String(response.data, Charset.forName(charset));
                    return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
                } catch (Exception e) {
                    String parsed = new String(response.data, Charset.forName("UTF-8"));
                    return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
                }
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                0,
                1.0f
        ));

        VolleySingleton.getInstance(this).add(req);
    }

    private String buildOriginFromUrl(String url) {
        try {
            Uri u = Uri.parse(url);
            String scheme = u.getScheme();
            String host = u.getHost();

            if (scheme == null || host == null) {
                return "https://magenta-owl-444153.hostingersite.com";
            }

            return scheme + "://" + host;

        } catch (Exception e) {
            return "https://magenta-owl-444153.hostingersite.com";
        }
    }

    private String buildVolleyErrorMessage(VolleyError err) {
        if (err == null) {
            return "Network error";
        }

        try {
            if (err.networkResponse != null) {
                int code = err.networkResponse.statusCode;
                String body = "";

                if (err.networkResponse.data != null) {
                    body = new String(err.networkResponse.data, Charset.forName("UTF-8")).trim();
                    if (body.length() > 300) {
                        body = body.substring(0, 300) + "...";
                    }
                }

                return "HTTP " + code + ": " + body;
            }
        } catch (Exception ignored) {
        }

        if (err.getCause() != null) {
            return "Network error: "
                    + err.getCause().getClass().getSimpleName()
                    + " - "
                    + err.getCause().getMessage();
        }

        if (err.getMessage() != null) {
            return "Network error: " + err.getMessage();
        }

        return "Network error";
    }

    private void showLoading(boolean show) {
        if (progress != null) {
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (rv != null) {
            rv.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        }

        if (btnContinue != null && show) {
            btnContinue.setEnabled(false);
        }
    }

    @Override
    public void onLanguageSelected(String[] lang) {
        selectedLanguage = lang;

        btnContinue.setEnabled(true);

        btnContinue.setScaleX(0.92f);
        btnContinue.setScaleY(0.92f);
        btnContinue.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();
    }

    private void goNext() {
        startActivity(new Intent(this, UserInfoActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}