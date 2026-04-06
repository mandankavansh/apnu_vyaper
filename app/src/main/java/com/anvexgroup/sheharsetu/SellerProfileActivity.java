package com.anvexgroup.sheharsetu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.anvexgroup.sheharsetu.Adapter.I18n;
import com.anvexgroup.sheharsetu.Adapter.LanguageManager;
import com.anvexgroup.sheharsetu.Adapter.ProductAdapter;
import com.anvexgroup.sheharsetu.core.SessionManager;
import com.anvexgroup.sheharsetu.net.ApiRoutes;
import com.anvexgroup.sheharsetu.net.VolleySingleton;
import com.anvexgroup.sheharsetu.utils.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SellerProfileActivity extends AppCompatActivity {

    private TextView tvSellerName, tvMemberSince, tvAvatarLetter;
    private View noListingsView;
    private TextView noListingsText;
    private ImageView ivSellerAvatar;
    private View btnCall, btnWhatsapp, btnLocation;
    private RecyclerView rvSellerListings;

    private int sellerId = 0;
    private String sellerPhone = "";
    private String sellerLocationQuery = "";
    private ProductAdapter productAdapter;
    private final List<Map<String, Object>> listings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        LanguageManager.apply(this,
                getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                        .getString("app_lang_code", "en"));

        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, android.R.color.black));
        setContentView(R.layout.activity_seller_profile);

        applyInsets();

        sellerId = getIntent().getIntExtra("seller_id", 0);
        if (sellerId <= 0) {
            Toast.makeText(this, I18n.t(this, "Invalid Seller ID"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        prefetchStaticTextsAndStart();
    }

    private void applyInsets() {
        View viewStatusBarBg = findViewById(R.id.viewStatusBarBackground);
        View viewNavBarBg = findViewById(R.id.viewNavBarBackground);
        View rootView = findViewById(R.id.root);

        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());

                if (viewStatusBarBg != null) {
                    viewStatusBarBg.getLayoutParams().height = systemBars.top;
                    viewStatusBarBg.requestLayout();
                }

                if (viewNavBarBg != null) {
                    viewNavBarBg.getLayoutParams().height = systemBars.bottom;
                    viewNavBarBg.requestLayout();
                }

                return insets;
            });
        }
    }

    private void setupViews() {
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvSellerName = findViewById(R.id.tvSellerName);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        ivSellerAvatar = findViewById(R.id.ivSellerAvatar);
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);

        btnCall = findViewById(R.id.btnCall);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnLocation = findViewById(R.id.btnLocation);

        rvSellerListings = findViewById(R.id.rvSellerListings);

        // IMPORTANT: this id is a container view in your layout, not a TextView
        noListingsView = findViewById(R.id.tvNoListings);
        noListingsText = findFirstTextView(noListingsView);

        productAdapter = new ProductAdapter(this);
        rvSellerListings.setAdapter(productAdapter);
        rvSellerListings.setLayoutManager(new GridLayoutManager(this, 2));

        btnCall.setOnClickListener(v -> actionCall());
        btnWhatsapp.setOnClickListener(v -> actionWhatsApp());
        btnLocation.setOnClickListener(v -> actionLocation());
    }

    private void prefetchStaticTextsAndStart() {
        Set<String> keys = new LinkedHashSet<>();

        View root = findViewById(R.id.root);
        collectTexts(root, keys);

        keys.add("Invalid Seller ID");
        keys.add("Loading profile...");
        keys.add("Network Error");
        keys.add("Seller");
        keys.add("Member since");
        keys.add("No active listings currently");
        keys.add("No phone number available");
        keys.add("Could not open dialer");
        keys.add("WhatsApp not installed");
        keys.add("Location not available");
        keys.add("Could not open location");
        keys.add("Location details coming soon");

        I18n.prefetch(this, new ArrayList<>(keys), () -> {
            translateTextsRecursively(findViewById(R.id.root));
            fetchSellerDetails();
        }, this::fetchSellerDetails);
    }

    private void fetchSellerDetails() {
        LoadingDialog.showLoading(this, I18n.t(this, "Loading profile..."));

        String url = ApiRoutes.BASE_URL + "/get_seller_details.php?user_id=" + sellerId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, resp -> {
            LoadingDialog.hideLoading();

            if ("success".equalsIgnoreCase(resp.optString("status"))) {
                JSONObject data = resp.optJSONObject("data");
                if (data != null) {
                    populateProfile(data.optJSONObject("profile"));
                    populateListings(data.optJSONArray("listings"));
                }
            } else {
                String message = resp.optString("message");
                Toast.makeText(
                        this,
                        TextUtils.isEmpty(message) ? I18n.t(this, "Network Error") : message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        }, err -> {
            LoadingDialog.hideLoading();
            Toast.makeText(this, I18n.t(this, "Network Error"), Toast.LENGTH_SHORT).show();
        });

        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        VolleySingleton.queue(this).add(req);
    }

    @SuppressLint("SetTextI18n")
    private void populateProfile(JSONObject p) {
        if (p == null) return;

        String sellerName = p.optString("name", "");
        if (TextUtils.isEmpty(sellerName)) {
            sellerName = I18n.t(this, "Seller");
        }
        tvSellerName.setText(sellerName);

        String memberSince = p.optString("member_since", "");
        if (!TextUtils.isEmpty(memberSince)) {
            tvMemberSince.setText(I18n.t(this, "Member since") + " " + memberSince);
        } else {
            tvMemberSince.setText(I18n.t(this, "Member since"));
        }

        if (!TextUtils.isEmpty(sellerName)) {
            tvAvatarLetter.setText(String.valueOf(sellerName.charAt(0)).toUpperCase());
        } else {
            tvAvatarLetter.setText("S");
        }

        sellerPhone = p.optString("phone", "");
        sellerLocationQuery = buildLocationQuery(p);

        String ava = p.optString("avatar_url", "");
        if (!TextUtils.isEmpty(ava)) {
            ivSellerAvatar.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(ava)
                    .placeholder(R.drawable.ic_placeholder_circle)
                    .into(ivSellerAvatar);
        } else {
            ivSellerAvatar.setVisibility(View.INVISIBLE);
        }
    }

    private String buildLocationQuery(JSONObject p) {
        List<String> parts = new ArrayList<>();
        addIfNotEmpty(parts, p.optString("address", ""));
        addIfNotEmpty(parts, p.optString("village_name", ""));
        addIfNotEmpty(parts, p.optString("district", ""));
        addIfNotEmpty(parts, p.optString("city", ""));
        addIfNotEmpty(parts, p.optString("state", ""));
        return TextUtils.join(", ", parts).trim();
    }

    private void addIfNotEmpty(List<String> parts, String value) {
        if (!TextUtils.isEmpty(value)) {
            String clean = value.trim();
            if (!clean.isEmpty()) {
                parts.add(clean);
            }
        }
    }

    private void populateListings(JSONArray arr) {
        listings.clear();

        if (arr != null && arr.length() > 0) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;

                Map<String, Object> m = new HashMap<>();
                m.put("id", o.optInt("id"));
                m.put("title", o.optString("title"));
                m.put("price", o.optString("price"));
                m.put("image_url", o.optString("image_url"));
                m.put("category", o.optString("category"));
                m.put("city", o.optString("city"));
                m.put("posted_when", o.optString("posted_time"));
                m.put("status", o.optString("status", "active"));

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

                if (images.isEmpty() && !TextUtils.isEmpty(o.optString("image_url"))) {
                    images.add(o.optString("image_url"));
                }

                m.put("images", images);
                listings.add(m);
            }
        }

        if (listings.isEmpty()) {
            if (noListingsText != null) {
                noListingsText.setText(I18n.t(this, "No active listings currently"));
            }
            if (noListingsView != null) {
                noListingsView.setVisibility(View.VISIBLE);
            }
            rvSellerListings.setVisibility(View.GONE);
        } else {
            if (noListingsView != null) {
                noListingsView.setVisibility(View.GONE);
            }
            rvSellerListings.setVisibility(View.VISIBLE);
            productAdapter.setItems(listings);
        }
    }

    private void actionCall() {
        if (TextUtils.isEmpty(sellerPhone)) {
            Toast.makeText(this, I18n.t(this, "No phone number available"), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sellerPhone)));
        } catch (Exception e) {
            Toast.makeText(this, I18n.t(this, "Could not open dialer"), Toast.LENGTH_SHORT).show();
        }
    }

    private void actionWhatsApp() {
        if (TextUtils.isEmpty(sellerPhone)) {
            Toast.makeText(this, I18n.t(this, "No phone number available"), Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanedPhone = sellerPhone.replace("+", "").replace(" ", "");
        String url = "https://api.whatsapp.com/send?phone=" + cleanedPhone;

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, I18n.t(this, "WhatsApp not installed"), Toast.LENGTH_SHORT).show();
        }
    }

    private void actionLocation() {
        if (TextUtils.isEmpty(sellerLocationQuery)) {
            Toast.makeText(this, I18n.t(this, "Location not available"), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(sellerLocationQuery));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, I18n.t(this, "Could not open location"), Toast.LENGTH_SHORT).show();
        }
    }

    private TextView findFirstTextView(View view) {
        if (view == null) return null;

        if (view instanceof TextView) {
            return (TextView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView found = findFirstTextView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }

        return null;
    }

    private void collectTexts(View view, Set<String> keys) {
        if (view == null) return;

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
}