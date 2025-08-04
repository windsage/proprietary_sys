/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

@VintfStability
oneway interface IQfpExtendedCancellationSignal {
    /**
     * Cancels an Asynchronous request
     */
    void cancel();
}
