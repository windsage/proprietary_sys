/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import vendor.qti.hardware.ListenSoundModelAidl.ListenModelEnum;

@VintfStability
parcelable ListenSoundModelInfo {
    ListenModelEnum type;
    /*
     * model type: Keyword, User, TargetSound
     */
    int version;
    /*
     * model version
     */
    int size;
}
