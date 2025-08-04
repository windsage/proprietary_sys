/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.utility;

import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.quicinc.voice.assist.sdk.VoiceAssist;
import com.quicinc.voice.assist.sdk.enrollment.Enrollment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * A public class which take charge of utterance receive or sound mode data transmission.
 */
public class DataUtils {
    private static final String TAG = DataUtils.class.getSimpleName();
    public static final String RECORDING_TYPE_NOISY_SUFFIX = "_noisy";

    /**
     * Gets a {@code ParcelFileDescriptor} from given sound model path.
     *
     * @param soundModelPath The path of sound model file to be opened.
     * @return a new ParcelFileDescriptor pointing to the given sound model if file exists,
     * otherwise return
     * <code>null</code>.
     */
    public static ParcelFileDescriptor openSoundModel(String soundModelPath) {
        if (TextUtils.isEmpty(soundModelPath)) return null;
        File soundModel = new File(soundModelPath);
        try {
            return ParcelFileDescriptor
                    .open(soundModel, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "sound model file not found, " + e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Read sound model data from an input stream which created on ParcelFileDescriptor and store it
     * to the given path.
     *
     * @param soundModelPath The path of file sound model while be stored to.
     * @param fileDescriptor The file descriptor which used to get access to the file in QVA.
     */
    public static void saveSoundModel(String soundModelPath, ParcelFileDescriptor fileDescriptor) {
        if (fileDescriptor == null || soundModelPath == null) {
            Log.e(TAG, "fileDescriptor or soundModelPath is null");
            return;
        }
        try {
            Path folder = Paths.get(soundModelPath).getParent();
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            Log.e(TAG,
                    "error occurred when create sound model path, " + e.getLocalizedMessage());
        }
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor)) {
            Files.copy(in, Paths.get(soundModelPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.e(TAG,
                    "error occurred when read and store sound model, " + e.getLocalizedMessage());
        } finally {
            closeFD(fileDescriptor);
        }
    }

    public static void closeFD(ParcelFileDescriptor fileDescriptor) {
        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG,
                        "Failed to close " + fileDescriptor + ": " + Log.getStackTraceString(e));
            }
        }
    }

    public static Path getEnrolledSoundModelPath(String soundModel, String userId) {
        if (!TextUtils.isEmpty(soundModel) && !TextUtils.isEmpty(userId)) {
            if (soundModel.endsWith(Constants.SUFFIX_TRAINED_SOUND_MODEL)) {
                return Paths.get(VoiceAssist.getInstance().getSMRootPath(),
                        Constants.UV_MODEL_PATH, userId, soundModel);
            } else if (soundModel.endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL)) {
                String enrolledModel = soundModel.substring(0, soundModel.lastIndexOf(".")) +
                        Constants.SUFFIX_TRAINED_SOUND_MODEL;
                return Paths.get(VoiceAssist.getInstance().getSMRootPath(),
                        Constants.UV_MODEL_PATH, userId, enrolledModel);
            }
        }
        return null;
    }

    public static Path getUtterancePath(String soundModel, String user, int recordingType) {
        if (!TextUtils.isEmpty(soundModel) && !TextUtils.isEmpty(user)) {
            int dotIndex = soundModel.lastIndexOf(".");
            String prettyName;
            if (recordingType == Enrollment.ENROLLMENT_RECORDING_TYPE_NOISY) {
                prettyName = soundModel.substring(0, dotIndex > 0 ? dotIndex : soundModel.length())
                                + RECORDING_TYPE_NOISY_SUFFIX;
            } else {
                prettyName = soundModel.substring(0, dotIndex > 0 ? dotIndex : soundModel.length());
            }
            return Paths.get(VoiceAssist.getInstance().getSMRootPath(),
                    Constants.UV_MODEL_UTTERANCE_PATH,
                    user, prettyName);
        }
        return null;
    }

    public static String[] getUtterances(Path utterancesFolder) {
        ArrayList<String> list = new ArrayList<>();
        File file = utterancesFolder.toFile();
        if (file != null && file.isDirectory() && file.exists()) {
            File[] files = file.listFiles();
            for (File item : files) {
                list.add(item.toString());
            }
        }
        return list.toArray(new String[]{});
    }

    public static void deleteUtterances(Path utterancePath) {
        if (utterancePath == null) return;
        try {
            Files.walkFileTree(utterancePath,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                                      IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.TERMINATE;
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
