/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.dspasr.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

public class ASRResultSaver {
    private static final String asrRootPath = "asrresult";
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static File createASRReusltFile(Context context) throws IOException {
        File internal = context.getFilesDir();
        Path asr = Paths.get(internal.getAbsolutePath(),
                asrRootPath, DATE_FORMAT.format(new Date()) + ".txt");
        if (!asr.toFile().exists()) {
            asr.getParent().toFile().mkdirs();
            asr.toFile().createNewFile();
        }
        return asr.toFile();
    }

    public static void save(Context context, String asrResult) {
        try {
            File asr = createASRReusltFile(context);
            Executors.newSingleThreadExecutor().submit(new SaveAsrResultRunnable(
                    asr, asrResult.getBytes()
            ));
        } catch (IOException e) {
            Log.e("ASRResultSaver", "save asr result error", e);
        }
    }

    static class SaveAsrResultRunnable implements Runnable {
        private FileOutputStream mFileOutputStream;
        private File mOutFile;
        private byte[] mLogData;
        SaveAsrResultRunnable(File outFile, byte[] logData) {
            mOutFile = outFile;
            mLogData = logData;
        }
        @Override
        public void run() {
            if (mOutFile == null) return;
            try {
                if (!mOutFile.exists()) mOutFile.createNewFile();
                mFileOutputStream = new FileOutputStream(mOutFile, true);
                mFileOutputStream.write(mLogData);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mOutFile = null;
                mLogData = null;
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close();
                        mFileOutputStream = null;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
