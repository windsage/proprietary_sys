/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import vendor.qti.hardware.ListenSoundModelAidl.ListenDetectionStatusEnum;

@VintfStability
parcelable ListenEventPayload {
    ListenDetectionStatusEnum status;
    /*
     * SUCCESS or FAILURE
     */
    byte[] data;
    /*
     * block of memory containing data payload
     */
    int size;
}
