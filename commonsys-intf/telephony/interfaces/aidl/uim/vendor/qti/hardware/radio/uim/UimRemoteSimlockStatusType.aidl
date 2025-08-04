/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

@VintfStability
@Backing(type="int")
enum UimRemoteSimlockStatusType {
    UIM_REMOTE_SIMLOCK_STATE_LOCKED = 0,
    UIM_REMOTE_SIMLOCK_STATE_TEMPERORY_UNLOCK = 1,
    UIM_REMOTE_SIMLOCK_STATE_PERMANENT_UNLOCK = 2,
}
