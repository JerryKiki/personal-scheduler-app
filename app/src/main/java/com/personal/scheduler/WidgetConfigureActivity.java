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
    static final String EXTRA_EDIT_EXISTING = "edit_existing";

    private static final int BG = Color.rgb(255, 248, 244);
    private static final int CARD = Color.WHITE;
    private static final int PEACH = Color.rgb(255, 239, 228);
    private static final int PINK = Color.rgb(245, 141, 151);
    private static final int INK = Color.rgb(42, 38, 48);
    private static final int MUTED = Color.rgb(139, 122, 128);
    private static final int LINE = Color.rgb(245, 220, 210);

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private long selectedCategoryId = -1;
    private int selectedFontSize = WidgetPrefs.FONT_SMALL;
    private int selectedBg = WidgetPrefs.BG_WHITE;
    private int selectedText = WidgetPrefs.TEXT_DARK;
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
        selectedCategoryId = WidgetPrefs.categoryId(this, widgetId);
        selectedFontSize = WidgetPrefs.fontSize(this, widgetId);
        selectedBg = WidgetPrefs.bgColor(this, widgetId);
        selectedText = WidgetPrefs.textColor(this, widgetId);
        if (selectedCategoryId < 0 && !categories.isEmpty()) {
            selectedCategoryId = categories.get(0).id;
        }
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, systemDimen("status_bar_height"), 0, systemDimen("navigation_bar_height"));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("위젯 설정", 28, INK, true);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, match());
        root.addView(text("홈 화면에 표시할 스케줄과 스타일을 골라요.", 15, MUTED, false), spaced());

        if (categories.isEmpty()) {
            LinearLayout card = card();
            card.addView(text("먼저 앱에서 카테고리를 만들어줘.", 16, MUTED, false), match());
            root.addView(card, spaced());
            return;
        }

        addCategoryChooser();
        addFontChooser();
        addBackgroundChooser();
        addTextColorChooser();

        Button save = button("저장", true);
        save.setOnClickListener(v -> saveAndFinish());
        root.addView(save, match());
    }

    private void addCategoryChooser() {
        LinearLayout card = card();
        card.addView(text("카테고리", 20, INK, true), match());
        card.addView(text("위젯에 띄울 카테고리를 선택해요.", 14, MUTED, false), spacedSmall());

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
        card.addView(text("위젯에 표시할 폰트 사이즈를 선택해요.", 14, MUTED, false), spacedSmall());

        LinearLayout row = row();
        addChoice(row, "Small", selectedFontSize == WidgetPrefs.FONT_SMALL, () -> selectedFontSize = WidgetPrefs.FONT_SMALL);
        addChoice(row, "Medium", selectedFontSize == WidgetPrefs.FONT_MEDIUM, () -> selectedFontSize = WidgetPrefs.FONT_MEDIUM);
        addChoice(row, "Large", selectedFontSize == WidgetPrefs.FONT_LARGE, () -> selectedFontSize = WidgetPrefs.FONT_LARGE);
        card.addView(row, match());
        root.addView(card, spaced());
    }

    private void addBackgroundChooser() {
        LinearLayout card = card();
        card.addView(text("배경색", 20, INK, true), match());
        card.addView(text("테마 색상 중 하나를 골라요.", 14, MUTED, false), spacedSmall());

        LinearLayout row = row();
        addChoice(row, "White", selectedBg == WidgetPrefs.BG_WHITE, () -> selectedBg = WidgetPrefs.BG_WHITE);
        addChoice(row, "Cream", selectedBg == WidgetPrefs.BG_CREAM, () -> selectedBg = WidgetPrefs.BG_CREAM);
        addChoice(row, "Peach", selectedBg == WidgetPrefs.BG_PEACH, () -> selectedBg = WidgetPrefs.BG_PEACH);
        addChoice(row, "Pink", selectedBg == WidgetPrefs.BG_PINK, () -> selectedBg = WidgetPrefs.BG_PINK);
        card.addView(row, match());
        root.addView(card, spaced());
    }

    private void addTextColorChooser() {
        LinearLayout card = card();
        card.addView(text("폰트색", 20, INK, true), match());
        card.addView(text("검정 또는 하양으로 선택해요.", 14, MUTED, false), spacedSmall());

        LinearLayout row = row();
        addChoice(row, "검정", selectedText == WidgetPrefs.TEXT_DARK, () -> selectedText = WidgetPrefs.TEXT_DARK);
        addChoice(row, "하양", selectedText == WidgetPrefs.TEXT_LIGHT, () -> selectedText = WidgetPrefs.TEXT_LIGHT);
        card.addView(row, match());
        root.addView(card, spaced());
    }

    private void addChoice(LinearLayout row, String label, boolean selected, Runnable action) {
        Button button = button(label, selected);
        button.setOnClickListener(v -> {
            action.run();
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginEnd(dp(8));
        row.addView(button, params);
    }

    private void saveAndFinish() {
        WidgetPrefs.save(this, widgetId, selectedCategoryId, selectedFontSize, selectedBg, selectedText);
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
        card.setElevation(0f);
        card.setBackground(rounded(CARD, 28, 0, CARD));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button button(String label, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(selected ? Color.WHITE : INK);
        button.setMinHeight(dp(48));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackground(rounded(selected ? PINK : PEACH, 22, selected ? 0 : 1, LINE));
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
