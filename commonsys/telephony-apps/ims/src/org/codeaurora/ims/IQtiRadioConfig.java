/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.os.RemoteException;

public interface IQtiRadioConfig {
    /**
     * Check if Modem/RIL supports a particular feature.
     *
     * @param int feature is the telephony side integer mapping for a particular feature.
     *
     * @return boolean true if feature is supported by RIL/modem, false otherwise.
     */
    public boolean isFeatureSupported(int feature) throws RemoteException;
}
