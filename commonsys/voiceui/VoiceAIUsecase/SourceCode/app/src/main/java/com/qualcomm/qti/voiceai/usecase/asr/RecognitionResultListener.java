/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.asr;

import android.os.Bundle;
import java.util.ArrayList;

public interface RecognitionResultListener{
    void onError(int errorCode);
    void onResults(Bundle results);
    void onPartialResult(Bundle partialResult);
}
