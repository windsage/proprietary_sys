/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.bluetooth.xpanprovider;

@VintfStability
interface IXpanProviderCallback {

    /**
     * Used to receive XPAN Event from XPAN Provider Service.
     *
     * @param event Event data received from XPAN Provider Service.
                    Data to be added in little endian format.
     */
    oneway void xpanEventReceivedCb(in byte[] event);
}
