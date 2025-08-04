/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

import java.util.List;

@VintfStability
parcelable ListenSoundModelHeader {
    char numKeywords;
    /*
     * total number of keywords
     */
    char numUsers;
    /*
     * total number of users
     */
    char numActiveUserKeywordPairs;
    /*
     * total number of active user+keyword pairs in SM
     */
    boolean isStripped;
    /*
     * if corresponding keyword is stripped or not
     */
    char[] langPerKw;
    /*
     * Language code of each keyword
     */
    char[] numUsersSetPerKw;
    /*
     * number active Users per keyword - included as convenience
     */
    boolean[] isUserDefinedKeyword;
    /*
     * Ordered 'truth' table of all possible pairs of users for each keyword.
     * Active entries marked with 1, inactive 0.keywordPhrase
     * 16-bit short (rather than boolean) is used to match SM model data size
     */
    List <String> userKeywordPairFlags;
    char modelIndicator;
}
