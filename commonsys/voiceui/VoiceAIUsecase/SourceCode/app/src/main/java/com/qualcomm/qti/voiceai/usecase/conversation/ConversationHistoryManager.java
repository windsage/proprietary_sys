/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.conversation;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationContent;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

final class ConversationHistoryManager {

    private static final String TAG = ConversationHistoryManager.class.getSimpleName();
    private static final String CONTENT_ATTRIBUTE_ID = "id";
    private final Context mContext;
    ConversationHistoryManager(Context context) {
        mContext = context;
    }

    void saveConversationRecord(ConversationRecord conversationRecord) {
        String fileNamePrefix = conversationRecord.getConversationName();
        String transcriptionLanguage = conversationRecord.getTranscriptionLanguage();
        String prefixPath = mContext.getFilesDir().getAbsolutePath() + File.separator
                + fileNamePrefix + File.separator + fileNamePrefix + "_";
        String suffixPath = ".xml";
        String sourceFilePath = prefixPath + transcriptionLanguage + suffixPath;
        conversationRecord.setConversationSourceFilePath(sourceFilePath);
        ArrayList<String> translationLanguages = conversationRecord.getTranslationLanguages();
        int size = translationLanguages.size();
        ArrayList<ConversationContent> contents = conversationRecord.getConversationContents();
        File file = new File(sourceFilePath);
        String parent = file.getParent();
        if (parent != null) {
            File parentDir = new File(parent);
            boolean mkdirs = parentDir.mkdirs();
            Log.i(TAG, "mkdirs dirPath: " + parent + " result: " + mkdirs);
        }
        writeXML(sourceFilePath, contents, -1);
        for (int i = 0; i < size; i++) {
            String translationFilePath = prefixPath + translationLanguages.get(i) + suffixPath;
            conversationRecord.addConversationTranslationFilePath(translationFilePath);
            writeXML(translationFilePath, contents, i);
        }
    }

    void readConversationRecord(ConversationRecord conversationRecord) {
        readXML(conversationRecord.getConversationSourceFilePath(), conversationRecord, -1);
        ArrayList<String> paths = conversationRecord.getConversationTranslationFilePaths();
        for (int i = 0; i < paths.size(); i++) {
            readXML(paths.get(i), conversationRecord, i);
        }
    }

    private void readXML(String filePath, ConversationRecord conversationRecord,
                         int translationLanguageIndex) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            FileInputStream fis = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            ConversationContent content = null;
            HashMap<Integer, String> translationContentsMap = new HashMap<>();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if (mContext.getResources().getString(R.string.content).equals(tagName)) {
                        String id = parser.getAttributeValue(null, CONTENT_ATTRIBUTE_ID);
                        int contentId = Integer.parseInt(id);
                        String text = parser.nextText();
                        if (translationLanguageIndex == -1) {
                            if (content == null) {
                                content = new ConversationContent(contentId, text);
                            } else {
                                content = (ConversationContent) content.clone();
                                content.setId(contentId);
                                content.updateTranscriptionContent(text);
                            }
                            conversationRecord.addConversationContent(content);
                        } else {
                            translationContentsMap.put(contentId, text);
                        }
                    }
                }
                eventType = parser.next();
            }
            if (translationLanguageIndex != -1) {
                ArrayList<ConversationContent> contents
                        = conversationRecord.getConversationContents();
                for (int i = 0; i < contents.size(); i++) {
                    ConversationContent conversationContent = contents.get(i);
                    String text = translationContentsMap.get(conversationContent.getId());
                    if (!TextUtils.isEmpty(text)) {
                        conversationContent.addTranslationContent(translationLanguageIndex, text);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeXML(String filePath, ArrayList<ConversationContent> contents,
                          int translationLanguageIndex) {
        File file = new File(filePath);
        XmlSerializer serializer = Xml.newSerializer();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            serializer.setOutput(fos, StandardCharsets.UTF_8.name());
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(StandardCharsets.UTF_8.name(), true);
            String conversationTag = mContext.getResources().getString(R.string.conversation);
            serializer.startTag(null, conversationTag);

            if (translationLanguageIndex == -1) {
                for (ConversationContent item : contents) {
                    int id = item.getId();
                    String transcriptionContent = item.getTranscriptionContent();
                    addContent(serializer, id, transcriptionContent);
                }
            } else {
                for (ConversationContent item : contents) {
                    int id = item.getId();
                    SparseArray<String> translationContents = item.getTranslationContents();
                    String translationContent = translationContents.get(translationLanguageIndex);
                    addContent(serializer, id, translationContent);
                }
            }

            serializer.endTag(null, conversationTag);
            serializer.endDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addContent(XmlSerializer serializer, int id, String content) throws Exception {
        String contentTag = mContext.getResources().getString(R.string.content);
        serializer.startTag(null, contentTag);
        serializer.attribute(null, CONTENT_ATTRIBUTE_ID, String.valueOf(id));
        serializer.text(content == null ? "":content);
        serializer.endTag(null, contentTag);
    }
}
