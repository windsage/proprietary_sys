/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wificfr;

import vendor.qti.hardware.wifi.wificfr.CaptureStatus;
import vendor.qti.hardware.wifi.wificfr.IWificfrDataCallback;

@VintfStability
interface IWificfr {
    /**
     * Start CSI capture on given interface.
     * @return CaptureStatus of the operation.
     */
    CaptureStatus csiCaptureStart();

    /**
     * Stop CSI capture on given interface.
     * @return CaptureStatus of the operation.
     */
    CaptureStatus csiCaptureStop();

    /**
     * Notifies csi capture data
     *
     * @param callback An instance of the IWifiCfrDataCallback HIDL interface
     *        object.
     * @return CaptureStatus of the operation.
     */
    CaptureStatus registerEventCallback(in IWificfrDataCallback callback);

    /**
     * Unregister IWifiCfrDataEventCallback callback if client is no more interested in
     * events.
     *
     * @param callback: Same instance of the IWificfrDataCallback HIDL interface object
     *                 passed during registration.
     * @return CaptureStatus of the operation.
     */
    CaptureStatus unregisterEventCallback(in IWificfrDataCallback callback);
}
