/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.data;

import android.text.TextUtils;

import com.quicinc.voiceassistant.reference.util.FileUtils;
import com.quicinc.voiceassistant.reference.util.SoundModelFileNameUtils;
import com.quicinc.voice.assist.sdk.voiceactivation.SoundModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SmModel {
    public static final String SUFFIX_PRESET_SOUND_MODEL = ".uim";
    private final Map<String, String> mSoundModelFiles = new HashMap<>();
    private String mPdkFileName;

    public SmModel(){
    }

    public void updateUsedPdkFileName() {
        mPdkFileName = new ArrayList<>(mSoundModelFiles.values()).get(0);
    }

    public void addSoundModelFile(String soundModelFile) {
        String locale = SoundModelFileNameUtils.parseSoundModelLocale(soundModelFile);
        mSoundModelFiles.put(locale, soundModelFile);
    }

    public String getPDKSoundModelFile(){
        return mPdkFileName;
    }

    public SoundModel convertToSoundModel() {
        String soundModelFile = getSoundModelFileName();
        return new SoundModel(soundModelFile, null,
                FileUtils.getLocaleFromFileName(soundModelFile));
    }

    private String getSoundModelFileName() {
        if (!TextUtils.isEmpty(mPdkFileName)) {
            return getPDKSoundModelFile();
        } else {
            throw new IllegalArgumentException("Sound Model file doesn't exist");
        }
    }
}
