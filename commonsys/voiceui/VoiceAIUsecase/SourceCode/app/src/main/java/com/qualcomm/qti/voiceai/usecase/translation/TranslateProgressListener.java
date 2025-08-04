/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.translation;

public interface TranslateProgressListener {
    void onDone(String translationId, String result);
    void onError(String translationId);
}
