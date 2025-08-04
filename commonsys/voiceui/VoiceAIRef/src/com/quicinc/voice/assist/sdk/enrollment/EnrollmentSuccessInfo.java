/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

/**
 * A public class which contains the basic information when enrolled success.
 */
public class EnrollmentSuccessInfo {
    private String mSoundModelPath;
    private int mQualityScore;

    EnrollmentSuccessInfo(String soundModelPath, int qualityScore) {
        mSoundModelPath = soundModelPath;
        mQualityScore = qualityScore;
    }

    /**
     * Gets the quality score of the sound model generated in this enrollment.
     * @return The quality score of the sound model.
     */
    public int getQualityScore() {
        return mQualityScore;
    }

    /**
     * Gets the full path of the sound model generated in this enrollment.
     * @return The full path of the sound model.
     */
    public String getSoundModelPath() {
        return mSoundModelPath;
    }
}
