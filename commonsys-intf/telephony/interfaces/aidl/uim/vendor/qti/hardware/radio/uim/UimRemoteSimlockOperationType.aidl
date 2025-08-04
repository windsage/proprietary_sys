/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

@VintfStability
@Backing(type="int")
enum UimRemoteSimlockOperationType {
    UIM_REMOTE_SIMLOCK_GENERATE_ENCRYPTED_KEY = 0,
    UIM_REMOTE_SIMLOCK_PROCESS_SIMLOCK_DATA = 1,
    UIM_REMOTE_SIMLOCK_GENERATE_HMAC = 2,
    UIM_REMOTE_SIMLOCK_GET_MAX_SUPPORTED_VERSION = 3,
    UIM_REMOTE_SIMLOCK_GET_STATUS = 4,
    UIM_REMOTE_SIMLOCK_GENERATE_BLOB_REQUEST = 5,
    UIM_REMOTE_SIMLOCK_UNLOCK_TIMER_START = 6,
    UIM_REMOTE_SIMLOCK_UNLOCK_TIMER_STOP = 7,
}
