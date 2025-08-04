/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

import vendor.qti.hardware.radio.uim.UimRemoteSimlockStatusType;

@VintfStability
parcelable UimRemoteSimlockStatus {
    UimRemoteSimlockStatusType status;
    int unlockTime;
}
