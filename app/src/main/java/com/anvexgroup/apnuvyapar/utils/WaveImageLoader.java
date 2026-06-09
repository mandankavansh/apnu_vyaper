package com.anvexgroup.apnuvyapar.utils;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

public final class WaveImageLoader {

    private WaveImageLoader() {
    }

    public static void load(@NonNull ImageView target, Object source, @DrawableRes int errorRes) {
        if (isEmpty(source)) {
            clearToFallback(target, errorRes);
            return;
        }
        base(target, source, errorRes, false)
                .into(target);
    }

    public static void loadCenterCrop(@NonNull ImageView target, Object source, @DrawableRes int errorRes) {
        if (isEmpty(source)) {
            clearToFallback(target, errorRes);
            return;
        }
        base(target, source, errorRes, false)
                .centerCrop()
                .into(target);
    }

    public static void loadCenterCropCrossFade(@NonNull ImageView target, Object source, @DrawableRes int errorRes) {
        if (isEmpty(source)) {
            clearToFallback(target, errorRes);
            return;
        }
        base(target, source, errorRes, false)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .into(target);
    }

    public static void loadCircleCrop(@NonNull ImageView target, Object source, @DrawableRes int errorRes) {
        if (isEmpty(source)) {
            clearToFallback(target, errorRes);
            return;
        }
        base(target, source, errorRes, true)
                .circleCrop()
                .into(target);
    }

    public static void loadCircleCropCached(@NonNull ImageView target, Object source, @DrawableRes int errorRes) {
        if (isEmpty(source)) {
            clearToFallback(target, errorRes);
            return;
        }
        base(target, source, errorRes, true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(target);
    }

    public static void clearToFallback(@NonNull ImageView target, @DrawableRes int fallbackRes) {
        Glide.with(target).clear(target);
        target.setImageResource(fallbackRes);
    }

    public static void clear(@NonNull ImageView target) {
        Glide.with(target).clear(target);
    }

    private static RequestBuilder<android.graphics.drawable.Drawable> base(
            @NonNull ImageView target,
            Object source,
            @DrawableRes int errorRes,
            boolean circle
    ) {
        return Glide.with(target)
                .load(source)
                .placeholder(new WavePlaceholderDrawable(circle))
                .error(errorRes)
                .fallback(errorRes);
    }

    private static boolean isEmpty(Object source) {
        if (source == null) return true;
        if (source instanceof String) return TextUtils.isEmpty((String) source);
        return false;
    }
}
