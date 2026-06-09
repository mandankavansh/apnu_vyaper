package com.anvexgroup.apnuvyapar.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class I18n {

    private static final String TAG = "I18n";

    private static final String APP_PREFS = "apnuvyapar_prefs";
    private static final String KEY_APP_LANG_CODE = "app_lang_code";

    private static final String SP_NAME = "i18n_cache_mlkit_v1";

    private static final int MEM_CACHE_CAP = 500;

    private static final Map<String, String> MEM =
            new LinkedHashMap<String, String>(MEM_CACHE_CAP, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MEM_CACHE_CAP;
                }
            };

    private I18n() {
    }

    public static String lang(Context ctx) {
        if (ctx == null) return "en";

        SharedPreferences sp = ctx.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        String code = sp.getString(KEY_APP_LANG_CODE, "en");

        if (code == null || code.trim().isEmpty()) return "en";

        return normalizeLangCode(code.trim());
    }

    public static String t(Context ctx, String key) {
        if (ctx == null) return key == null ? "" : key;
        if (key == null) return "";

        String cleanKey = key.trim();
        if (cleanKey.isEmpty()) return "";

        String targetLang = lang(ctx);
        if ("en".equalsIgnoreCase(targetLang)) return cleanKey;

        String memKey = buildCacheKey(targetLang, cleanKey);

        synchronized (MEM) {
            String cached = MEM.get(memKey);
            if (cached != null && !cached.trim().isEmpty()) {
                return cached;
            }
        }

        SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String stored = sp.getString(memKey, null);

        if (stored == null || stored.trim().isEmpty()) {
            return cleanKey;
        }

        synchronized (MEM) {
            MEM.put(memKey, stored);
        }

        return stored;
    }

    public static void translateAndApplyText(TextView tv, Context ctx) {
        if (tv == null || ctx == null) return;

        CharSequence now = tv.getText();
        String base = now == null ? "" : now.toString().trim();

        if (base.isEmpty()) {
            tv.setText("");
            return;
        }

        tv.setText(t(ctx, base));

        List<String> list = new ArrayList<>();
        list.add(base);

        prefetch(ctx, list, () -> tv.setText(t(ctx, base)), () -> tv.setText(base));
    }

    public static void translateAndApplyHint(TextInputLayout til, Context ctx) {
        if (til == null || ctx == null) return;

        CharSequence hint = til.getHint();
        String base = hint == null ? "" : hint.toString().trim();

        if (base.isEmpty()) {
            til.setHint("");
            return;
        }

        til.setHint(t(ctx, base));

        List<String> list = new ArrayList<>();
        list.add(base);

        prefetch(ctx, list, () -> til.setHint(t(ctx, base)), () -> til.setHint(base));
    }

    public static void prefetch(Context ctx, List<String> keys, Runnable onReady) {
        prefetch(ctx, keys, onReady, null);
    }

    public static void prefetch(Context ctx, List<String> keys, Runnable onReady, Runnable onError) {
        if (ctx == null) {
            runErrorOrReady(onReady, onError);
            return;
        }

        Context appCtx = ctx.getApplicationContext();
        String targetLang = lang(appCtx);

        if (keys == null || keys.isEmpty() || "en".equalsIgnoreCase(targetLang)) {
            if (onReady != null) onReady.run();
            return;
        }

        String mlKitTargetLang = TranslateLanguage.fromLanguageTag(targetLang);

        if (mlKitTargetLang == null || mlKitTargetLang.trim().isEmpty()) {
            Log.e(TAG, "Unsupported ML Kit language code: " + targetLang);
            runErrorOrReady(onReady, onError);
            return;
        }

        List<String> unique = new ArrayList<>();

        for (String k : keys) {
            if (k == null) continue;

            String clean = k.trim();
            if (clean.isEmpty()) continue;

            if (!unique.contains(clean)) {
                unique.add(clean);
            }
        }

        if (unique.isEmpty()) {
            if (onReady != null) onReady.run();
            return;
        }

        SharedPreferences sp = appCtx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        List<String> missing = new ArrayList<>();

        for (String k : unique) {
            String cacheKey = buildCacheKey(targetLang, k);

            boolean inMem;
            synchronized (MEM) {
                inMem = MEM.containsKey(cacheKey);
            }

            if (!inMem && sp.getString(cacheKey, null) == null) {
                missing.add(k);
            }
        }

        if (missing.isEmpty()) {
            if (onReady != null) onReady.run();
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlKitTargetLang)
                .build();

        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        Log.d(TAG, "ML Kit translation model download/check started. Target: " + targetLang);

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "ML Kit translation model ready. Missing keys: " + missing.size());

                    SharedPreferences.Editor editor = sp.edit();

                    translateOneByOne(
                            translator,
                            targetLang,
                            missing,
                            0,
                            editor,
                            () -> {
                                try {
                                    editor.apply();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed saving translation cache", e);
                                }

                                try {
                                    translator.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed closing translator", e);
                                }

                                Log.d(TAG, "ML Kit translation prefetch success.");

                                if (onReady != null) onReady.run();
                            },
                            () -> {
                                try {
                                    editor.apply();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed saving partial translation cache", e);
                                }

                                try {
                                    translator.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed closing translator after error", e);
                                }

                                runErrorOrReady(onReady, onError);
                            }
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ML Kit model download failed for target: " + targetLang, e);

                    try {
                        translator.close();
                    } catch (Exception closeError) {
                        Log.e(TAG, "Failed closing translator after model error", closeError);
                    }

                    runErrorOrReady(onReady, onError);
                });
    }

    private static void translateOneByOne(
            Translator translator,
            String targetLang,
            List<String> missing,
            int index,
            SharedPreferences.Editor editor,
            Runnable onDone,
            Runnable onError
    ) {
        if (translator == null || missing == null) {
            if (onError != null) onError.run();
            return;
        }

        if (index >= missing.size()) {
            if (onDone != null) onDone.run();
            return;
        }

        String original = missing.get(index);

        translator.translate(original)
                .addOnSuccessListener(translatedText -> {
                    try {
                        String finalText = cleanTranslatedText(translatedText, original);

                        String cacheKey = buildCacheKey(targetLang, original);
                        editor.putString(cacheKey, finalText);

                        synchronized (MEM) {
                            MEM.put(cacheKey, finalText);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Failed caching translated text at index: " + index, e);
                    }

                    translateOneByOne(
                            translator,
                            targetLang,
                            missing,
                            index + 1,
                            editor,
                            onDone,
                            onError
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ML Kit translation failed for: " + original, e);

                    String cacheKey = buildCacheKey(targetLang, original);
                    editor.putString(cacheKey, original);

                    synchronized (MEM) {
                        MEM.put(cacheKey, original);
                    }

                    translateOneByOne(
                            translator,
                            targetLang,
                            missing,
                            index + 1,
                            editor,
                            onDone,
                            onError
                    );
                });
    }

    private static String buildCacheKey(String lang, String key) {
        return "mlkit_v1|" + normalizeLangCode(lang) + "|" + key;
    }

    private static String normalizeLangCode(String code) {
        if (code == null) return "en";

        String c = code.trim().toLowerCase();

        if (c.isEmpty()) return "en";

        if (c.contains("-")) {
            c = c.substring(0, c.indexOf("-"));
        }

        if (c.contains("_")) {
            c = c.substring(0, c.indexOf("_"));
        }

        if ("iw".equals(c)) return "he";
        if ("in".equals(c)) return "id";
        if ("ji".equals(c)) return "yi";

        return c;
    }

    private static String cleanTranslatedText(String translatedText, String fallback) {
        if (translatedText == null) return fallback == null ? "" : fallback;

        String text = translatedText.trim();

        if (text.isEmpty()) return fallback == null ? "" : fallback;

        try {
            @SuppressLint({"NewApi", "LocalSuppress"})
            String plain = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();

            if (plain != null && !plain.trim().isEmpty()) {
                return plain.trim();
            }

        } catch (Exception ignored) {
        }

        return text;
    }

    private static void runErrorOrReady(Runnable onReady, Runnable onError) {
        if (onError != null) {
            onError.run();
        } else if (onReady != null) {
            onReady.run();
        }
    }

    public static List<String> concatUnique(List<String> a, List<String> b) {
        if (a == null && b == null) return Collections.emptyList();

        List<String> out = new ArrayList<>();

        if (a != null) {
            for (String s : a) {
                if (s != null) {
                    String clean = s.trim();
                    if (!clean.isEmpty() && !out.contains(clean)) {
                        out.add(clean);
                    }
                }
            }
        }

        if (b != null) {
            for (String s : b) {
                if (s != null) {
                    String clean = s.trim();
                    if (!clean.isEmpty() && !out.contains(clean)) {
                        out.add(clean);
                    }
                }
            }
        }

        return out;
    }
}