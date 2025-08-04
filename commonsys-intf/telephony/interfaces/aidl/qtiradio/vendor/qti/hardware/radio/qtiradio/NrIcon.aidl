/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NrIconType;

@VintfStability
@JavaDerive(equals = true, toString = true)
parcelable NrIcon {
    NrIconType type;    // One of the types defined in NrIconType.aidl
    int rxCount = -1;   // The number of receiving antennas (-1 if not provided)
}