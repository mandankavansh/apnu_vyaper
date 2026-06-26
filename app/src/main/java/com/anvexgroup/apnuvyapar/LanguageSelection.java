package com.anvexgroup.apnuvyapar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class LanguageSelection extends AppCompatActivity
        implements LanguageAdapter.OnLanguageClick {

    public static final String PREFS = "apnuvyapar_prefs";
    public static final String KEY_LANG_CODE = "app_lang_code";
    public static final String KEY_LANG_NAME = "app_lang_name";

    private static final String TAG = "LanguageSelection";

    private View mainRoot;
    private View bottomActionBar;

    private RecyclerView rv;
    private ProgressBar progress;
    private Button btnContinue;

    private final List<String[]> languages = new ArrayList<>();

    private LanguageAdapter adapter;
    private GridLayoutManager gridLayoutManager;

    private String[] selectedLanguage;

    private int gridSpacingPx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupSystemBars();

        SharedPreferences preferences =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        String savedLanguage =
                preferences.getString(KEY_LANG_CODE, null);

        if (savedLanguage != null &&
                !savedLanguage.trim().isEmpty()) {

            LanguageManager.apply(this, savedLanguage);
            goNext();
            return;
        }

        setContentView(R.layout.activity_language_selection);

        LanguageManager.enforceLtr(this);

        bindViews();
        setupWindowInsets();
        setupContinueButtonBackground();
        setupRecyclerView();

        btnContinue.setEnabled(false);

        btnContinue.setOnClickListener(v ->
                saveLanguageAndContinue()
        );

        fetchLanguages();
    }

    private void bindViews() {
        mainRoot = findViewById(R.id.main);
        bottomActionBar = findViewById(R.id.bottomActionBar);

        rv = findViewById(R.id.rvLanguages);
        progress = findViewById(R.id.progressLanguages);
        btnContinue = findViewById(R.id.btnContinue);
    }

    /**
     * Makes the screen edge-to-edge while keeping all content safe from:
     *
     * 1. Status bar
     * 2. Display cutout/notch
     * 3. Gesture navigation bar
     * 4. Traditional three-button navigation bar
     */
    private void setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    /**
     * Adds actual device insets instead of using a fixed bottom margin.
     * Therefore the Continue button never gets covered.
     */
    private void setupWindowInsets() {
        final int rootLeft = mainRoot.getPaddingLeft();
        final int rootTop = mainRoot.getPaddingTop();
        final int rootRight = mainRoot.getPaddingRight();
        final int rootBottom = mainRoot.getPaddingBottom();

        final int actionLeft =
                bottomActionBar.getPaddingLeft();

        final int actionTop =
                bottomActionBar.getPaddingTop();

        final int actionRight =
                bottomActionBar.getPaddingRight();

        final int actionBottom =
                bottomActionBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                mainRoot,
                (view, windowInsets) -> {

                    Insets systemInsets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout()
                            );

                    /*
                     * Status bar/notch safety is applied to the complete page.
                     */
                    view.setPadding(
                            rootLeft,
                            rootTop + systemInsets.top,
                            rootRight,
                            rootBottom
                    );

                    /*
                     * Navigation gesture/button safety is applied only
                     * to the bottom action area.
                     */
                    bottomActionBar.setPadding(
                            actionLeft,
                            actionTop,
                            actionRight,
                            actionBottom + systemInsets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(mainRoot);
    }

    private void setupRecyclerView() {
        /*
         * Two is used temporarily.
         * The correct column count is calculated after RecyclerView
         * receives its real available width.
         */
        gridLayoutManager =
                new GridLayoutManager(this, 2);

        rv.setLayoutManager(gridLayoutManager);
        rv.setHasFixedSize(false);
        rv.setClipToPadding(false);
        rv.setNestedScrollingEnabled(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        gridSpacingPx = Math.round(dpToPx(8));

        rv.addItemDecoration(
                new GridSpacingDecoration(gridSpacingPx)
        );

        adapter = new LanguageAdapter(languages, this);
        rv.setAdapter(adapter);

        /*
         * Recalculate columns automatically for:
         *
         * Small mobiles
         * Normal mobiles
         * Foldables
         * Landscape screens
         * Tablets
         * Large tablets
         */
        rv.addOnLayoutChangeListener(
                (view,
                 left,
                 top,
                 right,
                 bottom,
                 oldLeft,
                 oldTop,
                 oldRight,
                 oldBottom) -> updateGridSpanCount()
        );

        rv.post(this::updateGridSpanCount);
    }

    /**
     * Width-based responsive grid.
     *
     * It does not depend on fixed device names or fixed screen breakpoints.
     */
    private void updateGridSpanCount() {
        if (rv == null || gridLayoutManager == null) {
            return;
        }

        int recyclerWidth = rv.getWidth();

        if (recyclerWidth <= 0) {
            return;
        }

        int availableWidth =
                recyclerWidth
                        - rv.getPaddingLeft()
                        - rv.getPaddingRight();

        if (availableWidth <= 0) {
            return;
        }

        /*
         * Approximately 124dp minimum card width.
         *
         * Very narrow screens automatically use one column.
         * Normal phones normally use two columns.
         * Tablets automatically receive more columns.
         */
        int minimumCardWidth = Math.round(dpToPx(124));

        int spanCount =
                (availableWidth + gridSpacingPx)
                        / (minimumCardWidth + gridSpacingPx);

        spanCount = Math.max(1, spanCount);
        spanCount = Math.min(6, spanCount);

        if (gridLayoutManager.getSpanCount() != spanCount) {
            gridLayoutManager.setSpanCount(spanCount);
            gridLayoutManager.requestLayout();
        }
    }

    private void setupContinueButtonBackground() {
        float radius = dpToPx(27);

        GradientDrawable disabled =
                createButtonDrawable(
                        radius,
                        "#66334155",
                        "#4DFFFFFF"
                );

        GradientDrawable pressed =
                createButtonDrawable(
                        radius,
                        "#1E293B",
                        "#64748B"
                );

        GradientDrawable enabled =
                createButtonDrawable(
                        radius,
                        "#0F172A",
                        "#475569"
                );

        StateListDrawable stateList =
                new StateListDrawable();

        stateList.addState(
                new int[]{-android.R.attr.state_enabled},
                disabled
        );

        stateList.addState(
                new int[]{android.R.attr.state_pressed},
                pressed
        );

        stateList.addState(
                new int[]{},
                enabled
        );

        btnContinue.setBackground(stateList);
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setElevation(dpToPx(5));
    }

    private GradientDrawable createButtonDrawable(
            float radius,
            String backgroundColor,
            String strokeColor
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(radius);

        drawable.setColor(
                Color.parseColor(backgroundColor)
        );

        drawable.setStroke(
                Math.round(dpToPx(1)),
                Color.parseColor(strokeColor)
        );

        return drawable;
    }

    private void saveLanguageAndContinue() {
        if (selectedLanguage == null ||
                selectedLanguage.length < 2) {
            return;
        }

        btnContinue.setEnabled(false);

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(
                        KEY_LANG_CODE,
                        selectedLanguage[0]
                )
                .putString(
                        KEY_LANG_NAME,
                        selectedLanguage[1]
                )
                .apply();

        LanguageManager.apply(
                this,
                selectedLanguage[0]
        );

        goNext();
    }

    private float dpToPx(float dp) {
        return dp *
                getResources()
                        .getDisplayMetrics()
                        .density;
    }

    private void fetchLanguages() {
        showLoading(true);

        final String url = ApiRoutes.GET_LANGUAGES;
        final String origin = buildOriginFromUrl(url);
        final String referer = origin + "/";

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> handleLanguagesResponse(response),
                error -> handleLanguagesError(error)
        ) {
            @Override
            public Map<String, String> getHeaders()
                    throws AuthFailureError {

                HashMap<String, String> headers =
                        new HashMap<>();

                headers.put("Accept", "application/json");

                String currentLanguage =
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .getString(KEY_LANG_CODE, "en");

                if (currentLanguage == null ||
                        currentLanguage.trim().isEmpty()) {
                    currentLanguage = "en";
                }

                headers.put(
                        "Accept-Language",
                        currentLanguage
                );

                headers.put(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) " +
                                "AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 " +
                                "Mobile Safari/537.36"
                );

                headers.put("Referer", referer);
                headers.put("Origin", origin);

                return headers;
            }

            @Override
            protected Response<String> parseNetworkResponse(
                    NetworkResponse response
            ) {
                try {
                    String charset =
                            HttpHeaderParser.parseCharset(
                                    response.headers,
                                    "UTF-8"
                            );

                    String parsed = new String(
                            response.data,
                            Charset.forName(charset)
                    );

                    return Response.success(
                            parsed,
                            HttpHeaderParser.parseCacheHeaders(response)
                    );

                } catch (Exception exception) {
                    String parsed = new String(
                            response.data,
                            Charset.forName("UTF-8")
                    );

                    return Response.success(
                            parsed,
                            HttpHeaderParser.parseCacheHeaders(response)
                    );
                }
            }
        };

        request.setShouldCache(false);

        request.setRetryPolicy(
                new DefaultRetryPolicy(
                        20000,
                        0,
                        1.0f
                )
        );

        VolleySingleton
                .getInstance(this)
                .add(request);
    }

    private void handleLanguagesResponse(String response) {
        String cleaned =
                response == null ? "" : response.trim();

        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1).trim();
        }

        if (cleaned.startsWith("<!DOCTYPE") ||
                cleaned.startsWith("<html") ||
                cleaned.startsWith("<")) {

            Toast.makeText(
                    this,
                    "Unable to load languages. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

            showLoading(false);
            return;
        }

        try {
            JSONObject responseObject =
                    new JSONObject(cleaned);

            boolean success =
                    responseObject.optBoolean("ok", false);

            if (!success) {
                String errorMessage =
                        responseObject.optString(
                                "error",
                                "Unable to load languages"
                        );

                Toast.makeText(
                        this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();

                showLoading(false);
                return;
            }

            JSONArray languageArray =
                    responseObject.optJSONArray("data");

            if (languageArray == null ||
                    languageArray.length() == 0) {

                Toast.makeText(
                        this,
                        "No languages are available",
                        Toast.LENGTH_SHORT
                ).show();

                showLoading(false);
                return;
            }

            languages.clear();

            int englishIndex = -1;

            for (int i = 0;
                 i < languageArray.length();
                 i++) {

                JSONObject item =
                        languageArray.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                int enabled =
                        item.optInt("enabled", 1);

                if (enabled != 1) {
                    continue;
                }

                String code =
                        item.optString("code", "").trim();

                String nativeName =
                        item.optString(
                                "native_name",
                                ""
                        ).trim();

                String englishName =
                        item.optString(
                                "english_name",
                                ""
                        ).trim();

                if (code.isEmpty() ||
                        nativeName.isEmpty()) {
                    continue;
                }

                languages.add(
                        new String[]{
                                code,
                                nativeName,
                                englishName
                        }
                );

                if ("en".equalsIgnoreCase(code)) {
                    englishIndex = languages.size() - 1;
                }
            }

            if (languages.isEmpty()) {
                Toast.makeText(
                        this,
                        "No enabled languages are available",
                        Toast.LENGTH_SHORT
                ).show();

                showLoading(false);
                return;
            }

            /*
             * English remains first when available.
             */
            if (englishIndex > 0) {
                String[] english =
                        languages.remove(englishIndex);

                languages.add(0, english);
            }

            adapter.notifyDataSetChanged();

            rv.post(this::updateGridSpanCount);

        } catch (Exception exception) {
            Log.e(
                    TAG,
                    "Language response parsing failed",
                    exception
            );

            Toast.makeText(
                    this,
                    "Unable to read language information",
                    Toast.LENGTH_SHORT
            ).show();
        }

        showLoading(false);
    }

    private void handleLanguagesError(VolleyError error) {
        String message =
                buildVolleyErrorMessage(error);

        Log.e(
                TAG,
                "Language request failed: " + message,
                error
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        showLoading(false);
    }

    private String buildOriginFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return "https://magenta-owl-444153.hostingersite.com";
            }

            return scheme + "://" + host;

        } catch (Exception exception) {
            return "https://magenta-owl-444153.hostingersite.com";
        }
    }

    private String buildVolleyErrorMessage(VolleyError error) {
        if (error == null) {
            return "Network error. Please try again.";
        }

        try {
            if (error.networkResponse != null) {
                int statusCode =
                        error.networkResponse.statusCode;

                if (statusCode >= 500) {
                    return "Server is temporarily unavailable.";
                }

                if (statusCode == 401 ||
                        statusCode == 403) {
                    return "Request was blocked by the server.";
                }

                if (statusCode == 404) {
                    return "Language service was not found.";
                }

                return "Unable to load languages. Error "
                        + statusCode;
            }
        } catch (Exception ignored) {
        }

        if (error.getCause() != null &&
                error.getCause().getMessage() != null) {

            return "Network error: "
                    + error.getCause().getMessage();
        }

        return "Check your internet connection and try again.";
    }

    private void showLoading(boolean show) {
        if (progress != null) {
            progress.setVisibility(
                    show ? View.VISIBLE : View.GONE
            );
        }

        if (rv != null) {
            rv.setVisibility(
                    show ? View.INVISIBLE : View.VISIBLE
            );
        }

        if (btnContinue != null) {
            btnContinue.setEnabled(
                    !show && selectedLanguage != null
            );
        }
    }

    @Override
    public void onLanguageSelected(
            @NonNull String[] language
    ) {
        selectedLanguage = language;

        btnContinue.setEnabled(true);

        btnContinue.animate().cancel();

        btnContinue.setScaleX(0.96f);
        btnContinue.setScaleY(0.96f);

        btnContinue.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();
    }

    private void goNext() {
        Intent intent =
                new Intent(
                        this,
                        UserInfoActivity.class
                );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    }

    /**
     * Provides equal space between cards without fixed XML margins.
     */
    private static class GridSpacingDecoration
            extends RecyclerView.ItemDecoration {

        private final int halfSpacing;

        GridSpacingDecoration(int spacing) {
            halfSpacing = Math.max(1, spacing / 2);
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state
        ) {
            int position =
                    parent.getChildAdapterPosition(view);

            if (position == RecyclerView.NO_POSITION) {
                outRect.set(0, 0, 0, 0);
                return;
            }

            outRect.set(
                    halfSpacing,
                    halfSpacing,
                    halfSpacing,
                    halfSpacing
            );
        }
    }
}