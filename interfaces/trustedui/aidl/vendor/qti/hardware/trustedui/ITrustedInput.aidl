/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.trustedui;

import vendor.qti.hardware.trustedui.ITrustedInputCallback;
import vendor.qti.hardware.trustedui.TUIResponse;
import vendor.qti.hardware.trustedui.TUIOutputID;

/**
 * TrustedInput HAL
 *
 * Interface to control a TrustedInput Device to get user input event when a secure session is in
 * progress. These input events are further notified to the trusted Execute Environment(EE).
 * The trusted input "data" is, however, not read through this interface.
 * ITrustedUI HAL expects an instance of this interface, to control the selected input device.
 */
@VintfStability
interface ITrustedInput {
    /**
     * Queue an asynchronous request for an input event. The event will be notified via the
     * ITrustedInputCallback registered with a successful call to init(..).
     *
     * @note This method is expected to return immediately without blocking.
     *
     * @param timeout Timeout in ms. A request form the caller of this method to terminate the
     *                wait on input at timeout.
     *
     * @return TUIResponse 0 on success
     *                     EBUSY if an input request is pending.
     *                     or any other error.
     */
    TUIResponse getInput(in int timeout);

    /**
     * Initialize a TrustedInput device in secure mode.
     * @note The implementation of this method should ensure that the non-secure access to the
     * device is removed and only the trusted EE has the right to access.
     *
     * @param cb Callback to notify the client of input event with optional payload
     *
     * @param displayType Display type that is going to used by the TUI session.
     *
     * @param out trustedInputID TrustedInput device identifier.
     *
     * @return TUIResponse 0 on success.
     *                     EBUSY if the device is already initialized.
     *                     EIO   in case of errors related to power management.
     *                     or any other error.
     */
    TUIResponse init(in ITrustedInputCallback cb, in String displayType,
        out TUIOutputID outParam);

    /**
     * Deinitialize TrustedInput device.
     * @note The implementation of this method should restore the non-secure access to the device.
     *
     * @return TUIResponse 0 on success.
     *                     ENODEV if device is not enabled.
     *                     or any other error.
     */
    TUIResponse terminate();
}
