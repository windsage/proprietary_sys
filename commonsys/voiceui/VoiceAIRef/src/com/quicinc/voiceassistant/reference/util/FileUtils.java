/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.util;

import java.io.File;

public class FileUtils {
    public static final String FILE_PATH_SPLIT = "/";
    private static final String TAG = FileUtils.class.getSimpleName();
    private static final String DEFAULT_LOCALE = "en-US";

    public static boolean isExist(String path) {
        if (null == path) {
            LogUtils.e(TAG, "isExist: invalid input param");
            return false;
        }
        File target = new File(path);
        return target.exists();
    }

    public static File[] listFiles(String directoryPath, final String suffix){
        File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            return directory.listFiles(pathname -> pathname.getName().endsWith(suffix));
        } else {
            throw new IllegalArgumentException("invalid directory parameter");
        }
    }

    public static void createDirIfNotExists(String dir) {
        File file = new File(dir);
        if (!file.exists() && !file.isDirectory()) {
            if (!file.mkdirs()) {
                LogUtils.e(TAG, "fails to create dir = " + dir);
            }
        }
    }

    public static void deleteFile(File file) {
        try {
            if (file.isDirectory()){
                File[] files = file.listFiles();
                for (File child : files) {
                    deleteFile(child);
                }
                if (!file.delete()) {
                    LogUtils.e(TAG, "Fails to delete directory = " + file.getPath());
                }
            } else {
                if (!file.delete()) {
                    LogUtils.e(TAG, "Fails to delete file = " + file.getPath());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static String getLocaleFromFileName(String fileName) {
        String[] s = fileName.split("_");
        if (s.length >= 2) {
            return s[1];
        }
        return DEFAULT_LOCALE;
    }
}
