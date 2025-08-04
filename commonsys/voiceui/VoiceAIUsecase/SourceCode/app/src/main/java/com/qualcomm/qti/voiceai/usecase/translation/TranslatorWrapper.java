/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.translation;

import android.content.Context;
import android.util.Log;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.sdkinternal.MlKitContext;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public final class TranslatorWrapper {

    private static final String TAG = TranslatorWrapper.class.getSimpleName();
    private final Translator mTranslator;
    private static int modelsDownloaded;
    private static final Object numLock = new Object();

    TranslatorWrapper(TranslatorSupportedLanguage sourceLanguage,
                      TranslatorSupportedLanguage targetLanguage) {
        TranslatorOptions translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage.getLanguage())
                .setTargetLanguage(targetLanguage.getLanguage()).build();
        mTranslator = Translation.getClient(translatorOptions);
    }

    void translate(TranslateProgressListener listener, String translationId, String text) {
        mTranslator.translate(text)
                .addOnSuccessListener(s -> listener.onDone(translationId, s))
                .addOnFailureListener(e -> listener.onError(translationId));
    }

    void release() {
        mTranslator.close();
    }

    static void downloadAllModels() {
        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        Log.i(TAG, "start download models!");
        //english model do not need to download
        modelsDownloaded = 0;
        downloadModel(conditions, TranslatorSupportedLanguage.CHINESE);
        downloadModel(conditions, TranslatorSupportedLanguage.KOREAN);
        downloadModel(conditions, TranslatorSupportedLanguage.SPANISH);
        downloadModel(conditions, TranslatorSupportedLanguage.JAPANESE);
        downloadModel(conditions, TranslatorSupportedLanguage.FRENCH);
        downloadModel(conditions, TranslatorSupportedLanguage.GERMAN);
        downloadModel(conditions, TranslatorSupportedLanguage.ITALIAN);
        downloadModel(conditions, TranslatorSupportedLanguage.HINDI);
    }

    private static void downloadModel(DownloadConditions conditions,
                                      TranslatorSupportedLanguage target) {
        TranslatorWrapper translatorWrapper
                = new TranslatorWrapper(TranslatorSupportedLanguage.ENGLISH, target);
        Translator translator = translatorWrapper.mTranslator;
        addModelsSum();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(unused -> {
            Log.i(TAG, "en-" + target.getLanguage() + " model download successfully!");
            subModelsSum();
            translator.close();
        }).addOnFailureListener(e -> Log.i(TAG, "en-" + target.getLanguage()
                + " model download fail!" + e));
    }

    static void initializeIfNeeded(Context context) {
        MlKitContext.initializeIfNeeded(context);
    }

    static boolean isModesAllDownloaded() {
        return modelsDownloaded == 0;
    }

    private static void addModelsSum() {
        synchronized (numLock){
            modelsDownloaded++;
        }
    }
    private static void subModelsSum() {
        synchronized (numLock){
            modelsDownloaded--;
        }
    }
}
