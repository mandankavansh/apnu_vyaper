package com.anvexgroup.sheharsetu;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anvexgroup.sheharsetu.Adapter.ContactSupportAdapter;
import com.anvexgroup.sheharsetu.Adapter.I18n;
import com.anvexgroup.sheharsetu.Adapter.LanguageManager;
import com.anvexgroup.sheharsetu.core.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ContactUsActivity extends AppCompatActivity {

    private MaterialToolbar topBar;
    private RecyclerView recyclerSupport;

    private TextView tvPageTitle, tvPageSubtitle, tvHeading, tvSubHeading, tvResponseTitle, tvResponseNote;

    private static final String SUPPORT_EMAIL = "support@sheharsetu.com";
    private static final String SUPPORT_PHONE = "+916354355617";
    private static final String WHATSAPP_NUMBER = "+916354355617";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String langCode = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                .getString("app_lang_code", "en");
        LanguageManager.apply(this, langCode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact_us);
        LanguageManager.enforceLtr(this);
        bindViews();
        prefetchStaticTextsAndBuildUi();
    }

    private void bindViews() {
        topBar = findViewById(R.id.topBar);
        recyclerSupport = findViewById(R.id.recyclerSupport);

        tvPageTitle = findViewById(R.id.tvPageTitle);
        tvPageSubtitle = findViewById(R.id.tvPageSubtitle);
        tvHeading = findViewById(R.id.tvHeading);
        tvSubHeading = findViewById(R.id.tvSubHeading);
        tvResponseTitle = findViewById(R.id.tvResponseTitle);
        tvResponseNote = findViewById(R.id.tvResponseNote);
    }

    private void prefetchStaticTextsAndBuildUi() {
        List<String> keys = new ArrayList<>();

        View root = findViewById(R.id.root);
        collectTexts(root, keys);

        keys.add("Contact & Support");
        keys.add("Contact and Support");
        keys.add("Need help with Shehar Setu?");
        keys.add("Choose the fastest support option for your issue. We are here to help you quickly and clearly.");
        keys.add("We are here to help");
        keys.add("Connect with our team through email, call, or WhatsApp for fast support.");
        keys.add("Support Hours");
        keys.add("10:00 AM – 7:00 PM • Monday to Saturday");

        keys.add("Email Support");
        keys.add("Best for issue details and screenshots");
        keys.add("Send");

        keys.add("Call Support");
        keys.add("Best for urgent help and quick discussion");
        keys.add("Call");

        keys.add("WhatsApp Chat");
        keys.add("Best for fast chat and easy follow-up");
        keys.add("Chat");

        keys.add("Support - Shehar Setu");
        keys.add("Send email");
        keys.add("No email app found on this device.");
        keys.add("Unable to open dialer.");
        keys.add("WhatsApp is not available.");

        I18n.prefetch(this, keys, () -> {
            setupToolbar();
            setupTexts();
            setupRecycler();
            translateTextsRecursively(findViewById(R.id.root));
        }, () -> {
            setupToolbar();
            setupTexts();
            setupRecycler();
            translateTextsRecursively(findViewById(R.id.root));
        });
    }

    private void setupToolbar() {
        topBar.setTitle(I18n.t(this, "Contact & Support"));
        topBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupTexts() {
        tvPageTitle.setText(I18n.t(this, "Need help with Shehar Setu?"));
        tvPageSubtitle.setText(I18n.t(this, "Choose the fastest support option for your issue. We are here to help you quickly and clearly."));
        tvHeading.setText(I18n.t(this, "We are here to help"));
        tvSubHeading.setText(I18n.t(this, "Connect with our team through email, call, or WhatsApp for fast support."));
        tvResponseTitle.setText(I18n.t(this, "Support Hours"));
        tvResponseNote.setText(I18n.t(this, "10:00 AM – 7:00 PM • Monday to Saturday"));
    }

    private void setupRecycler() {
        recyclerSupport.setLayoutManager(new LinearLayoutManager(this));
        recyclerSupport.setNestedScrollingEnabled(false);

        List<ContactSupportAdapter.SupportItem> items = new ArrayList<>();

        items.add(new ContactSupportAdapter.SupportItem(
                I18n.t(this, "Email Support"),
                SUPPORT_EMAIL,
                I18n.t(this, "Best for issue details and screenshots"),
                I18n.t(this, "Send"),
                R.drawable.gmail
        ));

        items.add(new ContactSupportAdapter.SupportItem(
                I18n.t(this, "Call Support"),
                "+91 63543 55617",
                I18n.t(this, "Best for urgent help and quick discussion"),
                I18n.t(this, "Call"),
                R.drawable.ic_phone_24px
        ));

        items.add(new ContactSupportAdapter.SupportItem(
                I18n.t(this, "WhatsApp Chat"),
                "+91 63543 55617",
                I18n.t(this, "Best for fast chat and easy follow-up"),
                I18n.t(this, "Chat"),
                R.drawable.ic_whatsapp_24
        ));

        ContactSupportAdapter adapter = new ContactSupportAdapter(this, items, item -> {
            if (item.iconRes == R.drawable.gmail) {
                openEmail();
            } else if (item.iconRes == R.drawable.ic_phone_24px) {
                openDialer(SUPPORT_PHONE);
            } else {
                openWhatsApp(WHATSAPP_NUMBER);
            }
        });

        recyclerSupport.setAdapter(adapter);
    }

    private void openEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, I18n.t(this, "Support - Shehar Setu"));

        try {
            startActivity(Intent.createChooser(intent, I18n.t(this, "Send email")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, I18n.t(this, "No email app found on this device."), Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialer(String phoneRaw) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneRaw));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, I18n.t(this, "Unable to open dialer."), Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsApp(String phoneRaw) {
        String phone = phoneRaw.replace("+", "").replace(" ", "");
        Uri uri = Uri.parse("https://wa.me/" + phone);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, I18n.t(this, "WhatsApp is not available."), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void collectTexts(View view, List<String> keys) {
        if (view == null) return;

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            CharSequence txt = tv.getText();
            if (!TextUtils.isEmpty(txt)) {
                keys.add(txt.toString().trim());
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

            CharSequence txt = tv.getText();
            if (!TextUtils.isEmpty(txt)) {
                tv.setText(I18n.t(this, txt.toString()));
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