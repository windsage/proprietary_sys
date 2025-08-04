/*****************************************************************
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import vendor.qti.bluetooth.xpan.XpanDatabaseContract.DeviceData;

public class XpanContentProvider extends ContentProvider {
    private static String TAG = "XpanContentProvider";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    private static final String AUTHORITY = "vendor.qti.bluetooth.xpan.provider";
    private static final String DB_NAME = "xpan.db";
    private static final int DB_VERSION = 2;

    private static final String LIST_TYPE = "vnd.android.cursor.dir/vnd.qti.bluetooth.xpan";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int MATCH_DEVICE_DATA_ALL = 1;

    static {
        sURIMatcher.addURI(AUTHORITY, XpanDatabaseContract.TABLE_DEVICE_DATA, MATCH_DEVICE_DATA_ALL);
    }

    private XpanSqliteHelper mDbHelper;

    @Override
    public boolean onCreate() {
        if (DBG) {
            Log.d(TAG, "onCreate");
        }
        mDbHelper = new XpanSqliteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        if (db == null) {
            Log.w(TAG, "query: db is null");
            return null;
        }

        if (sURIMatcher.match(uri) == MATCH_DEVICE_DATA_ALL) {
            if (VDBG)
                Log.v(TAG, "query");
            cursor = db.query(XpanDatabaseContract.TABLE_DEVICE_DATA, projection,
                    selection, selectionArgs, null, null, sortOrder);
        }

        if (cursor == null) {
            Log.w(TAG, "query: failed");
            return null;
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case MATCH_DEVICE_DATA_ALL:
                return LIST_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Uri uriReturn = null;
        if (db == null) {
            Log.w(TAG, "insert: db is null");
            return null;
        }

        if (sURIMatcher.match(uri) == MATCH_DEVICE_DATA_ALL) {
            long newId = db.insert(XpanDatabaseContract.TABLE_DEVICE_DATA, null, values);
            uriReturn = Uri.parse("content://" + AUTHORITY + '/' +
                    XpanDatabaseContract.TABLE_DEVICE_DATA + '/' + newId);
            if (VDBG)
                Log.v(TAG, "insert: newId=" + newId);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return uriReturn;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;
        if (db == null) {
            Log.w(TAG, "delete: db is null");
            return 0;
        }

        if (sURIMatcher.match(uri) == MATCH_DEVICE_DATA_ALL) {
            count = db.delete(XpanDatabaseContract.TABLE_DEVICE_DATA, selection, selectionArgs);
            if (VDBG)
                Log.v(TAG, "delete: count=" + count);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;
        if (db == null) {
            Log.w(TAG, "update: db is null");
            return 0;
        }

        if (sURIMatcher.match(uri) == MATCH_DEVICE_DATA_ALL) {
            count = db.update(XpanDatabaseContract.TABLE_DEVICE_DATA, values,
                    selection, selectionArgs);
            if (VDBG)
                Log.v(TAG, "update: count=" + count);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static final class XpanSqliteHelper extends SQLiteOpenHelper {
        private final String TAG = "XpanSqliteHelper";

        public XpanSqliteHelper(final Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            if (DBG) {
                Log.d(TAG, "XpanSqliteHelper: version=" + DB_VERSION);
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (DBG) {
                Log.d(TAG, "onCreate");
            }
            createTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (DBG) {
                Log.d(TAG, "onUpgrade from version " + oldV + " to " + newV);
            }
            // if version is changed in future,
            // need to add code to migrate data to the new table.
            dropTable(db);
            createTable(db);
        }

        private void createTable(SQLiteDatabase db) {
            if (DBG) {
                Log.d(TAG, "createTable");
            }
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + XpanDatabaseContract.TABLE_DEVICE_DATA
                            + "("
                            + DeviceData.Columns.ADDRESS
                            + " TEXT NOT NULL PRIMARY KEY,"
                            + DeviceData.Columns.BEARER
                            + " TEXT, "
                            + DeviceData.Columns.WHC_SUPPORTED
                            + " INTEGER DEFAULT 0); ");
        }

        private void dropTable(SQLiteDatabase db) {
            if (DBG) {
                Log.d(TAG, "dropTable");
            }
            db.execSQL("DROP TABLE IF EXISTS " + XpanDatabaseContract.TABLE_DEVICE_DATA);
        }
    }
}
