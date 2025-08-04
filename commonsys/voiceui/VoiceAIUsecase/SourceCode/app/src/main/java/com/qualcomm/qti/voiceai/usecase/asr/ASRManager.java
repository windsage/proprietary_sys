/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.asr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.List;

public final class ASRManager {

    private static final String TAG = ASRManager.class.getSimpleName();
    private static final String EXTRA_ENABLE_CONTINUOUS_TRANSCRIPTION =
            "android.speech.extra.ENABLE_CONTINUOUS_TRANSCRIPTION";
    private static final String EXTRA_ENABLE_OUTPUT_BUFFER_MODE =
            "android.speech.extra.ENABLE_OUTPUT_BUFFER_MODE";
    private static final String QUALCOMM_SPEECH_SERVICE_PACKAGE_NAME =
            "com.qualcomm.qti.voiceai.speech";
    private static final String EXTRA_ENABLE_DSP_ASR =
            "android.speech.extra.ENABLE_DSP_ASR";
    private final Context mContext;
    private SpeechRecognizer mSpeechRecognizer;

    public ASRManager(Context context) {
        mContext = context;
    }

    /**
     *
     * @param transcriptionLanguage IETF language tag (as defined by BCP 47), for example "en-US".
     */
    public void startASR(String transcriptionLanguage, ConversationMode mode,
                         RecognitionResultListener listener) {
        boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(mContext);
        Log.i(TAG, "startASR SpeechRecognizer recognitionAvailable: " + recognitionAvailable);
        if (!recognitionAvailable) {
            return;
        }
        if (mSpeechRecognizer == null) {
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentServices(
                    new Intent(RecognitionService.SERVICE_INTERFACE), 0);
            if (list.size() > 0) {
                for (ResolveInfo item : list) {
                    ServiceInfo serviceInfo = item.serviceInfo;
                    String serviceName = serviceInfo.name;
                    String packageName = serviceInfo.packageName;
                    if (QUALCOMM_SPEECH_SERVICE_PACKAGE_NAME.equals(packageName)) {
                        ComponentName componentName = new ComponentName(packageName, serviceName);
                        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext,
                                componentName);
                        break;
                    }
                }
                if (mSpeechRecognizer == null) {
                    return;
                }
            } else {
                return;
            }
        }
        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onError(int error) {
                listener.onError(error);
            }

            @Override
            public void onResults(Bundle results) {
                listener.onResults(results);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                    listener.onPartialResult(partialResults);
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, transcriptionLanguage);

        intent.putExtra(EXTRA_ENABLE_DSP_ASR, true);
        if (mode == ConversationMode.REAL_TIME) {
            intent.putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1000);
            intent.putExtra(EXTRA_ENABLE_CONTINUOUS_TRANSCRIPTION, true);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        } else if (mode == ConversationMode.OUTPUT_AT_END_OF_CONVERSATION) {
            intent.putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    5000);
            intent.putExtra(EXTRA_ENABLE_OUTPUT_BUFFER_MODE, true);
            intent.putExtra(EXTRA_ENABLE_CONTINUOUS_TRANSCRIPTION, true);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        }
        mSpeechRecognizer.startListening(intent);
    }

    public void stopASR() {
        Log.i(TAG, "stopASR");
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
        }
    }

    public void destroy() {
        Log.i(TAG, "destroy");
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
        mSpeechRecognizer = null;
    }
}
