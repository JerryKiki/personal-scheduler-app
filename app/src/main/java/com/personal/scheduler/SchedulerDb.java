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
import java.util.List;

final class SchedulerDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "personal_scheduler.db";
    private static final int DB_VERSION = 1;

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    long addCategory(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void deleteCategory(long categoryId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("schedule_items", "category_id = ?", new String[]{String.valueOf(categoryId)});
        db.delete("categories", "id = ?", new String[]{String.valueOf(categoryId)});
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
        getWritableDatabase().delete("schedule_items", "id = ?", new String[]{String.valueOf(itemId)});
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
        return root;
    }

    void importJson(JSONObject root) throws JSONException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("schedule_items", null, null);
            db.delete("categories", null, null);
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
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
