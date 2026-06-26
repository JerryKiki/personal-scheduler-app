package com.personal.scheduler;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        SchedulerDb db = new SchedulerDb(this);
        List<Category> categories = db.categories();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("위젯에 표시할 스케줄");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        if (categories.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("먼저 앱에서 카테고리를 만들어줘.");
            empty.setTextSize(16);
            root.addView(empty);
            return;
        }

        for (Category category : categories) {
            Button button = new Button(this);
            button.setText(category.name);
            button.setAllCaps(false);
            button.setOnClickListener(v -> saveAndFinish(category.id));
            root.addView(button);
        }
    }

    private void saveAndFinish(long categoryId) {
        WidgetPrefs.save(this, widgetId, categoryId);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        TodayWidgetProvider.updateWidget(this, manager, widgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
