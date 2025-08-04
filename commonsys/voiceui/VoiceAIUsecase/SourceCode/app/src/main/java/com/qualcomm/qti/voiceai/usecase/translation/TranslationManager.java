/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.translation;

import android.content.Context;

public final class TranslationManager {

    public TranslationManager(Context context) {
        TranslatorWrapper.initializeIfNeeded(context);
    }
    public TranslatorWrapper createTranslator(TranslatorSupportedLanguage sourceLanguage,
                                              TranslatorSupportedLanguage targetLanguage) {
        return new TranslatorWrapper(sourceLanguage, targetLanguage);
    }
    public void translate(TranslatorWrapper translator, TranslateProgressListener listener,
                                 String translationId, String text) {
        translator.translate(listener, translationId, text);
    }

    public void releaseTranslator(TranslatorWrapper translatorWrapper) {
        translatorWrapper.release();
    }

    public void downloadAllModels() {
        TranslatorWrapper.downloadAllModels();
    }

    public static boolean isAllModesDownloaded() {
        return TranslatorWrapper.isModesAllDownloaded();
    }
}
