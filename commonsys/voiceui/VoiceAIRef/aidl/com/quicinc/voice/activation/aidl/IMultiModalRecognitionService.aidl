/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;
import com.quicinc.voice.activation.aidl.IResultCallback;

/*
 * Allows enable and disable Multi Modal Recognition.
 */
interface IMultiModalRecognitionService {

    /*
     * Enable Multi Modal Recognition.
     *
     * @param soundModelFileDescriptor object used to transfer sound model data.
     * @param params object used to transfer model information.
     * @param resultCallback object used to return the status and result.
     */
    oneway void enableRecognition(
        in ParcelFileDescriptor soundModelFileDescriptor,
        in Bundle params,
        IResultCallback resultCallback);

    /*
     * Disable Multi Modal Recognition.
     *
     * @param modelInfo object used to transfer model information.
     * @param resultCallback object used to return the status and result.
     */
    oneway void disableRecognition(
        in Bundle modelInfo, IResultCallback resultCallback);

    /*
     * Gets the status of Multi Modal Recognition.
     *
     * @param modelInfo object used to transfer the model information.
     *
     * @return True if Multi Modal Recognition is currently enabled.
     *         False otherwise.
     */
    boolean isRecognitionEnabled(in Bundle modelInfo);
}
