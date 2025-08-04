/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

/**
 * Data structure containing call forward details.
 * Lower layers will process CallForwardInfo when
 * CallForwardInfo.reason and CallForwardInfo.serviceclass is
 * not INT32_MAX .
 */
@VintfStability
parcelable CallForwardInfo {
    /*
    * Values are as defined in TS 27.007 section 7.11 (+CCFC) and 7.4 (CLCK)
    */
    int status;
    int reason;
    int serviceClass;
    int toa;
    String number;
    int timeSeconds;
    boolean expectMore;
}

