/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;

/*
 * Callback object used to return values and communicate success or error states.
 */
interface IResultCallback {

    /*
     * Called when an operation was performed successfully.
     *
     * @param returnValues If applies, contains the result of the operation
     *                     that was successfully performed.
     */
    void onSuccess(in Bundle returnValues);

    /*
     * Called when an operation was not performed successfully.
     *
     * @param params Parameters needed for the invocation of this method.
     */
    void onFailure(in Bundle params);
}
