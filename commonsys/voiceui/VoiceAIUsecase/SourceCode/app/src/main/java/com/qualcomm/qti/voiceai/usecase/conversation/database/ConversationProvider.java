/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public final class ConversationProvider extends ContentProvider {

    private static final UriMatcher mUriMatcher;
    private static final int MATCH_CODE_CONVERSATION_TABLE = 1;
    private static final String AUTHORITY = ConversationProvider.class.getName();
    public static final Uri CONTENT_URI_CONVERSATION_TABLE = Uri.parse("content://"
            + AUTHORITY + "/" + ConversationDatabaseHelper.CONVERSATION_TABLE);
    private ConversationDatabaseHelper mHelper;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY,
                ConversationDatabaseHelper.CONVERSATION_TABLE, MATCH_CODE_CONVERSATION_TABLE);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            mHelper = ConversationDatabaseHelper.getInstance(context.getApplicationContext());
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        return database.query(ConversationDatabaseHelper.CONVERSATION_TABLE, null, selection,
                selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        Uri result = null;
        Context context = getContext();
        if (context == null) {
            return null;
        }
        try {
            long rowId = database.insert(ConversationDatabaseHelper.CONVERSATION_TABLE,
                    null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(CONTENT_URI_CONVERSATION_TABLE, rowId);
                context.getContentResolver().notifyChange(uri, null);
            }
        } catch (SQLException e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        int result = 0;
        Context context = getContext();
        if (context == null) {
            return result;
        }
        result = database.delete(ConversationDatabaseHelper.CONVERSATION_TABLE,
                selection, selectionArgs);
        context.getContentResolver().notifyChange(uri, null);
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        int result = 0;
        Context context = getContext();
        if (context == null) {
            return result;
        }
        try {
            result = database.update(ConversationDatabaseHelper.CONVERSATION_TABLE, values,
                    selection, selectionArgs);
            context.getContentResolver().notifyChange(uri, null);
        } catch (SQLException e) {
            result = -1;
            e.printStackTrace();
        }
        return result;
    }
}
