/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

/**
 * Data structure used to store information related to clir.
 * Telephony/lower layers will process ClirInfo if ClirInfo#paramM
 * and ClirInfo#paranN are not set to Integer max.
 * Values are defined in spec 3GPP TS 27.007.
 */

@VintfStability
parcelable ClirInfo {
    /*
    * <n>: integer type (parameter sets the adjustment for outgoing calls)
    * 0 presentation indicator is used according to the subscription of the CLIR service
    * 1 CLIR invocation
    * 2 CLIR suppression
    */
    int paramM = -1;

    /*<m>: integer type (parameter shows the subscriber CLIR service status in the network)
    * 0 CLIR not provisioned
    * 1 CLIR provisioned in permanent mode
    * 2 unknown (e.g. no network, etc.)
    * 3 CLIR temporary mode presentation restricted
    * 4 CLIR temporary mode presentation allowed
    */
    int paramN = -1;
}