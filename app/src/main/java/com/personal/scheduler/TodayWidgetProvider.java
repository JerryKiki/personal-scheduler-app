package com.personal.scheduler;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.List;

public class TodayWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
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
        SchedulerDb db = new SchedulerDb(context);
        Category category = db.category(categoryId);

        applyFontSize(views, fontSize);
        if (category == null) {
            views.setTextViewText(R.id.widgetTitle, "오늘의 할일");
            views.setTextViewText(R.id.widgetBody, "앱을 열어 위젯 카테고리를 다시 설정해줘.");
        } else {
            views.setTextViewText(R.id.widgetTitle, category.name + " · " + DateText.todayHeader());
            views.setTextViewText(R.id.widgetBody, todayBody(db.todayItems(categoryId)));
        }

        Intent launch = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widgetBody,
                android.app.PendingIntent.getActivity(context, widgetId, launch,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(widgetId, views);
    }

    private static String todayBody(List<ScheduleItem> items) {
        if (items.isEmpty()) {
            return "오늘 반복되는 스케줄 없음";
        }
        StringBuilder builder = new StringBuilder();
        for (ScheduleItem item : items) {
            builder.append("○ ").append(item.content);
            if (item.amount != null && !item.amount.trim().isEmpty()) {
                builder.append(" ").append(item.amount.trim());
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    private static void applyFontSize(RemoteViews views, int fontSize) {
        float title;
        float body;
        if (fontSize == WidgetPrefs.FONT_LARGE) {
            title = 21;
            body = 18;
        } else if (fontSize == WidgetPrefs.FONT_MEDIUM) {
            title = 19;
            body = 16;
        } else {
            title = 17;
            body = 15;
        }
        views.setTextViewTextSize(R.id.widgetTitle, TypedValue.COMPLEX_UNIT_SP, title);
        views.setTextViewTextSize(R.id.widgetBody, TypedValue.COMPLEX_UNIT_SP, body);
    }
}
