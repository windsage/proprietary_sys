/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.activation.aidl;

/*
 * Client receives callback of TTS speaking progress
 *
 */
interface ISpeakProgressCallback {

    /*
     * Called when an utterance "starts" as perceived by the caller
     *
     * @param textId the identifier of the text
     */
    oneway void onStart(String textId);

    /*
     * Called when an utterance has been stopped while in progress
     * or flushed from the synthesis queue.
     *
     * @param textId the identifier of the text
     */
    oneway void onStop(String textId);

    /*
     * Called when an utterance has successfully completed processing
     *
     * @param textId the identifier of the text
     */
    oneway void onComplete(String textId);

    /*
     * Called when an error has occurred during processing
     *
     * @param textId the identifier of the text
     * @param errorCode the error code if error occurs in progress
     */
    oneway void onError(String textId, int errorCode);
}