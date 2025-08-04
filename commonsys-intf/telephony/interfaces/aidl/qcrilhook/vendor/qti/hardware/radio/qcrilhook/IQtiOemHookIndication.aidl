/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qcrilhook;

@VintfStability
interface IQtiOemHookIndication {

    /*
     * Send oemhook raw indication
     *
     * @param data returned as raw bytes
     *
     */
    oneway void oemHookRawIndication(in byte[] data);
}
