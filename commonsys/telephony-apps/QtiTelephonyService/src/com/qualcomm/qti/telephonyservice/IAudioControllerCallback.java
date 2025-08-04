/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

/**
 * This interface is extended by QcRilAudio AIDL/HIDL classes and
 * serves AudioManager indications forwarding it to lower layers i.e. RIL.
 */
public interface IAudioControllerCallback {

    /**
     * To indicate when audio server is up or down to the lower layers.
     */
    void onAudioStatusChanged(int status);

    /**
     * To indicate when QtiTelephonyService is destroyed.
     */
    void onDispose();
}
