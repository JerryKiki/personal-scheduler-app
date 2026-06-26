package com.personal.scheduler;

import android.content.Context;
import android.content.SharedPreferences;

final class WidgetPrefs {
    private static final String PREFS = "widget_prefs";
    private static final String CATEGORY_PREFIX = "category_";
    private static final String FONT_PREFIX = "font_";
    private static final String BG_PREFIX = "bg_";
    private static final String TEXT_PREFIX = "text_";
    private static final String DONE_PREFIX = "done_";

    static final int FONT_SMALL = 0;
    static final int FONT_MEDIUM = 1;
    static final int FONT_LARGE = 2;

    static final int BG_WHITE = 0;
    static final int BG_CREAM = 1;
    static final int BG_PEACH = 2;
    static final int BG_PINK = 3;

    static final int TEXT_DARK = 0;
    static final int TEXT_LIGHT = 1;

    private WidgetPrefs() {
    }

    static void save(Context context, int widgetId, long categoryId, int fontSize, int bgColor, int textColor) {
        prefs(context).edit()
                .putLong(CATEGORY_PREFIX + widgetId, categoryId)
                .putInt(FONT_PREFIX + widgetId, fontSize)
                .putInt(BG_PREFIX + widgetId, bgColor)
                .putInt(TEXT_PREFIX + widgetId, textColor)
                .apply();
    }

    static long categoryId(Context context, int widgetId) {
        return prefs(context).getLong(CATEGORY_PREFIX + widgetId, -1);
    }

    static int fontSize(Context context, int widgetId) {
        return prefs(context).getInt(FONT_PREFIX + widgetId, FONT_SMALL);
    }

    static int bgColor(Context context, int widgetId) {
        return prefs(context).getInt(BG_PREFIX + widgetId, BG_WHITE);
    }

    static int textColor(Context context, int widgetId) {
        return prefs(context).getInt(TEXT_PREFIX + widgetId, TEXT_DARK);
    }

    static boolean isDone(Context context, int widgetId, long itemId) {
        return prefs(context).getBoolean(doneKey(widgetId, itemId), false);
    }

    static void toggleDone(Context context, int widgetId, long itemId) {
        SharedPreferences preferences = prefs(context);
        String key = doneKey(widgetId, itemId);
        preferences.edit().putBoolean(key, !preferences.getBoolean(key, false)).apply();
    }

    static void remove(Context context, int widgetId) {
        prefs(context).edit()
                .remove(CATEGORY_PREFIX + widgetId)
                .remove(FONT_PREFIX + widgetId)
                .remove(BG_PREFIX + widgetId)
                .remove(TEXT_PREFIX + widgetId)
                .apply();
    }

    private static String doneKey(int widgetId, long itemId) {
        return DONE_PREFIX + widgetId + "_" + DateText.todayKey() + "_" + itemId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
