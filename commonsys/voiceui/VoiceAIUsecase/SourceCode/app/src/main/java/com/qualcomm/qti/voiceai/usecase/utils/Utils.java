/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.utils;

import android.util.Log;

import java.io.File;

public final class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static final String[] translateLanguageNames =
            {"English","Chinese","Korean","Spanish","Japanese","French","German","Italian","Hindi"};
    public static final String[] transcribeLanguageNames =
            {"English","Chinese","Korean","Spanish","Japanese","Hindi"};

    public static void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File item : files) {
                deleteFile(item);
            }
        }
        boolean delete = file.delete();
        if (!delete) {
            Log.i(TAG, "delete file fail.");
        }
    }
}
