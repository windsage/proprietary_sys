/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;
import com.quicinc.voice.activation.aidl.IResultCallback;

/*
 * Allows setting and getting Wakeword Specific parameters through Bundles.
 */
interface IWakewordSettingsService {

    /*
     * Changes the Wakeword specified parameters in settingsToChange.
     *
     * @param settingsToChange object used to transfer the settings.
     * @param resultCallback object used to return the status and result.
     */
    oneway void setParams(in Bundle settingsToChange, IResultCallback resultCallback);

    /*
     * Gets the result corresponding the request.
     *
     * @param request object used to transfer the request.
     * @param resultCallback object used to return the status and result.
     */
    oneway void getParams(in Bundle request, IResultCallback resultCallback);
}
