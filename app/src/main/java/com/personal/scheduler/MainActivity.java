package com.personal.scheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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

    private SchedulerDb db;
    private LinearLayout root;
    private long selectedCategoryId = -1;
    private String pendingBackupText;
    private String pendingBackupCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new SchedulerDb(this);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(root);
        setContentView(scroll);

        addTitle("개인 스케줄러");
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

    private void addBackupButtons() {
        LinearLayout row = row();
        Button export = button("백업 내보내기");
        export.setOnClickListener(v -> exportBackup());
        Button restore = button("백업 복원");
        restore.setOnClickListener(v -> chooseBackupFile());
        row.addView(export, weight());
        row.addView(restore, weight());
        root.addView(row);
    }

    private void addCategoryEditor() {
        addSection("카테고리");
        LinearLayout row = row();
        EditText input = input("운동, 공부, 업무");
        Button add = button("추가");
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
            render();
        });
        row.addView(input, weight());
        row.addView(add);
        root.addView(row);
    }

    private void addCategoryList(List<Category> categories) {
        if (categories.isEmpty()) {
            addMuted("아직 카테고리가 없어. 운동/공부 같은 카테고리를 먼저 추가해줘.");
            return;
        }

        for (Category category : categories) {
            LinearLayout row = row();
            Button select = button((category.id == selectedCategoryId ? "✓ " : "") + category.name);
            select.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            select.setOnClickListener(v -> {
                selectedCategoryId = category.id;
                render();
            });
            Button delete = button("삭제");
            delete.setOnClickListener(v -> {
                db.deleteCategory(category.id);
                if (selectedCategoryId == category.id) {
                    selectedCategoryId = -1;
                }
                updateWidgets();
                render();
            });
            row.addView(select, weight());
            row.addView(delete);
            root.addView(row);
        }
    }

    private void addItemEditor() {
        Category category = db.category(selectedCategoryId);
        if (category == null) {
            return;
        }

        addSection(category.name + " 상세 스케줄");
        EditText content = input("내용: 마운틴 클라이머");
        EditText amount = input("횟수/분량: 100회");
        root.addView(content, match());
        root.addView(amount, match());

        LinearLayout days = row();
        CheckBox[] boxes = new CheckBox[7];
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < labels.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(labels[i]);
            box.setChecked(i == today);
            boxes[i] = box;
            days.addView(box);
        }
        root.addView(days);

        Button add = button("상세 스케줄 추가");
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
            render();
        });
        root.addView(add, match());
    }

    private void addItemList() {
        List<ScheduleItem> items = db.items(selectedCategoryId);
        if (items.isEmpty()) {
            addMuted("아직 상세 스케줄이 없어.");
            return;
        }
        for (ScheduleItem item : items) {
            LinearLayout row = row();
            TextView text = text("○ " + item.content + suffix(item.amount) + "\n반복: " + DateText.repeatText(item.repeatMask), 15, "#1D2B27");
            Button delete = button("삭제");
            delete.setOnClickListener(v -> {
                db.deleteItem(item.id);
                updateWidgets();
                render();
            });
            row.addView(text, weight());
            row.addView(delete);
            root.addView(row);
        }
    }

    private void addTodayPreview() {
        Category category = db.category(selectedCategoryId);
        if (category == null) {
            return;
        }
        addSection("오늘의 할일 미리보기");
        List<ScheduleItem> items = db.todayItems(selectedCategoryId);
        StringBuilder builder = new StringBuilder();
        builder.append(category.name).append("\n").append(DateText.todayHeader()).append("\n");
        if (items.isEmpty()) {
            builder.append("오늘 반복되는 스케줄 없음");
        } else {
            for (ScheduleItem item : items) {
                builder.append("○ ").append(item.content).append(suffix(item.amount)).append("\n");
            }
        }
        addBody(builder.toString().trim());
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
        addTitle("백업 복원");
        EditText code = input("6자리 복원 코드");
        root.addView(code, match());
        Button restore = button("복원하기");
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
        root.addView(restore, match());
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

    private void addTitle(String title) {
        TextView view = text(title, 26, "#13231E");
        view.setGravity(Gravity.START);
        view.setPadding(0, 0, 0, dp(12));
        root.addView(view, match());
    }

    private void addSection(String title) {
        TextView view = text(title, 18, "#13231E");
        view.setPadding(0, dp(22), 0, dp(8));
        root.addView(view, match());
    }

    private void addMuted(String body) {
        TextView view = text(body, 14, "#65736E");
        view.setPadding(0, dp(6), 0, dp(6));
        root.addView(view, match());
    }

    private void addBody(String body) {
        TextView view = text(body, 16, "#20302B");
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackgroundColor(0xFFF4F6F2);
        root.addView(view, match());
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        return input;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private TextView text(String text, int sp, String color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(android.graphics.Color.parseColor(color));
        return view;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private String suffix(String amount) {
        return amount == null || amount.trim().isEmpty() ? "" : " " + amount.trim();
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
