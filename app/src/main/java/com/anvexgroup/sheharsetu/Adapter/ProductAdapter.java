package com.anvexgroup.sheharsetu.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.anvexgroup.sheharsetu.ProductDetail;
import com.anvexgroup.sheharsetu.R;
import com.anvexgroup.sheharsetu.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    private final List<Map<String, Object>> items = new ArrayList<>();
    private final Context ctx;

    public interface OnContactClickListener {
        void onContactClick(@NonNull Map<String, Object> product);
    }

    private OnContactClickListener contactClickListener;

    @DrawableRes
    private final int placeholderIcon = R.drawable.ic_placeholder_circle;

    public ProductAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public void setOnContactClickListener(OnContactClickListener l) {
        this.contactClickListener = l;
    }

    public void setItems(List<Map<String, Object>> list) {
        items.clear();
        if (list != null)
            items.addAll(list);
        notifyDataSetChanged();
    }

    /** Append items for infinite scroll pagination */
    public void addItems(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty())
            return;
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Map<String, Object> p = items.get(pos);

        // --- Text fields ---
        String title = getString(p, "title", "");
        String price = getString(p, "price", "");
        String city = getString(p, "city", "");
        int id = getInt(p, "id", 0);
        if (id == 0)
            id = getInt(p, "listing_id", 0);

        h.title.setText(title);

        // Format price with ₹ prefix
        String displayPrice;
        if (TextUtils.isEmpty(price) || price.equals("null") || price.equals("0")) {
            displayPrice = I18n.t(ctx, "Price on Request");
        } else {
            // Already has ₹? show as-is, else add prefix
            displayPrice = price.startsWith("₹") ? price : "₹ " + price;
        }
        h.price.setText(displayPrice);

        String displayCity = TextUtils.isEmpty(city) ? "" : city;
        String distance = getString(p, "distance", "");
        if (!TextUtils.isEmpty(distance) && !distance.equals("0") && !distance.equals("null")) {
            try {
                double distVal = Double.parseDouble(distance);
                displayCity += (TextUtils.isEmpty(displayCity) ? "" : " • ")
                        + String.format(java.util.Locale.getDefault(), "%.1f km", distVal);
            } catch (Exception e) {
                displayCity += (TextUtils.isEmpty(displayCity) ? "" : " • ") + distance + " km";
            }
        }

        // Apply location to view  ← THIS WAS THE MISSING LINE causing "Rajkot" to stick
        if (h.city != null) {
            h.city.setText(displayCity);
        }

        // Prefer raw UTC timestamp for accurate client-side relative time.
        // Fall back to server's pre-formatted string only if no raw timestamp present.
        String createdAt = getString(p, "created_at", "");
        String posted;
        if (!TextUtils.isEmpty(createdAt) && !"null".equalsIgnoreCase(createdAt)) {
            posted = TimeUtils.relativeTime(createdAt);
        } else {
            posted = getString(p, "posted_when", getString(p, "posted_time", ""));
        }

        // --- Posted Time ---
        if (h.posted != null) {
            h.posted.setText(posted);
        }

        String status = getString(p, "status", "active");
        boolean isSold = "sold".equalsIgnoreCase(status);
        if (h.soldBadge != null) {
            if (isSold) {
                h.soldBadge.setText(I18n.t(ctx, "SOLD"));
                h.soldBadge.setTextColor(ctx.getResources().getColor(android.R.color.white));
                h.soldBadge.setBackgroundResource(R.drawable.bg_sold_badge);
                h.soldBadge.setVisibility(View.VISIBLE);
            } else {
                // If not sold, show "New" as per mockup style
                h.soldBadge.setText(I18n.t(ctx, "New"));
                h.soldBadge.setTextColor(android.graphics.Color.parseColor("#27AE60"));
                h.soldBadge.setBackgroundResource(R.drawable.bg_new_badge);
                h.soldBadge.setVisibility(View.VISIBLE);
            }
        }

        int finalId = id;
        h.itemView.setOnClickListener(v -> openPdp(finalId, title, price, city, posted, 0));

        // --- Load images ---
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) p.get("images");
        String singleImageUrl = "";
        if (images != null && !images.isEmpty()) {
            singleImageUrl = images.get(0);
        }
        if (TextUtils.isEmpty(singleImageUrl)) {
            singleImageUrl = getString(p, "imageUrl", getString(p, "image_url", ""));
        }

        if (images != null && images.size() > 1) {
            // --- Multiple images: use ViewPager2 slider ---
            h.productImage.setVisibility(View.GONE);
            h.slider.setVisibility(View.VISIBLE);
            h.dotIndicator.setVisibility(View.VISIBLE);

            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(images, v -> {
                openPdp(finalId, title, price, city, posted, 0);
            });
            h.slider.setAdapter(sliderAdapter);
            h.slider.setCurrentItem(0, false);

            // Setup dot indicators
            setupDots(h.dotIndicator, images.size(), 0);

            h.slider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    setupDots(h.dotIndicator, images.size(), position);
                }
            });

        } else {
            // --- Single image or no image: use simple ImageView ---
            h.slider.setVisibility(View.GONE);
            h.dotIndicator.setVisibility(View.GONE);
            h.productImage.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(singleImageUrl)) {
                Glide.with(h.productImage.getContext())
                        .load(singleImageUrl)
                        .placeholder(placeholderIcon)
                        .error(placeholderIcon)
                        .centerCrop()
                        .into(h.productImage);
            } else {
                h.productImage.setImageResource(placeholderIcon);
            }
        }

        // Contact button (might be hidden in grid view)
        if (h.btn != null) {
            h.btn.setOnClickListener(v -> {
                if (contactClickListener != null) {
                    contactClickListener.onContactClick(p);
                } else {
                    openPdp(finalId, title, price, city, posted, 0);
                }
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /* ===================== Dot Indicator ===================== */
    private void setupDots(LinearLayout dotContainer, int count, int selected) {
        if (dotContainer == null)
            return;
        dotContainer.removeAllViews();

        for (int i = 0; i < count; i++) {
            View dot = new View(dotContainer.getContext());

            int size = (i == selected) ? dpToPx(7) : dpToPx(5);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            dot.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);

            if (i == selected) {
                shape.setColor(0xFFFFFFFF); // white
            } else {
                shape.setColor(0x80FFFFFF); // semi-transparent white
            }

            dot.setBackground(shape);
            dotContainer.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * dp);
    }

    /* ===================== ViewHolder ===================== */
    static class VH extends RecyclerView.ViewHolder {
        ImageView productImage;
        ViewPager2 slider;
        LinearLayout dotIndicator;
        TextView title, price, city, soldBadge, posted;
        View btn, btnFavorite;

        VH(@NonNull View v) {
            super(v);
            productImage = v.findViewById(R.id.productImage);
            slider = v.findViewById(R.id.productImageSlider);
            dotIndicator = v.findViewById(R.id.dotIndicator);
            title = v.findViewById(R.id.tvTitle);
            price = v.findViewById(R.id.tvPrice);
            city = v.findViewById(R.id.tvCity);
            btn = v.findViewById(R.id.btnContact);
            // soldBadge = v.findViewById(R.id.tvSoldBadge);
            posted = v.findViewById(R.id.tvPosted);
            // btnFavorite = v.findViewById(R.id.btnFavorite);
        }

    }

    /* ===================== Helpers ===================== */
    private void openPdp(int id, String title, String price, String city, String posted, int imgRes) {
        Intent i = new Intent(ctx, ProductDetail.class);
        i.putExtra("listing_id", id);
        i.putExtra("title", title);
        i.putExtra("price", price);
        i.putExtra("city", city);
        i.putExtra("posted", TextUtils.isEmpty(posted) ? "" : posted);
        i.putExtra("desc", buildDescForPdp(title, price, city));
        // We no longer pass resource IDs for main flow, but keeping compatible for now
        i.putExtra("images", new int[] { imgRes != 0 ? imgRes : placeholderIcon });
        ctx.startActivity(i);
    }

    private static String buildDescForPdp(String title, String price, String city) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(title))
            sb.append(title);
        if (!TextUtils.isEmpty(price))
            sb.append(" • ").append(price);
        if (!TextUtils.isEmpty(city))
            sb.append(" • ").append(city);
        return sb.toString();
    }

    private static String getString(Map<String, Object> m, String key, String def) {
        if (m == null)
            return def;
        Object o = m.get(key);
        if (o == null)
            return def;
        String s = String.valueOf(o);
        return TextUtils.isEmpty(s) ? def : s;
    }

    private static int getInt(Map<String, Object> m, String key, int def) {
        if (m == null)
            return def;
        Object o = m.get(key);
        if (o instanceof Integer)
            return (Integer) o;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ignore) {
        }
        return def;
    }
}
