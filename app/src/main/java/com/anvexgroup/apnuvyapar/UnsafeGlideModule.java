package com.anvexgroup.apnuvyapar;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public final class UnsafeGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull android.content.Context context, @NonNull GlideBuilder builder) {
        // default Glide config
    }

    @Override
    public void registerComponents(@NonNull android.content.Context context,
                                   @NonNull Glide glide,
                                   @NonNull Registry registry) {
        // No custom unsafe client
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}