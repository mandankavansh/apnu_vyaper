package com.anvexgroup.apnuvyapar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set status bar color to black
        getWindow().setStatusBarColor(android.graphics.Color.BLACK);
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        new androidx.core.view.WindowInsetsControllerCompat(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);
        
        String langCode = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                .getString("app_lang_code", "en");
        LanguageManager.apply(this, langCode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help);
        LanguageManager.enforceLtr(this);
        View mainView = findViewById(R.id.main);
        prefetchAndTranslateViewTree(mainView);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Setup Expandable Cards
        setupExpandableCard(R.id.cardStep1, R.id.layoutExpand1, R.id.ivChevron1);
        setupExpandableCard(R.id.cardStep2, R.id.layoutExpand2, R.id.ivChevron2);
        setupExpandableCard(R.id.cardStep3, R.id.layoutExpand3, R.id.ivChevron3);
        setupExpandableCard(R.id.cardStep4, R.id.layoutExpand4, R.id.ivChevron4);

        com.google.android.material.button.MaterialButton btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupExpandableCard(int cardId, int layoutId, int chevronId) {
        View card = findViewById(cardId);
        View layout = findViewById(layoutId);
        View chevron = findViewById(chevronId);

        if (card != null && layout != null && chevron != null) {
            card.setOnClickListener(v -> {
                boolean isExpanded = layout.getVisibility() == View.VISIBLE;
                
                // Toggle visibility with simple animation
                if (isExpanded) {
                    layout.setVisibility(View.GONE);
                    chevron.animate().rotation(0).setDuration(200).start();
                } else {
                    layout.setVisibility(View.VISIBLE);
                    chevron.animate().rotation(180).setDuration(200).start();
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

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
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

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyTranslationsToViewTree(vg.getChildAt(i));
            }
        }
    }

    private void prefetchAndTranslateViewTree(View view) {
        if (view == null) return;
        List<String> keys = new ArrayList<>();
        collectTranslatableTexts(view, keys);
        I18n.prefetch(this, keys, () -> applyTranslationsToViewTree(view), () -> applyTranslationsToViewTree(view));
    }
}