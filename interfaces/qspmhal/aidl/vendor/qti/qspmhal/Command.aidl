/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.qspmhal;

@VintfStability
@Backing(type="int")
enum Command {
    DELETE_ALL_PROFILES = 0,
    UPDATE_PROFILE = 1,
    DELETE_PROFILE = 2,
}
