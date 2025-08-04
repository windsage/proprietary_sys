/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation.data;

import java.util.ArrayList;

public final class ConversationRecord {

    private final String mConversationName;
    private String mConversationAlias;
    private final String mTranscriptionLanguage;
    private final ArrayList<String> mTranslationLanguages = new ArrayList<>();
    private String mConversationSourceFilePath;
    private final ArrayList<String> mConversationTranslationFilePaths = new ArrayList<>();
    private final ArrayList<ConversationContent> mConversationContents = new ArrayList<>();

    private boolean mIsChecked;

    public ConversationRecord(String conversationName, String transcriptionLanguage,
                       ArrayList<String> translationLanguages) {
        mConversationName = conversationName;
        mTranscriptionLanguage = transcriptionLanguage;
        if (translationLanguages != null) {
            mTranslationLanguages.addAll(translationLanguages);
        }
    }

    public String getConversationName() {
        return mConversationName;
    }

    public void setConversationAlias(String conversationAlias) {
        mConversationAlias = conversationAlias;
    }

    public String getConversationAlias() {
        return mConversationAlias;
    }

    public String getTranscriptionLanguage() {
        return mTranscriptionLanguage;
    }

    public ArrayList<String> getTranslationLanguages() {
        return mTranslationLanguages;
    }

    public void setConversationSourceFilePath(String sourceFilePath) {
        mConversationSourceFilePath = sourceFilePath;
    }

    public String getConversationSourceFilePath() {
        return mConversationSourceFilePath;
    }

    public void addConversationTranslationFilePath(String translationFilePath) {
        mConversationTranslationFilePaths.add(translationFilePath);
    }

    public ArrayList<String> getConversationTranslationFilePaths() {
        return mConversationTranslationFilePaths;
    }

    public void addConversationContent(ConversationContent content) {
        mConversationContents.add(content);
    }

    public ArrayList<ConversationContent> getConversationContents() {
        return mConversationContents;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public String toString() {
        return "name: " + mConversationName + ", alias: " + mConversationAlias
                + ", transcription: " + mTranscriptionLanguage
                + ", translation: " + mTranslationLanguages
                + ", source file: " + mConversationSourceFilePath
                + ", translation files:" + mConversationTranslationFilePaths;
    }
}
