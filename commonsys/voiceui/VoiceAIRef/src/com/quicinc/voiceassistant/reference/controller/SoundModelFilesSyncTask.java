/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.controller;

import android.content.Context;
import android.content.res.AssetManager;

import com.quicinc.voiceassistant.reference.ClientApplication;
import com.quicinc.voiceassistant.reference.data.SmModel;
import com.quicinc.voiceassistant.reference.util.FileUtils;
import com.quicinc.voiceassistant.reference.util.LogUtils;
import com.quicinc.voiceassistant.reference.util.SoundModelFileNameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.quicinc.voiceassistant.reference.util.FileUtils.FILE_PATH_SPLIT;

class SoundModelFilesSyncTask implements Runnable{
    private static final String TAG = SoundModelFilesSyncTask.class.getSimpleName();
    private final Context mContext;
    private final FilesSyncCallback mCallback;

    public interface FilesSyncCallback {
        void onSoundModelsSyncCompleted(List<List<String>> upgradeLists);
    }

    SoundModelFilesSyncTask(Context context, FilesSyncCallback callback){
        mContext = context;
        mCallback = callback;
    }

    @Override
    public void run() {
        copyAssetsIfNotExists(mContext, ClientApplication.getInstance().getAppFilesDir());
        List<List<String>> upgradeLists = checkSoundModelsUpgrade(
                ClientApplication.getInstance().getAppFilesDir());
        SoundModelFilesManager.mMainHandler.post(() -> {
            if (mCallback != null) {
                mCallback.onSoundModelsSyncCompleted(upgradeLists);
            }
        });
    }

    private static void copyAssetsIfNotExists(Context context, String rootDir) {
        LogUtils.d(TAG, "copyAssetsIfNotExists: destDir = " + rootDir);
        if (null == rootDir) {
            LogUtils.e(TAG, "copyAssetsIfNotExists: invalid param");
            return;
        }

        FileUtils.createDirIfNotExists(rootDir);

        if (FileUtils.isExist(rootDir)) {
            copyAssets(context, rootDir);
        } else {
            LogUtils.e(TAG, "copyAssetsIfNotExists: create destDir failure");
        }
    }

    private static void copyAssets(Context context, String rootDir) {
        AssetManager assetManager = context.getAssets();
        String[] assetFiles = null;

        // Get asset files.
        try {
            assetFiles = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == assetFiles || 0 == assetFiles.length) {
            return;
        }

        //copy asset files
        try {
            for (String filename : assetFiles) {
                //filter only sound model files
                if (filename.endsWith(SmModel.SUFFIX_PRESET_SOUND_MODEL)
                        && SoundModelFileNameUtils.isValid(filename)) {
                    final String outputFilePath = rootDir + FILE_PATH_SPLIT + filename;
                    LogUtils.d(TAG, "copyAssets: outputFile = " + outputFilePath);
                    if (new File(outputFilePath).exists()) {
                        LogUtils.d(TAG, "copyAssets: exists yet, ignore");
                        continue;
                    }
                    copySingleAssetFile(context, filename, outputFilePath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copySingleAssetFile(Context context, String filename,
                                     String outputFilePath) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream in = assetManager.open(filename);
        OutputStream out = new FileOutputStream(outputFilePath);
        byte[] buffer = new byte[1024];
        int readCount;
        while ((readCount = in.read(buffer)) != -1) {
            out.write(buffer, 0, readCount);
        }
        in.close();
        out.flush();
        out.close();
    }

    private List<List<String>> checkSoundModelsUpgrade(String rootDir) {
        Map<String, List<String>> map = new HashMap<>();
        File[] files = FileUtils.listFiles(rootDir, SmModel.SUFFIX_PRESET_SOUND_MODEL);
        for (File file : files) {
            String fileName = file.getName();
            LogUtils.e(TAG, "checkSoundModelsUpgrade fileName = "+fileName);
            String identity;
            if (SoundModelFileNameUtils.isValid(fileName)) {
                if (SoundModelFileNameUtils.isOneVoiceModel(fileName)) {
                    identity = SoundModelFileNameUtils.parseOneVoiceModelIdentity(fileName);
                } else {
                    identity = SoundModelFileNameUtils.parseSoundModelIdentity(fileName);
                }
                List<String> soundModelFiles = map.get(identity);
                if (soundModelFiles == null) {
                    soundModelFiles = new ArrayList<>();
                    map.put(identity, soundModelFiles);
                }
                if (!soundModelFiles.contains(fileName)) {
                    soundModelFiles.add(fileName);
                }
            }
        }

        List<List<String>> lists = new ArrayList<>(map.values());
        List<List<String>> upgradeLists = new ArrayList<>();
        for (List<String> list : lists) {
            LogUtils.d(TAG, "list  = " + list);
            if (list.size() > 1) {
                upgradeLists.add(list);
            }
        }
        return upgradeLists;
    }
}
