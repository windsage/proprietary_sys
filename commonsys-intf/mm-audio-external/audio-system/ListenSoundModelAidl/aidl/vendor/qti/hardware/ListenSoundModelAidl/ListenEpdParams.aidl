/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
parcelable ListenEpdParams {
    /*
     * state machine parameters
     */
    float minSnrOnset;
    /*
     * The minimum snr of frame that speech segment starts
     */
    float minSnrLeave;
    /*
     * The minimum snr of frame that speech segment ends
     */
    float snrFloor;
    /*
     * The minimum snr value assumed in the end point detector
     */
    float snrThresholds;
    /*
     * The minimum snr value of speech to verify
     */
    float forgettingFactorNoise;
    /*
     * The forgetting factor used for noise estimation
     */
    int numFrameTransientFrame;
    /*
     * the number of frames in the beginning that are used for noise estimate(valid only for online mode)
     */
    float minEnergyFrameRatio;
    /*
     * the number of frame are used for noise estimation = minenergyFrameRatio * #frames of input(valid only for batch mode)
     */
    float minNoiseEnergy;
    /*
     * post processing parameters
     */
    int numMinFramesInPhrase;
    /*
     * the minimum nubmer of samples for a speech phrase (targetted speech)
     */
    int numMinFramesInSpeech;
    /*
     * the minimum number of samples for a speech intereval
     */
    int numMaxFrameInSpeechGap;
    /*
     * the maximum allowable number of samples for a speech gap
     */
    int numFramesInHead;
    /*
     * the speech head
     */
    int numFramesInTail;
    /*
     * the speech tail
     */
    int preEmphasize;
    int numMaxFrames;
    int keywordThreshold;
    int[4] reserved;
}
