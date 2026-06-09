package com.anvexgroup.apnuvyapar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.anvexgroup.apnuvyapar.net.ApiRoutes;
import com.anvexgroup.apnuvyapar.net.VolleySingleton;
import com.anvexgroup.apnuvyapar.utils.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etMobile;
    private MaterialButton btnSendOtp;
    private TextView tvGoRegister, tvLangBadge, tvTitle;

    private BottomSheetDialog otpDialog;
    private CountDownTimer resendTimer;

    private SessionManager session;

    private boolean isSendingOtp = false;
    private boolean isVerifyingOtp = false;
    private String btnIdleText;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String langCode = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                .getString(SessionManager.KEY_LANG_CODE, "en");
        LanguageManager.apply(this, langCode);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_login);
        LanguageManager.enforceLtr(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        session = new SessionManager(this);

        etMobile = findViewById(R.id.etMobile);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        tvLangBadge = findViewById(R.id.tvLangBadge);
        tvTitle = findViewById(R.id.tvTitle);

        I18n.prefetch(this, java.util.Arrays.asList(
                "Language",
                "Login",
                "Send OTP",
                "Loading…",
                "Enter 10-digit mobile",
                "Not registered. Please register.",
                "Failed to send OTP",
                "Network error",
                "Login failed",
                "OTP sent to",
                "Enter 6 digits",
                "Resend in",
                "Already have an account? Log in",
                "Sending OTP...",
                "Verifying OTP...",
                "Invalid/expired OTP"
        ), () -> {
        });

        if (tvLangBadge != null) {
            tvLangBadge.setText(I18n.t(this, "Language") + ": " + session.getLangName());
        }

        if (tvTitle != null) {
            I18n.translateAndApplyText(tvTitle, this);
        }

        if (tvGoRegister != null) {
            I18n.translateAndApplyText(tvGoRegister, this);
        }

        View til = etMobile.getParent() != null ? (View) etMobile.getParent().getParent() : null;
        if (til instanceof TextInputLayout) {
            I18n.translateAndApplyHint((TextInputLayout) til, this);
        }

        btnIdleText = btnSendOtp.getText() == null
                ? I18n.t(this, "Send OTP")
                : I18n.t(this, btnSendOtp.getText().toString());
        btnSendOtp.setText(btnIdleText);

        if (getIntent().hasExtra("prefill_phone")) {
            String pre = getIntent().getStringExtra("prefill_phone");
            if (pre != null) {
                etMobile.setText(pre);
            }
        }

        btnSendOtp.setOnClickListener(v -> {
            if (isSendingOtp) {
                return;
            }

            String mobile = etMobile.getText() == null
                    ? ""
                    : etMobile.getText().toString().trim();

            if (!mobile.matches("^[6-9]\\d{9}$")) {
                etMobile.setError(I18n.t(this, "Enter 10-digit mobile"));
                etMobile.requestFocus();
                return;
            }

            setSending(true);
            sendOtpToServer(mobile);
        });

        tvGoRegister.setOnClickListener(v -> {
            if (isSendingOtp || isVerifyingOtp) {
                return;
            }

            String mobile = etMobile.getText() == null
                    ? ""
                    : etMobile.getText().toString().trim();

            Intent intent = new Intent(this, UserInfoActivity.class);
            if (mobile.matches("^[6-9]\\d{9}$")) {
                intent.putExtra("prefill_phone", mobile);
            }
            startActivity(intent);
        });
    }

    private void setSending(boolean sending) {
        isSendingOtp = sending;
        btnSendOtp.setEnabled(!sending);

        if (sending) {
            if (btnIdleText == null) {
                btnIdleText = I18n.t(this, "Send OTP");
            }
            btnSendOtp.setText(I18n.t(this, "Loading…"));
        } else {
            btnSendOtp.setText(btnIdleText == null ? I18n.t(this, "Send OTP") : btnIdleText);
        }
    }

    private void sendOtpToServer(String mobile) {
        LoadingDialog.showLoading(this, I18n.t(this, "Sending OTP..."));

        try {
            JSONObject body = new JSONObject();
            body.put("phone", mobile);
            body.put("flow", "login");

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiRoutes.SEND_OTP,
                    body,
                    resp -> {
                        boolean ok = resp.optBoolean("ok", resp.optBoolean("success", false));
                        String next = resp.optString("next", "");
                        String errorCode = resp.optString("error_code", "");
                        boolean userExists = resp.optBoolean("user_exists", false);

                        if (!ok && "NOT_REGISTERED".equalsIgnoreCase(errorCode)) {
                            LoadingDialog.hideLoading();
                            Toast.makeText(
                                    this,
                                    I18n.t(this, "Not registered. Please register."),
                                    Toast.LENGTH_SHORT
                            ).show();
                            setSending(false);

                            Intent intent = new Intent(this, UserInfoActivity.class);
                            intent.putExtra("prefill_phone", mobile);
                            startActivity(intent);
                            return;
                        }

                        LoadingDialog.hideLoading();

                        if (userExists || "login_flow".equalsIgnoreCase(next) || "complete_profile".equalsIgnoreCase(next) || ok) {
                            showOtpSheet(mobile);
                        } else {
                            Toast.makeText(
                                    this,
                                    I18n.t(this, "Failed to send OTP"),
                                    Toast.LENGTH_SHORT
                            ).show();
                            setSending(false);
                        }
                    },
                    err -> {
                        LoadingDialog.hideLoading();

                        if (err.networkResponse != null && err.networkResponse.statusCode == 429) {
                            try {
                                String errBody = new String(err.networkResponse.data, "UTF-8");
                                JSONObject errObj = new JSONObject(errBody);
                                String msg = errObj.optString(
                                        "error",
                                        "Too many OTP requests. Please wait."
                                );
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(
                                        this,
                                        "Too many OTP requests. Please wait.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        } else {
                            Toast.makeText(
                                    this,
                                    I18n.t(this, "Network error"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        setSending(false);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> h = new HashMap<>();
                    h.put("Content-Type", "application/json; charset=utf-8");
                    h.put("Accept-Language", I18n.lang(LoginActivity.this));
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            VolleySingleton.getInstance(this).add(req);
        } catch (JSONException e) {
            LoadingDialog.hideLoading();
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setSending(false);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showOtpSheet(String mobile) {
        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
        }

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        isVerifyingOtp = false;

        otpDialog = new BottomSheetDialog(
                this,
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        );

        @SuppressLint("InflateParams")
        View sheet = LayoutInflater.from(this).inflate(R.layout.sheet_otp, null, false);

        otpDialog.setContentView(sheet);
        otpDialog.setCancelable(false);
        otpDialog.setCanceledOnTouchOutside(false);

        TextView tvMobile = sheet.findViewById(R.id.tvMobile);
        TextView tvTimer = sheet.findViewById(R.id.tvTimer);
        TextView tvResend = sheet.findViewById(R.id.tvResend);
        View btnVerify = sheet.findViewById(R.id.btnVerify);
        View btnClose = sheet.findViewById(R.id.btnClose);

        EditText d1 = sheet.findViewById(R.id.d1);
        EditText d2 = sheet.findViewById(R.id.d2);
        EditText d3 = sheet.findViewById(R.id.d3);
        EditText d4 = sheet.findViewById(R.id.d4);
        EditText d5 = sheet.findViewById(R.id.d5);
        EditText d6 = sheet.findViewById(R.id.d6);

        tvMobile.setText(I18n.t(this, "OTP sent to") + " " + mobile);

        setupOtpInputs(d1, d2, d3, d4, d5, d6);
        startResendTimer(tvTimer, tvResend);

        tvResend.setOnClickListener(v -> {
            if (!tvResend.isEnabled() || isSendingOtp || isVerifyingOtp) {
                return;
            }

            clearOtp(d1, d2, d3, d4, d5, d6);
            d1.requestFocus();
            startResendTimer(tvTimer, tvResend);
            setSending(true);
            sendOtpToServer(mobile);
        });

        btnVerify.setOnClickListener(v -> {
            if (isVerifyingOtp) {
                return;
            }

            String code = get(d1) + get(d2) + get(d3) + get(d4) + get(d5) + get(d6);

            if (code.length() != 6) {
                d6.setError(I18n.t(this, "Enter 6 digits"));
                d6.requestFocus();
                return;
            }

            isVerifyingOtp = true;
            btnVerify.setEnabled(false);
            verifyOtpOnServer(mobile, code, tvTimer, btnVerify);
        });

        btnClose.setOnClickListener(v -> {
            if (isVerifyingOtp) {
                return;
            }

            if (resendTimer != null) {
                resendTimer.cancel();
            }

            otpDialog.dismiss();
        });

        setSending(false);

        otpDialog.show();
        d1.requestFocus();

        if (otpDialog.getWindow() != null) {
            otpDialog.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            );
            otpDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

    private void verifyOtpOnServer(String mobile, String otp, View errorField, View btnVerify) {
        LoadingDialog.showLoading(this, I18n.t(this, "Verifying OTP..."));

        try {
            JSONObject body = new JSONObject();
            body.put("phone", mobile);
            body.put("otp", otp);
            body.put("device", "Android/" + android.os.Build.MODEL);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiRoutes.VERIFY_OTP,
                    body,
                    resp -> {
                        LoadingDialog.hideLoading();

                        boolean ok = resp.optBoolean("ok", resp.optBoolean("success", false));
                        if (!ok) {
                            isVerifyingOtp = false;
                            btnVerify.setEnabled(true);

                            if (errorField instanceof TextView) {
                                ((TextView) errorField).setText(I18n.t(this, "Invalid/expired OTP"));
                            }
                            return;
                        }

                        String access = resp.optString("access_token", "");
                        String refresh = resp.optString("refresh_token", "");
                        int userId = resp.optInt("user_id", -1);
                        int expiresIn = resp.optInt("expires_in", 0);

                        if (access.trim().isEmpty()
                                || refresh.trim().isEmpty()
                                || userId <= 0
                                || expiresIn <= 0) {
                            isVerifyingOtp = false;
                            btnVerify.setEnabled(true);

                            if (errorField instanceof TextView) {
                                ((TextView) errorField).setText(I18n.t(this, "Login failed"));
                            }
                            return;
                        }

                        long accessExpiryEpoch =
                                (System.currentTimeMillis() / 1000L) + expiresIn;

                        session.saveTokens(access, refresh, userId, accessExpiryEpoch);
                        session.setOnboarded(true);
                        session.setLoggedIn(true);
                        session.markActive();

                        JSONObject userObj = resp.optJSONObject("user");
                        if (userObj != null) {
                            String name = userObj.optString("full_name", "");
                            String phone = userObj.optString("phone", mobile);
                            session.saveUserProfile(name, phone);
                        }

                        saveCompatibilityAuthPrefs(userId, access, accessExpiryEpoch);

                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        android.util.Log.e(
                                                "LoginActivity",
                                                "FCM token fetch failed after login",
                                                task.getException()
                                        );
                                        return;
                                    }

                                    String fcmToken = task.getResult();
                                    if (!TextUtils.isEmpty(fcmToken)) {
                                        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putString("fcm_token", fcmToken)
                                                .putString("last_uploaded_fcm_token", "")
                                                .apply();
                                    }
                                });

                        if (resendTimer != null) {
                            resendTimer.cancel();
                        }

                        if (otpDialog != null) {
                            otpDialog.dismiss();
                        }

                        boolean isNew = resp.optBoolean("is_new", false);
                        boolean profileComplete = resp.optBoolean("profile_complete", !isNew);
                        String next = resp.optString("next", "");

                        Intent intent;

                        if (isNew || !profileComplete || "complete_profile".equalsIgnoreCase(next)) {
                            intent = new Intent(this, UserInfoActivity.class);
                            intent.putExtra("prefill_phone", mobile);
                            intent.putExtra("from_login_otp", true);
                        } else {
                            intent = new Intent(this, MainActivity.class);
                        }

                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    },
                    err -> {
                        LoadingDialog.hideLoading();
                        isVerifyingOtp = false;
                        btnVerify.setEnabled(true);

                        if (errorField instanceof TextView) {
                            ((TextView) errorField).setText(I18n.t(this, "Network error"));
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> h = new HashMap<>();
                    h.put("Content-Type", "application/json; charset=utf-8");
                    h.put("Accept-Language", I18n.lang(LoginActivity.this));
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            VolleySingleton.getInstance(this).add(req);
        } catch (JSONException e) {
            LoadingDialog.hideLoading();
            isVerifyingOtp = false;
            btnVerify.setEnabled(true);

            if (errorField instanceof TextView) {
                ((TextView) errorField).setText("JSON error");
            }
        }
    }

    private void saveCompatibilityAuthPrefs(int userId, String accessToken, long accessExpiryEpoch) {
        SharedPreferences sp = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE);

        sp.edit()
                .putString(SessionManager.KEY_ACCESS_TOKEN, accessToken)
                .putLong(SessionManager.KEY_ACCESS_EXPIRY_EPOCH, accessExpiryEpoch)
                .putLong(SessionManager.KEY_USER_ID, (long) userId)
                .putBoolean(SessionManager.KEY_ONBOARDED, true)
                .putBoolean(SessionManager.KEY_LOGGED_IN, true)
                .putLong(
                        SessionManager.KEY_LAST_ACTIVE_EPOCH,
                        System.currentTimeMillis() / 1000L
                )
                .apply();
    }

    private void setupOtpInputs(EditText... boxes) {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            EditText et = boxes[i];

            et.setFilters(new android.text.InputFilter[]{
                    new android.text.InputFilter.LengthFilter(1)
            });
            et.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

            et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                }

                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }
            });

            et.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL
                        && et.getText() != null
                        && et.getText().length() == 0
                        && index > 0) {
                    boxes[index - 1].requestFocus();
                    boxes[index - 1].setText("");
                    return true;
                }

                return false;
            });
        }
    }

    private void startResendTimer(TextView tvTimer, TextView tvResend) {
        tvResend.setEnabled(false);
        tvResend.setAlpha(0.5f);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(30_000, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long ms) {
                tvTimer.setText(I18n.t(LoginActivity.this, "Resend in") + " " + (ms / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("");
                tvResend.setEnabled(true);
                tvResend.setAlpha(1f);
            }
        }.start();
    }

    private static void clearOtp(EditText... boxes) {
        for (EditText e : boxes) {
            e.setText("");
        }
    }

    private static String get(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
        }
    }
}