/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import static com.qualcomm.qti.voiceai.usecase.Facade.getConversationManager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationContent;
import com.qualcomm.qti.voiceai.usecase.conversation.data.ConversationRecord;
import com.qualcomm.qti.voiceai.usecase.conversation.data.Settings;
import com.qualcomm.qti.voiceai.usecase.translation.TranslateProgressListener;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorWrapper;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BufferModeHandlerThread extends HandlerThread {

    private static final int Event_Partial = 0;
    private static final int Event_Final = 1;
    private static final int Event_Name = 2;



    protected final static String TAG = BufferModeHandlerThread.class.getSimpleName();
    private static Handler mHandler;
    private static int mIndex = 0;
    private final ArrayList<ConversationContent> mConversationContents = new ArrayList<>();
    private static TranslatorWrapper[] mTranslatorWrappers;
    private static int mConversationId = -1;
    private static int mTranslatorSize = -1;

    private ReentrantLock mReentrantLock = new ReentrantLock();
    private Condition mCondition = mReentrantLock.newCondition();
    private static Context mContext = null;
    private static String defaultConversationName;

    public BufferModeHandlerThread(String name, Context context) {
        super(name);
        mContext = context;
        mTranslatorWrappers = new TranslatorWrapper[2];
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // process the msg
                processMessage(msg);
            }
        };
    }

    public static void addPartialResult(Bundle data) {
        Log.d(TAG, "addPartialResult data = " + data);
        initTranslatorifNeed();
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(Event_Partial);
            msg.obj = data;
            mHandler.sendMessage(msg);
        } else {
            Log.d(TAG, "handle = null");
        }
    }

    private static void initTranslatorifNeed() {
        if(mTranslatorSize < 0) {
            mTranslatorSize = 0;
            int indexLanguage =  Settings.getTranscriptionLanguage(mContext);
            String transcriptLanguage =
                    TranslatorSupportedLanguage.getSupportedLanguages().get(indexLanguage);
            Log.d(TAG, "transcriptLanguage = "+ transcriptLanguage);
            for(String item : Settings.getTranslationLanguages(mContext)) {
                mTranslatorWrappers[mTranslatorSize] = Facade.getTranslationManager().createTranslator(
                        TranslatorSupportedLanguage.convertLanguage(transcriptLanguage),
                        TranslatorSupportedLanguage.convertLanguage(item));
                Log.d(TAG," item=" + item);
                mTranslatorSize++;
            }
        }
    }

    private static void releaseTranslator() {
        if(mTranslatorSize >= 0) {
            for(int i = 0; i < mTranslatorSize; i++) {
                Facade.getTranslationManager().releaseTranslator(mTranslatorWrappers[i]);
                mTranslatorWrappers[i] = null;
            }
        }
        mTranslatorSize = -1;
    }

    public static void addFinalResult(Bundle data) {
        Log.d(TAG, "addFinalResult data = " + data);
        initTranslatorifNeed();
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(Event_Final);
            msg.obj = data;
            mHandler.sendMessage(msg);
        }
    }

    public static void addName(String name, String aliasName) {
        Log.d(TAG, "addName name = " + name + " aliasName = "+aliasName);
        defaultConversationName = name;
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(Event_Name);
            msg.obj = aliasName;
            mHandler.sendMessage(msg);
        }
    }


    private void processMessage(Message msg) {
        // process msg
        switch (msg.what) {
            case Event_Partial:
                Log.d(TAG, "Event_Partial");
                Bundle partialResult = (Bundle) msg.obj;
                ArrayList<String> finalsBufferMode = partialResult.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                addASRResult(finalsBufferMode, false);
                break;
            case Event_Final:
                Log.d(TAG, "Event_Final");
                Bundle finalResult = (Bundle) msg.obj;
                ArrayList<String> finals = finalResult.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                addASRResult(finals, true);
                releaseTranslator();
                break;
            case Event_Name:
                Log.d(TAG, "Event_Name");
                savaResult((String)msg.obj);
                cleanUp();
                break;

        }
    }

    private void cleanUp() {
        mConversationContents.clear();
        mConversationId = -1;
        defaultConversationName = null;
    }

    private void savaResult(String aliasName) {
        Log.d(TAG,"savaResult");
        ArrayList<String> arrayList = new ArrayList<>(Settings.getTranslationLanguages(mContext));
        int transcriptionLanguage = Settings.getTranscriptionLanguage(mContext);
        ConversationRecord conversationRecord = new ConversationRecord(defaultConversationName,
                TranslatorSupportedLanguage.getSupportedLanguages().get(transcriptionLanguage),arrayList);
        if(aliasName != null) {
            conversationRecord.setConversationAlias(aliasName);
        }
        for (int i =0; i< mConversationContents.size(); i++){
            conversationRecord.addConversationContent(mConversationContents.get(i));
        }
        getConversationManager().saveConversationRecord(conversationRecord);
    }

    private void addASRResult(ArrayList<String> finalsBufferMode, boolean isFinal) {
        for(int index = 0; index < finalsBufferMode.size(); index++) {
            ConversationContent conversationContent
                    = new ConversationContent(++mConversationId,finalsBufferMode.get(index));
            mConversationContents.add(conversationContent);

            for(int i = 0; i < mTranslatorSize; i++) {
                mReentrantLock.lock();
                try {
                    Facade.getTranslationManager().translate(mTranslatorWrappers[i],new TranslateProgressListener() {
                        @Override
                        public void onDone(String translationId, String result) {
                            Log.d(TAG, "onDone " + "translationId : " + translationId
                                    + " result : " + result);
                            mConversationContents.get(mConversationContents.size() - 1).
                                    addTranslationContent(Integer.parseInt(translationId), result);
                            mReentrantLock.lock();
                            try {
                                mCondition.signalAll();
                            }catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                mReentrantLock.unlock();
                            }
                        }

                        @Override
                        public void onError(String translationId) {
                            Log.e(TAG, "onError translationId : " + translationId);
                            mReentrantLock.lock();
                            try {
                                mCondition.signalAll();
                            }catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                mReentrantLock.unlock();
                            }
                        }
                    },Integer.toString(i), finalsBufferMode.get(index));
                    mCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG,  "addASRResult Exception = "+ e);
                } finally {
                    mReentrantLock.unlock();
                }
            }
        }
    }
}

