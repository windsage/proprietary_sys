/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase.translation;

import java.util.ArrayList;

public enum TranslatorSupportedLanguage {

    ENGLISH("en"),
    CHINESE("zh"),
    KOREAN("ko"),
    SPANISH("es"),
    JAPANESE("ja"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    HINDI("hi");

    private final String mLanguage;
    TranslatorSupportedLanguage(final String language) {
        mLanguage = language;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public static ArrayList<String> getSupportedLanguages() {
        TranslatorSupportedLanguage[] values = values();
        ArrayList<String> supportedLanguages = new ArrayList<>();
        for (TranslatorSupportedLanguage item : values) {
            supportedLanguages.add(item.getLanguage());
        }
        return supportedLanguages;
    }

    public static TranslatorSupportedLanguage convert(String languageStr) {
        for (TranslatorSupportedLanguage language : values()) {
            if (language.name().equalsIgnoreCase(languageStr)) {
                return language;
            }
        }
        return ENGLISH;
    }

    public static TranslatorSupportedLanguage convertLanguage(String languageStr) {
        for (TranslatorSupportedLanguage language : values()) {
            if (language.getLanguage().equals(languageStr)) {
                return language;
            }
        }
        return ENGLISH;
    }
}
