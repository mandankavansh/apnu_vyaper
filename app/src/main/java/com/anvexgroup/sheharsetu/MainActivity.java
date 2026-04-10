package com.anvexgroup.apnuvyapar;

import static android.widget.Toast.makeText;
import com.google.firebase.messaging.FirebaseMessaging;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import com.anvexgroup.apnuvyapar.Adapter.CategoryAdapter;
import com.anvexgroup.apnuvyapar.Adapter.LanguageAdapter;
import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.Adapter.ProductAdapter;
import com.anvexgroup.apnuvyapar.Adapter.SubFilterGridAdapter;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;
import com.anvexgroup.apnuvyapar.utils.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String TAG_FETCH_PRODUCTS = "TAG_FETCH_PRODUCTS";

    private static final String PREFS_FCM = "fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_LAST_UPLOADED_FCM_TOKEN = "last_uploaded_fcm_token";

    private static final String APP_BRAND = "Shaher Setu";

    // ===== Views (Header) =====
    private ImageView btnDrawer, btnVoiceSearch;
    private TextInputEditText etSearch;
    private TextView tvSectionTitle;
    private TextView tvLocation;
    private ActivityResultLauncher<Intent> speechLauncher;

    // ===== Lists =====
    private RecyclerView rvCategories, rvSubFiltersGrid, rvProducts;
    private Chip chipCondition;

    // ===== Bottom banner =====
    private ImageButton btnPost, btnHelp;
    private TextView tvMarquee;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean ignoreSearchTextChanges = false;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // ===== Drawer =====
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    // ===== Data (dynamic) =====
    private final List<Map<String, Object>> categories = new ArrayList<>();
    private final Map<Integer, List<Map<String, Object>>> mapSubFilters = new HashMap<>();
    private final List<Map<String, Object>> currentProducts = new ArrayList<>();

    // ===== State =====
    private int selectedCategoryId = -1;
    private int selectedSubFilterId = -1; // -1 = none, 0 = ALL
    private Boolean showNew = null; // null = all, true=new, false=old
    private String searchQuery = "";

    // ===== KM Filter State =====
    private Integer selectedRadiusKm = null;
    private Double userLat = null;
    private Double userLng = null;
    private Chip chipKmFilter;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int HEADER_LOCATION_PERMISSION_REQUEST = 1002;

    // ===== Price Filter State =====
//    private Double selectedMinPrice = null;
//    private Double selectedMaxPrice = null;
    private Chip chipPriceFilter;
    private ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> locationSettingsLauncher;

    // ===== Adapters =====
    private CategoryAdapter catAdapter;
    private ProductAdapter productAdapterRef;

    // ===== Locale Prefs =====
    private static final String PREFS = LanguageSelection.PREFS;
    private static final String KEY_LANG = LanguageSelection.KEY_LANG_CODE;

    // ===== Session =====
    private SessionManager session;

    // ===== User Profile Cache =====
    private String cachedUserName = "User";
    private String cachedUserPhone = "";
    private TextView tvNavUserName;
    private TextView tvNavUserPhone;

    // ===== Pagination =====
    private int currentPage = 1;
    private static final int LIMIT = 20;
    private boolean hasMoreProducts = true;
    private boolean isLoadingMore = false;

    // ===== Network correctness =====
    private android.util.LruCache<String, JSONObject> productCache;
    private String lastProductsUrl = null;
    private boolean productsInFlight = false;
    private boolean isConditionFilterAvailable = false;
    private Integer selectedMinPrice = null;
    private Integer selectedMaxPrice = null;
    private void initCachedUserData() {
        cachedUserName = session.getCachedUserName();
        cachedUserPhone = session.getCachedUserPhone();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applySavedLocale();
        session = new SessionManager(this);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        productCache = new android.util.LruCache<>(20);
        initCachedUserData();


        locationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        doFetchLocation();
                    } else {
                        makeText(this, I18n.t(this, "GPS is required for distance filter"), Toast.LENGTH_SHORT).show();
                        selectedRadiusKm = null;
                        updateKmChipText();
                    }
                });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
        }

        setContentView(R.layout.activity_main);
        LanguageManager.enforceLtr(this);
        setupNotificationPermissionLauncher();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotificationPermissionIfNeeded();
        }
        fetchFcmToken();

        View viewStatusBarBackground = findViewById(R.id.viewStatusBarBackground);
        View viewNavBarBackground = findViewById(R.id.viewNavBarBackground);

        View rootContainer = findViewById(R.id.rootContainer);
        if (rootContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                if (viewStatusBarBackground != null) {
                    viewStatusBarBackground.getLayoutParams().height = systemBars.top;
                    viewStatusBarBackground.requestLayout();
                }

                if (viewNavBarBackground != null) {
                    viewNavBarBackground.getLayoutParams().height = systemBars.bottom;
                    viewNavBarBackground.requestLayout();
                }

                return insets;
            });
        }

        bindHeader();

        rvCategories = findViewById(R.id.rvCategories);
        rvSubFiltersGrid = findViewById(R.id.rvSubFiltersGrid);
        rvProducts = findViewById(R.id.rvProducts);
