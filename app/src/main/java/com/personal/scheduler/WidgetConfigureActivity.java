package com.personal.scheduler;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class WidgetConfigureActivity extends Activity {
    private static final int BG = Color.rgb(255, 248, 244);
    private static final int CARD = Color.WHITE;
    private static final int PEACH = Color.rgb(255, 239, 228);
    private static final int PINK = Color.rgb(245, 141, 151);
    private static final int PINK_DARK = Color.rgb(207, 92, 112);
    private static final int INK = Color.rgb(42, 38, 48);
    private static final int MUTED = Color.rgb(139, 122, 128);
    private static final int LINE = Color.rgb(245, 220, 210);

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private long selectedCategoryId = -1;
    private int selectedFontSize = WidgetPrefs.FONT_SMALL;
    private LinearLayout root;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        categories = new SchedulerDb(this).categories();
        if (!categories.isEmpty()) {
            selectedCategoryId = categories.get(0).id;
        }
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setPadding(0, systemDimen("status_bar_height"), 0, systemDimen("navigation_bar_height"));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("위젯 설정", 28, INK, true);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, match());
        root.addView(text("홈 화면에 표시할 스케줄과 글자 크기를 골라요.", 15, MUTED, false), spaced());

        if (categories.isEmpty()) {
            LinearLayout card = card();
            card.addView(text("먼저 앱에서 카테고리를 만들어줘.", 16, MUTED, false), match());
            root.addView(card, spaced());
            return;
        }

        addCategoryChooser();
        addFontChooser();

        Button save = button("위젯 추가", true);
        save.setOnClickListener(v -> saveAndFinish());
        root.addView(save, match());
    }

    private void addCategoryChooser() {
        LinearLayout card = card();
        card.addView(text("스케줄", 20, INK, true), match());
        card.addView(text("위젯에 띄울 카테고리", 14, MUTED, false), spacedSmall());

        for (Category category : categories) {
            Button button = button((category.id == selectedCategoryId ? "✓ " : "") + category.name,
                    category.id == selectedCategoryId);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setOnClickListener(v -> {
                selectedCategoryId = category.id;
                render();
            });
            card.addView(button, spacedSmall());
        }
        root.addView(card, spaced());
    }

    private void addFontChooser() {
        LinearLayout card = card();
        card.addView(text("글자 크기", 20, INK, true), match());
        card.addView(text("Small도 기존 위젯보다 살짝 크게 잡았어요.", 14, MUTED, false), spacedSmall());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addFontButton(row, "Small", WidgetPrefs.FONT_SMALL);
        addFontButton(row, "Medium", WidgetPrefs.FONT_MEDIUM);
        addFontButton(row, "Large", WidgetPrefs.FONT_LARGE);
        card.addView(row, match());
        root.addView(card, spaced());
    }

    private void addFontButton(LinearLayout row, String label, int value) {
        Button button = button(label, selectedFontSize == value);
        button.setOnClickListener(v -> {
            selectedFontSize = value;
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginEnd(value == WidgetPrefs.FONT_LARGE ? 0 : dp(8));
        row.addView(button, params);
    }

    private void saveAndFinish() {
        WidgetPrefs.save(this, widgetId, selectedCategoryId, selectedFontSize);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        TodayWidgetProvider.updateWidget(this, manager, widgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(CARD, 28, 0, CARD));
        return card;
    }

    private Button button(String label, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(selected ? Color.WHITE : (label.equals("위젯 추가") ? Color.WHITE : INK));
        button.setMinHeight(dp(48));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackground(rounded(selected || label.equals("위젯 추가") ? PINK : PEACH, 22, selected ? 0 : 1, LINE));
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1f);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = match();
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private LinearLayout.LayoutParams spacedSmall() {
        LinearLayout.LayoutParams params = match();
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private int systemDimen(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
