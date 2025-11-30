package com.infowave.sheharsetu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.ImageButton;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    // View mode widgets (main screen)
    private TextView tvAvatarLetter, tvFullName, tvPhone, tvPlaceTypeChip;
    private TextView tvSurnameValue, tvContactPhone;
    private TextView tvAddressLine, tvVillage, tvDistrict, tvState, tvPincode;

    private ImageButton btnBack;
    private TextView tvToolbarTitle;

    // Bottom-right FAB for edit
    private FloatingActionButton btnEditToggle;

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
        bindStaticData();
        setupToolbar();
        setupEditFab();
    }

    private void bindViews() {
        btnBack        = findViewById(R.id.btnBack);
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

        // FAB (bottom-right)
        btnEditToggle = findViewById(R.id.btnEditToggle);
    }

    /**
     * अभी static data है – बाद में API / Session से replace कर सकते हो।
     */
    private void bindStaticData() {
        String fullName   = "Vansh Mandanka";
        String surname    = "Mandanka";
        String phoneFull  = "+91 63543 55617";   // Display format
        String placeType  = "Village";
        String address    = "Nana Sakhpur";
        String village    = "Nana Sakhpur";
        String district   = "Pune";
        String state      = "Maharashtra";
        String pincode    = "364470";

        // View mode
        tvFullName.setText(fullName);
        tvPhone.setText(phoneFull);
        tvContactPhone.setText(phoneFull);
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
    }

    private void setupToolbar() {
        tvToolbarTitle.setText("Profile");
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupEditFab() {
        btnEditToggle.setOnClickListener(v -> showEditProfileBottomSheet());
    }

    /**
     * Professional bottom sheet for editing profile.
     */
    private void showEditProfileBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(
                this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog
        );

        View view = LayoutInflater.from(this)
                .inflate(R.layout.layout_profile_edit_bottom_sheet, null, false);

        dialog.setContentView(view);

        // --- Bottom sheet fields ---
        TextInputEditText etFullNameBottom   = view.findViewById(R.id.etFullNameBottom);
        TextInputEditText etSurnameBottom    = view.findViewById(R.id.etSurnameBottom);
        TextInputEditText etPhoneBottom      = view.findViewById(R.id.etPhoneBottom);
        AutoCompleteTextView etPlaceTypeBottom = view.findViewById(R.id.etPlaceTypeBottom);
        TextInputEditText etAddressBottom    = view.findViewById(R.id.etAddressBottom);
        TextInputEditText etVillageBottom    = view.findViewById(R.id.etVillageBottom);
        TextInputEditText etDistrictBottom   = view.findViewById(R.id.etDistrictBottom);
        TextInputEditText etStateBottom      = view.findViewById(R.id.etStateBottom);
        TextInputEditText etPincodeBottom    = view.findViewById(R.id.etPincodeBottom);

        MaterialButton btnCancelBottom       = view.findViewById(R.id.btnCancelBottom);
        MaterialButton btnSaveProfileBottom  = view.findViewById(R.id.btnSaveProfileBottom);

        // --- Prefill current values from view mode ---
        String currentFullName   = tvFullName.getText() != null ? tvFullName.getText().toString().trim() : "";
        String currentSurname    = tvSurnameValue.getText() != null ? tvSurnameValue.getText().toString().trim() : "";
        String currentPhoneFull  = tvContactPhone.getText() != null ? tvContactPhone.getText().toString().trim() : "";
        String currentPlaceType  = tvPlaceTypeChip.getText() != null ? tvPlaceTypeChip.getText().toString().trim() : "";
        String currentAddress    = tvAddressLine.getText() != null ? tvAddressLine.getText().toString().trim() : "";
        String currentVillage    = tvVillage.getText() != null ? tvVillage.getText().toString().trim() : "";
        String currentDistrict   = tvDistrict.getText() != null ? tvDistrict.getText().toString().trim() : "";
        String currentState      = tvState.getText() != null ? tvState.getText().toString().trim() : "";
        String currentPincode    = tvPincode.getText() != null ? tvPincode.getText().toString().trim() : "";

        // Strip +91 from phone for edit box (only digits)
        String phoneDigits = currentPhoneFull
                .replace("+91", "")
                .replace(" ", "")
                .trim();

        if (etFullNameBottom != null)  etFullNameBottom.setText(currentFullName);
        if (etSurnameBottom != null)   etSurnameBottom.setText(currentSurname);
        if (etPhoneBottom != null)     etPhoneBottom.setText(phoneDigits);
        if (etAddressBottom != null)   etAddressBottom.setText(currentAddress);
        if (etVillageBottom != null)   etVillageBottom.setText(currentVillage);
        if (etDistrictBottom != null)  etDistrictBottom.setText(currentDistrict);
        if (etStateBottom != null)     etStateBottom.setText(currentState);
        if (etPincodeBottom != null)   etPincodeBottom.setText(currentPincode);
        if (etPlaceTypeBottom != null) etPlaceTypeBottom.setText(currentPlaceType, false);

        // --- Setup dropdown for Place Type (Village / City) ---
        if (etPlaceTypeBottom != null) {
            String[] placeTypes = new String[] { "Village", "City" };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    placeTypes
            );
            etPlaceTypeBottom.setAdapter(adapter);
        }

        // Cancel
        if (btnCancelBottom != null) {
            btnCancelBottom.setOnClickListener(v -> dialog.dismiss());
        }

        // Save
        if (btnSaveProfileBottom != null) {
            btnSaveProfileBottom.setOnClickListener(v -> {

                String newFullName  = etFullNameBottom != null && etFullNameBottom.getText() != null
                        ? etFullNameBottom.getText().toString().trim() : "";
                String newSurname   = etSurnameBottom != null && etSurnameBottom.getText() != null
                        ? etSurnameBottom.getText().toString().trim() : "";
                String newPhone     = etPhoneBottom != null && etPhoneBottom.getText() != null
                        ? etPhoneBottom.getText().toString().trim() : "";
                String newPlaceType = etPlaceTypeBottom != null && etPlaceTypeBottom.getText() != null
                        ? etPlaceTypeBottom.getText().toString().trim() : "";
                String newAddress   = etAddressBottom != null && etAddressBottom.getText() != null
                        ? etAddressBottom.getText().toString().trim() : "";
                String newVillage   = etVillageBottom != null && etVillageBottom.getText() != null
                        ? etVillageBottom.getText().toString().trim() : "";
                String newDistrict  = etDistrictBottom != null && etDistrictBottom.getText() != null
                        ? etDistrictBottom.getText().toString().trim() : "";
                String newState     = etStateBottom != null && etStateBottom.getText() != null
                        ? etStateBottom.getText().toString().trim() : "";
                String newPincode   = etPincodeBottom != null && etPincodeBottom.getText() != null
                        ? etPincodeBottom.getText().toString().trim() : "";

                // ---- Basic validation (professional but simple) ----
                if (newFullName.isEmpty()) {
                    if (etFullNameBottom != null) etFullNameBottom.setError("Enter full name");
                    return;
                }
                if (newPhone.isEmpty()) {
                    if (etPhoneBottom != null) etPhoneBottom.setError("Enter phone");
                    return;
                }
                if (newPhone.length() < 10) {
                    if (etPhoneBottom != null) etPhoneBottom.setError("Enter valid 10-digit phone");
                    return;
                }
                if (newPincode.isEmpty()) {
                    if (etPincodeBottom != null) etPincodeBottom.setError("Enter pincode");
                    return;
                }

                // ---- Update main view values ----
                String displayPhone = "+91 " + newPhone;

                tvFullName.setText(newFullName);
                tvSurnameValue.setText(newSurname.isEmpty() ? newSurname : newSurname);
                tvContactPhone.setText(displayPhone);
                tvPhone.setText(displayPhone);
                tvPlaceTypeChip.setText(newPlaceType.isEmpty() ? "Village" : newPlaceType);
                tvAddressLine.setText(newAddress);
                tvVillage.setText(newVillage);
                tvDistrict.setText(newDistrict);
                tvState.setText(newState);
                tvPincode.setText(newPincode);

                // Avatar update
                if (!newFullName.trim().isEmpty()) {
                    char first = Character.toUpperCase(newFullName.trim().charAt(0));
                    tvAvatarLetter.setText(String.valueOf(first));
                }

                // TODO: यहां future में API call लगा सकते हो (update profile to server)

                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

}
