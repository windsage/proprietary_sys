/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

/**
 * TrustedInputCallback
 *
 * Callback interface for asynchronous input event notifications.
 */
@VintfStability
interface ITrustedInputCallback {
    /**
     * This method notifies the caller of secure input event from the TrustedInput device with an
     * optional event payload.
     *
     * @param  inputData optional input payload
     */
    oneway void notifyInput(in byte[] inputData);

    /**
     * This method notifies the caller of timeout error on input, in case a timeout has been set by
     * the caller in @getInput method @ITrustedInput.hal.
     */
    oneway void notifyTimeout();
}
