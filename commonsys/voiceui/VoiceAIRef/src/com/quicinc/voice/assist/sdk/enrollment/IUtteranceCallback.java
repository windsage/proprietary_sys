/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;


/**
 * This interface is used to receive events from QVA when training is started.
 */
public interface IUtteranceCallback {

    void onStartRecording();


    void onStopRecording();

    void onSuccess(String utteranceId, EnrollmentExtras enrollmentExtras);

    void onFailure(String utteranceId, EnrollmentExtras enrollmentExtras, String reason);

    void onFeedback(int volume);
}
