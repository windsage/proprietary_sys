/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.voiceactivation;

import android.os.Bundle;

import com.quicinc.voice.assist.sdk.VoiceAssist;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.DataUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A public class which contains the basic information of a sound model.
 */
public class SoundModel {
    private final String mFileName;
    private final String mUserId;
    private final String mLocale;
    private Bundle mBundle;

    public SoundModel(String fileName, String userId, String locale) {
        mFileName = fileName;
        mUserId = userId;
        mLocale = locale;
    }

    public String getSoundModelPath() {
        if (mFileName.endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL)) {
            return Paths.get(VoiceAssist.getInstance().getSMRootPath(), mFileName).toString();
        } else {
            Path enrolled = DataUtils.getEnrolledSoundModelPath(mFileName, mUserId);
            return enrolled != null ? enrolled.toString() : null;
        }
    }

    public String getFileName() {
        return mFileName;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getLocale() {
        return mLocale;
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public void setBundle(Bundle bundle) {
        mBundle = bundle;
    }
}
