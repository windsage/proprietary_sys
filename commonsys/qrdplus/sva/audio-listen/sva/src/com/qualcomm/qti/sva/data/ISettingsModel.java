/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sva.data;

import android.content.Intent;

public interface ISettingsModel {
    String PREFIX_OF_SOUND_MODEL_PREFERENCES = "settings_";
    String NAME_OF_GLOBAL_PREFERENCES = "global_settings";

    int getGlobal1stKeyphraseConfidenceLevel(ISmModel.ModelVersion version);

    void setGlobal1stKeyphraseConfidenceLevel(ISmModel.ModelVersion version, int level);

    int getGlobal1stUserConfidenceLevel(ISmModel.ModelVersion version);

    void setGlobal1stUserConfidenceLevel(ISmModel.ModelVersion version, int level);

    int getGlobal2ndKeyphraseConfidenceLevel(ISmModel.ModelVersion version);

    void setGlobal2ndKeyphraseConfidenceLevel(ISmModel.ModelVersion version, int level);

    int getGlobal2ndUserConfidenceLevel(ISmModel.ModelVersion version);

    void setGlobal2ndUserConfidenceLevel(ISmModel.ModelVersion version, int level);

    int getGlobalGMMTrainingConfidenceLevel();

    void setGlobalGMMTrainingConfidenceLevel(int level);

    int getGlobalTrainingPath();

    void setGlobalTrainingPath(int path);

    String getUDK4BaseSoundModel();

    void setUDK4BaseSoundModel(String name);

    String getUDK7BaseSoundModel();

    void setUDK7BaseSoundModel(String name);

    int get1stKeyphraseConfidenceLevel();

    void set1stKeyphraseConfidenceLevel(int level);

    int get1stUserConfidenceLevel();

    void set1stUserConfidenceLevel(int level);

    int get2ndKeyphraseConfidenceLevel();

    void set2ndKeyphraseConfidenceLevel(int level);

    int get2ndUserConfidenceLevel();

    void set2ndUserConfidenceLevel(int level);

    boolean getGlobalDetectionToneEnabled();

    void setGlobalDetectionToneEnabled(boolean enabled);

    boolean getGlobalIsDisplayAdvancedDetails();

    void setGlobalIsDisplayAdvancedDetails(boolean bDisplay);

    boolean getUserVerificationEnabled();

    void setUserVerificationEnabled(boolean enabled);

    boolean getMultiKWThresholdEnabled();

    void setMultiKWThresholdEnabled(boolean enabled);

    String getMultiFirstKWConfidenceLevel();

    void setMultiFirstKWConfidenceLevel(String confidencelevel);

    boolean getVoiceRequestEnabled();

    void setVoiceRequestEnabled(boolean enabled);

    int getVoiceRequestLength();

    void setVoiceRequestLength(int len);

    boolean getOpaqueDataTransferEnabled();

    void setOpaqueDataTransferEnabled(boolean enabled);

    int getHistBufferTime();

    void setHistBufferTime(int len);

    int getPreRollDuration();

    void setPreRollDuration(int len);

    String getActionName();

    void setActionName(String actionName);

    Intent getActionIntent();

    void setActionIntent(Intent actionIntent);

    void setBaseSoundModel(String pdkName);

    String getBaseSoundModel();

    int getPDKEnrollmentRecordingTimes();

    void setPDKEnrollmentRecordingTimes(int recordingTimes);
}
