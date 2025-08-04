/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qcrilhook;

import vendor.qti.hardware.radio.qcrilhook.IQtiOemHookIndication;
import vendor.qti.hardware.radio.qcrilhook.IQtiOemHookResponse;

@VintfStability
interface IQtiOemHook {

    /**
     * Set callback for oemhook requests and oemhook indications
     *
     * @param responseCallback Object containing response callback functions
     * @param indicationCallback Object containing oem hook indication callback functions
     */
    oneway void setCallback(in IQtiOemHookResponse responseCallback,
            in IQtiOemHookIndication indicationCallback);

    /**
     * Send raw oemhook request
     *
     * @param serial number of requests. Responses must include the same serial number as requests
     * @param data oemhook data passed as raw bytes
     *
     */
    oneway void oemHookRawRequest(in int serial, in byte[] data);
}
