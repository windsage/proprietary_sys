/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.soter;

@VintfStability
parcelable SoterBufferReturn {
    byte[] data;
    /*
     * length of the data
     */
    int dataLength;
}
