/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.EctType;

/**
 * Data structure to store explicit call transfer related information.
 * Lower layer will process ExplicitCallTransferInfo based on individual default parameters.
 */
@VintfStability
parcelable ExplicitCallTransferInfo {
    /*
     * Active call id to be transferred; Mandatory parameter
     */
    int callId;
    /*
     * Explicit Call Transfer type; Mandatory parameter
     */
    EctType ectType = EctType.INVALID;
    /*
     * Transfer Target address; Mandatory for Blind and Assured transfer
     */
    String targetAddress;
    int targetCallId;
}

