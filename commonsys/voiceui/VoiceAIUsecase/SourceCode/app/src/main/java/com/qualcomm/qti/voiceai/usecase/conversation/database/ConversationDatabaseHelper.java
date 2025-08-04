/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class ConversationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "conversation.db";
    private static final int DATABASE_VERSION = 1;
    static final String CONVERSATION_TABLE = "ConversationTab";
    public static final String ID = "_id";
    public static final String CONVERSATION_NAME = "conversation_name";
    public static final String CONVERSATION_ALIAS = "conversation_alias";
    public static final String TRANSCRIPTION_LANGUAGE = "transcription_language";
    public static final String TRANSLATION_LANGUAGES = "translation_languages";
    public static final String CONVERSATION_SOURCE_FILE_PATH = "conversation_source_file_path";
    public static final String CONVERSATION_TRANSLATION_FILE_PATHS
            = "conversation_translation_file_paths";
    private static ConversationDatabaseHelper sInstance;

    private ConversationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static synchronized ConversationDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConversationDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createConversationTable(db);
    }

    private void createConversationTable(SQLiteDatabase db) {
        String sql = "create table if not exists " + CONVERSATION_TABLE
                + " ( " + ID + " integer primary key autoincrement , "
                + CONVERSATION_NAME + " text unique not null , "
                + CONVERSATION_ALIAS + " text , "
                + TRANSCRIPTION_LANGUAGE + " text not null, "
                + TRANSLATION_LANGUAGES + " text , "
                + CONVERSATION_SOURCE_FILE_PATH + " text not null , "
                + CONVERSATION_TRANSLATION_FILE_PATHS + " text ); ";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
