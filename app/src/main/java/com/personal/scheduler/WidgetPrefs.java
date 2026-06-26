package com.personal.scheduler;

import android.content.Context;
import android.content.SharedPreferences;

final class WidgetPrefs {
    private static final String PREFS = "widget_prefs";
    private static final String KEY_PREFIX = "category_";

    private WidgetPrefs() {
    }

    static void save(Context context, int widgetId, long categoryId) {
        prefs(context).edit().putLong(KEY_PREFIX + widgetId, categoryId).apply();
    }

    static long categoryId(Context context, int widgetId) {
        return prefs(context).getLong(KEY_PREFIX + widgetId, -1);
    }

    static void remove(Context context, int widgetId) {
        prefs(context).edit().remove(KEY_PREFIX + widgetId).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
