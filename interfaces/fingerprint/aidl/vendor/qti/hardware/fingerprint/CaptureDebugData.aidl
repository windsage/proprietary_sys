/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

/**
 * @brief: Structure for debug data
 */
@VintfStability
parcelable CaptureDebugData {
    String key;
    byte[] data;
    int dataSize;
}
