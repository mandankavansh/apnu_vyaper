package com.anvexgroup.sheharsetu;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.anvexgroup.sheharsetu.utils.TimeUtils;

import java.util.List;

/**
 * RecyclerView adapter for the notifications inbox.
 *
 * Key fix: relative-time calculation is now delegated to {@link TimeUtils#relativeTime},
 * which correctly parses DB timestamps as UTC before computing the diff.
 * The old inline formatTimeAgo() used a timezone-unaware SDF which caused UTC
 * timestamps to be misread as IST, inflating the displayed age by ~5.5h.
 *
 * Design decisions:
 * - All business logic (mark-read, navigate) stays in the Activity.
 * - Adapter only renders state and fires callbacks.
 * - No heavy operations inside onBindViewHolder.
 */
public final class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        /** Called when the user taps a notification row. */
        void onNotificationClick(int position, NotificationItem item);
    }

    private final List<NotificationItem> items;
    private final OnItemClickListener    listener;

    public NotificationsAdapter(List<NotificationItem> items, OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NotificationItem item = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvTitle.setText(item.getTitle());
        h.tvBody.setText(item.getBody());

        // Use shared TimeUtils so UTC DB timestamps are parsed correctly.
        h.tvTime.setText(TimeUtils.relativeTime(item.getCreatedAt()));

        // Unread visual state
        boolean unread = !item.isRead();
        h.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        h.viewUnreadStrip.setVisibility(unread ? View.VISIBLE : View.GONE);

        if (unread) {
            h.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextPrimary));
            h.card.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.colorSurface));
            h.card.setAlpha(1f);
        } else {
            h.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextSecondary));
            h.card.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.colorBackground));
            h.card.setAlpha(0.9f);
        }

        // Optional image
        if (item.hasImage() && h.ivNotifIcon != null) {
            Glide.with(ctx)
                    .load(item.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .placeholder(R.drawable.ic_bell_vector)
                    .error(R.drawable.ic_bell_vector)
                    .into(h.ivNotifIcon);
        } else if (h.ivNotifIcon != null) {
            Glide.with(ctx).clear(h.ivNotifIcon);
            h.ivNotifIcon.setImageResource(R.drawable.ic_bell_vector);
        }

        // Click
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(h.getAdapterPosition(), item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView        ivNotifIcon;
        final View             viewUnreadDot;
        final View             viewUnreadStrip;
        final TextView         tvTitle;
        final TextView         tvBody;
        final TextView         tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card            = (MaterialCardView) itemView;
            ivNotifIcon     = itemView.findViewById(R.id.ivNotifIcon);
            viewUnreadDot   = itemView.findViewById(R.id.viewUnreadDot);
            viewUnreadStrip = itemView.findViewById(R.id.viewUnreadStrip);
            tvTitle         = itemView.findViewById(R.id.tvNotifTitle);
            tvBody          = itemView.findViewById(R.id.tvNotifBody);
            tvTime          = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}
