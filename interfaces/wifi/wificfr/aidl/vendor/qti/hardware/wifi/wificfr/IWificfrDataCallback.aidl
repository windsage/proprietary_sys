/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wificfr;

@VintfStability
interface IWificfrDataCallback {
    /**
     * Called when Cfr capture data is available
     *
     * @param Cfr capture information
     */
    oneway void onCfrDataAvailable(in byte[] captureInfo);
}
