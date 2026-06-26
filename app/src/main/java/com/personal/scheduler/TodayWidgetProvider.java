package com.personal.scheduler;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.List;

public class TodayWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TOGGLE = "com.personal.scheduler.ACTION_TOGGLE";
    private static final String EXTRA_WIDGET_ID = "widget_id";
    private static final String EXTRA_ITEM_ID = "item_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            long itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && itemId >= 0) {
                WidgetPrefs.toggleDone(context, widgetId, itemId);
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetPrefs.remove(context, appWidgetId);
        }
    }

    static void updateAll(Context context, AppWidgetManager manager) {
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.today_widget);
        long categoryId = WidgetPrefs.categoryId(context, widgetId);
        int fontSize = WidgetPrefs.fontSize(context, widgetId);
        int textMode = WidgetPrefs.textColor(context, widgetId);
        SchedulerDb db = new SchedulerDb(context);
        Category category = db.category(categoryId);

        int textColor = textMode == WidgetPrefs.TEXT_LIGHT ? Color.WHITE : Color.rgb(42, 38, 48);
        int bodyColor = textMode == WidgetPrefs.TEXT_LIGHT ? Color.WHITE : Color.rgb(91, 75, 82);
        int accent = Color.rgb(245, 141, 151);

        views.setInt(R.id.widgetRoot, "setBackgroundResource", backgroundResource(context, widgetId));
        views.setTextColor(R.id.widgetTitle, textColor);
        views.setTextColor(R.id.widgetEdit, textColor);
        views.setTextColor(R.id.widgetOpen, textColor);
        applyFontSize(views, fontSize);
        views.removeAllViews(R.id.widgetTaskContainer);

        if (category == null) {
            views.setTextViewText(R.id.widgetTitle, "오늘의 할일");
            addMessageRow(context, views, "앱을 열어 위젯 카테고리를 다시 설정해줘.", bodyColor, fontSize);
        } else {
            views.setTextViewText(R.id.widgetTitle, category.name + " · " + DateText.todayHeader());
            List<ScheduleItem> items = db.todayItems(categoryId);
            if (items.isEmpty()) {
                addMessageRow(context, views, "오늘 반복되는 스케줄 없음", bodyColor, fontSize);
            } else {
                for (ScheduleItem item : items) {
                    addTaskRow(context, views, widgetId, item, bodyColor, accent, fontSize);
                }
            }
        }

        Intent launch = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widgetOpen,
                PendingIntent.getActivity(context, widgetId, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent configure = new Intent(context, WidgetConfigureActivity.class);
        configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        configure.putExtra(WidgetConfigureActivity.EXTRA_EDIT_EXISTING, true);
        views.setOnClickPendingIntent(R.id.widgetEdit,
                PendingIntent.getActivity(context, widgetId + 100_000, configure,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(widgetId, views);
    }

    private static void addMessageRow(Context context, RemoteViews views, String message, int bodyColor, int fontSize) {
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_task_row);
        row.setTextViewText(R.id.widgetTaskCheck, "");
        row.setTextViewText(R.id.widgetTaskText, message);
        row.setTextColor(R.id.widgetTaskText, bodyColor);
        applyRowFont(row, fontSize);
        views.addView(R.id.widgetTaskContainer, row);
    }

    private static void addTaskRow(Context context, RemoteViews views, int widgetId, ScheduleItem item,
                                   int bodyColor, int accent, int fontSize) {
        boolean done = WidgetPrefs.isDone(context, widgetId, item.id);
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_task_row);
        row.setTextViewText(R.id.widgetTaskCheck, done ? "●" : "○");
        row.setTextColor(R.id.widgetTaskCheck, accent);
        row.setTextViewText(R.id.widgetTaskText, item.content + suffix(item.amount));
        row.setTextColor(R.id.widgetTaskText, bodyColor);
        applyRowFont(row, fontSize);

        Intent toggle = new Intent(context, TodayWidgetProvider.class);
        toggle.setAction(ACTION_TOGGLE);
        toggle.putExtra(EXTRA_WIDGET_ID, widgetId);
        toggle.putExtra(EXTRA_ITEM_ID, item.id);
        row.setOnClickPendingIntent(R.id.widgetTaskCheck,
                PendingIntent.getBroadcast(context, (int) (widgetId * 10_000L + item.id), toggle,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.addView(R.id.widgetTaskContainer, row);
    }

    private static String suffix(String amount) {
        return amount == null || amount.trim().isEmpty() ? "" : " " + amount.trim();
    }

    private static int backgroundResource(Context context, int widgetId) {
        int bg = WidgetPrefs.bgColor(context, widgetId);
        if (bg == WidgetPrefs.BG_PINK) {
            return R.drawable.widget_background_pink;
        }
        if (bg == WidgetPrefs.BG_PEACH) {
            return R.drawable.widget_background_peach;
        }
        if (bg == WidgetPrefs.BG_CREAM) {
            return R.drawable.widget_background_cream;
        }
        return R.drawable.widget_background;
    }

    private static void applyFontSize(RemoteViews views, int fontSize) {
        float title;
        if (fontSize == WidgetPrefs.FONT_LARGE) {
            title = 21;
        } else if (fontSize == WidgetPrefs.FONT_MEDIUM) {
            title = 19;
        } else {
            title = 17;
        }
        views.setTextViewTextSize(R.id.widgetTitle, TypedValue.COMPLEX_UNIT_SP, title);
    }

    private static void applyRowFont(RemoteViews row, int fontSize) {
        float body;
        float check;
        if (fontSize == WidgetPrefs.FONT_LARGE) {
            body = 18;
            check = 22;
        } else if (fontSize == WidgetPrefs.FONT_MEDIUM) {
            body = 16;
            check = 20;
        } else {
            body = 15;
            check = 18;
        }
        row.setTextViewTextSize(R.id.widgetTaskText, TypedValue.COMPLEX_UNIT_SP, body);
        row.setTextViewTextSize(R.id.widgetTaskCheck, TypedValue.COMPLEX_UNIT_SP, check);
    }
}
