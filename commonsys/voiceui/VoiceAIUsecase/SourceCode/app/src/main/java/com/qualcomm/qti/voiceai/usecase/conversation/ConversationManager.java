/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;
import com.qualcomm.qti.voiceai.usecase.conversation.database.ConversationDatabaseHelper;
import com.qualcomm.qti.voiceai.usecase.conversation.database.ConversationProvider;
import com.qualcomm.qti.voiceai.usecase.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConversationManager {

    private static final String TAG = ConversationManager.class.getSimpleName();
    private static final String SEPARATOR = ",";
    private final Context mContext;
    private final ConversationHistoryManager mConversationHistoryManager;
    private ExecutorService mExecutorService;

    public ConversationManager(Context context) {
        mContext = context.getApplicationContext();
        mConversationHistoryManager = new ConversationHistoryManager(context);
    }

    public boolean isConversationRecordAliasExists(String alias) {
        try (Cursor query = mContext.getContentResolver().query(
                ConversationProvider.CONTENT_URI_CONVERSATION_TABLE, null,
                ConversationDatabaseHelper.CONVERSATION_ALIAS + " = ? ", new String[]{ alias },
                null)) {
            if (query != null && query.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public void updateConversationRecordAliasName(ConversationRecord record) {
        ContentValues values = new ContentValues();
        String conversationName = record.getConversationName();
        String conversationAlias = record.getConversationAlias();
        values.put(ConversationDatabaseHelper.CONVERSATION_ALIAS, conversationAlias);
        mContext.getContentResolver().update(ConversationProvider.CONTENT_URI_CONVERSATION_TABLE,
                values, ConversationDatabaseHelper.CONVERSATION_NAME + " = ?",
                new String[] {conversationName});
    }

    public void saveConversationRecord(ConversationRecord record) {
        mConversationHistoryManager.saveConversationRecord(record);
        ContentValues values = new ContentValues();
        String conversationName = record.getConversationName();
        values.put(ConversationDatabaseHelper.CONVERSATION_NAME, conversationName);
        String conversationAlias = record.getConversationAlias();
        values.put(ConversationDatabaseHelper.CONVERSATION_ALIAS, conversationAlias);
        String transcriptionLanguage = record.getTranscriptionLanguage();
        values.put(ConversationDatabaseHelper.TRANSCRIPTION_LANGUAGE, transcriptionLanguage);
        ArrayList<String> translationLanguages = record.getTranslationLanguages();
        String translation = "";
        if (translationLanguages.size() > 0) {
            for (String language : translationLanguages) {
                translation += language + SEPARATOR;
            }
            translation = translation.substring(0, translation.length() - 1);
        }
        values.put(ConversationDatabaseHelper.TRANSLATION_LANGUAGES, translation);
        String conversationSourceFilePath = record.getConversationSourceFilePath();
        values.put(ConversationDatabaseHelper.CONVERSATION_SOURCE_FILE_PATH,
                conversationSourceFilePath);
        ArrayList<String> paths = record.getConversationTranslationFilePaths();
        String path = "";
        if (paths.size() > 0) {
            for (String item : paths) {
                path += item + SEPARATOR;
            }
            path = path.substring(0, path.length() - 1);
        }
        values.put(ConversationDatabaseHelper.CONVERSATION_TRANSLATION_FILE_PATHS, path);
        boolean result = mContext.getContentResolver().insert(
                ConversationProvider.CONTENT_URI_CONVERSATION_TABLE, values) != null;
        Log.i(TAG, "add conversation record: " + record + ", result: " + result);
    }

    public ArrayList<ConversationRecord> getAllConversationRecords() {
        ArrayList<ConversationRecord> result = new ArrayList<>();
        try (Cursor query = mContext.getContentResolver().query(
                ConversationProvider.CONTENT_URI_CONVERSATION_TABLE, null,
                null, null, ConversationDatabaseHelper.ID + " desc")) {
            if (query != null && query.getCount() > 0) {
                while (query.moveToNext()) {
                    int columnIndex= query.getColumnIndex(
                            ConversationDatabaseHelper.CONVERSATION_NAME);
                    String conversationName = query.getString(columnIndex);
                    columnIndex = query.getColumnIndex(
                            ConversationDatabaseHelper.CONVERSATION_ALIAS);
                    String conversationAlias = query.getString(columnIndex);
                    columnIndex = query.getColumnIndex(
                            ConversationDatabaseHelper.TRANSCRIPTION_LANGUAGE);
                    String transcriptionLanguage = query.getString(columnIndex);
                    columnIndex = query.getColumnIndex(
                            ConversationDatabaseHelper.TRANSLATION_LANGUAGES);
                    String translationLanguage = query.getString(columnIndex);
                    columnIndex = query.getColumnIndex(
                            ConversationDatabaseHelper.CONVERSATION_SOURCE_FILE_PATH);
                    String conversationSourceFilePath = query.getString(columnIndex);
                    columnIndex = query.getColumnIndex(
                            ConversationDatabaseHelper.CONVERSATION_TRANSLATION_FILE_PATHS);
                    String conversationTranslationFilePaths = query.getString(columnIndex);
                    ArrayList<String> translationLanguages = new ArrayList<>();
                    if (!TextUtils.isEmpty(translationLanguage)) {
                        String[] languages = translationLanguage.split(SEPARATOR);
                        translationLanguages.addAll(Arrays.asList(languages));
                    }
                    ConversationRecord conversationRecord = new ConversationRecord(conversationName,
                            transcriptionLanguage, translationLanguages);
                    if (!TextUtils.isEmpty(conversationAlias)) {
                        conversationRecord.setConversationAlias(conversationAlias);
                    }
                    conversationRecord.setConversationSourceFilePath(conversationSourceFilePath);
                    if (!TextUtils.isEmpty(conversationTranslationFilePaths)) {
                        String[] paths = conversationTranslationFilePaths.split(SEPARATOR);
                        for (String item : paths) {
                            conversationRecord.addConversationTranslationFilePath(item);
                        }
                    }
                    result.add(conversationRecord);
                }
            }
        }
        return result;
    }

    public void readConversationContentFromLocal(ConversationRecord conversationRecord) {
        Log.i(TAG, "read conversation from local record: " + conversationRecord);
        mConversationHistoryManager.readConversationRecord(conversationRecord);
    }

    public void deleteConversationRecord(ConversationRecord conversationRecord) {
        Log.i(TAG, "delete conversation record: " + conversationRecord);
        mContext.getContentResolver().delete(ConversationProvider.CONTENT_URI_CONVERSATION_TABLE,
                ConversationDatabaseHelper.CONVERSATION_NAME + " = ? ",
                new String[] { conversationRecord.getConversationName() });
        String conversationSourceFilePath = conversationRecord.getConversationSourceFilePath();
        if (mExecutorService == null) {
            mExecutorService
                    = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        File parentFile = new File(conversationSourceFilePath).getParentFile();
        if (parentFile != null) {
            mExecutorService.execute(() -> Utils.deleteFile(parentFile));
        }
    }
}
