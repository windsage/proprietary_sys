/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
@Backing(type="int")
enum ListenModelEnum {
    kKeywordModel = 1,
    /*
     * Keyword model
     */
    kUserKeywordModel = 2,
    /*
     * Userkeyword model
     */
    kTargetSoundModel = 3,
    kMultiUserKeywordModel = 4,
    /*
     * Multiple Keyword models
     */
    kKeywordModelWithVop = 5,
    kSecondStageKeywordModel = 6,
    kSecondStageKeywordModelWithVop = 7,
}
