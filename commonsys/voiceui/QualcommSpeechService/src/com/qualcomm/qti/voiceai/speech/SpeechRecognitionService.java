/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.speech;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

public class SpeechRecognitionService extends RecognitionService {

    private static final String EXTRA_ENABLE_DSP_ASR = "android.speech.extra.ENABLE_DSP_ASR";
    private IASRModule mAsrModule;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback listener) {
        if(!checkPermission()) {
            return;
        }
        if (listener != null) {
            boolean enableDSPASR =
                    recognizerIntent.getBooleanExtra(EXTRA_ENABLE_DSP_ASR, true);
            if (enableDSPASR) {
                if (mAsrModule == null) {
                    mAsrModule = new ASRModuleForDSP();
                }
            }

            if (mAsrModule != null) {
                mAsrModule.startListening(recognizerIntent, listener);
            }
        }
    }

    @Override
    protected void onCancel(Callback listener) {
        if (listener != null) {
            if (mAsrModule != null) {
                mAsrModule.cancel(listener);
            } else {
                try {
                    listener.error(SpeechRecognizer.ERROR_CLIENT);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onStopListening(Callback listener) {
        if (listener != null) {
            if (mAsrModule != null) {
                mAsrModule.stopListening(listener);
            } else {
                try {
                    listener.error(SpeechRecognizer.ERROR_CLIENT);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCheckRecognitionSupport(Intent recognizerIntent,
                                          SupportCallback supportCallback) {
        boolean enableDSPASR =
                recognizerIntent.getBooleanExtra(EXTRA_ENABLE_DSP_ASR, true);
        if (enableDSPASR) {
            if (mAsrModule == null) {
                mAsrModule = new ASRModuleForDSP();
            }
        }
        if (mAsrModule != null) {
            mAsrModule.checkRecognitionSupport(recognizerIntent, supportCallback);
        }
    }

    private boolean checkPermission() {
        if(checkSelfPermission(
                PermissionActivity.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return false;
        }
        return true;
    }
}
