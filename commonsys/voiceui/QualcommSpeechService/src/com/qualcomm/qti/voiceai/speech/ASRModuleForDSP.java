/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.speech;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.speech.RecognitionService;
import android.speech.RecognitionSupport;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vendor.qti.hardware.qasr.IQasr;
import vendor.qti.hardware.qasr.IQasrCallback;
import vendor.qti.hardware.qasr.IQasrGlobalCallback;
import vendor.qti.hardware.qasr.QasrConfig;
import vendor.qti.hardware.qasr.QasrEvent;
import vendor.qti.hardware.qasr.QasrModel;
import vendor.qti.hardware.qasr.QasrParameter;

public final class ASRModuleForDSP implements IASRModule {

    private static final String TAG = ASRModuleForDSP.class.getSimpleName();
    private static final String AIDL_SERVICE_NAME = "vendor.qti.hardware.qasr.IQasr/default";
    private static final String VENDOR_UUID = "018ebfb8-1364-7417-b92e-f6ab16b55431";
    private static final String EXTRA_ENABLE_TRANSLATE = "android.speech.extra.ENABLE_TRANSLATE";
    private static final String EXTRA_ENABLE_CONTINUOUS_TRANSCRIPTION =
            "android.speech.extra.ENABLE_CONTINUOUS_TRANSCRIPTION";
    private static final String EXTRA_ENABLE_OUTPUT_BUFFER_MODE =
            "android.speech.extra.ENABLE_OUTPUT_BUFFER_MODE";
    private static final boolean DEBUG = true;
    /**
     * same with LanguageCode which defined in QasrConfig.aidl
     */
    private static final List<String> mSupportedLanguages =
            Arrays.asList("EN_US",  "ZH_CN",  "GR_DE",  "ES_US",  "RS_RU",  "KO_KR",  "FR_FC",
                          "JA_JP",  "PT_PU",  "TR_TK",  "PO_PL",  "CA_CT",  "DU_DT",  "AR_AB",
                          "SW_SD",  "IT_IA",  "IN_ID",  "IN_HI",  "FI_FN",  "VI_VT",  "HE_HB",
                          "UK_UR",  "GR_GK",  "MA_ML",  "CZ_CH",  "RO_RM",  "DA_DN",  "HU_HG",
                          "IN_TM",  "NO_NW",  "TH_TA",  "IN_UR",  "CR_CT",  "BL_BG",  "LI_LT",
                          "LA_LT",  "MA_MO",  "IN_ML",  "WL_CY",  "SO_SK",  "IN_TG",  "PE_PR",
                          "LA_LV",  "IN_BN",  "SE_SR",  "AZ_AB",  "SL_SV",  "IN_KD",  "ES_ET",
                          "MC_MD",  "BR_TN",  "BS_BQ",  "IS_ID",  "AR_AM",  "NP_NA",  "MN_MG",
                          "BS_BN",  "KZ_KH",  "AL_AN",  "SW_SH",  "GA_GL",  "IN_MR",  "IN_PB",
                          "SI_SH",  "KM_KH",  "SH_SO",  "YO_RB",  "SO_SM",  "AF_AK",  "OC_TA",
                          "GE_GA",  "BE_LA",  "TA_JI",  "IN_SD",  "IN_GJ",  "AH_MR",  "YI_DH",
                          "LA_LO",  "UZ_BK",  "FA_RS",  "HT_CL",  "PK_PT",  "TK_MN",  "NY_SK",
                          "ML_TS",  "IN_SK",  "LX_BG",  "MY_MR",  "TB_TA",  "TA_LG",  "ML_GS",
                          "IN_AS",  "TA_TR",  "HW_WN",  "LN_GL",  "HA_US",  "BS_KR",  "JV_SE",
                          "SD_NS");
    private static IQasr mService;
    private static int mSessionHandle = -1;
    private static final ArrayList<String> mTranscriptionResults = new ArrayList<>();
    private static RecognitionService.Callback mRecognitionServiceCallback;
    private boolean mContinuousTranscription;
    private boolean mPartialTranscription;
    private boolean mOutputBufferMode;
    private boolean mIsForceGetResult;
    private static final String EXTRA_IS_FINAL = "is_final";
    private static final QasrProxyDeathRecipient mProxyDeathRecipient
            = new QasrProxyDeathRecipient();

