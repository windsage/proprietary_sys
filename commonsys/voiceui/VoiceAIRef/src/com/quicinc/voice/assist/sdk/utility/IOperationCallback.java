/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.utility;

public interface IOperationCallback<T, V> {
    void onSuccess(T param);
    void onFailure(V param);
}
