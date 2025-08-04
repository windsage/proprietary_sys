/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.speech;

import android.content.Intent;
import android.speech.RecognitionService;

public interface IASRModule {

    void startListening(Intent intent, RecognitionService.Callback callback);

    void cancel(RecognitionService.Callback callback);

    void stopListening(RecognitionService.Callback callback);

    void checkRecognitionSupport(Intent recognizerIntent,
                                   RecognitionService.SupportCallback supportCallback);

}
