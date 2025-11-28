package com.infowave.sheharsetu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    // View mode widgets
    private TextView tvAvatarLetter, tvFullName, tvPhone, tvPlaceTypeChip;
    private TextView tvSurnameValue, tvContactPhone;
    private TextView tvAddressLine, tvVillage, tvDistrict, tvState, tvPincode;

    // Edit mode widgets
    private TextInputEditText etFullName, etPhone;
    private AutoCompleteTextView etPlaceType;
    private TextInputEditText etSurname, etAddress, etVillage, etDistrict, etState, etPincode;
    private MaterialButton btnSaveProfile;

    // Sections for toggling
    private View sectionViewHeader, sectionEditHeader;
    private View sectionViewDetails, sectionEditDetails;

    private ImageButton btnBack, btnEditToggle;
    private TextView tvToolbarTitle;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Black status + navigation bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.activity_profile);

        bindViews();
        setupPlaceTypeDropdown();
        bindStaticData();
        setupToolbar();
        setupEditLogic();
    }

    private void bindViews() {
        btnBack        = findViewById(R.id.btnBack);
        btnEditToggle  = findViewById(R.id.btnEditToggle);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);

        tvAvatarLetter   = findViewById(R.id.tvAvatarLetter);
        tvFullName       = findViewById(R.id.tvFullName);
        tvPhone          = findViewById(R.id.tvPhone);
        tvPlaceTypeChip  = findViewById(R.id.tvPlaceTypeChip);
        tvSurnameValue   = findViewById(R.id.tvSurnameValue);
        tvContactPhone   = findViewById(R.id.tvContactPhone);
        tvAddressLine    = findViewById(R.id.tvAddressLine);
        tvVillage        = findViewById(R.id.tvVillage);
        tvDistrict       = findViewById(R.id.tvDistrict);
        tvState          = findViewById(R.id.tvState);
        tvPincode        = findViewById(R.id.tvPincode);

        sectionViewHeader   = findViewById(R.id.sectionViewHeader);
        sectionEditHeader   = findViewById(R.id.sectionEditHeader);
        sectionViewDetails  = findViewById(R.id.sectionViewDetails);
        sectionEditDetails  = findViewById(R.id.sectionEditDetails);

        etFullName = findViewById(R.id.etFullName);
        etPhone    = findViewById(R.id.etPhone);
        etPlaceType = findViewById(R.id.etPlaceType);

        etSurname  = findViewById(R.id.etSurname);
        etAddress  = findViewById(R.id.etAddress);
        etVillage  = findViewById(R.id.etVillage);
        etDistrict = findViewById(R.id.etDistrict);
        etState    = findViewById(R.id.etState);
        etPincode  = findViewById(R.id.etPincode);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void setupPlaceTypeDropdown() {
        String[] placeTypes = new String[] { "Village", "City" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                placeTypes
        );
        etPlaceType.setAdapter(adapter);
    }

    /**
     * अभी static data है – बाद में API / Session से replace कर सकते हो।
     */
    private void bindStaticData() {
        String fullName   = "Vansh Mandanka";
        String surname    = "Mandanka";
        String phone      = "+91 63543 55617";
        String placeType  = "Village";
        String address    = "Nana Sakhpur";
        String village    = "Nana Sakhpur";
        String district   = "Pune";
        String state      = "Maharashtra";
        String pincode    = "364470";

        // View mode
        tvFullName.setText(fullName);
        tvPhone.setText(phone);
        tvContactPhone.setText(phone);
        tvSurnameValue.setText(surname);
        tvPlaceTypeChip.setText(placeType);
        tvAddressLine.setText(address);
        tvVillage.setText(village);
        tvDistrict.setText(district);
        tvState.setText(state);
        tvPincode.setText(pincode);

        // Avatar = first letter of full name
        if (fullName != null && !fullName.trim().isEmpty()) {
            char first = Character.toUpperCase(fullName.trim().charAt(0));
            tvAvatarLetter.setText(String.valueOf(first));
        }

        // Prefill edit fields
        etFullName.setText(fullName);
        // remove +91 space if you want only number
        etPhone.setText(phone.replace("+91 ", "").replace("+91", ""));
        etPlaceType.setText(placeType, false);

        etSurname.setText(surname);
        etAddress.setText(address);
        etVillage.setText(village);
        etDistrict.setText(district);
        etState.setText(state);
        etPincode.setText(pincode);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupEditLogic() {
        btnEditToggle.setOnClickListener(v -> toggleEditMode());

        btnSaveProfile.setOnClickListener(v -> {
            String fullName = safeText(tvFullName, etFullName);
            String phone    = safeText(tvPhone, etPhone);
            String address  = safeText(tvAddressLine, etAddress);

            if (fullName.isEmpty()) {
                etFullName.setError("Enter full name");
                return;
            }
            if (phone.isEmpty()) {
                etPhone.setError("Enter phone");
                return;
            }

            String surname   = safeText(tvSurnameValue, etSurname);
            String village   = safeText(tvVillage, etVillage);
            String district  = safeText(tvDistrict, etDistrict);
            String state     = safeText(tvState, etState);
            String pincode   = safeText(tvPincode, etPincode);
            String placeType = safeText(tvPlaceTypeChip, etPlaceType);

            // Update view mode
            tvFullName.setText(fullName);
            tvPhone.setText("+91 " + phone);
            tvContactPhone.setText("+91 " + phone);
            tvSurnameValue.setText(surname);
            tvAddressLine.setText(address);
            tvVillage.setText(village);
            tvDistrict.setText(district);
            tvState.setText(state);
            tvPincode.setText(pincode);
            tvPlaceTypeChip.setText(placeType);

            if (!fullName.trim().isEmpty()) {
                char first = Character.toUpperCase(fullName.trim().charAt(0));
                tvAvatarLetter.setText(String.valueOf(first));
            }

            // यहां future में API call लगा सकते हो (update profile to server)

            Toast.makeText(this, "Profile updated (local)", Toast.LENGTH_SHORT).show();
            setViewMode();
        });
    }

    private void toggleEditMode() {
        if (isEditMode) {
            setViewMode();
        } else {
            setEditMode();
        }
    }

    private void setEditMode() {
        isEditMode = true;
        sectionViewHeader.setVisibility(View.GONE);
        sectionViewDetails.setVisibility(View.GONE);
        sectionEditHeader.setVisibility(View.VISIBLE);
        sectionEditDetails.setVisibility(View.VISIBLE);

        tvToolbarTitle.setText("Edit Profile");
        btnEditToggle.setImageResource(R.drawable.ic_close_24);
    }

    private void setViewMode() {
        isEditMode = false;
        sectionViewHeader.setVisibility(View.VISIBLE);
        sectionViewDetails.setVisibility(View.VISIBLE);
        sectionEditHeader.setVisibility(View.GONE);
        sectionEditDetails.setVisibility(View.GONE);

        tvToolbarTitle.setText("Profile");
        btnEditToggle.setImageResource(R.drawable.ic_edit_24);
    }

    /**
     * Helper: अगर editText खाली हो तो पुरानी value से fallback कर सकता है।
     */
    private String safeText(TextView viewValue, TextView editValue) {
        if (editValue != null && editValue.getText() != null) {
            String t = editValue.getText().toString().trim();
            if (!t.isEmpty()) return t;
        }
        if (viewValue != null && viewValue.getText() != null) {
            return viewValue.getText().toString().trim();
        }
        return "";
    }
}
