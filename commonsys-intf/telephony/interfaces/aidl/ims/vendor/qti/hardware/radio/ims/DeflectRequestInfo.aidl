/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
parcelable DeflectRequestInfo {
    int connIndex;
    /*
     * Connection id to be deflected
     */
    String number;
}

