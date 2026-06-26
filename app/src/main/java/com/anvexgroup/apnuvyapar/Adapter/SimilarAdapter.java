package com.anvexgroup.apnuvyapar.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.anvexgroup.apnuvyapar.ProductDetail;
import com.anvexgroup.apnuvyapar.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SimilarAdapter
        extends RecyclerView.Adapter<SimilarAdapter.SimilarViewHolder> {

    private static final int MIN_CARD_WIDTH_DP = 156;
    private static final int MAX_CARD_WIDTH_DP = 220;
    private static final float IMAGE_RATIO = 0.64f;

    private final Context context;
    private final List<Map<String, Object>> items = new ArrayList<>();

    public SimilarAdapter(@NonNull Context context) {
        this.context = context;
        setHasStableIds(true);
    }

    public void setItems(List<Map<String, Object>> newItems) {
        items.clear();

        if (newItems != null) {
            items.addAll(newItems);
        }

        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) {
            return RecyclerView.NO_ID;
        }

        return readInt(items.get(position), "id", position);
    }

    @NonNull
    @Override
    public SimilarViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_similar, parent, false);

        applyResponsiveCardSize(view, parent);

        return new SimilarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SimilarViewHolder holder,
            int position
    ) {
        Map<String, Object> item = items.get(position);

        int listingId = readInt(item, "id", 0);
        String title = readString(item, "title");
        String price = readString(item, "price");
        String city = readString(item, "city");
        String imageUrl = readString(item, "image_url");

        String category = firstNonEmpty(
                readString(item, "subcategory_name"),
                readString(item, "category_name"),
                "Listing"
        );

        holder.simCategory.setText(category.toUpperCase(Locale.getDefault()));
        holder.simTitle.setText(
                TextUtils.isEmpty(title) ? "Untitled listing" : title
        );
        holder.simPrice.setText(
                TextUtils.isEmpty(price) ? "Price not available" : price
        );
        holder.simLocation.setText(
                TextUtils.isEmpty(city) ? "Location not available" : city
        );

        Glide.with(holder.simImage)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.image1)
                .error(R.drawable.image1)
                .into(holder.simImage);

        holder.itemView.setContentDescription(
                holder.simTitle.getText()
                        + ", "
                        + holder.simPrice.getText()
                        + ", "
                        + holder.simLocation.getText()
        );

        holder.itemView.setOnClickListener(v -> {
            if (listingId <= 0) {
                return;
            }

            Intent intent = new Intent(context, ProductDetail.class);
            intent.putExtra("listing_id", listingId);
            intent.putExtra("title", title);
            intent.putExtra("price", price);
            intent.putExtra("city", city);

            context.startActivity(intent);
        });
    }

    /**
     * Gives approximately two cards on a normal phone, slightly fewer on very
     * small phones, and more cards on tablets/landscape without making cards
     * excessively wide.
     */
    private void applyResponsiveCardSize(
            @NonNull View itemView,
            @NonNull ViewGroup parent
    ) {
        int parentWidth = parent.getMeasuredWidth();

        if (parentWidth <= 0) {
            DisplayMetrics metrics =
                    parent.getResources().getDisplayMetrics();
            parentWidth = metrics.widthPixels;
        }

        int availableWidth = parentWidth
                - parent.getPaddingLeft()
                - parent.getPaddingRight();

        int gap = dp(parent, 10);
        int minimumWidth = dp(parent, MIN_CARD_WIDTH_DP);
        int maximumWidth = dp(parent, MAX_CARD_WIDTH_DP);

        /*
         * Two complete cards are preferred on standard phones.
         * On a narrow device the minimum width keeps title/price readable and
         * the horizontal list naturally shows part of the next card.
         */
        int calculatedWidth = (availableWidth - gap) / 2;
        int cardWidth = Math.max(
                minimumWidth,
                Math.min(maximumWidth, calculatedWidth)
        );

        ViewGroup.LayoutParams itemParams = itemView.getLayoutParams();
        itemParams.width = cardWidth;
        itemParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        itemView.setLayoutParams(itemParams);

        ImageView image = itemView.findViewById(R.id.simImage);

        if (image != null) {
            ViewGroup.LayoutParams imageParams = image.getLayoutParams();

            int minimumImageHeight = dp(parent, 108);
            int maximumImageHeight = dp(parent, 142);
            int calculatedImageHeight =
                    Math.round(cardWidth * IMAGE_RATIO);

            imageParams.height = Math.max(
                    minimumImageHeight,
                    Math.min(maximumImageHeight, calculatedImageHeight)
            );

            image.setLayoutParams(imageParams);
        }
    }

    private static int dp(@NonNull View view, int value) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        value,
                        view.getResources().getDisplayMetrics()
                )
        );
    }

    private static String readString(
            @NonNull Map<String, Object> item,
            @NonNull String key
    ) {
        Object value = item.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int readInt(
            @NonNull Map<String, Object> item,
            @NonNull String key,
            int fallback
    ) {
        Object value = item.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value != null) {
            try {
                return Integer.parseInt(
                        String.valueOf(value).trim()
                );
            } catch (Exception ignored) {
            }
        }

        return fallback;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }

        return "";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class SimilarViewHolder
            extends RecyclerView.ViewHolder {

        final MaterialCardView similarCard;
        final ImageView simImage;
        final TextView simCategory;
        final TextView simTitle;
        final TextView simPrice;
        final TextView simLocation;

        SimilarViewHolder(@NonNull View itemView) {
            super(itemView);

            similarCard =
                    itemView.findViewById(R.id.similarCard);
            simImage =
                    itemView.findViewById(R.id.simImage);
            simCategory =
                    itemView.findViewById(R.id.simCategory);
            simTitle =
                    itemView.findViewById(R.id.simTitle);
            simPrice =
                    itemView.findViewById(R.id.simPrice);
            simLocation =
                    itemView.findViewById(R.id.simLocation);
        }
    }
}
