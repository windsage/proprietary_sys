/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;

@VintfStability
@Backing(type="int")
enum ListenStatusEnum {
    kSucess = 0,
    kFailed = 1,
    kBadParam,
    kKeywordNotFound,
    kUserNotFound,
    kUserKwPairNotActive,
    kSMVersionUnsupported,
    kUserDataForKwAlreadyPresent,
    kDuplicateKeyword,
    kDuplicateUserKeywordPair,
    kMaxKeywordsExceeded,
    kMaxUsersExceeded,
    kEventStructUnsupported,
    /*
     * payload contains event data that can not be processed, or mismatches SM version
     */
    kLastKeyword,
    kNoSignal,
    kLowSnr,
    kRecordingTooShort,
    kRecordingTooLong,
    kNeedRetrain,
    kUserUDKPairNotRemoved,
    kCannotCreateUserUDK,
    kOutputArrayTooSmall,
    kTooManyAbnormalUserScores,
    kWrongModel,
    kWrongModelAndIndicator,
    kDuplicateModel,
    kChoppedSample,
    kSecondStageKeywordNotFound,
    kClippedSample,
}
