/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wificfr;

import vendor.qti.hardware.wifi.wificfr.CaptureStatusCode;

/**
 * Return Cfr capture status csi start/stop is called.
 */
@VintfStability
parcelable CaptureStatus {

/* CSI capture status code corresponding to csi start/stop */
    CaptureStatusCode code;

/* CSI capture debugMessage corresponding to csi start/stop */
    String debugMessage;
}
