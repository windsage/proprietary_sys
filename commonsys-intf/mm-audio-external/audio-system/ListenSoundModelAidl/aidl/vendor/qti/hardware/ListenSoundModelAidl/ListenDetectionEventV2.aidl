/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import vendor.qti.hardware.ListenSoundModelAidl.ListenConfidenceLevels;

@VintfStability
parcelable ListenDetectionEventV2 {
    String keywordPhrase;
    /*
     * string containing phrase string of keyword with highest confidence score
     */
    String userName;
    /*
     * string containing name of user with highest confidence score
     */
    byte highestKeywordConfidenceLevel;
    byte highestUserConfidenceLevel;
    ListenConfidenceLevels pairConfidenceLevels;
}
