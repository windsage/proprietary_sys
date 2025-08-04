/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation.data;

import android.util.SparseArray;

public final class ConversationContent implements Cloneable {
    private int mId;
    private String mTranscriptionContent;
    private SparseArray<String> mTranslationContents = new SparseArray<>();
    private boolean isFinal;

    public ConversationContent(int id, String transcriptionContent) {
        mId = id;
        mTranscriptionContent = transcriptionContent;
        isFinal = false;
    }

    public void updateTranscriptionContent(String transcriptionContent) {
        mTranscriptionContent = transcriptionContent;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public boolean  isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isfinal) {
        isFinal = isfinal;
    }

    public String getTranscriptionContent() {
        return mTranscriptionContent;
    }

    public void addTranslationContent(int translationLanguageIndex, String translationContent) {
        mTranslationContents.put(translationLanguageIndex, translationContent);
    }

    public SparseArray<String> getTranslationContents() {
        return mTranslationContents;
    }

    @Override
    public ConversationContent clone() {
        ConversationContent object = null;
        try {
            object = (ConversationContent) super.clone();
            object.mTranslationContents = new SparseArray<>();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return object;
    }
}