//        chipCondition = findViewById(R.id.chipCondition);
//        tvSectionTitle = findViewById(R.id.tvSectionTitle);

        chipCondition = findViewById(R.id.chipCondition);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);

        if (chipCondition != null) {
            chipCondition.setVisibility(View.GONE);
            chipCondition.setOnClickListener(v -> showCombinedFilterSheet());
            showNew = null;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        chipKmFilter = findViewById(R.id.chipKmFilter);
        if (chipKmFilter != null) {
            chipKmFilter.setOnClickListener(v -> showCombinedFilterSheet());
            updateKmChipText();
        }
//        chipPriceFilter = findViewById(R.id.chipPriceFilter);
//        if (chipPriceFilter != null) {
//            chipPriceFilter.setOnClickListener(v -> showPriceFilterSheet());
//            updatePriceChipText();
//        }
        btnPost = findViewById(R.id.btnPost);
        btnHelp = findViewById(R.id.btnHelp);
        tvMarquee = findViewById(R.id.tvMarquee);
        if (tvMarquee != null) {
            applyContinuousMarquee(tvMarquee.getText() == null ? "" : tvMarquee.getText().toString());
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
            swipeRefresh.setOnRefreshListener(() -> {
                productCache.evictAll();
                lastProductsUrl = null;
                productsInFlight = false;
                resetPagination();
                fetchProducts();
            });
        }

        TextView tvLangBadge = findViewById(R.id.tvLangBadge);
        if (tvLangBadge != null) {
            tvLangBadge.setText(I18n.t(this, "Language") + ": " + session.getLangName());
        }

        setupVoiceLauncher();
        setupSearch();
        setupLanguageToggle();
        setupAppDrawer();

        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSubFiltersGrid.setLayoutManager(new GridLayoutManager(this, 3));
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return;

                RecyclerView.LayoutManager lm = rvProducts.getLayoutManager();
                if (lm == null) return;

                int totalItems = lm.getItemCount();
                int lastVisible = 0;

                if (lm instanceof GridLayoutManager) {
                    lastVisible = ((GridLayoutManager) lm).findLastVisibleItemPosition();
                } else if (lm instanceof LinearLayoutManager) {
                    lastVisible = ((LinearLayoutManager) lm).findLastVisibleItemPosition();
                }

                if (!isLoadingMore && hasMoreProducts && lastVisible >= totalItems - 4) {
                    loadMoreProducts();
                }
            }
        });

        setupAdapters();

        if (btnPost != null) {
            btnPost.setOnClickListener(v ->
                    startActivity(new Intent(this, CategorySelectActivity.class)));
        }
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v ->
                    startActivity(new Intent(this, HelpActivity.class)));
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        chipKmFilter = findViewById(R.id.chipKmFilter);
        if (chipKmFilter != null) {
            chipKmFilter.setOnClickListener(v -> showKmFilterSheet());
        }
        // Note: chipPriceFilter already wired in the first bind block above.

        fetchAndDisplayHeaderLocation();

        applyFixedBrandText();
        prefetchAndApplyStaticTexts();

        showProducts();
        LoadingDialog.showLoading(this, I18n.t(this, "Loading data..."));
        fetchCategories();
        fetchProducts();
        fetchUserProfileOnStartup();

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        View tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        ImageView topImageOuter = findViewById(R.id.topImage);
        ImageView headerOverlayOuter = findViewById(R.id.headerOverlay);
        ImageView toolbarSearchIcon = findViewById(R.id.toolbarSearchIcon);

        if (toolbarSearchIcon != null) {
            toolbarSearchIcon.setOnClickListener(v -> {
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, true);
                    if (etSearch != null) etSearch.requestFocus();
                }
            });
        }

        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
                int scrollRange = appBarLayout1.getTotalScrollRange();
                if (scrollRange == 0) return;

                float fraction = (float) Math.abs(verticalOffset) / (float) scrollRange;

                if (fraction > 0.8f) {
                    float alpha = (fraction - 0.8f) * 5f;
                    if (tvToolbarTitle != null) {
                        tvToolbarTitle.setVisibility(View.VISIBLE);
                        tvToolbarTitle.setAlpha(alpha);
                    }
                    if (topImageOuter != null) topImageOuter.setVisibility(View.GONE);
                    if (headerOverlayOuter != null) headerOverlayOuter.setVisibility(View.GONE);
                    if (toolbarSearchIcon != null) {
                        toolbarSearchIcon.setVisibility(View.VISIBLE);
                        toolbarSearchIcon.setAlpha(alpha);
                    }
                } else {
                    if (tvToolbarTitle != null) {
                        tvToolbarTitle.setVisibility(View.GONE);
                        tvToolbarTitle.setAlpha(0f);
                    }
                    if (topImageOuter != null) topImageOuter.setVisibility(View.VISIBLE);
                    if (headerOverlayOuter != null) headerOverlayOuter.setVisibility(View.VISIBLE);
                    if (toolbarSearchIcon != null) {
                        toolbarSearchIcon.setVisibility(View.GONE);
                        toolbarSearchIcon.setAlpha(0f);
                    }
                }

                if (toolbar != null) {
                    if (fraction > 0.9f) {
                        toolbar.setBackgroundColor(
                                ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                    } else {
                        toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    }
                }
            });

        }
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "FCM token fetch failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM TOKEN = " + token);

                    if (TextUtils.isEmpty(token)) {
                        Log.e(TAG, "FCM token is empty");
                        return;
                    }

                    saveFcmTokenLocally(token);
                    uploadFcmTokenToServer(token);
                });
    }

    private void saveFcmTokenLocally(@NonNull String token) {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        sp.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    private String getLastUploadedFcmToken() {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        return sp.getString(KEY_LAST_UPLOADED_FCM_TOKEN, null);
    }

    private void setLastUploadedFcmToken(@NonNull String token) {
        SharedPreferences sp = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_UPLOADED_FCM_TOKEN, token).apply();
    }

    private void uploadFcmTokenToServer(@NonNull String token) {
        String accessToken = session != null ? session.getAccessToken() : null;

        if (TextUtils.isEmpty(accessToken)) {
            Log.d(TAG, "Skipping FCM upload: user not logged in");
            return;
        }

        String lastUploaded = getLastUploadedFcmToken();
        if (token.equals(lastUploaded)) {
            Log.d(TAG, "Skipping FCM upload: same token already uploaded");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("token", token);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build FCM JSON body", e);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ApiRoutes.FCM_TOKEN,
                body,
                response -> {
                    Log.d(TAG, "FCM token upload success: " + response);
                    setLastUploadedFcmToken(token);
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
    }

    // ================= Helpers =================

    private static String ltrim(String str, char ch) {
        if (str == null || str.isEmpty()) return str;
        while (str.length() > 0 && str.charAt(0) == ch) {
            str = str.substring(1);
        }
        return str;
    }

    private String makeAbsoluteImageUrl(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) return "";

        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }

        String cleanPath = ltrim(imagePath, '/');
        return ApiRoutes.BASE_URL + "/" + cleanPath;
    }

    // ================= Header =================

    private void bindHeader() {
        btnDrawer = findViewById(R.id.btnDrawer);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        etSearch = findViewById(R.id.etSearch);
        tvLocation = findViewById(R.id.tvlocation);

        ImageView topImage = findViewById(R.id.topImage); // Notification
        ImageView headerOverlay = findViewById(R.id.headerOverlay); // Profile

        if (topImage != null) {
            topImage.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, NotificationsActivity.class)));
        }

        if (headerOverlay != null) {
            headerOverlay.setOnClickListener(v -> showLanguageBottomSheet());
        }
    }

    /**
     * Show language picker bottom sheet
     */
    private void showLanguageBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);

        View view = LayoutInflater.from(this).inflate(R.layout.sheet_language_picker, null, false);
        dialog.setContentView(view);

        RecyclerView rvLangs = view.findViewById(R.id.rvLanguages);
        View progress = view.findViewById(R.id.progressLanguages);

        java.util.List<String[]> languages = new java.util.ArrayList<>();

        LanguageAdapter adapter = new LanguageAdapter(languages, lang -> {
            session.setLang(lang[0], lang[1]);

            getSharedPreferences("apnuvyapar_prefs", MODE_PRIVATE).edit()
                    .putString("app_lang_code", lang[0])
                    .putString("app_lang_name", lang[1])
                    .apply();

            LanguageManager.apply(this, lang[0]);

            dialog.dismiss();

            Toast.makeText(this, I18n.t(this, "Language changed to") + " " + lang[1], Toast.LENGTH_SHORT).show();
            recreate();
        });

        rvLangs.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        rvLangs.setAdapter(adapter);

        fetchLanguagesForPicker(languages, adapter, progress, rvLangs);

        dialog.show();
    }

    /**
     * Fetch languages from API for picker
     */
    private void fetchLanguagesForPicker(java.util.List<String[]> languages,
                                         LanguageAdapter adapter,
                                         View progress,
                                         RecyclerView rvLangs) {
        progress.setVisibility(View.VISIBLE);
        rvLangs.setVisibility(View.INVISIBLE);

        String url = ApiRoutes.GET_LANGUAGES;

        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    progress.setVisibility(View.GONE);
                    rvLangs.setVisibility(View.VISIBLE);

                    try {
                        org.json.JSONObject resp = new org.json.JSONObject(response.trim());
                        if (!resp.optBoolean("ok", false))
                            return;

                        org.json.JSONArray arr = resp.optJSONArray("data");
                        if (arr == null)
                            return;

                        languages.clear();
                        int englishIndex = -1;

                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject o = arr.optJSONObject(i);
                            if (o == null)
                                continue;
                            if (o.optInt("enabled", 1) != 1)
                                continue;

                            String code = o.optString("code", "").trim();
                            String nativeName = o.optString("native_name", "").trim();
                            String englishName = o.optString("english_name", "").trim();

                            if (code.isEmpty() || nativeName.isEmpty())
                                continue;

                            languages.add(new String[] { code, nativeName, englishName });

                            if ("en".equalsIgnoreCase(code)) {
                                englishIndex = languages.size() - 1;
                            }
                        }

                        if (englishIndex > 0) {
                            String[] en = languages.remove(englishIndex);
                            languages.add(0, en);
                        }

                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing languages: " + e.getMessage());
                    }
                },
                error -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, I18n.t(this, "Failed to load languages"), Toast.LENGTH_SHORT).show();
                });

        req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1.0f));
        VolleySingleton.getInstance(this).add(req);
    }

    // ================= Header Location =================

    private void fetchAndDisplayHeaderLocation() {
        if (tvLocation == null || fusedLocationClient == null) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    HEADER_LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateHeaderLocationText(location.getLatitude(), location.getLongitude());
                    } else {
                        requestFreshHeaderLocation();
                    }
                })
                .addOnFailureListener(this, e ->
                        Log.e(TAG, "Header location fetch failed", e));
    }

    @SuppressLint("MissingPermission")
    private void requestFreshHeaderLocation() {
        com.google.android.gms.location.LocationRequest req =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(1)
                        .setInterval(3000)
                        .setMaxWaitTime(8000);

        fusedLocationClient.requestLocationUpdates(
                req,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(com.google.android.gms.location.LocationResult result) {
                        fusedLocationClient.removeLocationUpdates(this);

                        if (result != null && result.getLastLocation() != null) {
                            updateHeaderLocationText(
                                    result.getLastLocation().getLatitude(),
                                    result.getLastLocation().getLongitude()
                            );
                        }
                    }
                },
                android.os.Looper.getMainLooper()
        );
    }

    private void updateHeaderLocationText(double lat, double lng) {
        new Thread(() -> {
            String finalLocation = "";

            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    String district = firstNonEmpty(
                            address.getSubAdminArea(),
                            address.getLocality(),
                            address.getSubLocality()
                    );

                    String state = firstNonEmpty(address.getAdminArea());

                    if (!TextUtils.isEmpty(district) && !TextUtils.isEmpty(state)) {
                        finalLocation = district + ", " + state;
                    } else if (!TextUtils.isEmpty(state)) {
                        finalLocation = state;
                    } else if (!TextUtils.isEmpty(district)) {
                        finalLocation = district;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Geocoder failed", e);
            }

            final String textToShow = finalLocation;
            runOnUiThread(() -> {
                if (tvLocation != null && !TextUtils.isEmpty(textToShow)) {
                    tvLocation.setText(textToShow);
                }
            });
        }).start();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return "";
    }

    // ================= Voice =================

    private void setupVoiceLauncher() {
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> list =
                                result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (list != null && !list.isEmpty()) {
                            etSearch.setText(list.get(0));
                            performSearch(list.get(0));
                        }
                    }
                });

        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> startVoiceInput());
        }
    }

    private void startVoiceInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, I18n.t(this, "Speak to search…"));
        try {
            speechLauncher.launch(i);
        } catch (Exception e) {
            makeText(this, I18n.t(this, "Voice search not available"), Toast.LENGTH_SHORT).show();
        }
    }

    // ================= Search =================

    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
                performSearch(q);

                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (ignoreSearchTextChanges) return;

                String q = s == null ? "" : s.toString().trim();
                performSearch(q);
            }
        });
    }

    private void cancelPendingSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
    }

    private void clearSearchSilently() {
        cancelPendingSearch();
        searchQuery = "";

        if (etSearch != null) {
            ignoreSearchTextChanges = true;
            etSearch.setText("");
            ignoreSearchTextChanges = false;
        }
    }

    private void clearSearch() {
        clearSearchSilently();
    }

    private void performSearch(String query) {
        cancelPendingSearch();

        searchRunnable = () -> {
            ensureProductsView();
            searchQuery = TextUtils.isEmpty(query) ? "" : query.toLowerCase(Locale.ROOT).trim();
            resetPagination();
            fetchProducts();
        };

        searchHandler.postDelayed(searchRunnable, 500);
    }

    private void applySavedLocale() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String lang = sp.getString(KEY_LANG, "en");
        LanguageManager.apply(this, lang);
    }

    // ================= Language / Drawer =================

    private void setupLanguageToggle() {
        if (btnDrawer == null) return;

        btnDrawer.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        btnDrawer.setOnLongClickListener(v -> {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            String cur = sp.getString(KEY_LANG, "en");
            String next = cur.equals("en") ? "hi" : "en";
            String nextName = next.equals("en") ? "English" : "हिन्दी";

            sp.edit()
                    .putString(KEY_LANG, next)
                    .putString(LanguageSelection.KEY_LANG_NAME, nextName)
                    .apply();

            LanguageManager.apply(this, next);
            makeText(this, I18n.t(this, "Language") + ": " + nextName, Toast.LENGTH_SHORT).show();

            recreate();
            return true;
        });
    }

    private void applyDrawerWidth60Percent() {
        if (navigationView == null) return;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int drawerWidth = (int) (screenWidth * 0.60f);

        DrawerLayout.LayoutParams params =
                (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
        params.width = drawerWidth;
        navigationView.setLayoutParams(params);
    }

    private void setupAppDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navView);
        if (drawerLayout == null || navigationView == null) return;
        applyDrawerWidth60Percent();
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                android.R.string.ok, android.R.string.cancel);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        View header = navigationView.getHeaderView(0);
        if (header != null) {
            ImageView ivProfile = header.findViewById(R.id.ivProfile);
            tvNavUserName = header.findViewById(R.id.tvUserName);
            tvNavUserPhone = header.findViewById(R.id.tvUserPhone);
            ImageView ivEdit = header.findViewById(R.id.ivEdit);

            if (tvNavUserName != null) {
                tvNavUserName.setText(I18n.t(this, cachedUserName));
            }
            if (tvNavUserPhone != null) {
                tvNavUserPhone.setText(cachedUserPhone);
            }

            View.OnClickListener openProfileClick = v ->
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));

            header.setOnClickListener(openProfileClick);
            if (ivProfile != null) ivProfile.setOnClickListener(openProfileClick);
            if (ivEdit != null) ivEdit.setOnClickListener(openProfileClick);
        }

        navigationView.setItemIconTintList(null);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            item.setChecked(true);
            drawerLayout.closeDrawer(GravityCompat.START);
            navigationView.setItemIconTintList(null);

            if (id == R.id.nav_home) {
                Toast.makeText(MainActivity.this, I18n.t(this, "Home"), Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_post) {
                startActivity(new Intent(MainActivity.this, CategorySelectActivity.class));

            } else if (id == R.id.nav_my_ads) {
                startActivity(new Intent(MainActivity.this, MyAdsActivity.class));

            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(MainActivity.this, NotificationsActivity.class));

            } else if (id == R.id.nav_invite) {
                shareApp();

            } else if (id == R.id.nav_rate) {
                rateUs();

            } else if (id == R.id.nav_contact) {
                startActivity(new Intent(MainActivity.this, ContactUsActivity.class));

            } else if (id == R.id.nav_about) {
                startActivity(new Intent(MainActivity.this, AboutUsActivity.class));

            } else if (id == R.id.nav_weather) {
                startActivity(new Intent(MainActivity.this, WeatherActivity.class));

            } else if (id == R.id.nav_youtube) {
                openUrl("https://www.youtube.com/@AnvexGroup");

            } else if (id == R.id.nav_whatsapp) {
                openWhatsApp("+91 6354355617");

            } else if (id == R.id.nav_instagram) {
                openUrl("https://www.instagram.com/anvexgroup?igsh=bmY4NGNnYnE3ejV1");

            } else if (id == R.id.nav_logout) {
                doLogout();
            } else {
                startActivity(new Intent(MainActivity.this, termsandcondition.class));
            }
            return true;
        });
    }

    private void applyDrawerMenuTranslations() {
        if (navigationView == null) return;
        android.view.Menu menu = navigationView.getMenu();
        if (menu == null) return;

        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            if (item != null && item.getTitle() != null) {
                item.setTitle(I18n.t(this, item.getTitle().toString()));
            }
        }
    }

    private void doLogout() {
        getSharedPreferences("user", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PREFS_FCM, MODE_PRIVATE).edit().clear().apply();

        makeText(this, I18n.t(this, "Logged out"), Toast.LENGTH_SHORT).show();

        Intent i = new Intent(this, LanguageSelection.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // ================= Static text prefetch =================

    private void prefetchAndApplyStaticTexts() {
        Set<String> keys = new LinkedHashSet<>();

        View root = findViewById(R.id.rootContainer);
        collectTexts(root, keys);

        keys.add("LOCAL MARKETPLACE PLATFORM");
        keys.add("Ahmedabad, Gujarat");
        keys.add("Find the Best");
        keys.add("Near You");
        keys.add("LISTINGS");
        keys.add("NEARBY");
        keys.add("CITIES");
        keys.add("RATED");
        keys.add("Categories");
        keys.add("Featured Listings");
        keys.add("No products found");
        keys.add("Loading data...");

        keys.add("Speak to search…");
        keys.add("Voice search not available");
        keys.add("Home");
        keys.add("My Ads");
        keys.add("Notifications");
        keys.add("Coming soon");
        keys.add("Categories error");
        keys.add("Parse categories failed");
        keys.add("Network error (categories)");
        keys.add("Subcategories error");
        keys.add("Parse subcategories failed");
        keys.add("Network error (subcategories)");
        keys.add("Products error");
        keys.add("Parse products failed");
        keys.add("Network error (products)");
        keys.add("Share via");
        keys.add("No app found to share");
        keys.add("Condition: All");
        keys.add("Condition: New");
        keys.add("Condition: Used");
        keys.add("New Items Only");
        keys.add("Used Items Only");
        keys.add("Logged out");
        keys.add("Nearby");
        keys.add("Getting your location...");
        keys.add("Location error. Please try again.");
        keys.add("Could not get location. Try again.");
        keys.add("Enable Location");
        keys.add("GPS is turned off. Please enable location services to filter listings by distance.");
        keys.add("Open Settings");
        keys.add("Cancel");
        keys.add("Location permission needed for distance filter");
        keys.add("GPS is required for distance filter");
        keys.add("Filter");
        keys.add("Filter listings");
        keys.add("New");
        keys.add("Used");
        keys.add("Clear");
        keys.add("Apply");
        keys.add("Enter value");
        keys.add("Must be > 0");
        keys.add("Invalid number");
        keys.add("Language");
        keys.add("Language changed to");
        keys.add("Failed to load languages");

        // Price filter strings
//        keys.add("Price");
        keys.add("Price filter");
        keys.add("Refine results by budget");
        keys.add("Minimum price");
        keys.add("Maximum price");
        keys.add("Min price");
        keys.add("Max price");
        keys.add("Invalid price range");
        keys.add("Maximum price must be greater than or equal to minimum price");
        keys.add("Up to");
        keys.add("From");
        keys.add("All prices");
//        keys.add("All");
        keys.add("Custom");
        keys.add("Custom range");
        keys.add("CUSTOM RANGE");
        keys.add("Quick ranges");
        keys.add("Choose a budget range");
        keys.add("Apply filter");
        keys.add("to");
        // Chip preset labels
//        keys.add("Under ₹1,000");
//        keys.add("Under ₹5,000");
//        keys.add("₹5K – ₹20K");
//        keys.add("₹20K – ₹50K");
//        keys.add("₹50,000+");

        keys.add("Price");
        keys.add("Price Range");
        keys.add("Min Price");
        keys.add("Max Price");
        keys.add("All");
        keys.add("Max must be greater than or equal to Min");

        if (navigationView != null && navigationView.getMenu() != null) {
            android.view.Menu menu = navigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                android.view.MenuItem item = menu.getItem(i);
                if (item != null && item.getTitle() != null) {
                    keys.add(item.getTitle().toString());
                }
            }
        }

        I18n.prefetch(this, new ArrayList<>(keys), () -> {
            translateTextsRecursively(findViewById(R.id.rootContainer));
            applyDrawerMenuTranslations();
            applyFixedBrandText();

            TextView tvLangBadge = findViewById(R.id.tvLangBadge);
            if (tvLangBadge != null) {
                tvLangBadge.setText(I18n.t(this, "Language") + ": " + session.getLangName());
            }

            if (chipCondition != null && showNew == null) {
                chipCondition.setText(I18n.t(this, "Condition: All"));
            }

            if (tvMarquee != null) {
                applyContinuousMarquee(tvMarquee.getText() == null ? "" : tvMarquee.getText().toString());
            }

            updateKmChipText();
//            updatePriceChipText();
        });
    }

    private void collectTexts(View view, Set<String> keys) {
        if (view == null) return;

        if (shouldSkipAutoTranslation(view)) {
            return;
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            CharSequence text = tv.getText();
            if (!TextUtils.isEmpty(text)) {
                keys.add(text.toString().trim());
            }

            CharSequence hint = tv.getHint();
            if (!TextUtils.isEmpty(hint)) {
                keys.add(hint.toString().trim());
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectTexts(vg.getChildAt(i), keys);
            }
        }
    }

    private void translateTextsRecursively(View view) {
        if (view == null) return;

        if (shouldSkipAutoTranslation(view)) {
            return;
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            CharSequence text = tv.getText();
            if (!TextUtils.isEmpty(text)) {
                tv.setText(I18n.t(this, text.toString()));
            }

            CharSequence hint = tv.getHint();
            if (!TextUtils.isEmpty(hint)) {
                tv.setHint(I18n.t(this, hint.toString()));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                translateTextsRecursively(vg.getChildAt(i));
            }
        }
    }

    private boolean shouldSkipAutoTranslation(View view) {
        if (view == null) return false;

        int id = view.getId();
        if (id == R.id.tvToolbarTitle
                || id == R.id.tvGreeting
                || id == R.id.tvGreetingSub) {
            return true;
        }

        View parent = (view.getParent() instanceof View) ? (View) view.getParent() : null;
        while (parent != null) {
            if (parent.getId() == R.id.tvToolbarTitle) {
                return true;
            }
            parent = (parent.getParent() instanceof View) ? (View) parent.getParent() : null;
        }

        return false;
    }

    private void applyFixedBrandText() {
        setTitle(APP_BRAND);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(APP_BRAND);
        }

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            tvGreeting.setText(APP_BRAND);
        }

        View toolbarTitleView = findViewById(R.id.tvToolbarTitle);
        TextView titleTextView = findFirstTextView(toolbarTitleView);
        if (titleTextView != null) {
            titleTextView.setText(APP_BRAND);
        }
    }

    @Nullable
    private TextView findFirstTextView(View view) {
        if (view == null) return null;

        if (view instanceof TextView) {
            return (TextView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findFirstTextView(group.getChildAt(i));
                if (result != null) return result;
            }
        }

        return null;
    }

    private void applyContinuousMarquee(String text) {
        if (tvMarquee == null) return;

        String safe = text == null ? "" : text.trim();
        if (safe.isEmpty()) safe = APP_BRAND;

        String loopText = safe + "     •     " + safe + "     •     " + safe + "     •     ";

        tvMarquee.setSingleLine(true);
        tvMarquee.setHorizontallyScrolling(true);
        tvMarquee.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tvMarquee.setMarqueeRepeatLimit(-1);
        tvMarquee.setFocusable(true);
        tvMarquee.setFocusableInTouchMode(true);
        tvMarquee.setSelected(true);
        tvMarquee.setText(loopText);

        tvMarquee.post(() -> tvMarquee.setSelected(true));
    }

    // ================= Adapters =================

    private void setupAdapters() {
        catAdapter = new CategoryAdapter(categories, cat -> {
            selectedCategoryId = toInt(cat.get("id"), -1);
            selectedSubFilterId = -1;
            showNew = null;
            isConditionFilterAvailable = false;
            clearSearchSilently();

            if (chipCondition != null) {
                chipCondition.setVisibility(View.GONE);
                chipCondition.setText(I18n.t(this, "Condition: All"));
//                showNew = null;
            }
            updateKmChipText();
            resetPagination();
//
//            VolleySingleton.getInstance(this).getQueue().cancelAll(TAG_FETCH_PRODUCTS);
//            productsInFlight = false;
//            lastProductsUrl = null;
//
//            if (selectedCategoryId == 0) {
//                mapSubFilters.remove(0);
//                showProducts();
//                fetchProducts();
//            } else {
//                fetchSubFilters(selectedCategoryId);
//            }
            VolleySingleton.getInstance(this).getQueue().cancelAll(TAG_FETCH_PRODUCTS);
            productsInFlight = false;
            lastProductsUrl = null;

            if (selectedCategoryId == 0) {
                mapSubFilters.remove(0);
                showProducts();
                fetchProducts();
            } else {
                fetchSubFilters(selectedCategoryId);
            }
        });

        rvCategories.setAdapter(catAdapter);

        productAdapterRef = new ProductAdapter(this);
        rvProducts.setAdapter(productAdapterRef);
    }

    //    private void bindSubFilters(List<Map<String, Object>> subs) {
