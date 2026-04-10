package com.anvexgroup.apnuvyapar.utils;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Shared utility for converting UTC server timestamps to human-friendly
 * relative-time strings.
 *
 * Usage:
 *   TimeUtils.relativeTime("2026-04-05 06:00:00")  →  "30m ago"
 *
 * Design rules:
 * - The DB / API always stores timestamps in UTC ("yyyy-MM-dd HH:mm:ss").
 * - System.currentTimeMillis() is in the device epoch (UTC-based), so we must
 *   parse the server string as UTC before computing the diff.
 * - SimpleDateFormat instances are NOT thread-safe; each call creates its own.
 */
public final class TimeUtils {

    private static final String FMT_SERVER = "yyyy-MM-dd HH:mm:ss";
    private static final String FMT_DAY    = "d MMM";
    private static final String FMT_YEAR   = "d MMM yyyy";

    private TimeUtils() { /* no instances */ }

    /**
     * Converts a UTC "yyyy-MM-dd HH:mm:ss" timestamp to a human-readable
     * relative string: "Just now", "5m ago", "3h ago", "2d ago", "5 Apr", "5 Apr 2025".
     *
     * @param serverTs UTC timestamp string from DB/API. May be null or empty.
     * @return Human-readable relative time, or "" if the input is unusable.
     */
    public static String relativeTime(String serverTs) {
        if (TextUtils.isEmpty(serverTs) || "null".equalsIgnoreCase(serverTs)) {
            return "";
        }

        try {
            // Parse as UTC – this is the critical fix for the "5h ago" timezone bug.
            SimpleDateFormat sdfIn = new SimpleDateFormat(FMT_SERVER, Locale.US);
            sdfIn.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date past = sdfIn.parse(serverTs.trim());
            if (past == null) return "";

            long diffSeconds = (System.currentTimeMillis() - past.getTime()) / 1000L;

            // Guard against future timestamps (clock skew)
            if (diffSeconds < 0) return "Just now";

            if (diffSeconds < 60)          return "Just now";
            if (diffSeconds < 3_600)       return (diffSeconds / 60)     + "m ago";
            if (diffSeconds < 86_400)      return (diffSeconds / 3_600)  + "h ago";
            if (diffSeconds < 7 * 86_400)  return (diffSeconds / 86_400) + "d ago";

            // Older than a week: show calendar date
            Calendar calPast = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar calNow  = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calPast.setTime(past);
            calNow.setTime(new Date());

            String pattern = (calPast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR))
                    ? FMT_DAY : FMT_YEAR;
            SimpleDateFormat sdfOut = new SimpleDateFormat(pattern, Locale.US);
            sdfOut.setTimeZone(TimeZone.getDefault()); // display in device's local zone
            return sdfOut.format(past);

        } catch (Exception e) {
            return "";
        }
    }
}
