package ru.superplushkin.twofactorauthapp.db;

import android.annotation.SuppressLint;

import android.content.ContentValues;
import android.content.Context;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.superplushkin.twofactorauthapp.model.Service;
import ru.superplushkin.twofactorauthapp.model.SimpleService;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "authenticator.db";
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_SERVICES = "services";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SERVICE_NAME = "service_name";
    public static final String COLUMN_SECRET_KEY = "secret_key";
    public static final String COLUMN_ACCOUNT = "account";
    public static final String COLUMN_ISSUER = "issuer";
    public static final String COLUMN_ALGORITHM = "algorithm";
    public static final String COLUMN_DIGITS = "digits";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_USAGE_COUNT = "usage_count";
    public static final String COLUMN_IS_FAVORITE = "is_favorite";
    public static final String COLUMN_SORT_ORDER = "sort_order";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SERVICES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SERVICE_NAME + " TEXT,"
            + COLUMN_SECRET_KEY + " TEXT NOT NULL,"
            + COLUMN_ACCOUNT + " TEXT,"
            + COLUMN_ISSUER + " TEXT,"
            + COLUMN_ALGORITHM + " TEXT DEFAULT 'SHA1',"
            + COLUMN_DIGITS + " INTEGER DEFAULT 6,"
            + COLUMN_CREATED_AT + " TEXT,"
            + COLUMN_USAGE_COUNT + " INTEGER DEFAULT 0,"
            + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"
            + COLUMN_SORT_ORDER + " INTEGER DEFAULT 0"
            + ")";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
        onCreate(db);
    }

    public long addService(String serviceName, String secretKey, String account, String issuer, String algorithm, short digits) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVICE_NAME, serviceName);
        values.put(COLUMN_SECRET_KEY, secretKey);
        values.put(COLUMN_ACCOUNT, account);
        values.put(COLUMN_ISSUER, issuer);
        values.put(COLUMN_ALGORITHM, algorithm);
        values.put(COLUMN_DIGITS, digits);
        values.put(COLUMN_CREATED_AT, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        int maxSortOrder = getMaxSortOrder();
        values.put(COLUMN_SORT_ORDER, maxSortOrder + 1);

        return db.insert(TABLE_SERVICES, null, values);
    }
    public long restoreService(Service service) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVICE_NAME, service.getServiceName());
        values.put(COLUMN_SECRET_KEY, service.getSecretKey());
        values.put(COLUMN_ACCOUNT, service.getAccount());
        values.put(COLUMN_ISSUER, service.getIssuer());
        values.put(COLUMN_ALGORITHM, service.getAlgorithm());
        values.put(COLUMN_DIGITS, service.getDigits());
        values.put(COLUMN_CREATED_AT, service.getCreatedAt());
        values.put(COLUMN_USAGE_COUNT, service.getUsageCount());
        values.put(COLUMN_IS_FAVORITE, service.isFavorite() ? 1 : 0);

        int maxSortOrder = getMaxSortOrder();
        values.put(COLUMN_SORT_ORDER, maxSortOrder + 1);

        return db.insert(TABLE_SERVICES, null, values);
    }

    private int getMaxSortOrder() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + COLUMN_SORT_ORDER + ") FROM " + TABLE_SERVICES;
        Cursor cursor = db.rawQuery(query, null);

        int maxOrder = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0))
            maxOrder = cursor.getInt(0);

        cursor.close();
        return maxOrder;
    }

    public List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SERVICES, null, null, null, null, null, COLUMN_SORT_ORDER + " ASC");

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range")
                Service service = new Service(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_SECRET_KEY)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ISSUER)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ALGORITHM)),
                    cursor.getShort(cursor.getColumnIndex(COLUMN_DIGITS)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_USAGE_COUNT)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_IS_FAVORITE)) == 1,
                    cursor.getInt(cursor.getColumnIndex(COLUMN_SORT_ORDER))
                );
                services.add(service);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return services;
    }
    public List<SimpleService> getAllSimpleServices() {
        List<SimpleService> services = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {
            COLUMN_ID,
            COLUMN_SERVICE_NAME,
            COLUMN_ACCOUNT,
            COLUMN_CREATED_AT,
            COLUMN_IS_FAVORITE,
            COLUMN_SORT_ORDER
        };

        Cursor cursor = db.query(TABLE_SERVICES, columns, null, null, null, null, COLUMN_SORT_ORDER + " ASC");

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range")
                SimpleService service = new SimpleService(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_IS_FAVORITE)) == 1,
                    cursor.getInt(cursor.getColumnIndex(COLUMN_SORT_ORDER))
                );
                services.add(service);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return services;
    }

    public Service getService(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(TABLE_SERVICES, null, selection, selectionArgs, null, null, null);

        Service service = null;
        if (cursor.moveToFirst())
            service = extractServiceFromCursor(cursor);

        cursor.close();
        return service;
    }
    public SimpleService getSimpleService(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        String[] columns = {
            COLUMN_ID,
            COLUMN_SERVICE_NAME,
            COLUMN_ACCOUNT,
            COLUMN_CREATED_AT,
            COLUMN_IS_FAVORITE,
            COLUMN_SORT_ORDER
        };

        Cursor cursor = db.query(TABLE_SERVICES, columns, selection, selectionArgs, null, null, null);

        SimpleService simpleService = null;
        if (cursor.moveToFirst()) {
            @SuppressLint("Range")
            SimpleService service = new SimpleService(
                cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_NAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT)),
                cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_IS_FAVORITE)) == 1,
                cursor.getInt(cursor.getColumnIndex(COLUMN_SORT_ORDER))
            );
            simpleService = service;
        }

        cursor.close();
        return simpleService;
    }

    public void updateServiceOrder(long id, int newPosition) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SORT_ORDER, newPosition);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        db.update(TABLE_SERVICES, values, whereClause, whereArgs);
    }

    public void incrementUsageCount(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        Service service = getService(id);
        if (service == null)
            return;

        ContentValues values = new ContentValues();
        values.put(COLUMN_USAGE_COUNT, service.getUsageCount() + 1);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        int rowsAffected = db.update(TABLE_SERVICES, values, whereClause, whereArgs);
    }
    public void toggleFavorite(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        Service service = getService(id);
        if (service == null)
            return;

        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_FAVORITE, service.isFavorite() ? 0 : 1);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        int rowsAffected = db.update(TABLE_SERVICES, values, whereClause, whereArgs);
    }

    public void deleteService(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SERVICES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private Service extractServiceFromCursor(Cursor cursor) {
        @SuppressLint("Range")
        long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
        @SuppressLint("Range")
        String serviceName = cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_NAME));
        @SuppressLint("Range")
        String secretKey = cursor.getString(cursor.getColumnIndex(COLUMN_SECRET_KEY));
        @SuppressLint("Range")
        String account = cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT));
        @SuppressLint("Range")
        String issuer = cursor.getString(cursor.getColumnIndex(COLUMN_ISSUER));
        @SuppressLint("Range")
        String algorithm = cursor.getString(cursor.getColumnIndex(COLUMN_ALGORITHM));
        @SuppressLint("Range")
        short digits = cursor.getShort(cursor.getColumnIndex(COLUMN_DIGITS));
        @SuppressLint("Range")
        String createdAt = cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT));
        @SuppressLint("Range")
        int usageCount = cursor.getInt(cursor.getColumnIndex(COLUMN_USAGE_COUNT));
        @SuppressLint("Range")
        boolean isFavorite = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_FAVORITE)) == 1;
        @SuppressLint("Range")
        int sortOrder = cursor.getInt(cursor.getColumnIndex(COLUMN_SORT_ORDER));

        return new Service(id, serviceName, secretKey, account, issuer, algorithm, digits, createdAt, usageCount, isFavorite, sortOrder);
    }
}