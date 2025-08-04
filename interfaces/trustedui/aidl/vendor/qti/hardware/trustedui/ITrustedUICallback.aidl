/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

/**
 * Callback interface for ITrustedUI HAL
 */
@VintfStability
interface ITrustedUICallback {
    /**
     * ITrustedUI HAL will call this callback method to notify the client of session completion on
     * success.
     */
    oneway void onComplete();

    /**
     * ITrustedUI HAL will call this method to notify the client in case of session error.
     */
    oneway void onError();
}
