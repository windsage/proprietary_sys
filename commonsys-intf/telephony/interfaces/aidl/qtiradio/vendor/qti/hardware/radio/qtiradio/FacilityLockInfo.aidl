/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;


@VintfStability
parcelable FacilityLockInfo {
    String facility;
    String password;
    int serviceClass;
    String appId;
    boolean expectMore;
}

