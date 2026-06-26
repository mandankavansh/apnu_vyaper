package com.anvexgroup.apnuvyapar.Adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.anvexgroup.apnuvyapar.R;

import java.util.List;

public class LanguageAdapter
        extends RecyclerView.Adapter<LanguageAdapter.VH> {

    public interface OnLanguageClick {
        void onLanguageSelected(String[] lang);
    }

    private final List<String[]> languages;
    private final OnLanguageClick onLanguageClick;

    private int selectedPosition = RecyclerView.NO_POSITION;

    public LanguageAdapter(
            @NonNull List<String[]> languages,
            OnLanguageClick onLanguageClick
    ) {
        this.languages = languages;
        this.onLanguageClick = onLanguageClick;

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= languages.size()) {
            return RecyclerView.NO_ID;
        }

        String[] language = languages.get(position);

        if (language == null ||
                language.length == 0 ||
                language[0] == null) {
            return position;
        }

        return language[0].hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);

        return new VH(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH holder,
            int position
    ) {
        String[] language = languages.get(position);

        String nativeName =
                language.length > 1 ? language[1] : "";

        String englishName =
                language.length > 2 ? language[2] : "";

        holder.tvNativeName.setText(nativeName);
        holder.tvEnglishName.setText(englishName);

        holder.tvEnglishName.setVisibility(
                TextUtils.isEmpty(englishName)
                        ? View.GONE
                        : View.VISIBLE
        );

        boolean isSelected = position == selectedPosition;

        applySelectionStyle(holder, isSelected);

        String description = nativeName;

        if (!TextUtils.isEmpty(englishName)) {
            description += ", " + englishName;
        }

        if (isSelected) {
            description += ", selected";
        }

        holder.itemView.setContentDescription(description);
        holder.itemView.setSelected(isSelected);

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition =
                    holder.getBindingAdapterPosition();

            if (clickedPosition == RecyclerView.NO_POSITION ||
                    clickedPosition >= languages.size()) {
                return;
            }

            if (clickedPosition == selectedPosition) {
                return;
            }

            int oldPosition = selectedPosition;
            selectedPosition = clickedPosition;

            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition);
            }

            notifyItemChanged(selectedPosition);

            animateCard(holder.cardView);

            if (onLanguageClick != null) {
                onLanguageClick.onLanguageSelected(
                        languages.get(clickedPosition)
                );
            }
        });
    }

    private void animateCard(@NonNull CardView cardView) {
        cardView.animate().cancel();

        cardView.setScaleX(0.96f);
        cardView.setScaleY(0.96f);

        cardView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();
    }

    private void applySelectionStyle(
            @NonNull VH holder,
            boolean isSelected
    ) {
        if (isSelected) {
            holder.cardView.setCardBackgroundColor(
                    Color.parseColor("#0F172A")
            );

            holder.cardView.setCardElevation(
                    dp(holder.itemView, 7f)
            );

            holder.tvNativeName.setTextColor(Color.WHITE);

            holder.tvEnglishName.setTextColor(
                    Color.parseColor("#CBD5E1")
            );

            holder.tvSelectionCheck.setVisibility(View.VISIBLE);

            holder.itemView.setAlpha(1f);

        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);

            holder.cardView.setCardElevation(
                    dp(holder.itemView, 2f)
            );

            holder.tvNativeName.setTextColor(
                    Color.parseColor("#111827")
            );

            holder.tvEnglishName.setTextColor(
                    Color.parseColor("#64748B")
            );

            holder.tvSelectionCheck.setVisibility(View.GONE);

            holder.itemView.setAlpha(1f);
        }
    }

    private float dp(@NonNull View view, float value) {
        return value *
                view.getResources()
                        .getDisplayMetrics()
                        .density;
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        final TextView tvNativeName;
        final TextView tvEnglishName;
        final TextView tvSelectionCheck;
        final CardView cardView;

        VH(@NonNull View itemView) {
            super(itemView);

            cardView = (CardView) itemView;

            tvNativeName =
                    itemView.findViewById(R.id.tvNativeName);

            tvEnglishName =
                    itemView.findViewById(R.id.tvEnglishName);

            tvSelectionCheck =
                    itemView.findViewById(R.id.tvSelectionCheck);
        }
    }
}