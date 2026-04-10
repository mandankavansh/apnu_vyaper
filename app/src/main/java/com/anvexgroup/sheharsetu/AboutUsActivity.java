package com.anvexgroup.apnuvyapar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anvexgroup.apnuvyapar.Adapter.AboutSectionAdapter;
import com.anvexgroup.apnuvyapar.Adapter.I18n;
import com.anvexgroup.apnuvyapar.Adapter.LanguageManager;
import com.anvexgroup.apnuvyapar.core.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AboutUsActivity extends AppCompatActivity {

    private MaterialToolbar topBar;
    private RecyclerView recyclerAboutSections;

    private TextView tvPageTitle, tvPageSubtitle, tvHeadingApp, tvSubHeadingApp, tvFooterTitle, tvFooterDesc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String langCode = getSharedPreferences(SessionManager.PREFS, MODE_PRIVATE)
                .getString("app_lang_code", "en");
        LanguageManager.apply(this, langCode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about_us);
        LanguageManager.enforceLtr(this);

        bindViews();
        prefetchStaticTextsAndBuildUi();
    }

    private void bindViews() {
        topBar = findViewById(R.id.topBar);
        recyclerAboutSections = findViewById(R.id.recyclerAboutSections);

        tvPageTitle = findViewById(R.id.tvPageTitle);
        tvPageSubtitle = findViewById(R.id.tvPageSubtitle);
        tvHeadingApp = findViewById(R.id.tvHeadingApp);
        tvSubHeadingApp = findViewById(R.id.tvSubHeadingApp);
        tvFooterTitle = findViewById(R.id.tvFooterTitle);
        tvFooterDesc = findViewById(R.id.tvFooterDesc);
    }

    private void prefetchStaticTextsAndBuildUi() {
        List<String> keys = new ArrayList<>();

        View root = findViewById(R.id.root);
        collectTexts(root, keys);

        // Toolbar / explicit Java strings
        keys.add("About Shehar Setu");

        // Hero / page strings actually applied from Java
        keys.add("A modern local marketplace built for clarity");
        keys.add("Shehar Setu connects people, local businesses, and services through a simple and well-structured experience.");
        keys.add("Connecting City, Markets and People");
        keys.add("Shehar Setu is your bridge between local buyers, sellers, and service providers.");
        keys.add("Built for trust, clarity and local growth");
        keys.add("A cleaner interface and structured listing flow help users browse and post with confidence.");

        // Also include XML-only/hardcoded texts currently present in layout
        keys.add("Built for local trust, clarity and growth");
        keys.add("Shehar Setu helps people discover, post and connect across local products, services and opportunities through a simpler and more structured experience.");
        keys.add("TRUSTED LOCAL PLATFORM");
        keys.add("A local-first marketplace for buyers, sellers and service providers.");
        keys.add("Local-first");
        keys.add("Structured");
        keys.add("Supportive");
        keys.add("Designed to feel clear and trustworthy");
        keys.add("Clean structure, clear information and local relevance help users browse and post with more confidence.");

        // Recycler strings
        keys.add("Platform");
        keys.add("What is Shehar Setu?");
        keys.add("Local marketplace made simple");
        keys.add("Shehar Setu is a local marketplace platform designed to simplify buying, selling, and service discovery in your city. From agriculture to services, real estate to daily needs, users can post and explore listings in a structured and easy-to-use format.");

        keys.add("For Buyers");
        keys.add("Discover nearby opportunities");
        keys.add("Buyers can quickly explore nearby products and services with better filtering and more structured details.");
        keys.add("• Discover nearby products and services\n• Filter by category, subcategory and condition\n• View clear, structured information with localized language support");

        keys.add("For Sellers");
        keys.add("Post with confidence");
        keys.add("Sellers and service providers get a guided listing experience with category-based forms and richer details.");
        keys.add("• Post detailed listings with dynamic forms\n• Highlight price, quantity, condition, location and images\n• Reach relevant local users without complexity");

        keys.add("Vision");
        keys.add("Our Vision");
        keys.add("Trusted digital bridge for every city");
        keys.add("To become the trusted digital bridge of every city by connecting citizens, markets, and services with clarity, transparency, and simplicity.");
        keys.add("• Structured\n• Local-first\n• Supportive");

        keys.add("Support");
        keys.add("Support and Feedback");
        keys.add("For any issues, suggestions, or feedback related to Shehar Setu, users can reach out to the development team.");
        keys.add("• Email: support@apnuvyapar.com");

        I18n.prefetch(this, keys, () -> {
            setupToolbar();
            setupTexts();
            setupRecycler();

            // Translate any remaining hardcoded XML text like badge/chips
            translateTextsRecursively(findViewById(R.id.root));
        }, () -> {
            // Fallback: build UI even if API prefetch fails
            setupToolbar();
            setupTexts();
            setupRecycler();

            // Still apply whatever is already cached
            translateTextsRecursively(findViewById(R.id.root));
        });
    }

    private void setupToolbar() {
        topBar.setTitle(I18n.t(this, "About Shehar Setu"));
        topBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupTexts() {
        tvPageTitle.setText(I18n.t(this, "A modern local marketplace built for clarity"));
        tvPageSubtitle.setText(I18n.t(this, "Shehar Setu connects people, local businesses, and services through a simple and well-structured experience."));
        tvHeadingApp.setText(I18n.t(this, "Connecting City, Markets and People"));
        tvSubHeadingApp.setText(I18n.t(this, "Shehar Setu is your bridge between local buyers, sellers, and service providers."));
        tvFooterTitle.setText(I18n.t(this, "Built for trust, clarity and local growth"));
        tvFooterDesc.setText(I18n.t(this, "A cleaner interface and structured listing flow help users browse and post with confidence."));
    }

    private void setupRecycler() {
        recyclerAboutSections.setLayoutManager(new LinearLayoutManager(this));
        recyclerAboutSections.setNestedScrollingEnabled(false);

        List<AboutSectionAdapter.SectionItem> items = new ArrayList<>();

        items.add(new AboutSectionAdapter.SectionItem(
                R.drawable.ic_about_img,
                I18n.t(this, "Platform"),
                I18n.t(this, "What is Shehar Setu?"),
                I18n.t(this, "Local marketplace made simple"),
                I18n.t(this, "Shehar Setu is a local marketplace platform designed to simplify buying, selling, and service discovery in your city. From agriculture to services, real estate to daily needs, users can post and explore listings in a structured and easy-to-use format."),
                null
        ));

        items.add(new AboutSectionAdapter.SectionItem(
                R.drawable.ic_about_buyer,
                I18n.t(this, "For Buyers"),
                I18n.t(this, "Discover nearby opportunities"),
                null,
                I18n.t(this, "Buyers can quickly explore nearby products and services with better filtering and more structured details."),
                I18n.t(this,
                        "• Discover nearby products and services\n" +
                                "• Filter by category, subcategory and condition\n" +
                                "• View clear, structured information with localized language support")
        ));

        items.add(new AboutSectionAdapter.SectionItem(
                R.drawable.ic_about_seller,
                I18n.t(this, "For Sellers"),
                I18n.t(this, "Post with confidence"),
                null,
                I18n.t(this, "Sellers and service providers get a guided listing experience with category-based forms and richer details."),
                I18n.t(this,
                        "• Post detailed listings with dynamic forms\n" +
                                "• Highlight price, quantity, condition, location and images\n" +
                                "• Reach relevant local users without complexity")
        ));

        items.add(new AboutSectionAdapter.SectionItem(
                R.drawable.ic_about_secure,
                I18n.t(this, "Vision"),
                I18n.t(this, "Our Vision"),
                I18n.t(this, "Trusted digital bridge for every city"),
                I18n.t(this, "To become the trusted digital bridge of every city by connecting citizens, markets, and services with clarity, transparency, and simplicity."),
                I18n.t(this, "• Structured\n• Local-first\n• Supportive")
        ));

        items.add(new AboutSectionAdapter.SectionItem(
                R.drawable.ic_about_support,
                I18n.t(this, "Support"),
                I18n.t(this, "Support and Feedback"),
                null,
                I18n.t(this, "For any issues, suggestions, or feedback related to Shehar Setu, users can reach out to the development team."),
                I18n.t(this, "• Email: support@apnuvyapar.com")
        ));

        recyclerAboutSections.setAdapter(new AboutSectionAdapter(this, items));
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