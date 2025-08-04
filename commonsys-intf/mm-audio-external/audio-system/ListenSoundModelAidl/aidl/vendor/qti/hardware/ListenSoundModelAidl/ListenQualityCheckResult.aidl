/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
parcelable ListenQualityCheckResult {
    boolean isEpdFilteredSegmentSet;
    boolean isLowSnrSet;
    float epdSnr;
    int epdStart;
    /*
     * with guard frames
     */
    int epdEnd;
    /*
     * with guard frames
     */
    int exactEpdStart;
    int exactEpdEnd;
    char keywordConfidenceLevel;
    float epdPeakLevel;
    float epdRmsLevel;
    int n_epdSamplesClipping;
    float percentageEpdSamplesClipping;
    int keywordStart;
    int keywordEnd;
}
