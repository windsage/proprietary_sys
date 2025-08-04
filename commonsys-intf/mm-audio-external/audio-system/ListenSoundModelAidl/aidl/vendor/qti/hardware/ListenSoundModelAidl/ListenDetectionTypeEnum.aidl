/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
@Backing(type="int")
enum ListenDetectionTypeEnum {
    kSingleKWDetectionEvent = 1,
    /*
     * SVA 1.0 model
     */
    kMultiKWDetectionEvent = 2,
}
