/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

/**
 * This class contains extra information for each training, currently is empty.
 * <p>
 * This class exists to avoid API modifications in case a new extra information needs to be back to
 * client.
 */
public class EnrollmentExtras {
    private int mSNRScore;
    private int mConfidenceLevel;

    /**
     * Constructs a EnrollmentExtras object.
     */
    public EnrollmentExtras(int snr, int confidence) {
        mSNRScore = snr;
        mConfidenceLevel = confidence;
    }

    /**
     * Gets the SNR score of this utterance training.
     * @return The SNR score of this utterance training.
     */
    public int getSNRScore() {
        return mSNRScore;
    }

    /**
     * Gets the confidence level of this utterance training.
     * @return The confidence level of this utterance training.
     */
    public int getConfidenceLevel() {
        return mConfidenceLevel;
    }
}
