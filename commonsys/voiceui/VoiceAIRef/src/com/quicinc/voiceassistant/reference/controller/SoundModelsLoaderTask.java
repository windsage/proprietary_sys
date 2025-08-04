/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.controller;

import com.quicinc.voiceassistant.reference.ClientApplication;
import com.quicinc.voiceassistant.reference.data.SmModel;
import com.quicinc.voiceassistant.reference.util.FileUtils;
import com.quicinc.voiceassistant.reference.util.SoundModelFileNameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SoundModelsLoaderTask implements Runnable {
    private final LoaderCallback mCallback;
    private final String mExternalFilesDirPath;

    public interface LoaderCallback {
        void onSoundModelsLoaded(List<SmModel> smModels);
    }

    SoundModelsLoaderTask(LoaderCallback callback) {
        mCallback = callback;
        mExternalFilesDirPath = ClientApplication.getInstance().getAppFilesDir();
    }

    @Override
    public void run() {
        final Map<String, SmModel> smModelsMap = new HashMap<>();
        File[] files = FileUtils.listFiles(mExternalFilesDirPath,
                SmModel.SUFFIX_PRESET_SOUND_MODEL);
        List<File> fileList = Arrays.asList(files);
        fileList.sort(Comparator.comparing(File::getName));
        boolean isHasOneVoiceModel = false;
        for (File file : files) {
            String fileName = file.getName();
            if (SoundModelFileNameUtils.isValid(fileName)) {
                String keyword = SoundModelFileNameUtils.parseSoundModelKeyword(fileName);
                SmModel smModel = smModelsMap.get(keyword);
                if (smModel == null) {
                    smModel = new SmModel();
                    smModelsMap.put(keyword, smModel);
                    isHasOneVoiceModel = false;
                }
                if (SoundModelFileNameUtils.isOneVoiceModel(fileName)) {
                    smModel.addSoundModelFile(fileName);
                    isHasOneVoiceModel = true;
                } else {
                    if (!isHasOneVoiceModel) {
                        smModel.addSoundModelFile(fileName);
                    }
                }
            }
        }
        List<SmModel> smModels = new ArrayList<>(smModelsMap.values());
        for (SmModel smModel : smModels) {
            smModel.updateUsedPdkFileName();
        }
        SoundModelFilesManager.mMainHandler.post(() -> {
            if (mCallback != null) {
                mCallback.onSoundModelsLoaded(smModels);
            }
        });
    }
}
