/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundModelFileNameUtils {

    private static final String SOUND_MODEL_FILE_SPLIT = "_";
    private static final int KEYWORD_INDEX = 0;
    private static final int LOCALE_INDEX = 1;
    private static final int SVA_VERSION_INDEX = 2;
    private static final int VERSION_CODE_INDEX = 2;
    private static final String LOCALE_SPLIT = "-";

    public static String parseSoundModelLocale(String fileName){
        String[] values = splitSoundModelFile(fileName);
        return values.length > LOCALE_INDEX ? values[LOCALE_INDEX] : "";
    }

    public static String parseSoundModelKeyword(String fileName){
        String[] values = splitSoundModelFile(fileName);
        return values[KEYWORD_INDEX];
    }

    public static String parseSoundModelIdentity(String fileName) {
        String[] values = splitSoundModelFile(fileName);
        return values[KEYWORD_INDEX] + SOUND_MODEL_FILE_SPLIT + values[LOCALE_INDEX]
                + SOUND_MODEL_FILE_SPLIT + values[SVA_VERSION_INDEX];
    }

    public static String parseOneVoiceModelIdentity(String fileName) {
        String[] values = splitSoundModelFile(fileName);
        return values[KEYWORD_INDEX] + SOUND_MODEL_FILE_SPLIT + values[LOCALE_INDEX];
    }

    public static boolean isOneVoiceModel(String fileName) {
        String[] split = fileName.split("\\.");
        if (split.length > 1) {
            String model = split[0];
            return model.substring(model.length() - 4).startsWith("_");
        }
        return false;
    }

    public static long parseSoundModelVersion(String fileName) {
        String[] values = splitSoundModelFile(fileName);
        if (values.length >= 3) {
            String version = values[VERSION_CODE_INDEX];
            if (isNumeric(version)) {
                return Long.parseLong(version);
            } else {
                return 0;
            }
        }
        return 0;
    }

    public static boolean isValid(String fileName){
        String[] values = splitSoundModelFile(fileName);
        return values.length > LOCALE_INDEX;
    }

    public static Locale getDisplayLocale(String localeStr){
        String[] values = localeStr.split(LOCALE_SPLIT);
        String language = values[0];
        if (values.length >= 2) {
            String country = values[1];
            return new Locale(language, country);
        } else {
            return new Locale(language);
        }
    }

    private static String[] splitSoundModelFile(String fileName){
        final String fileNameNoEx = FileUtils.getFileNameNoEx(fileName);
        return fileNameNoEx.split(SOUND_MODEL_FILE_SPLIT);
    }

    private static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }
}
