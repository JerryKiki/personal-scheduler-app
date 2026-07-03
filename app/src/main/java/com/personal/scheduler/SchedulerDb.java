package com.personal.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SchedulerDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "personal_scheduler.db";
    private static final int DB_VERSION = 2;

    SchedulerDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE TABLE schedule_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category_id INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "amount TEXT NOT NULL," +
                "repeat_mask INTEGER NOT NULL," +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
        createCompletionTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createCompletionTable(db);
        }
    }

    private void createCompletionTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS completion_records (" +
                "date_key TEXT NOT NULL," +
                "item_id INTEGER NOT NULL," +
                "category_id INTEGER NOT NULL," +
                "done INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "PRIMARY KEY(date_key, item_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_completion_category_date " +
                "ON completion_records(category_id, date_key)");
    }

    long addCategory(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void deleteCategory(long categoryId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("completion_records", "category_id = ?", new String[]{String.valueOf(categoryId)});
        db.delete("schedule_items", "category_id = ?", new String[]{String.valueOf(categoryId)});
        db.delete("categories", "id = ?", new String[]{String.valueOf(categoryId)});
    }

    boolean updateCategory(long categoryId, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().update("categories", values, "id = ?", new String[]{String.valueOf(categoryId)}) > 0;
    }

    long addItem(long categoryId, String content, String amount, int repeatMask) {
        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("content", content.trim());
        values.put("amount", amount.trim());
        values.put("repeat_mask", repeatMask);
        return getWritableDatabase().insert("schedule_items", null, values);
    }

    void deleteItem(long itemId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("completion_records", "item_id = ?", new String[]{String.valueOf(itemId)});
        db.delete("schedule_items", "id = ?", new String[]{String.valueOf(itemId)});
    }

    boolean updateItem(long itemId, String content, String amount, int repeatMask) {
        ContentValues values = new ContentValues();
        values.put("content", content.trim());
        values.put("amount", amount.trim());
        values.put("repeat_mask", repeatMask);
        return getWritableDatabase().update("schedule_items", values, "id = ?", new String[]{String.valueOf(itemId)}) > 0;
    }

    List<Category> categories() {
        ArrayList<Category> categories = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, name FROM categories ORDER BY name", null)) {
            while (c.moveToNext()) {
                categories.add(new Category(c.getLong(0), c.getString(1)));
            }
        }
        return categories;
    }

    Category category(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, name FROM categories WHERE id = ?", new String[]{String.valueOf(id)})) {
            if (c.moveToNext()) {
                return new Category(c.getLong(0), c.getString(1));
            }
        }
        return null;
    }

    List<ScheduleItem> items(long categoryId) {
        ArrayList<ScheduleItem> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, category_id, content, amount, repeat_mask FROM schedule_items WHERE category_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(categoryId)})) {
            while (c.moveToNext()) {
                items.add(new ScheduleItem(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4)));
            }
        }
        return items;
    }

    List<ScheduleItem> todayItems(long categoryId) {
        ArrayList<ScheduleItem> items = new ArrayList<>();
        int todayBit = DateText.todayBit();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, category_id, content, amount, repeat_mask FROM schedule_items WHERE category_id = ? AND (repeat_mask & ?) != 0 ORDER BY id",
                new String[]{String.valueOf(categoryId), String.valueOf(todayBit)})) {
            while (c.moveToNext()) {
                items.add(new ScheduleItem(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4)));
            }
        }
        return items;
    }

    boolean isDoneToday(long itemId) {
        return isDone(DateText.todayKey(), itemId);
    }

    boolean isDone(String dateKey, long itemId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT done FROM completion_records WHERE date_key = ? AND item_id = ?",
                new String[]{dateKey, String.valueOf(itemId)})) {
            return c.moveToNext() && c.getInt(0) == 1;
        }
    }

    void toggleDoneToday(ScheduleItem item) {
        setDone(DateText.todayKey(), item.id, item.categoryId, !isDoneToday(item.id));
    }

    void toggleDoneToday(long itemId) {
        ScheduleItem item = item(itemId);
        if (item != null) {
            toggleDoneToday(item);
        }
    }

    void setDone(String dateKey, long itemId, long categoryId, boolean done) {
        ContentValues values = new ContentValues();
        values.put("date_key", dateKey);
        values.put("item_id", itemId);
        values.put("category_id", categoryId);
        values.put("done", done ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("completion_records", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    ScheduleItem item(long itemId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, category_id, content, amount, repeat_mask FROM schedule_items WHERE id = ?",
                new String[]{String.valueOf(itemId)})) {
            if (c.moveToNext()) {
                return new ScheduleItem(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4));
            }
        }
        return null;
    }

    List<CategoryStats> todayStats() {
        Calendar start = Calendar.getInstance();
        return statsBetween(start, start);
    }

    List<CategoryStats> weekStats() {
        Calendar start = Calendar.getInstance();
        int diffFromMonday = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        start.add(Calendar.DAY_OF_MONTH, -diffFromMonday);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        return statsBetween(start, end);
    }

    List<CategoryStats> monthStats() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return statsBetween(start, end);
    }

    private List<CategoryStats> statsBetween(Calendar startInclusive, Calendar endInclusive) {
        ArrayList<CategoryStats> result = new ArrayList<>();
        Calendar start = atStartOfDay(startInclusive);
        Calendar end = atStartOfDay(endInclusive);

        for (Category category : categories()) {
            List<ScheduleItem> categoryItems = items(category.id);
            int total = 0;
            int done = 0;
            Calendar day = (Calendar) start.clone();
            while (!day.after(end)) {
                String dateKey = DateText.key(day);
                Set<Long> doneIds = doneIds(category.id, dateKey);
                for (ScheduleItem item : categoryItems) {
                    if (item.repeatsOn(day.get(Calendar.DAY_OF_WEEK))) {
                        total++;
                        if (doneIds.contains(item.id)) {
                            done++;
                        }
                    }
                }
                day.add(Calendar.DAY_OF_MONTH, 1);
            }
            result.add(new CategoryStats(category.id, category.name, done, total));
        }
        return result;
    }

    private Set<Long> doneIds(long categoryId, String dateKey) {
        HashSet<Long> ids = new HashSet<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT item_id FROM completion_records WHERE category_id = ? AND date_key = ? AND done = 1",
                new String[]{String.valueOf(categoryId), dateKey})) {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
            }
        }
        return ids;
    }

    private Calendar atStartOfDay(Calendar source) {
        Calendar cal = (Calendar) source.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    JSONObject exportJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        JSONArray categoriesJson = new JSONArray();
        for (Category category : categories()) {
            JSONObject cat = new JSONObject();
            cat.put("id", category.id);
            cat.put("name", category.name);
            JSONArray itemsJson = new JSONArray();
            for (ScheduleItem item : items(category.id)) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("content", item.content);
                itemJson.put("amount", item.amount);
                itemJson.put("repeatMask", item.repeatMask);
                itemsJson.put(itemJson);
            }
            cat.put("items", itemsJson);
            categoriesJson.put(cat);
        }
        root.put("categories", categoriesJson);
        JSONArray completionsJson = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT date_key, item_id, category_id, done, updated_at FROM completion_records ORDER BY date_key, item_id",
                null)) {
            while (c.moveToNext()) {
                JSONObject completion = new JSONObject();
                completion.put("dateKey", c.getString(0));
                completion.put("itemId", c.getLong(1));
                completion.put("categoryId", c.getLong(2));
                completion.put("done", c.getInt(3) == 1);
                completion.put("updatedAt", c.getLong(4));
                completionsJson.put(completion);
            }
        }
        root.put("completions", completionsJson);
        return root;
    }

    void importJson(JSONObject root) throws JSONException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("schedule_items", null, null);
            db.delete("categories", null, null);
            db.delete("completion_records", null, null);
            db.delete("sqlite_sequence", "name IN (?, ?)", new String[]{"categories", "schedule_items"});
            JSONArray categoriesJson = root.getJSONArray("categories");
            for (int i = 0; i < categoriesJson.length(); i++) {
                JSONObject cat = categoriesJson.getJSONObject(i);
                long categoryId = cat.getLong("id");
                ContentValues catValues = new ContentValues();
                catValues.put("id", categoryId);
                catValues.put("name", cat.getString("name"));
                db.insertWithOnConflict("categories", null, catValues, SQLiteDatabase.CONFLICT_REPLACE);
                JSONArray itemsJson = cat.getJSONArray("items");
                for (int j = 0; j < itemsJson.length(); j++) {
                    JSONObject item = itemsJson.getJSONObject(j);
                    addItem(categoryId, item.getString("content"), item.getString("amount"), item.getInt("repeatMask"));
                }
            }
            JSONArray completionsJson = root.optJSONArray("completions");
            if (completionsJson != null) {
                for (int i = 0; i < completionsJson.length(); i++) {
                    JSONObject completion = completionsJson.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("date_key", completion.getString("dateKey"));
                    values.put("item_id", completion.getLong("itemId"));
                    values.put("category_id", completion.getLong("categoryId"));
                    values.put("done", completion.optBoolean("done", true) ? 1 : 0);
                    values.put("updated_at", completion.optLong("updatedAt", System.currentTimeMillis()));
                    db.insertWithOnConflict("completion_records", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