    public ASRModuleForDSP() {
        if (isHalServiceDeclared()) {
            connectHalService();
        }
    }

    private static void connectHalService() {
        Log.i(TAG, "Connecting to default IQasr service");
        try {
            IBinder binder = ServiceManager.waitForService(AIDL_SERVICE_NAME);
            mService = IQasr.Stub.asInterface(binder);
            binder.linkToDeath(mProxyDeathRecipient, 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to IQasr AIDL service!");
        }
        Log.i(TAG, "Connected to IQasr service");
    }

    private static class QasrProxyDeathRecipient implements IBinder.DeathRecipient {

        @Override
        public void binderDied() {
            Log.i(TAG, "IQasr service died, connect to new one");
            try {
                if(mRecognitionServiceCallback != null) {
                    mRecognitionServiceCallback.error(SpeechRecognizer.ERROR_SERVER);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Client occur exception!");
            }
            mSessionHandle = -1;
            mTranscriptionResults.clear();
            mService = null;
            connectHalService();
        }
    }

    private boolean isHalServiceDeclared() {
        return ServiceManager.isDeclared(AIDL_SERVICE_NAME);
    }

    @Override
    public void startListening(Intent intent, RecognitionService.Callback callback) {
        initSession();
        if (mSessionHandle >= 0) {
            QasrConfig config = new QasrConfig();
            String language = intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE);
            if (!TextUtils.isEmpty(language)) {
                int languageCodeIndex = mSupportedLanguages.indexOf(language);
                if (languageCodeIndex != -1) {
                    config.input_language_code = (byte) languageCodeIndex;
                    config.output_language_code = (byte) languageCodeIndex;
                } else {
                    Log.e(TAG, "Language " + language + " not supported currently!");
                    errorCallback(callback, SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED);
                    deinitSession();
                    return;
                }
            }

            boolean languageDetect = intent.getBooleanExtra(
                    RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, false);
            config.enable_language_detection = languageDetect;

            boolean translation = intent.getBooleanExtra(EXTRA_ENABLE_TRANSLATE, false);
            config.enable_translation = translation;

            mContinuousTranscription =
                    intent.getBooleanExtra(EXTRA_ENABLE_CONTINUOUS_TRANSCRIPTION, false);
            config.enable_continuous_mode = mContinuousTranscription;

            mPartialTranscription =
                    intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            config.enable_partial_transcription = mPartialTranscription;

            int threshold = 50;
            config.threshold = threshold;

            int timeout_duration = 0;
            config.timeout_duration = timeout_duration;

            int timeout = intent.getIntExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    0);
            config.silence_detection_duration = timeout;

            mOutputBufferMode =
                    intent.getBooleanExtra(EXTRA_ENABLE_OUTPUT_BUFFER_MODE, false);
            config.outputBufferMode = mOutputBufferMode;

            if(mOutputBufferMode) {
                config.enable_continuous_mode = true;
                config.enable_partial_transcription = false;
            }

            byte[] data = new byte[0];
            config.data = data;

            if (DEBUG) {
                Log.d(TAG, "startListening language: " + language
                        + ", languageDetect: " + languageDetect + ", translation: " + translation
                        + ", continuousTranscription: " + config.enable_continuous_mode
                        + ", partialTranscription: " + config.enable_partial_transcription
                        + ", timeout: " + timeout + ", outputBufferMode: " + mOutputBufferMode);
            }
            try {
                mIsForceGetResult = false;
                mService.startListening(mSessionHandle, config);
                mRecognitionServiceCallback = callback;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "call hal service startListening occur exception!");
                errorCallback(callback, SpeechRecognizer.ERROR_SERVER);
                deinitSession();
            }
        } else {
            Log.e(TAG, "Hal service init session fail!");
            errorCallback(callback, SpeechRecognizer.ERROR_SERVER);
        }
    }

    private void errorCallback(RecognitionService.Callback callback, int errorCode) {
        try {
            callback.error(errorCode);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Client occur exception!");
        }
    }

    @Override
    public void cancel(RecognitionService.Callback callback) {
        if (callback != null) {
            stopListening(callback);
        }
    }

    @Override
    public void stopListening(RecognitionService.Callback callback) {
        if (mService != null && mSessionHandle >= 0) {
            if (mOutputBufferMode) {
                mIsForceGetResult = true;
                QasrParameter parameter = new QasrParameter();
                parameter.data = new byte[0];
                parameter.param = QasrParameter.Parameter.FORCE_OUTPUT_EVENT;
                try {
                    mService.setParameter(mSessionHandle, parameter);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "call hal service setParameter occur exception!");
                }
            } else {
                try {
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                            mTranscriptionResults);
                    mRecognitionServiceCallback.results(bundle);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Callback results occur exception!");
                }
                releaseSession();
            }
        } else {
            Log.e(TAG, "Service is null or session not init successful!");
            errorCallback(callback, SpeechRecognizer.ERROR_SERVER);
        }
    }

    private synchronized void releaseSession() {
        if (mService != null && mSessionHandle >= 0) {
            try {
                mService.stopListening(mSessionHandle);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "call hal service stopSession occur exception!");
            }
            try {
                mService.releaseSession(mSessionHandle);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "call hal service releaseSession occur exception!");
            }
            mRecognitionServiceCallback = null;
            mSessionHandle = -1;
            mTranscriptionResults.clear();
        }
    }

    private synchronized void deinitSession() {
        if (mService != null && mSessionHandle >= 0) {
            try {
                mService.releaseSession(mSessionHandle);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "call hal service releaseSession occur exception!");
            }
            mRecognitionServiceCallback = null;
            mSessionHandle = -1;
            mTranscriptionResults.clear();
        }
    }


    @Override
    public void checkRecognitionSupport(Intent recognizerIntent,
                                        RecognitionService.SupportCallback supportCallback) {
        if (supportCallback == null) {
            return;
        }
        RecognitionSupport.Builder builder = new RecognitionSupport.Builder();
        builder.setInstalledOnDeviceLanguages(mSupportedLanguages);
        ArrayList<String> emptyList = new ArrayList<>();
        builder.setPendingOnDeviceLanguages(emptyList);
        builder.setSupportedOnDeviceLanguages(emptyList);
        builder.setOnlineLanguages(emptyList);
        RecognitionSupport support = builder.build();
        supportCallback.onSupportResult(support);
    }

    private synchronized void initSession() {
        mSessionHandle = -1;
        mTranscriptionResults.clear();
        if (mService == null) {
            return;
        }
        try {
            mService.registerGlobalCallback(mGlobalCallback);
            QasrModel model = new QasrModel();
            model.vendorUuid = VENDOR_UUID;
            model.data = null;
            model.dataSize = 0;
            int result = mService.initSession(model, mASRCallback);
            if (result >= 0) {
                mSessionHandle = result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final IQasrGlobalCallback mGlobalCallback = new GlobalCallback();

    private class GlobalCallback extends IQasrGlobalCallback.Stub {

        @Override
        public void onResourcesAvailable() {
        }

        @Override
        public String getInterfaceHash() {
            return this.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return this.VERSION;
        }
    }

    private final IQasrCallback mASRCallback = new QAsrCallback();

    private class QAsrCallback extends IQasrCallback.Stub {

        @Override
        public void eventCallback(int handle, QasrEvent event) {
            try {
                if (handle == mSessionHandle && mRecognitionServiceCallback != null) {
                    QasrEvent.Event[] events = event.event;
                    Bundle bundle = new Bundle();
                    if (event.status != QasrEvent.EventStatus.SUCCESS) {
                        handleAbort(events, bundle);
                    } else {
                        if (events != null && events.length > 0) {
                            if (mOutputBufferMode) {
                                handleBufferMode(events, bundle);
                            } else {
                                handleNonBufferMode(events[0], bundle);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Client occur exception when handle callback!");
            }
        }

        @Override
        public String getInterfaceHash() {
            return this.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return this.VERSION;
        }
    }

    private void handleAbort(QasrEvent.Event[] events, Bundle bundle) throws RemoteException {
        if (events != null) {
            for (QasrEvent.Event item : events) {
                mTranscriptionResults.add(item.text);
                if (DEBUG) {
                    Log.d(TAG, "Speech Recognition abort asr result: " + item.text);
                }
            }
        }
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, mTranscriptionResults);
        mRecognitionServiceCallback.results(bundle);
        releaseSession();
    }

    private void handleBufferMode(QasrEvent.Event[] events, Bundle bundle) throws RemoteException {
        for (QasrEvent.Event item : events) {
            String suppressNonSpeech = new String(item.text);
            suppressNonSpeech = suppressNonSpeech.replaceAll("\\(.*\\)", "");
            suppressNonSpeech = suppressNonSpeech.replaceAll("\\[.*\\]", "");
            if(TextUtils.isEmpty(suppressNonSpeech) || TextUtils.isEmpty(suppressNonSpeech.trim())) {
                Log.d(TAG,"EMPTY STRING");
            } else {
                mTranscriptionResults.add(suppressNonSpeech);
            }
            if (DEBUG) {
                Log.d(TAG, "Speech Recognition success asr result: " + item.text);
            }
        }
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, mTranscriptionResults);
        if (mIsForceGetResult) {
            mIsForceGetResult = false;
            mRecognitionServiceCallback.results(bundle);
            releaseSession();
        } else {
            mRecognitionServiceCallback.partialResults(bundle);
            mTranscriptionResults.clear();
        }
    }

    private void handleNonBufferMode(QasrEvent.Event item, Bundle bundle)
            throws RemoteException {
        if (item == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Speech Recognition success"
                    + (mContinuousTranscription ? " continuous" : " non-continuous")
                    + (mPartialTranscription ? " and partial mode" : " and non-partial mode")
                    + (item.is_final ? " final asr result: " : " non-final asr result: ")
                    + item.text);
        }
        int text_size = -1;
        String suppressNonSpeech = new String(item.text);
        if(TextUtils.isEmpty(suppressNonSpeech)) {
            suppressNonSpeech = "";
            text_size = 0;
        } else {
            suppressNonSpeech = suppressNonSpeech.replaceAll("\\(.*\\)", "");
            suppressNonSpeech = suppressNonSpeech.replaceAll("\\[.*\\]", "");
            text_size = suppressNonSpeech.length();
            if(TextUtils.isEmpty(suppressNonSpeech) || TextUtils.isEmpty(suppressNonSpeech.trim())) {
                Log.d(TAG,"EMPTY STRING");
                suppressNonSpeech = "";
                text_size = 0;
            }
        }

        if (mContinuousTranscription) {
            if(text_size == 0) {
                return;
            }
            if (mPartialTranscription) {
                if (item.is_final) {
                    mTranscriptionResults.add(suppressNonSpeech);
                    bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                            mTranscriptionResults);
                    bundle.putBoolean(EXTRA_IS_FINAL, true);
                } else {
                    ArrayList<String> tmp = new ArrayList<>(mTranscriptionResults);
                    tmp.add(suppressNonSpeech);
                    bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, tmp);
                    bundle.putBoolean(EXTRA_IS_FINAL, false);
                }
                mRecognitionServiceCallback.partialResults(bundle);
            } else {
                if (item.is_final) {
                    mTranscriptionResults.add(suppressNonSpeech);
                    bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                            mTranscriptionResults);
                    bundle.putBoolean(EXTRA_IS_FINAL, true);
                    mRecognitionServiceCallback.partialResults(bundle);
                } else {
                    if (DEBUG) {
                       Log.d(TAG, "continuous and non-partial mode receive partial result, drop!");
                    }
                }
            }
        } else {
            if (mPartialTranscription) {
                if (item.is_final) {
                    if(text_size == 0) {
                        mRecognitionServiceCallback.error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                    }else {
                        mTranscriptionResults.add(suppressNonSpeech);
                        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                                mTranscriptionResults);
                        mRecognitionServiceCallback.results(bundle);
                    }
                    releaseSession();
                } else {
                    if(text_size != 0) {
                        ArrayList<String> tmp = new ArrayList<>(mTranscriptionResults);
                        tmp.add(suppressNonSpeech);
                        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, tmp);
                        mRecognitionServiceCallback.partialResults(bundle);
                    }
                }
            } else {
                if (item.is_final) {
                    if (text_size == 0) {
                        mRecognitionServiceCallback.error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                    } else {
                        mTranscriptionResults.add(suppressNonSpeech);
                        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                                mTranscriptionResults);
                        mRecognitionServiceCallback.results(bundle);
                    }
                    releaseSession();
                } else {
                    if (DEBUG) {
                       Log.d(TAG, "non-continuous and non-partial mode receive partial result, drop!");
                    }
                }
             }
        }
    }

}
