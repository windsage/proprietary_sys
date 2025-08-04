/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase;

import android.content.Context;

import com.qualcomm.qti.voiceai.usecase.asr.ASRManager;
import com.qualcomm.qti.voiceai.usecase.conversation.ConversationManager;
import com.qualcomm.qti.voiceai.usecase.translation.TranslationManager;

public final class Facade {

    private static Facade sInstance;
    private final Context mApplicationContext;
    private final TranslationManager mTranslationManager;
    private final ASRManager mASRManager;
    private final ConversationManager mConversationManager;

    private Facade(final Context context) {
        mApplicationContext = context;
        mTranslationManager = new TranslationManager(context);
        mASRManager = new ASRManager(context);
        mConversationManager = new ConversationManager(context);
    }

    static void register(final Context applicationContext) {
        sInstance = new Facade(applicationContext);
    }

    public static TranslationManager getTranslationManager() {
        return sInstance.mTranslationManager;
    }

    public static ASRManager getASRManager() {
        return sInstance.mASRManager;
    }

    public static ConversationManager getConversationManager() {
        return sInstance.mConversationManager;
    }

    public static Context getApplicationContext() {
        return sInstance.mApplicationContext;
    }
}
