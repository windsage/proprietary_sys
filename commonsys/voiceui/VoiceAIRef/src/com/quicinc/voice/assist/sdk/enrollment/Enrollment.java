/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

import android.text.TextUtils;

import com.quicinc.voice.assist.sdk.VoiceAssist;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.DataUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A public class which contains parameters an user voice enrollment required.
 */
public class Enrollment {
    /**
     * Indicates the recording for the enrollment is clean.
     */
    public static final int ENROLLMENT_RECORDING_TYPE_CLEAN = 0;
    /**
     * Indicates the recording for the enrollment is noisy.
     */
    public static final int ENROLLMENT_RECORDING_TYPE_NOISY = 1;

    private static final String GENERAL_UV_FOLDER = "general_uv";

    public enum EnrollmentType {
        /**
         * Indicates it is a PDK enrollment, PDK+UV model will be generated
         */
        PDK(0),
        /**
         * Indicates it is a platform enrollment, TI+UV model will be generated
         */
        TI(1);

        private final int mValue;

        EnrollmentType(int value) {
            mValue = value;
        }

        public int value() {
            return mValue;
        }
    }
    private final int mEnrollmentType;
    private String mBaseSoundModelFileName;
    private String mSoundModelName;
    private String mUserId;
    private String mLocale;
    private int mRecordingType; // 0:clean; 1:noisy

    private int mCleanUtterances;

    /**
     * Constructs a Enrollment object with the sound model file name.
     *
     * @param baseSoundModel Which sound model this enrollment based on.
     * @param userId Which user will enroll in this sound model.
     * @param locale Which locale will this sound model been used in.
     */
    public Enrollment(String baseSoundModel, String userId, String locale) {
        this(baseSoundModel, userId, locale, ENROLLMENT_RECORDING_TYPE_CLEAN);
    }

    /**
     * Constructs a Enrollment object with the sound model file name.
     *
     * @param baseSoundModel Which sound model this enrollment based on.
     * @param userId Which user will enroll in this sound model.
     * @param locale Which locale will this sound model been used in.
     * @param recordingType recording type, 0 for clean and 1 for noisy.
     */
    public Enrollment(String baseSoundModel, String userId, String locale, int recordingType) {
        this(EnrollmentType.PDK, baseSoundModel, userId, locale, recordingType);
    }

    /**
     * Constructs a Enrollment object with the sound model file name.
     *
     * @param enrollmentType Which type this enrollment based on.
     * @param baseSoundModel Which sound model this enrollment based on, for TI can be null.
     * @param userId Which user will enroll in this sound model, for TI can be null.
     * @param locale Which locale will this sound model been used in, for TI can be null.
     * @param recordingType recording type, 0 for clean and 1 for noisy.
     */
    public Enrollment(EnrollmentType enrollmentType, String baseSoundModel, String userId,
                      String locale, int recordingType) {
        mEnrollmentType = enrollmentType.value();
        if (mEnrollmentType == EnrollmentType.TI.value()) {
            mBaseSoundModelFileName = GENERAL_UV_FOLDER;
        } else {
            mBaseSoundModelFileName = baseSoundModel;
        }
        mSoundModelName = getEnrolledSoundModelName(baseSoundModel);
        mUserId = userId;
        mLocale = locale;
        mRecordingType = recordingType;
    }

    /**
     * Gets the Enrollment type.
     *
     * @return The enrollment type.
     */
    int getEnrollmentType() {
        return mEnrollmentType;
    }

    /**
     * Gets the file name of base sound model for PDK+UV enrollment.
     *
     * @return The base sound model name.
     */
    String getBaseSoundModelFileName() {
        return mBaseSoundModelFileName;
    }

    /**
     * Gets the folder path which to save the utterance audio.
     *
     * @return The utterance path.
     */
    String getUtterancePath() {
        Path utterance = DataUtils.getUtterancePath(mBaseSoundModelFileName, mUserId,
                mRecordingType);
        return utterance == null ? null : utterance.toString();
    }

    /**
     * Gets the base sound model file path.
     *
     * @return The sound model path.
     */
    String getBaseSoundModelPath() {
        return Paths.get(VoiceAssist.getInstance().getSMRootPath(),
                mBaseSoundModelFileName).toString();
    }

    /**
     * Gets the file path which to save the target sound model.
     *
     * @return The sound model path.
     */
    String getSoundModelPath() {
        Path enrolled = DataUtils.getEnrolledSoundModelPath(mSoundModelName, mUserId);
        return enrolled != null ? enrolled.toString() : null;
    }

    /**
     * Gets the recording type for the target sound model.
     *
     * @return recording type.
     */
    int getRecordingType() {
        return mRecordingType;
    }

    String getUserId() {
        return mUserId;
    }

    String getLocale() {
        return mLocale;
    }

    private String getEnrolledSoundModelName(String soundModelName) {
        if (!TextUtils.isEmpty(soundModelName)
                && soundModelName.endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL)) {
            return getPrettyName(soundModelName) + Constants.SUFFIX_TRAINED_SOUND_MODEL;
        }
        return null;
    }

    private String getPrettyName(String soundModelName) {
        if (!TextUtils.isEmpty(soundModelName)
                && soundModelName.endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL)) {
            int index = soundModelName.lastIndexOf(".");
            if (index > 0)
                return soundModelName.substring(0, index);
        }
        return soundModelName;
    }

    public int getCleanUtterances() {
        return mCleanUtterances;
    }

    public void setCleanUtterances(int cleanUtterances) {
        mCleanUtterances = cleanUtterances;
    }
}
