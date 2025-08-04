/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
parcelable ListenDetectionEventV1 {
    String keyword;
    char keywordConfidenceLevel;
    char userConfidenceLevel;
}
