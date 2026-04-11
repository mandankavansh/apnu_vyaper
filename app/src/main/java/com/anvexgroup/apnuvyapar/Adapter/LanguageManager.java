package com.anvexgroup.apnuvyapar.Adapter;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

public class LanguageManager {

    public static void apply(Activity activity, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(activity.getResources().getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            //noinspection deprecation
            config.locale = locale;
        }

        // Force app layout to stay LTR
        config.setLayoutDirection(Locale.ENGLISH);

        activity.getResources().updateConfiguration(
                config,
                activity.getResources().getDisplayMetrics()
        );
    }

    public static void enforceLtr(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                decor.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }

            View content = activity.findViewById(android.R.id.content);
            if (content != null) {
                forceLtrRecursive(content);
            }
        } catch (Exception ignored) {
        }
    }

    private static void forceLtrRecursive(View view) {
        if (view == null) return;

        view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            // keep text aligned from left side of screen
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            int gravity = tv.getGravity();

            gravity &= ~Gravity.RIGHT;
            gravity &= ~Gravity.END;

            if ((gravity & Gravity.CENTER_HORIZONTAL) == 0) {
                gravity |= Gravity.START;
            }

            tv.setGravity(gravity);
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                forceLtrRecursive(vg.getChildAt(i));
            }
        }
    }
}