package com.infowave.sheharsetu;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class AboutUsActivity extends AppCompatActivity {

    private MaterialToolbar topBar;

    // Text views
    private TextView tvHeadingApp, tvSubHeadingApp;
    private TextView tvAboutTitle, tvAboutDesc;
    private TextView tvChipMarketplace, tvChipMultiCategory, tvChipDynamicForms;
    private TextView tvBuyersTitle, tvBuyersPoints;
    private TextView tvSellersTitle, tvSellersPoints;
    private TextView tvVisionTitle, tvVisionDesc;
    private TextView tvValueStructured, tvValueLocalFirst, tvValueSupportive;
    private TextView tvSupportTitle, tvSupportDesc, tvSupportEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about_us);

        bindViews();
        setupToolbar();
        setupTexts();
    }

    private void bindViews() {
        topBar = findViewById(R.id.topBar);

        tvHeadingApp        = findViewById(R.id.tvHeadingApp);
        tvSubHeadingApp     = findViewById(R.id.tvSubHeadingApp);

        tvAboutTitle        = findViewById(R.id.tvAboutTitle);
        tvAboutDesc         = findViewById(R.id.tvAboutDesc);
        tvChipMarketplace   = findViewById(R.id.tvChipMarketplace);
        tvChipMultiCategory = findViewById(R.id.tvChipMultiCategory);
        tvChipDynamicForms  = findViewById(R.id.tvChipDynamicForms);

        tvBuyersTitle       = findViewById(R.id.tvBuyersTitle);
        tvBuyersPoints      = findViewById(R.id.tvBuyersPoints);
        tvSellersTitle      = findViewById(R.id.tvSellersTitle);
        tvSellersPoints     = findViewById(R.id.tvSellersPoints);

        tvVisionTitle       = findViewById(R.id.tvVisionTitle);
        tvVisionDesc        = findViewById(R.id.tvVisionDesc);
        tvValueStructured   = findViewById(R.id.tvValueStructured);
        tvValueLocalFirst   = findViewById(R.id.tvValueLocalFirst);
        tvValueSupportive   = findViewById(R.id.tvValueSupportive);

        tvSupportTitle      = findViewById(R.id.tvSupportTitle);
        tvSupportDesc       = findViewById(R.id.tvSupportDesc);
        tvSupportEmail      = findViewById(R.id.tvSupportEmail);
    }

    private void setupToolbar() {
        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> onBackPressed());
            // Title can also be controlled from here if needed:
            topBar.setTitle("About Shehar Setu");
        }
    }

    /**
     * All visible text for the About screen is defined here,
     * so you can later plug in I18n / dynamic content easily.
     */
    private void setupTexts() {

        // Hero section
        tvHeadingApp.setText("Connecting City, Markets and People");
        tvSubHeadingApp.setText(
                "Shehar Setu is your bridge between local buyers, sellers and service providers."
        );

        // About section
        tvAboutTitle.setText("What is Shehar Setu?");
        tvAboutDesc.setText(
                "Shehar Setu is a local marketplace platform designed to simplify buying, " +
                        "selling and service discovery in your city. From agriculture to services, " +
                        "real estate to daily needs, citizens can post and explore listings in a " +
                        "structured, easy-to-use format."
        );
        tvChipMarketplace.setText("Local Marketplace");
        tvChipMultiCategory.setText("Multi-category");
        tvChipDynamicForms.setText("Dynamic Forms");

        // Buyers / Sellers
        tvBuyersTitle.setText("For Buyers");
        tvBuyersPoints.setText(
                "- Discover nearby products and services.\n" +
                        "- Filter by category, subcategory and condition.\n" +
                        "- View clear, structured information with localized language support."
        );

        tvSellersTitle.setText("For Sellers and Service Providers");
        tvSellersPoints.setText(
                "- Post detailed listings with category-specific dynamic forms.\n" +
                        "- Highlight condition, price, quantity, location and images.\n" +
                        "- Reach relevant local users without complexity."
        );

        // Vision & values
        tvVisionTitle.setText("Our Vision");
        tvVisionDesc.setText(
                "To become the trusted digital bridge of every city – connecting citizens, " +
                        "markets and services with clarity, transparency and simplicity."
        );
        tvValueStructured.setText("Structured");
        tvValueLocalFirst.setText("Local-first");
        tvValueSupportive.setText("Supportive");

        // Support
        tvSupportTitle.setText("Support and Feedback");
        tvSupportDesc.setText(
                "For any issues, suggestions or feedback related to Shehar Setu, you can " +
                        "reach out to the development team."
        );
        tvSupportEmail.setText("Email: support@sheharsetu.com");
    }
}
