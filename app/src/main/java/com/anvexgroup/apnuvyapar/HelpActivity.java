package com.anvexgroup.apnuvyapar;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    private View statusBarInset;
    private View navigationBarInset;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String langCode = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                .getString("app_lang_code", "en");
        LanguageManager.apply(this, langCode);

        setContentView(R.layout.activity_help);
        LanguageManager.enforceLtr(this);

        bindViews();
        setupBlackSystemBars();
        prefetchAndTranslateViewTree(findViewById(R.id.main));
        setupToolbar();
        setupExpandableCard(R.id.cardStep1, R.id.layoutExpand1, R.id.ivChevron1);
        setupExpandableCard(R.id.cardStep2, R.id.layoutExpand2, R.id.ivChevron2);
        setupExpandableCard(R.id.cardStep3, R.id.layoutExpand3, R.id.ivChevron3);
        setupExpandableCard(R.id.cardStep4, R.id.layoutExpand4, R.id.ivChevron4);

        MaterialButton btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void bindViews() {
        statusBarInset = findViewById(R.id.statusBarInset);
        navigationBarInset = findViewById(R.id.navigationBarInset);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle(I18n.t(this, "App Guide"));
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupBlackSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams topParams = statusBarInset.getLayoutParams();
            if (topParams.height != bars.top) {
                topParams.height = bars.top;
                statusBarInset.setLayoutParams(topParams);
            }

            ViewGroup.LayoutParams bottomParams = navigationBarInset.getLayoutParams();
            if (bottomParams.height != bars.bottom) {
                bottomParams.height = bars.bottom;
                navigationBarInset.setLayoutParams(bottomParams);
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void setupExpandableCard(int cardId, int layoutId, int chevronId) {
        View card = findViewById(cardId);
        View layout = findViewById(layoutId);
        View chevron = findViewById(chevronId);

        if (card != null && layout != null && chevron != null) {
            card.setOnClickListener(v -> {
                boolean isExpanded = layout.getVisibility() == View.VISIBLE;

                if (isExpanded) {
                    layout.setVisibility(View.GONE);
                    chevron.animate().rotation(0f).setDuration(200).start();
                } else {
                    layout.setVisibility(View.VISIBLE);
                    chevron.animate().rotation(180f).setDuration(200).start();
                }
            });
        }
    }

    private void collectTranslatableTexts(View view, List<String> out) {
        if (view == null) return;

        if (view instanceof TextInputLayout) {
            CharSequence hint = ((TextInputLayout) view).getHint();
            if (!TextUtils.isEmpty(hint)) out.add(hint.toString());
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (!(tv instanceof TextInputEditText) && !(tv instanceof AutoCompleteTextView)) {
                CharSequence text = tv.getText();
                if (!TextUtils.isEmpty(text)) out.add(text.toString());
            }
            CharSequence hint = tv.getHint();
            if (!TextUtils.isEmpty(hint)) out.add(hint.toString());
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectTranslatableTexts(vg.getChildAt(i), out);
            }
        }
    }

    private void applyTranslationsToViewTree(View view) {
        if (view == null) return;

        if (view instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) view;
            CharSequence hint = til.getHint();
            if (!TextUtils.isEmpty(hint)) {
                til.setHint(I18n.t(this, hint.toString()));
            }
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (!(tv instanceof TextInputEditText) && !(tv instanceof AutoCompleteTextView)) {
                CharSequence text = tv.getText();
                if (!TextUtils.isEmpty(text)) {
                    tv.setText(I18n.t(this, text.toString()));
                }
            }
            CharSequence hint = tv.getHint();
            if (!TextUtils.isEmpty(hint)) {
                tv.setHint(I18n.t(this, hint.toString()));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyTranslationsToViewTree(vg.getChildAt(i));
            }
        }
    }

    private void prefetchAndTranslateViewTree(View view) {
        if (view == null) return;

        List<String> keys = new ArrayList<>();
        collectTranslatableTexts(view, keys);
        keys.add("App Guide");

        I18n.prefetch(this, keys, () -> {
            applyTranslationsToViewTree(view);
            setupToolbar();
        }, () -> {
            applyTranslationsToViewTree(view);
            setupToolbar();
        });
    }
}