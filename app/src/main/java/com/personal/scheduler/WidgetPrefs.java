package com.personal.scheduler;

import android.content.Context;
import android.content.SharedPreferences;

final class WidgetPrefs {
    private static final String PREFS = "widget_prefs";
    private static final String CATEGORY_PREFIX = "category_";
    private static final String FONT_PREFIX = "font_";

    static final int FONT_SMALL = 0;
    static final int FONT_MEDIUM = 1;
    static final int FONT_LARGE = 2;

    private WidgetPrefs() {
    }

    static void save(Context context, int widgetId, long categoryId, int fontSize) {
        prefs(context).edit()
                .putLong(CATEGORY_PREFIX + widgetId, categoryId)
                .putInt(FONT_PREFIX + widgetId, fontSize)
                .apply();
    }

    static long categoryId(Context context, int widgetId) {
        return prefs(context).getLong(CATEGORY_PREFIX + widgetId, -1);
    }

    static int fontSize(Context context, int widgetId) {
        return prefs(context).getInt(FONT_PREFIX + widgetId, FONT_SMALL);
    }

    static void remove(Context context, int widgetId) {
        prefs(context).edit()
                .remove(CATEGORY_PREFIX + widgetId)
                .remove(FONT_PREFIX + widgetId)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
