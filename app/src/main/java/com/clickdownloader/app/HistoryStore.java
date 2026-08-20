package com.clickdownloader.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.util.Date;

public final class HistoryStore {

    private static final String PREFS = "history";
    private static final String KEY = "items";

    private HistoryStore() {
    }

    public static synchronized void add(Context context, String title, String url, boolean success) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String old = prefs.getString(KEY, "");

        String line = (success ? "✓ " : "✕ ")
                + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date())
                + "\n"
                + (title == null || title.trim().isEmpty() ? url : title)
                + "\n"
                + url
                + "\n\n";

        String merged = line + old;

        if (merged.length() > 30000) {
            merged = merged.substring(0, 30000);
        }

        prefs.edit().putString(KEY, merged).apply();
    }

    public static String get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "");
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY)
                .apply();
    }
}
