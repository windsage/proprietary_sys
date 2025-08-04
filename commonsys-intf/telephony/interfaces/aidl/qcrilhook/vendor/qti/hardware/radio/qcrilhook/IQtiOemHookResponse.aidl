/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qcrilhook;

import vendor.qti.hardware.radio.qcrilhook.RadioError;

@VintfStability
interface IQtiOemHookResponse {

    /**
     * Send oemhook raw response
     *
     * @param serial number of request/response. Responses must include the same
     *        serial number as requests
     * @param error returned by radio
     * @param data returned as raw bytes
     *
     */
    oneway void oemHookRawResponse(in int serial, in RadioError error, in byte[] data);
}
