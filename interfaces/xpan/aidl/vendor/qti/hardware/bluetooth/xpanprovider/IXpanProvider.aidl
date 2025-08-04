/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.bluetooth.xpanprovider;

import vendor.qti.hardware.bluetooth.xpanprovider.IXpanProviderCallback;

@VintfStability
interface IXpanProvider {

    /**
     * Used to register XPan Events callbacks to be received from
     * XPan Provider Service.
     *
     * @param cb An instance of the |IXpanProviderCallback| AIDL
     *        interface object.
     */
    oneway void registerXpanCallbacks(in IXpanProviderCallback cb);

    /**
     * Used to send XPAN Commands to XPAN Provider Service.
     *
     * @param cmd Command data to be sent. Data to be added in little
     *            endian format.
     */
    oneway void sendXpanCommand(in byte[] cmd);
}
