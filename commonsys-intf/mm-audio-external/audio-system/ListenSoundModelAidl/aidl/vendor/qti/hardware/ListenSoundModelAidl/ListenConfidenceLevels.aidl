/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

/*
 * this should match the 'sensitivity' data structure input in VoiceWakeupParamType
 */
@VintfStability
parcelable ListenConfidenceLevels {
    byte size;
    /*
     * number of keyword plus activePair confidence levels set
     */
    byte[] confLevels;
}