//        rvSubFiltersGrid.setAdapter(new SubFilterGridAdapter(subs, sub -> {
//            selectedSubFilterId = toInt(sub.get("id"), 0);
//
//            clearSearchSilently();
//            showProducts();
//
//            if (selectedSubFilterId > 0) {
//                boolean hasNewOld = toBool(sub.get("hasNewOld"), false);
//                if (hasNewOld && chipCondition != null) {
//                    chipCondition.setVisibility(View.VISIBLE);
//                } else if (chipCondition != null) {
//                    chipCondition.setVisibility(View.GONE);
//                    showNew = null;
//                    chipCondition.setText(I18n.t(this, "Condition: All"));
//                }
//            } else {
//                if (chipCondition != null) {
//                    chipCondition.setVisibility(View.GONE);
//                    showNew = null;
//                    chipCondition.setText(I18n.t(this, "Condition: All"));
//                }
//            }
//
//            productCache.evictAll();
//            resetPagination();
//            fetchProducts();
//        }));
//
//        showSubFilters();
//        if (catAdapter != null) {
//            catAdapter.setSelectedId(selectedCategoryId);
//        }
//    }
    private void bindSubFilters(List<Map<String, Object>> subs) {
        rvSubFiltersGrid.setAdapter(new SubFilterGridAdapter(subs, sub -> {
            selectedSubFilterId = toInt(sub.get("id"), 0);

            clearSearchSilently();
            showProducts();

            isConditionFilterAvailable = false;

            if (selectedSubFilterId > 0) {
                boolean hasNewOld = toBool(sub.get("hasNewOld"), false);
                isConditionFilterAvailable = hasNewOld;

                if (!hasNewOld) {
                    showNew = null;
                }
            } else {
                showNew = null;
            }

            if (chipCondition != null) {
                chipCondition.setVisibility(View.GONE);
                chipCondition.setText(I18n.t(this, "Condition: All"));
            }

            updateKmChipText();

            productCache.evictAll();
            resetPagination();
            fetchProducts();
        }));

        showSubFilters();
        if (catAdapter != null) {
            catAdapter.setSelectedId(selectedCategoryId);
        }
    }

    private void bindProducts(List<Map<String, Object>> items) {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

        if (items == null || items.isEmpty()) {
            rvProducts.setVisibility(View.GONE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvProducts.setVisibility(View.VISIBLE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (productAdapterRef != null) productAdapterRef.setItems(items);
            runLayoutAnimation();
        }
    }

    private void runLayoutAnimation() {
        if (rvProducts == null) return;
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                this, R.anim.layout_animation_fall_down);
        rvProducts.setLayoutAnimation(controller);
        rvProducts.scheduleLayoutAnimation();
    }

    private void showSubFilters() {
        rvProducts.setVisibility(View.GONE);
        if (tvSectionTitle != null) tvSectionTitle.setVisibility(View.GONE);
        rvSubFiltersGrid.setVisibility(View.VISIBLE);
    }

    private void showProducts() {
        rvSubFiltersGrid.setVisibility(View.GONE);
        if (tvSectionTitle != null) {
            tvSectionTitle.setVisibility(View.VISIBLE);
            tvSectionTitle.setText(I18n.t(this, "Featured Listings"));
        }
        rvProducts.setVisibility(View.VISIBLE);
    }

    private void ensureProductsView() {
        if (rvSubFiltersGrid != null && rvSubFiltersGrid.getVisibility() == View.VISIBLE) {
            showProducts();
        }
    }

    // ================= URLs =================

    private String urlCategories() {
        return ApiRoutes.BASE_URL + "/list_categories.php";
    }

    private String urlSubcategories(int categoryId) {
        return ApiRoutes.BASE_URL + "/list_subcategories.php?category_id=" + categoryId;
    }

    private String urlProducts() {
        StringBuilder sb = new StringBuilder(ApiRoutes.BASE_URL)
                .append("/list_products.php?page=").append(currentPage)
                .append("&limit=").append(LIMIT)
                .append("&sort=newest");

        if (selectedCategoryId > 0) sb.append("&category_id=").append(selectedCategoryId);
        if (selectedSubFilterId > 0) sb.append("&subcategory_id=").append(selectedSubFilterId);

        if (!TextUtils.isEmpty(searchQuery)) {
            sb.append("&q=").append(android.net.Uri.encode(searchQuery));
        }

        if (showNew != null) {
            sb.append("&is_new=").append(showNew ? "1" : "0");
        }

        if (selectedRadiusKm != null && userLat != null && userLng != null) {
            sb.append("&lat=").append(userLat)
                    .append("&lng=").append(userLng)
                    .append("&radius=").append(selectedRadiusKm);
        }

        if (selectedMinPrice != null) {
            sb.append("&min_price=").append(selectedMinPrice);
        }

        if (selectedMaxPrice != null) {
            sb.append("&max_price=").append(selectedMaxPrice);
        }

        return sb.toString();
    }

    private Integer parseOptionalPositiveInt(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return null;

        String value = editText.getText().toString().trim();
        if (TextUtils.isEmpty(value)) return null;

        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String buildPriceSummaryText() {
        if (selectedMinPrice == null && selectedMaxPrice == null) {
            return null;
        }

        if (selectedMinPrice != null && selectedMaxPrice != null) {
            return "\u20B9" + selectedMinPrice + " - \u20B9" + selectedMaxPrice;
        }

        if (selectedMinPrice != null) {
            return "\u2265 \u20B9" + selectedMinPrice;
        }

        return "\u2264 \u20B9" + selectedMaxPrice;
    }
    private void resetPagination() {
        currentPage = 1;
        hasMoreProducts = true;
        isLoadingMore = false;
        productsInFlight = false;
        lastProductsUrl = null;
    }

    private void loadMoreProducts() {
        if (isLoadingMore || !hasMoreProducts) return;
        isLoadingMore = true;
        currentPage++;
        fetchProducts();
    }

    // ================= Network =================

    private void fetchCategories() {
        final String url = urlCategories();
        Log.e(TAG, "========== FETCH CATEGORIES START ==========");
        Log.e(TAG, "URL: " + url);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                resp -> {
                    Log.e(TAG, "✅ Categories Response: " + resp.toString());
                    try {
                        if (!"success".equalsIgnoreCase(resp.optString("status"))) {
                            Log.e(TAG, "❌ Status is not success: " + resp.optString("status"));
                            makeText(this, I18n.t(this, "Categories error"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray arr = resp.optJSONArray("data");
                        Log.e(TAG, "Categories count: " + (arr != null ? arr.length() : 0));

                        categories.clear();

                        Map<String, Object> allCat = new HashMap<>();
                        allCat.put("id", 0);
                        allCat.put("name", "All Listings");
                        allCat.put("iconRes", R.drawable.ic_all_listings);
                        categories.add(allCat);

                        List<String> catNameKeys = new ArrayList<>();
                        catNameKeys.add("All Listings");

                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                Map<String, Object> m = new HashMap<>();
                                int catId = o.optInt("id", 0);
                                m.put("id", catId);
                                String nameEn = o.optString("name", "");

                                // Patch misspelling from API
                                if ("mobail".equalsIgnoreCase(nameEn)) {
                                    nameEn = "Mobile";
                                }

                                m.put("name", nameEn);

                                String iconUrl = o.optString("icon", "");
                                if (TextUtils.isEmpty(iconUrl)) {
                                    iconUrl = o.optString("icon_url", "");
                                }

                                iconUrl = makeAbsoluteImageUrl(iconUrl);
                                m.put("iconUrl", iconUrl);

                                // Use the perfect smart phone vector
                                if ("Mobile".equalsIgnoreCase(nameEn)) {
                                    m.put("iconRes", R.drawable.ic_smartphone_24);
                                    m.put("iconUrl", "");
                                }

                                m.put("hasNewOld", o.optInt("hasNewOld", 0) == 1);

                                categories.add(m);

                                if (!TextUtils.isEmpty(nameEn)) {
                                    catNameKeys.add(nameEn);
                                }
                            }
                        } else {
                            Log.e(TAG, "fetchCategories(): data array is NULL!");
                        }

                        if (catAdapter != null) catAdapter.notifyDataSetChanged();

                        I18n.prefetch(this, catNameKeys, () -> {
                            for (Map<String, Object> m : categories) {
                                Object nObj = m.get("name");
                                if (nObj != null) {
                                    String en = String.valueOf(nObj);
                                    m.put("name", I18n.t(this, en));
                                }
                            }
                            if (catAdapter != null) catAdapter.notifyDataSetChanged();
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "fetchCategories(): parse exception", e);
                        makeText(this, I18n.t(this, "Parse categories failed"), Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    Log.e(TAG, "❌ Categories Fetch Error: " + err);
                    if (err.networkResponse != null) {
                        Log.e(TAG, "Status Code: " + err.networkResponse.statusCode);
                        Log.e(TAG, "Body: " + new String(err.networkResponse.data));
                    }
                    makeText(this, I18n.t(this, "Network error (categories)"), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Accept-Language", I18n.lang(MainActivity.this));
                return headers;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        req.setShouldCache(false);
        VolleySingleton.getInstance(this).add(req);
    }

    private void fetchSubFilters(int categoryId) {
        final String url = urlSubcategories(categoryId);

        if (mapSubFilters.containsKey(categoryId)) {
            List<Map<String, Object>> subs = mapSubFilters.get(categoryId);
            bindSubFilters(subs);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                resp -> {
                    try {
                        if (!"success".equalsIgnoreCase(resp.optString("status"))) {
                            Log.e(TAG, "fetchSubFilters(): status != success, status=" + resp.optString("status"));
                            makeText(this, I18n.t(this, "Subcategories error"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray arr = resp.optJSONArray("data");
                        List<Map<String, Object>> subs = new ArrayList<>();

                        Map<String, Object> all = new HashMap<>();
                        all.put("id", 0);
                        all.put("name", I18n.t(this, "All"));
                        all.put("iconRes", R.drawable.ic_placeholder_circle);
                        all.put("hasNewOld", false);
                        subs.add(all);

                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                Map<String, Object> m = new HashMap<>();
                                int subId = o.optInt("id", 0);
                                m.put("id", subId);
                                m.put("category_id", o.optInt("category_id", categoryId));
                                String subName = o.optString("name", "");

                                // Patch misspelling from API
                                if ("mobail".equalsIgnoreCase(subName)) {
                                    subName = "Mobile";
                                }

                                m.put("name", subName);

                                String iconUrl = o.optString("icon", "");
                                if (TextUtils.isEmpty(iconUrl)) {
                                    iconUrl = o.optString("icon_url", "");
                                }

                                iconUrl = makeAbsoluteImageUrl(iconUrl);
                                m.put("iconUrl", iconUrl);

                                // Use the perfect smart phone vector
                                if ("Mobile".equalsIgnoreCase(subName)) {
                                    m.put("iconRes", R.drawable.ic_smartphone_24);
                                    m.put("iconUrl", "");
                                }

                                m.put("hasNewOld", o.optInt("hasNewOld", 0) == 1);

                                subs.add(m);
                            }
                        } else {
                            Log.e(TAG, "fetchSubFilters(): data array is NULL!");
                        }

                        List<String> subNameKeys = new ArrayList<>();
                        for (Map<String, Object> m : subs) {
                            Object nObj = m.get("name");
                            if (nObj != null) {
                                subNameKeys.add(String.valueOf(nObj));
                            }
                        }

                        I18n.prefetch(this, subNameKeys, () -> {
                            for (Map<String, Object> m : subs) {
                                Object nObj = m.get("name");
                                if (nObj != null) {
                                    String en = String.valueOf(nObj);
                                    m.put("name", I18n.t(this, en));
                                }
                            }
                            mapSubFilters.put(categoryId, subs);
                            bindSubFilters(subs);
                        }, () -> {
                            mapSubFilters.put(categoryId, subs);
                            bindSubFilters(subs);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "fetchSubFilters(): parse exception", e);
                        makeText(this, I18n.t(this, "Parse subcategories failed"), Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    Log.e(TAG, "fetchSubFilters() error=" + buildVolleyError(err), err);
                    makeText(this, I18n.t(this, "Network error (subcategories)"), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Accept-Language", I18n.lang(MainActivity.this));
                return headers;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
        VolleySingleton.getInstance(this).add(req);
    }

    private void fetchProducts() {
        final String url = urlProducts();
        final boolean isAppend = currentPage > 1;

        if (!isAppend && productsInFlight) {
            VolleySingleton.getInstance(this).getQueue().cancelAll(TAG_FETCH_PRODUCTS);
        }
        productsInFlight = true;

        if (!isAppend) {
            JSONObject cachedResp = productCache.get(url);
            if (cachedResp != null) {
                try {
                    productsInFlight = false;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    parseProductsResponse(cachedResp, false);
                    lastProductsUrl = url;
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        lastProductsUrl = url;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                resp -> {
                    productsInFlight = false;
                    isLoadingMore = false;

                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    if (resp != null) {
                        if (!isAppend) productCache.put(url, resp);
                        parseProductsResponse(resp, isAppend);
                    }
                },
                err -> {
                    Log.e(TAG, "fetchProducts() ERROR: " + err);
                    productsInFlight = false;
                    isLoadingMore = false;

                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    makeText(this, I18n.t(this, "Network error (products)"), Toast.LENGTH_SHORT).show();

                    if (currentProducts.isEmpty()) {
                        bindProducts(new ArrayList<>());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Accept-Language", I18n.lang(MainActivity.this));
                return headers;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        req.setShouldCache(false);
        req.setTag(TAG_FETCH_PRODUCTS);
        VolleySingleton.getInstance(this).add(req);
    }

    private void parseProductsResponse(JSONObject resp, boolean isAppend) {
        try {
            if (!"success".equalsIgnoreCase(resp.optString("status"))) {
                Log.e(TAG, "parseProductsResponse(): status != success");
                makeText(this, I18n.t(this, "Products error"), Toast.LENGTH_SHORT).show();
                if (!isAppend) bindProducts(new ArrayList<>());
                return;
            }

            hasMoreProducts = resp.optBoolean("has_more", false);

            JSONArray arr = resp.optJSONArray("data");
            List<Map<String, Object>> newItems = new ArrayList<>();

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", o.optInt("id", 0));
                    m.put("category_id", o.optInt("category_id", 0));
                    m.put("subFilterId", o.optInt("subcategory_id", 0));
                    m.put("title", o.optString("title", ""));
                    m.put("price", String.valueOf(o.opt("price")));
                    m.put("city", o.optString("city", ""));

                    if (!o.isNull("distance")) {
                        m.put("distance", String.valueOf(o.opt("distance")));
                    }

                    String imageUrl = "";
                    if (!o.isNull("cover_image")) imageUrl = o.optString("cover_image", "");
                    if (TextUtils.isEmpty(imageUrl) && !o.isNull("image_url")) imageUrl = o.optString("image_url", "");
                    if (TextUtils.isEmpty(imageUrl) && !o.isNull("image")) imageUrl = o.optString("image", "");

                    m.put("imageUrl", makeAbsoluteImageUrl(imageUrl));
                    m.put("isNew", o.optInt("is_new", 0) == 1);

                    String posted = o.optString("posted_when", "");
                    if (TextUtils.isEmpty(posted)) {
                        posted = o.optString("posted_time", "");
                    }
                    m.put("posted_when", posted);

                    // Raw UTC timestamp – preferred for client-side relative-time via TimeUtils
                    String createdAt = o.optString("created_at", "");
                    m.put("created_at", createdAt);

                    List<String> images = new ArrayList<>();
                    JSONArray imgArr = o.optJSONArray("images");
                    if (imgArr != null) {
                        for (int k = 0; k < imgArr.length(); k++) {
                            String url = imgArr.optString(k, "");
                            if (!TextUtils.isEmpty(url)) {
                                images.add(url);
                            }
                        }
                    }
                    if (images.isEmpty() && !TextUtils.isEmpty(String.valueOf(m.get("imageUrl")))) {
                        images.add(String.valueOf(m.get("imageUrl")));
                    }
                    m.put("images", images);

                    newItems.add(m);
                }
            }

            if (isAppend) {
                currentProducts.addAll(newItems);
                if (productAdapterRef != null) {
                    productAdapterRef.addItems(newItems);
                }
            } else {
                currentProducts.clear();
                currentProducts.addAll(newItems);
                bindProducts(new ArrayList<>(currentProducts));
            }

            LoadingDialog.hideLoading();

        } catch (Exception e) {
            Log.e(TAG, "parseProductsResponse(): exception", e);
            if (!isAppend) bindProducts(new ArrayList<>());
        }
    }

    /** Format a price double to a compact, human-friendly string (e.g. 1000 -> "1K"). */
    private String formatPriceShort(Double value) {
        if (value == null) return "";
        long v = value.longValue();
        if (v >= 100000) return (v / 100000) + "L";
        if (v >= 1000) return (v / 1000) + "K";
        return String.valueOf(v);
    }

    /** Format a price double for input field display (no decimals unless needed). */
    private String formatPriceInput(Double value) {
        if (value == null) return "";
        long lv = value.longValue();
        return (value == lv) ? String.valueOf(lv) : String.valueOf(value);
    }

    /**
     * Recursively walk every TextView in the price filter sheet and translate its text via I18n.
     * The title TextView (tvPriceFilterTitle) is already handled before this call, so it will
     * simply be re-translated (idempotent — safe).
     */
    private void translatePriceSheetTexts(View root) {
        if (root == null) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            CharSequence text = tv.getText();
            if (!TextUtils.isEmpty(text)) {
                tv.setText(I18n.t(this, text.toString().trim()));
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                translatePriceSheetTexts(vg.getChildAt(i));
            }
        }
    }

    /**
     * Recursively collect all TextInputLayout instances inside a View tree.
     * Used to set floating hints on the Min / Max price fields programmatically with I18n.
     */
    private void collectTextInputLayouts(View root,
                                         List<com.google.android.material.textfield.TextInputLayout> out) {
        if (root == null) return;
        if (root instanceof com.google.android.material.textfield.TextInputLayout) {
            out.add((com.google.android.material.textfield.TextInputLayout) root);
            // Don't recurse further — children are the EditText, not more TILs
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectTextInputLayouts(vg.getChildAt(i), out);
            }
        }
    }

    /**
     * Translate the text label of every Chip inside a ChipGroup via I18n.
     */
    private void translateChipGroup(com.google.android.material.chip.ChipGroup group) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                com.google.android.material.chip.Chip chip =
                        (com.google.android.material.chip.Chip) child;
                CharSequence text = chip.getText();
                if (!TextUtils.isEmpty(text)) {
                    chip.setText(I18n.t(this, text.toString().trim()));
                }
            }
        }
    }

    private void showKmFilterSheet() {
        showCombinedFilterSheet();
    }

    private void showCombinedFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_main_filter, null);
        dialog.setContentView(sheet);

        ChipGroup chipGroupDistance = sheet.findViewById(R.id.chipGroupDistance);
        ChipGroup chipGroupCondition = sheet.findViewById(R.id.chipGroupCondition);

        View layoutCustomKm = sheet.findViewById(R.id.layoutCustomKm);
        TextInputEditText etCustomKm = sheet.findViewById(R.id.etCustomKm);

        View layoutConditionSection = sheet.findViewById(R.id.layoutConditionSection);
        TextInputEditText etMinPrice = sheet.findViewById(R.id.etMinPrice);
        TextInputEditText etMaxPrice = sheet.findViewById(R.id.etMaxPrice);

        com.google.android.material.button.MaterialButton btnClear =
                sheet.findViewById(R.id.btnClearFilter);
        com.google.android.material.button.MaterialButton btnApply =
                sheet.findViewById(R.id.btnApplyFilter);

        final Integer[] tempRadius = new Integer[]{selectedRadiusKm};
        final Boolean[] tempShowNew = new Boolean[]{showNew};
        final Integer[] tempMinPrice = new Integer[]{selectedMinPrice};
        final Integer[] tempMaxPrice = new Integer[]{selectedMaxPrice};

        // Translate all texts inside this bottom sheet
        translateCombinedFilterSheetTexts(sheet);
        dialog.setOnShowListener(d -> translateCombinedFilterSheetTexts(sheet));

        // ----- distance preselect -----
        if (selectedRadiusKm == null) {
            chipGroupDistance.check(R.id.chipDistanceAll);
        } else if (selectedRadiusKm == 5) {
            chipGroupDistance.check(R.id.chip5km);
        } else if (selectedRadiusKm == 10) {
            chipGroupDistance.check(R.id.chip10km);
        } else if (selectedRadiusKm == 25) {
            chipGroupDistance.check(R.id.chip25km);
        } else if (selectedRadiusKm == 50) {
            chipGroupDistance.check(R.id.chip50km);
        } else if (selectedRadiusKm == 100) {
            chipGroupDistance.check(R.id.chip100km);
        } else {
            chipGroupDistance.check(R.id.chipCustomDistance);
            if (layoutCustomKm != null) layoutCustomKm.setVisibility(View.VISIBLE);
            if (etCustomKm != null) {
                etCustomKm.setText(String.valueOf(selectedRadiusKm));
            }
        }

        chipGroupDistance.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipCustomDistance) {
                if (layoutCustomKm != null) layoutCustomKm.setVisibility(View.VISIBLE);
                return;
            }

            if (layoutCustomKm != null) layoutCustomKm.setVisibility(View.GONE);

            if (checkedId == R.id.chipDistanceAll) tempRadius[0] = null;
            else if (checkedId == R.id.chip5km) tempRadius[0] = 5;
            else if (checkedId == R.id.chip10km) tempRadius[0] = 10;
            else if (checkedId == R.id.chip25km) tempRadius[0] = 25;
            else if (checkedId == R.id.chip50km) tempRadius[0] = 50;
            else if (checkedId == R.id.chip100km) tempRadius[0] = 100;
        });

        // ----- condition section -----
        if (layoutConditionSection != null) {
            layoutConditionSection.setVisibility(isConditionFilterAvailable ? View.VISIBLE : View.GONE);
        }

        if (isConditionFilterAvailable && chipGroupCondition != null) {
            if (showNew == null) {
                chipGroupCondition.check(R.id.chipConditionAll);
            } else if (showNew) {
                chipGroupCondition.check(R.id.chipConditionNew);
            } else {
                chipGroupCondition.check(R.id.chipConditionUsed);
            }
        } else {
            tempShowNew[0] = null;
        }

        // ----- price prefill -----
        if (etMinPrice != null && selectedMinPrice != null) {
            etMinPrice.setText(String.valueOf(selectedMinPrice));
        }
        if (etMaxPrice != null && selectedMaxPrice != null) {
            etMaxPrice.setText(String.valueOf(selectedMaxPrice));
        }

        btnClear.setOnClickListener(v -> {
            selectedRadiusKm = null;
            showNew = null;
            selectedMinPrice = null;
            selectedMaxPrice = null;

            updateKmChipText();
            dialog.dismiss();
            applyKmFilter();
        });

        btnApply.setOnClickListener(v -> {
            int checkedDistanceId = chipGroupDistance.getCheckedChipId();

            if (checkedDistanceId == R.id.chipCustomDistance) {
                String value = etCustomKm != null && etCustomKm.getText() != null
                        ? etCustomKm.getText().toString().trim()
                        : "";

                if (TextUtils.isEmpty(value)) {
                    if (etCustomKm != null) etCustomKm.setError(I18n.t(this, "Enter value"));
                    return;
                }

                try {
                    int km = Integer.parseInt(value);
                    if (km <= 0) {
                        if (etCustomKm != null) etCustomKm.setError(I18n.t(this, "Must be > 0"));
                        return;
                    }
                    tempRadius[0] = km;
                } catch (Exception e) {
                    if (etCustomKm != null) etCustomKm.setError(I18n.t(this, "Invalid number"));
                    return;
                }
            }

            if (isConditionFilterAvailable && chipGroupCondition != null) {
                int checkedConditionId = chipGroupCondition.getCheckedChipId();

                if (checkedConditionId == R.id.chipConditionNew) {
                    tempShowNew[0] = true;
                } else if (checkedConditionId == R.id.chipConditionUsed) {
                    tempShowNew[0] = false;
                } else {
                    tempShowNew[0] = null;
                }
            } else {
                tempShowNew[0] = null;
            }

            Integer parsedMin = parseOptionalPositiveInt(etMinPrice);
            Integer parsedMax = parseOptionalPositiveInt(etMaxPrice);

            if (parsedMin != null && parsedMin <= 0) {
                if (etMinPrice != null) etMinPrice.setError(I18n.t(this, "Must be > 0"));
                return;
            }

            if (parsedMax != null && parsedMax <= 0) {
                if (etMaxPrice != null) etMaxPrice.setError(I18n.t(this, "Must be > 0"));
                return;
            }

            if (parsedMin != null && parsedMin == -1) {
                if (etMinPrice != null) etMinPrice.setError(I18n.t(this, "Invalid number"));
                return;
            }

            if (parsedMax != null && parsedMax == -1) {
                if (etMaxPrice != null) etMaxPrice.setError(I18n.t(this, "Invalid number"));
                return;
            }

            if (parsedMin != null && parsedMax != null && parsedMin > parsedMax) {
                if (etMaxPrice != null) {
                    etMaxPrice.setError(I18n.t(this, "Max must be greater than or equal to Min"));
                }
                return;
            }

            tempMinPrice[0] = parsedMin;
            tempMaxPrice[0] = parsedMax;

            selectedRadiusKm = tempRadius[0];
            showNew = tempShowNew[0];
            selectedMinPrice = tempMinPrice[0];
            selectedMaxPrice = tempMaxPrice[0];

            updateKmChipText();
            dialog.dismiss();

            if (selectedRadiusKm != null && (userLat == null || userLng == null)) {
                fetchUserLocationThenFilter();
            } else {
                applyKmFilter();
            }
        });

        dialog.show();
    }

    private void updateKmChipText() {
        if (chipKmFilter == null) return;

        List<String> active = new ArrayList<>();

        if (selectedRadiusKm != null) {
            active.add(selectedRadiusKm + " km");
        }

        if (isConditionFilterAvailable && showNew != null) {
            active.add(I18n.t(this, showNew ? "New" : "Used"));
        }

        String priceText = buildPriceSummaryText();
        if (!TextUtils.isEmpty(priceText)) {
            active.add(priceText);
        }

        if (active.isEmpty()) {
            chipKmFilter.setText(I18n.t(this, "Filter"));
        } else {
            chipKmFilter.setText(I18n.t(this, "Filter") + " • " + TextUtils.join(" • ", active));
        }
    }
    private void applyKmFilter() {
        ensureProductsView();
        productCache.evictAll();
        lastProductsUrl = null;
        productsInFlight = false;
        resetPagination();
        fetchProducts();
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationThenFilter() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        com.google.android.gms.location.LocationRequest locationRequest =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        com.google.android.gms.location.LocationSettingsRequest settingsRequest =
                new com.google.android.gms.location.LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)
                        .build();

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener(this, response -> doFetchLocation())
                .addOnFailureListener(this, e -> {
                    if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                        try {
                            com.google.android.gms.common.api.ResolvableApiException resolvable =
                                    (com.google.android.gms.common.api.ResolvableApiException) e;
                            locationSettingsLauncher.launch(
                                    new androidx.activity.result.IntentSenderRequest.Builder(
                                            resolvable.getResolution()).build());
                        } catch (Exception ex) {
                            Log.e(TAG, "Could not show location settings dialog", ex);
                            showManualGpsPrompt();
                        }
                    } else {
                        showManualGpsPrompt();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void doFetchLocation() {
        LoadingDialog.showLoading(this, I18n.t(this, "Getting your location..."));
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    LoadingDialog.hideLoading();
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.d(TAG, "User location: " + userLat + ", " + userLng);
                        applyKmFilter();
                    } else {
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(this, e -> {
                    LoadingDialog.hideLoading();
                    Log.e(TAG, "Location fetch failed", e);
                    makeText(this, I18n.t(this, "Location error. Please try again."), Toast.LENGTH_SHORT).show();
                    selectedRadiusKm = null;
                    updateKmChipText();
                });
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        com.google.android.gms.location.LocationRequest req =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(1)
                        .setInterval(5000)
                        .setMaxWaitTime(10000);

        fusedLocationClient.requestLocationUpdates(req,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(com.google.android.gms.location.LocationResult result) {
                        fusedLocationClient.removeLocationUpdates(this);
                        LoadingDialog.hideLoading();
                        if (result != null && result.getLastLocation() != null) {
                            userLat = result.getLastLocation().getLatitude();
                            userLng = result.getLastLocation().getLongitude();
                            Log.d(TAG, "Fresh location: " + userLat + ", " + userLng);
                            applyKmFilter();
                        } else {
                            makeText(MainActivity.this,
                                    I18n.t(MainActivity.this, "Could not get location. Try again."),
                                    Toast.LENGTH_SHORT).show();
                            selectedRadiusKm = null;
                            updateKmChipText();
                        }
                    }
                }, android.os.Looper.getMainLooper());
    }

    private void showManualGpsPrompt() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(I18n.t(this, "Enable Location"))
                .setMessage(I18n.t(this, "GPS is turned off. Please enable location services to filter listings by distance."))
                .setPositiveButton(I18n.t(this, "Open Settings"), (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(I18n.t(this, "Cancel"), (d, w) -> {
                    selectedRadiusKm = null;
                    updateKmChipText();
                })
                .setCancelable(false)
                .show();
    }

    // ================= Permission =================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == HEADER_LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndDisplayHeaderLocation();
            }
            return;
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationThenFilter();
            } else {
                makeText(this, I18n.t(this, "Location permission needed for distance filter"), Toast.LENGTH_SHORT).show();
                selectedRadiusKm = null;
                updateKmChipText();
            }
        }
    }

    // ================= User Profile =================

    @SuppressLint("SetTextI18n")
    private void fetchUserProfile(TextView tvUserName, TextView tvUserPhone) {
        String accessToken = session.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            if (tvUserName != null) tvUserName.setText(I18n.t(this, "Guest User"));
            if (tvUserPhone != null) tvUserPhone.setText("");
            return;
        }

        String url = ApiRoutes.BASE_URL + "/get_user_profile.php";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject user = response.getJSONObject("user");
                            String name = user.optString("name", "User");
                            String formattedPhone = user.optString("formatted_phone", "");
                            if (tvUserName != null) {
                                tvUserName.setText(I18n.t(this, name));
                            }
                            if (tvUserPhone != null) {
                                tvUserPhone.setText(formattedPhone);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing user profile response", e);
                    }
                },
                error -> {
                    Log.e(TAG, "❌ USER PROFILE API ERROR", error);

                    if (tvUserName != null) {
                        tvUserName.setText(I18n.t(this, "User"));
                    }
                    if (tvUserPhone != null) {
                        tvUserPhone.setText("");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(this).add(req);
    }

    private void fetchUserProfileOnStartup() {
        String accessToken = session.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            cachedUserName = I18n.t(this, "Guest User");
            cachedUserPhone = "";
            return;
        }

        String url = ApiRoutes.BASE_URL + "/get_user_profile.php";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject user = response.getJSONObject("user");
                            cachedUserName = I18n.t(this, user.optString("name", "User"));
                            cachedUserPhone = user.optString("formatted_phone", "");
                            session.saveUserProfile(cachedUserName, cachedUserPhone);

                            runOnUiThread(() -> {
                                if (tvNavUserName != null) {
                                    tvNavUserName.setText(I18n.t(this, cachedUserName));
                                }
                                if (tvNavUserPhone != null) {
                                    tvNavUserPhone.setText(cachedUserPhone);
                                }
                            });
                        } else {
                            cachedUserName = I18n.t(this, "User");
                            cachedUserPhone = "";
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing user profile response", e);
                        cachedUserName = I18n.t(this, "User");
                        cachedUserPhone = "";
                    }
                },
                error -> {
                    Log.e(TAG, "❌ USER PROFILE API ERROR ON STARTUP", error);
                    cachedUserName = "User";
                    cachedUserPhone = "";
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(this).add(req);
    }

    // ================= Condition popup =================

    private void showConditionPopup() {
        if (chipCondition == null) return;

        androidx.appcompat.widget.PopupMenu popup =
                new androidx.appcompat.widget.PopupMenu(this, chipCondition);
        popup.getMenu().add(0, 0, 0, I18n.t(this, "Condition: All"));
        popup.getMenu().add(0, 1, 1, I18n.t(this, "New Items Only"));
        popup.getMenu().add(0, 2, 2, I18n.t(this, "Used Items Only"));

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                showNew = null;
                chipCondition.setText(I18n.t(this, "Condition: All"));
            } else if (id == 1) {
                showNew = true;
                chipCondition.setText(I18n.t(this, "Condition: New"));
            } else if (id == 2) {
                showNew = false;
                chipCondition.setText(I18n.t(this, "Condition: Used"));
            }

            productCache.evictAll();
            resetPagination();
            fetchProducts();
            return true;
        });

        popup.show();
    }
    private void translateCombinedFilterSheetTexts(View root) {
        if (root == null) return;

        if (root instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout til =
                    (com.google.android.material.textfield.TextInputLayout) root;

            CharSequence hint = til.getHint();
            if (!TextUtils.isEmpty(hint)) {
                til.setHint(I18n.t(this, hint.toString().trim()));
            }
        } else if (root instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip =
                    (com.google.android.material.chip.Chip) root;

            CharSequence text = chip.getText();
            if (!TextUtils.isEmpty(text)) {
                chip.setText(I18n.t(this, text.toString().trim()));
            }
        } else if (root instanceof TextView) {
            TextView tv = (TextView) root;

            CharSequence text = tv.getText();
            if (!TextUtils.isEmpty(text)) {
                tv.setText(I18n.t(this, text.toString().trim()));
            }

            CharSequence hint = tv.getHint();
            if (!TextUtils.isEmpty(hint)) {
                tv.setHint(I18n.t(this, hint.toString().trim()));
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                translateCombinedFilterSheetTexts(vg.getChildAt(i));
            }
        }
    }
    // ================= Back =================

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (rvSubFiltersGrid != null && rvSubFiltersGrid.getVisibility() == View.VISIBLE) {
            showProducts();
            selectedSubFilterId = -1;
            productsInFlight = false;
            lastProductsUrl = null;
            resetPagination();
            fetchProducts();
            return;
        }

        if (selectedCategoryId != -1) {
            selectedCategoryId = -1;
            selectedSubFilterId = -1;
            showNew = null;

            if (chipCondition != null) {
                chipCondition.setVisibility(View.GONE);
            }
            if (catAdapter != null) {
                catAdapter.setSelectedId(-1);
            }

            showProducts();
            clearSearchSilently();

            productsInFlight = false;
            lastProductsUrl = null;
            resetPagination();
            fetchProducts();
            return;
        }

        super.onBackPressed();
    }

    // ================= Misc =================

    private static int toInt(Object o, int def) {
        if (o instanceof Integer) return (Integer) o;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean toBool(Object o, boolean def) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o == null) return def;

        String s = String.valueOf(o);
        if ("1".equals(s)) return true;
        if ("0".equals(s)) return false;

        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            return def;
        }
    }

    private String buildVolleyError(VolleyError err) {
        if (err == null) return "VolleyError=null";

        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(err.getClass().getSimpleName());

        if (err.getCause() != null) {
            sb.append(" cause=").append(err.getCause().getClass().getSimpleName())
                    .append(":").append(err.getCause().getMessage());
        }

        try {
            if (err.networkResponse != null) {
                sb.append(" status=").append(err.networkResponse.statusCode);
                if (err.networkResponse.data != null) {
                    String body = new String(err.networkResponse.data).trim();
                    if (body.length() > 300) body = body.substring(0, 300) + "...";
                    sb.append(" body=").append(body);
                }
            } else {
                sb.append(" networkResponse=null");
            }
        } catch (Exception ignored) {
        }

        return sb.toString();
    }

    private void shareApp() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        i.putExtra(Intent.EXTRA_TEXT,
                "Check out apnuvyapar: https://play.google.com/store/apps/details?id=" + getPackageName());
        try {
            startActivity(Intent.createChooser(i, I18n.t(this, "Share via")));
        } catch (Exception e) {
            makeText(this, I18n.t(this, "No app found to share"), Toast.LENGTH_SHORT).show();
        }
    }

    private void rateUs() {
        String pkg = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + pkg)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Notification permission already granted");
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted");
                    } else {
                        Log.w(TAG, "Notification permission denied");
                    }
                });
    }

    private void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, I18n.t(this, "Could not open link"), Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsApp(String phoneRaw) {
        String phone = phoneRaw.replace("+", "").replace(" ", "");
        android.net.Uri uri = android.net.Uri.parse("https://wa.me/" + phone);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ex) {
                Toast.makeText(this, I18n.t(this, "WhatsApp is not available."), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
