package com.anvexgroup.sheharsetu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.graphics.Insets;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anvexgroup.sheharsetu.Adapter.I18n;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";
    private static final String API_KEY = "69b3babc2523fe78f1140527e55c3fd4";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView tvCityName, tvDate, tvTemperature, tvCondition, tvFeelsLike;
    private TextView tvHumidity, tvWindSpeed, tvPressure, tvVisibility;
    private TextView tvGreeting, tvGreetingSub;
    private ImageView ivWeatherIcon;
    private ProgressBar progressBar;
    private MaterialButton btnRefresh;
    private Toolbar toolbar;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Proper Edge-to-Edge and Status Bar logic
        // Set edge-to-edge (Seamless Gradient Flow)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            // false = white icons (for dark gradient background)
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
        }

        setContentView(R.layout.activity_weather);
        initViews();
        setupToolbar();

        // Dynamic Status Bar & Nav Bar Height logic (Proper Black Frame)
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> checkLocationPermission());
        }

        checkLocationPermission();

        startFloatingAnimation();
    }

    private void startFloatingAnimation() {
        if (ivWeatherIcon == null) return;
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                ivWeatherIcon, "translationY", -20f, 20f);
        animator.setDuration(3000);
        animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvGreetingSub = findViewById(R.id.tvGreetingSub);
        tvCityName = findViewById(R.id.tvCityName);
        tvDate = findViewById(R.id.tvDate);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvCondition = findViewById(R.id.tvCondition);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvPressure = findViewById(R.id.tvPressure);
        tvVisibility = findViewById(R.id.tvVisibility);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        if (tvDate != null) {
            tvDate.setText(sdf.format(new Date()));
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initial titles (matching Home Page style)
        if (tvGreeting != null) tvGreeting.setText(I18n.t(this, "Weather"));
        if (tvGreetingSub != null) tvGreetingSub.setText(I18n.t(this, "LOCAL UPDATES & FORECAST"));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndFetchWeather();
        }
    }

    private void getLocationAndFetchWeather() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (location != null) {
                            fetchWeatherData(location.getLatitude(), location.getLongitude());
                        } else {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Location error: " + e.getMessage());
                        Toast.makeText(this, "Location error. Ensure GPS is on.", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    private void fetchWeatherData(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
                lat, lon, API_KEY);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    parseAndDisplayWeather(response);
                },
                error -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    int status = error.networkResponse != null ? error.networkResponse.statusCode : 0;
                    if (status == 401) {
                        Toast.makeText(this, "API Key is activating. Please try again later.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to fetch weather data.", Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Weather fetch error: " + error.getMessage());
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void parseAndDisplayWeather(JSONObject response) {
        try {
            String city = response.getString("name");
            JSONObject main = response.getJSONObject("main");
            double temp = main.getDouble("temp");
            double feelsLike = main.getDouble("feels_like");
            int humidity = main.getInt("humidity");
            int pressure = main.getInt("pressure");

            JSONObject wind = response.getJSONObject("wind");
            double windSpeed = wind.getDouble("speed");

            int visibility = response.getInt("visibility") / 1000; // convert to km

            JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
            String condition = weather.getString("main");
            String description = weather.getString("description");
            String iconCode = weather.getString("icon");

            // Update UI
            if (tvCityName != null) tvCityName.setText(city);
            if (tvTemperature != null) tvTemperature.setText(Math.round(temp) + "°");
            if (tvCondition != null) tvCondition.setText(capitalize(description));
            if (tvFeelsLike != null) tvFeelsLike.setText(I18n.t(this, "Feels like") + " " + Math.round(feelsLike) + "°");
            if (tvHumidity != null) tvHumidity.setText(humidity + "%");
            if (tvWindSpeed != null) tvWindSpeed.setText(windSpeed + " km/h");
            if (tvPressure != null) tvPressure.setText(pressure + " hPa");
            if (tvVisibility != null) tvVisibility.setText(visibility + " km");

            updateWeatherIcon(iconCode);

            // Apply translations to everything
            prefetchAndTranslate();

        } catch (JSONException e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
            Toast.makeText(this, "Error parsing weather data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void prefetchAndTranslate() {
        List<String> keys = new ArrayList<>();
        keys.add(tvCityName.getText().toString());
        keys.add(tvCondition.getText().toString());
        keys.add("Humidity");
        keys.add("Wind");
        keys.add("Pressure");
        keys.add("Visibility");
        keys.add("Refresh Data");
        keys.add("Weather");
        keys.add("LOCAL UPDATES & FORECAST");

        I18n.prefetch(this, keys, () -> {
            if (isFinishing() || isDestroyed()) return;
            if (tvCityName != null) tvCityName.setText(I18n.t(this, tvCityName.getText().toString()));
            if (tvCondition != null) tvCondition.setText(I18n.t(this, tvCondition.getText().toString()));
            if (tvGreeting != null) tvGreeting.setText(I18n.t(this, "Weather"));
            if (tvGreetingSub != null) tvGreetingSub.setText(I18n.t(this, "LOCAL UPDATES & FORECAST"));
        });
    }

    private void updateWeatherIcon(String iconCode) {
        if (ivWeatherIcon == null) return;
        // Icon codes from OWM: 01d, 02d, etc.
        // For now using the generic ic_weather_24, but logic can be added here
        // if more specific vectors are available.
        ivWeatherIcon.setImageResource(R.drawable.ic_weather_24);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndFetchWeather();
            } else {
                Toast.makeText(this, "Location permission is required for weather.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
