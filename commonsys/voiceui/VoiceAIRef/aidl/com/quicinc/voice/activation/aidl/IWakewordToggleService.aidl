/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;
import com.quicinc.voice.activation.aidl.IResultCallback;

/*
 * Allows enable and disable Wakeword Rcognition.
 */
interface IWakewordToggleService {

    /*
     * Enable Wakeword Rcognition.
     *
     * @param wakewordInfo object used to transfer Wakeword information.
     * @param soundModelFileDescriptor object used to transfer sound model data.
     * @param resultCallback object used to return the status and result.
     */
    oneway void enableWakewordRecognition(
        in Bundle wakewordInfo,
        in ParcelFileDescriptor soundModelFileDescriptor,
        IResultCallback resultCallback);

    /*
     * Disable Wakeword Rcognition.
     *
     * @param wakewordInfo object used to transfer Wakeword information.
     * @param resultCallback object used to return the status and result.
     */
    oneway void disableWakewordRecognition(
        in Bundle wakewordInfo, IResultCallback resultCallback);

    /*
     * Gets the status of Wakeword Rcognition.
     *
     * @param wakewordInfo object used to transfer the Wakeword information.
     *
     * @return True if Wakeword Rcognition is currently enabled.
     *         False otherwise.
     */
    boolean isWakewordRecognitionEnabled(in Bundle wakewordInfo);
}
