/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;

/*
 * Callback used to provide updates for the voice enrollment UI
 */
interface IUtteranceCallback {

    /*
     * Called to indicate that the QVA started recording an utterance.
     *
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void onStartRecording(in Bundle params);

    /*
     * Called to indicate that the QVA has stopped recording.
     *
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void onStopRecording(in Bundle params);

    /*
     * Called to indicate that the QVA has started processing the audio of an utterance.
     *
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to thsis method.
     */
    oneway void onStartProcessing(in Bundle params);

    /*
     * Called to indicate that the QVA has stopped processing the audio of an utterance.
     *
     * @param params Parameters containers. This field exists to avoid AIDL modifications in case
     *               a new parameter needs to be added to this method.
     */
    oneway void onStopProcessing(in Bundle params);

    /*
     * Called to indicate that the QVA has successfully collected an utterance for enrollment.
     *
     * @param params Parameters needed for the invocation of this method.
     */
    oneway void onSuccess(in Bundle params);

    /*
     * Called to indicate that the QVA has rejected an utterance for enrollment.
     *
     * @param params Parameters needed for the invocation of this method.
     */
    oneway void onError(in Bundle params);

    /*
     * This method allows the QVA to send audio feedback on an utterance being captured.
     * This feedback can be used to display to the user a visual interpretation of his voice
     * while it's being recorded.
     *
     * @param params Parameters needed for the invocation of this method.
     */
    oneway void onFeedback(in Bundle params);
}
