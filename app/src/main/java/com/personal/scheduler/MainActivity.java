package com.personal.scheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_EXPORT = 100;
    private static final int REQ_IMPORT = 101;

    private static final int BG = Color.rgb(255, 248, 244);
    private static final int CARD = Color.WHITE;
    private static final int PEACH = Color.rgb(255, 214, 190);
    private static final int PEACH_SOFT = Color.rgb(255, 239, 228);
    private static final int PINK = Color.rgb(245, 141, 151);
    private static final int PINK_DARK = Color.rgb(207, 92, 112);
    private static final int INK = Color.rgb(42, 38, 48);
    private static final int MUTED = Color.rgb(139, 122, 128);
    private static final int LINE = Color.rgb(245, 220, 210);

    private SchedulerDb db;
    private LinearLayout root;
    private ScrollView scroll;
    private long selectedCategoryId = -1;
    private String pendingBackupText;
    private String pendingBackupCode;
    private boolean categoryListExpanded = true;
    private boolean routineListExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        db = new SchedulerDb(this);
        render();
    }

    private void render() {
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(22));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        applySystemBarPadding();

        addHero();
        addBackupButtons();
        addCategoryEditor();

        List<Category> categories = db.categories();
        if (selectedCategoryId < 0 && !categories.isEmpty()) {
            selectedCategoryId = categories.get(0).id;
        }

        addCategoryList(categories);
        if (selectedCategoryId >= 0) {
            addItemEditor();
            addItemList();
            addTodayPreview();
        }
    }

    private void addHero() {
        LinearLayout card = card(PEACH);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView eyebrow = label("TODAY ROUTINE");
        eyebrow.setTextColor(PINK_DARK);
        card.addView(eyebrow, match());

        TextView title = title("루틴 스케줄러", 30);
        title.setPadding(0, dp(4), 0, dp(6));
        card.addView(title, match());

        TextView sub = body("카테고리별 반복 일정을 만들고, 오늘 할 일을 홈 위젯에 띄워요.", 15, MUTED);
        card.addView(sub, match());

        root.addView(card, spaced());
    }

    private void addBackupButtons() {
        LinearLayout row = row();
        Button export = actionButton("백업 내보내기", true);
        export.setOnClickListener(v -> exportBackup());
        Button restore = actionButton("백업 복원", false);
        restore.setOnClickListener(v -> chooseBackupFile());
        row.addView(export, weightedWithEndMargin());
        row.addView(restore, weight());
        root.addView(row, compactSpaced());
    }

    private void addCategoryEditor() {
        LinearLayout card = card(CARD);
        addCardHeader(card, "카테고리", "운동, 공부, 업무처럼 원하는 묶음을 만들어요.");

        LinearLayout row = row();
        EditText input = input("운동, 공부, 업무");
        Button add = actionButton("추가", true);
        add.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                toast("카테고리 이름을 입력해줘");
                return;
            }
            long id = db.addCategory(name);
            if (id >= 0) {
                selectedCategoryId = id;
            }
            hideKeyboard(input);
            rerenderKeepingScroll();
        });
        row.addView(input, weightedWithEndMargin());
        row.addView(add, wrap());
        card.addView(row, match());
        root.addView(card, spaced());
    }

    private void addCategoryList(List<Category> categories) {
        LinearLayout card = card(CARD);
        addCardHeader(card, "카테고리 목록", "위젯에 띄울 카테고리를 선택해요.", categoryListExpanded, () -> {
            categoryListExpanded = !categoryListExpanded;
            rerenderKeepingScroll();
        });

        if (categories.isEmpty()) {
            card.addView(muted("아직 카테고리가 없어. 먼저 하나 추가해줘."), match());
            root.addView(card, spaced());
            return;
        }

        for (Category category : categories) {
            if (!categoryListExpanded && category.id != selectedCategoryId) {
                continue;
            }
            LinearLayout row = categoryRow(category.id == selectedCategoryId);
            Button select = pillButton((category.id == selectedCategoryId ? "✓ " : "") + category.name,
                    category.id == selectedCategoryId);
            select.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            select.setOnClickListener(v -> {
                selectedCategoryId = category.id;
                rerenderKeepingScroll();
            });

            Button edit = iconButton("•••", PEACH_SOFT, PINK_DARK);
            edit.setOnClickListener(v -> showCategoryEditor(category));
            Button delete = iconButton("X", Color.rgb(255, 232, 232), PINK_DARK);
            delete.setOnClickListener(v -> confirmDeleteCategory(category));

            row.addView(select, weightedWithEndMargin());
            row.addView(edit, iconButtonParams());
            row.addView(delete, iconButtonParamsNoMargin());
            card.addView(row, compactSpaced());
        }
        root.addView(card, spaced());
    }

    private void addItemEditor() {
        Category category = db.category(selectedCategoryId);
        if (category == null) {
            return;
        }

        LinearLayout card = card(CARD);
        addCardHeader(card, category.name + " 루틴", "내용, 횟수, 반복 요일을 정해요.");

        EditText content = input("내용: 마운틴 클라이머");
        EditText amount = input("횟수/분량: 100회");
        card.addView(content, compactSpaced());
        card.addView(amount, compactSpaced());

        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.HORIZONTAL);
        days.setGravity(Gravity.CENTER_VERTICAL);
        days.setPadding(0, dp(8), 0, dp(8));
        CheckBox[] boxes = new CheckBox[7];
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < labels.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(labels[i]);
            box.setTextColor(INK);
            box.setTextSize(14);
            box.setButtonTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{PINK_DARK, Color.rgb(210, 192, 188)}));
            box.setChecked(i == today);
            boxes[i] = box;
            days.addView(box, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        card.addView(days, match());

        Button add = actionButton("상세 스케줄 추가", true);
        add.setOnClickListener(v -> {
            String contentText = content.getText().toString().trim();
            String amountText = amount.getText().toString().trim();
            int mask = 0;
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isChecked()) {
                    mask |= 1 << i;
                }
            }
            if (contentText.isEmpty()) {
                toast("내용을 입력해줘");
                return;
            }
            if (mask == 0) {
                toast("반복 요일을 하나 이상 골라줘");
                return;
            }
            db.addItem(selectedCategoryId, contentText, amountText, mask);
            updateWidgets();
            hideKeyboard(content);
            rerenderKeepingScroll();
        });
        card.addView(add, match());
        root.addView(card, spaced());
    }

    private void addItemList() {
        List<ScheduleItem> items = db.items(selectedCategoryId);
        LinearLayout card = card(CARD);
        addCardHeader(card, "등록된 루틴", "반복 규칙을 한눈에 확인해요.", routineListExpanded, () -> {
            routineListExpanded = !routineListExpanded;
            rerenderKeepingScroll();
        });

        if (!routineListExpanded) {
            root.addView(card, spaced());
            return;
        }

        if (items.isEmpty()) {
            card.addView(muted("아직 상세 스케줄이 없어."), match());
            root.addView(card, spaced());
            return;
        }

        for (ScheduleItem item : items) {
            LinearLayout row = roundedRow();
            TextView text = body("• " + item.content + suffix(item.amount) + "\n반복: " + DateText.repeatText(item.repeatMask),
                    15, INK);
            Button edit = iconButton("•••", PEACH_SOFT, PINK_DARK);
            edit.setOnClickListener(v -> showRoutineEditor(item));
            Button delete = iconButton("X", Color.rgb(255, 232, 232), PINK_DARK);
            delete.setOnClickListener(v -> confirmDeleteRoutine(item));
            row.addView(text, weightedWithEndMargin());
            row.addView(edit, iconButtonParams());
            row.addView(delete, iconButtonParamsNoMargin());
            card.addView(row, compactSpaced());
        }
        root.addView(card, spaced());
    }

    private void addTodayPreview() {
        Category category = db.category(selectedCategoryId);
        if (category == null) {
            return;
        }

        LinearLayout card = card(PEACH_SOFT);
        addCardHeader(card, "오늘의 할일", DateText.todayHeader());

        List<ScheduleItem> items = db.todayItems(selectedCategoryId);
        StringBuilder builder = new StringBuilder();
        builder.append(category.name).append("\n");
        if (items.isEmpty()) {
            builder.append("오늘 반복되는 스케줄 없음");
        } else {
            for (ScheduleItem item : items) {
                builder.append("• ").append(item.content).append(suffix(item.amount)).append("\n");
            }
        }
        TextView preview = body(builder.toString().trim(), 16, INK);
        preview.setPadding(dp(14), dp(12), dp(14), dp(12));
        preview.setBackground(rounded(CARD, 24, 0, CARD));
        card.addView(preview, match());
        root.addView(card, spaced());
    }

    private void showCategoryEditor(Category category) {
        EditText input = input("카테고리 이름");
        input.setText(category.name);
        input.setSelection(input.getText().length());
        int padding = dp(18);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(padding, dp(8), padding, 0);
        box.addView(input, match());

        new AlertDialog.Builder(this)
                .setTitle("카테고리 수정")
                .setView(box)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("카테고리 이름을 입력해줘");
                        return;
                    }
                    db.updateCategory(category.id, name);
                    updateWidgets();
                    rerenderKeepingScroll();
                })
                .show();
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("카테고리 삭제")
                .setMessage("'" + category.name + "' 카테고리와 안에 있는 루틴을 모두 삭제할까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.deleteCategory(category.id);
                    if (selectedCategoryId == category.id) {
                        selectedCategoryId = -1;
                    }
                    updateWidgets();
                    rerenderKeepingScroll();
                })
                .show();
    }

    private void showRoutineEditor(ScheduleItem item) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);

        EditText content = input("내용");
        content.setText(item.content);
        content.setSelection(content.getText().length());
        EditText amount = input("횟수/분량");
        amount.setText(item.amount);
        amount.setSelection(amount.getText().length());
        box.addView(content, compactSpaced());
        box.addView(amount, compactSpaced());

        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.HORIZONTAL);
        days.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox[] boxes = new CheckBox[7];
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        for (int i = 0; i < labels.length; i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(labels[i]);
            checkBox.setTextColor(INK);
            checkBox.setTextSize(14);
            checkBox.setButtonTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{PINK_DARK, Color.rgb(210, 192, 188)}));
            checkBox.setChecked((item.repeatMask & (1 << i)) != 0);
            boxes[i] = checkBox;
            days.addView(checkBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(days, match());

        new AlertDialog.Builder(this)
                .setTitle("루틴 수정")
                .setView(box)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String contentText = content.getText().toString().trim();
                    String amountText = amount.getText().toString().trim();
                    int mask = 0;
                    for (int i = 0; i < boxes.length; i++) {
                        if (boxes[i].isChecked()) {
                            mask |= 1 << i;
                        }
                    }
                    if (contentText.isEmpty()) {
                        toast("내용을 입력해줘");
                        return;
                    }
                    if (mask == 0) {
                        toast("반복 요일을 하나 이상 골라줘");
                        return;
                    }
                    db.updateItem(item.id, contentText, amountText, mask);
                    updateWidgets();
                    rerenderKeepingScroll();
                })
                .show();
    }

    private void confirmDeleteRoutine(ScheduleItem item) {
        new AlertDialog.Builder(this)
                .setTitle("루틴 삭제")
                .setMessage("'" + item.content + suffix(item.amount) + "' 루틴을 삭제할까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.deleteItem(item.id);
                    updateWidgets();
                    rerenderKeepingScroll();
                })
                .show();
    }

    private void exportBackup() {
        try {
            pendingBackupCode = BackupCrypto.newCode();
            pendingBackupText = BackupCrypto.encrypt(db.exportJson(), pendingBackupCode);
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "personal-scheduler-backup.json");
            startActivityForResult(intent, REQ_EXPORT);
        } catch (Exception e) {
            toast("백업 생성 실패: " + e.getMessage());
        }
    }

    private void chooseBackupFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (requestCode == REQ_EXPORT) {
            writeBackup(data.getData());
        } else if (requestCode == REQ_IMPORT) {
            askRestoreCode(data.getData());
        }
    }

    private void writeBackup(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            out.write(pendingBackupText.getBytes(StandardCharsets.UTF_8));
            new AlertDialog.Builder(this)
                    .setTitle("백업 완료")
                    .setMessage("새 폰에서 이 파일을 고르고 아래 복원 코드를 입력해줘.\n\n복원 코드: " + pendingBackupCode)
                    .setPositiveButton("확인", null)
                    .show();
        } catch (Exception e) {
            toast("백업 저장 실패: " + e.getMessage());
        }
    }

    private void askRestoreCode(Uri uri) {
        root.removeAllViews();
        addHero();
        LinearLayout card = card(CARD);
        addCardHeader(card, "백업 복원", "백업 파일을 만들 때 표시된 6자리 코드를 입력해요.");
        EditText code = input("6자리 복원 코드");
        card.addView(code, compactSpaced());
        Button restore = actionButton("복원하기", true);
        restore.setOnClickListener(v -> {
            try {
                String text = readAll(uri);
                db.importJson(BackupCrypto.decrypt(text, code.getText().toString().trim()));
                selectedCategoryId = -1;
                updateWidgets();
                toast("복원 완료");
                render();
            } catch (Exception e) {
                toast("복원 실패. 코드나 파일을 확인해줘.");
            }
        });
        card.addView(restore, match());
        root.addView(card, spaced());
    }

    private String readAll(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void updateWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        TodayWidgetProvider.updateAll(this, manager);
    }

    private void addCardHeader(LinearLayout card, String title, String subtitle) {
        addCardHeader(card, title, subtitle, null, null);
    }

    private void addCardHeader(LinearLayout card, String title, String subtitle, Boolean expanded, Runnable toggle) {
        if (expanded == null) {
            TextView titleView = title(title, 20);
            card.addView(titleView, match());
        } else {
            LinearLayout row = row();
            TextView titleView = title(title, 20);
            TextView toggleButton = chevron(expanded);
            toggleButton.setOnClickListener(v -> toggle.run());
            row.addView(titleView, weightedWithEndMargin());
            row.addView(toggleButton, chevronParams());
            card.addView(row, match());
        }
        TextView sub = body(subtitle, 14, MUTED);
        sub.setPadding(0, dp(4), 0, dp(12));
        card.addView(sub, match());
    }

    private void legacyUnusedHeader(LinearLayout card, String title, String subtitle) {
        TextView titleView = title(title, 20);
        card.addView(titleView, match());
        TextView sub = body(subtitle, 14, MUTED);
        sub.setPadding(0, dp(4), 0, dp(12));
        card.addView(sub, match());
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(color, 28, 0, color));
        return card;
    }

    private LinearLayout roundedRow() {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(rounded(Color.rgb(255, 250, 247), 22, 1, LINE));
        return row;
    }

    private LinearLayout categoryRow(boolean selected) {
        LinearLayout row = row();
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(rounded(selected ? PINK : Color.rgb(255, 250, 247), 22, selected ? 0 : 1, selected ? PINK : LINE));
        return row;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextSize(15);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setMinHeight(dp(48));
        input.setBackground(rounded(Color.rgb(255, 250, 247), 22, 1, LINE));
        return input;
    }

    private Button actionButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : PINK_DARK);
        button.setMinHeight(dp(48));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(rounded(primary ? PINK : PEACH_SOFT, 22, 0, PINK));
        return button;
    }

    private Button pillButton(String text, boolean selected) {
        Button button = actionButton(text, selected);
        button.setTextColor(selected ? Color.WHITE : INK);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(38));
        button.setMinimumHeight(dp(38));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private Button smallButton(String text) {
        Button button = actionButton(text, false);
        button.setTextSize(14);
        button.setMinHeight(dp(44));
        button.setBackground(rounded(PEACH_SOFT, 20, 0, PEACH_SOFT));
        return button;
    }

    private Button iconButton(String text, int bgColor, int textColor) {
        Button button = actionButton(text, false);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinEms(0);
        button.setMinHeight(dp(38));
        button.setMinimumHeight(dp(38));
        button.setPadding(0, 0, 0, 0);
        button.setBackground(rounded(bgColor, 18, 0, bgColor));
        return button;
    }

    private TextView chevron(boolean expanded) {
        TextView view = body("›", 24, PINK_DARK);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, 0, 0, 0);
        view.setRotation(expanded ? 90f : 0f);
        view.setTranslationY(expanded ? dp(-1) : 0f);
        view.setTranslationX(expanded ? dp(1) : 0f);
        return view;
    }

    private TextView title(String text, int sp) {
        TextView view = body(text, sp, INK);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView label(String text) {
        TextView view = body(text, 12, MUTED);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView muted(String text) {
        TextView view = body(text, 15, MUTED);
        view.setPadding(0, dp(4), 0, dp(2));
        return view;
    }

    private TextView body(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1.0f);
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

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams iconButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(38));
        params.setMarginEnd(dp(8));
        return params;
    }

    private LinearLayout.LayoutParams iconButtonParamsNoMargin() {
        return new LinearLayout.LayoutParams(dp(38), dp(38));
    }

    private LinearLayout.LayoutParams chevronParams() {
        return new LinearLayout.LayoutParams(dp(32), dp(32));
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightedWithEndMargin() {
        LinearLayout.LayoutParams params = weight();
        params.setMarginEnd(dp(10));
        return params;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = match();
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private LinearLayout.LayoutParams compactSpaced() {
        LinearLayout.LayoutParams params = match();
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private String suffix(String amount) {
        return amount == null || amount.trim().isEmpty() ? "" : " " + amount.trim();
    }

    private void applySystemBarPadding() {
        int top = systemDimen("status_bar_height");
        int bottom = systemDimen("navigation_bar_height");
        scroll.setPadding(0, top, 0, bottom);
    }

    private int systemDimen(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void rerenderKeepingScroll() {
        int y = scroll == null ? 0 : scroll.getScrollY();
        render();
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
