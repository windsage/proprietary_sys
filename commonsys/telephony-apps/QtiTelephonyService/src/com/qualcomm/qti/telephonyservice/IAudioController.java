/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

/**
 * This interface abstracts Audio implementation from clients
 * It's used by HAL to set/get audio related parameters from Audio Subsystem/HAL.
 */
public interface IAudioController {
    /**
     * To set audio parameters and serve RIL set request via QcRilAudio AIDL/HIDL.
     *
     * @param keyValuePairs used to set audio related parameters.
     *
     * @return int error code indicating the audio server status.
     */
    int setParameters(String keyValuePairs);

    /**
     * To get audio parameters and serve RIL get request via QcRilAudio AIDL/HIDL.
     *
     * @param key used to query existing audio related parameters.
     *
     * @return String value of audio parameters.
     */
    String getParameters(String key);
}
