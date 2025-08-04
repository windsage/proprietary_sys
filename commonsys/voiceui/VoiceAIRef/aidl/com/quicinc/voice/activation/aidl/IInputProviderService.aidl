/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.quicinc.voice.activation.aidl;

import android.os.Bundle;
import com.quicinc.voice.activation.aidl.IInputReceiverCallback;
import com.quicinc.voice.activation.aidl.IResultCallback;

interface IInputProviderService {
    oneway void registerClient(in Bundle params, IResultCallback resultCallback);

    oneway void unregisterClient(in Bundle params);

    oneway void registerInputReceiverCallback(IInputReceiverCallback iInputReceiverCallback);

    oneway void onResponseReceived(in Bundle params);

    oneway void getParams(in Bundle params, IResultCallback resultCallback);
    oneway void setParams(in Bundle params, IResultCallback resultCallback);
    oneway void startRecording(in Bundle params, IInputReceiverCallback inputReceiverCallback);
    oneway void stopRecording(in Bundle params);
}