/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.ListenSoundModelAidl;
@VintfStability
@Backing(type="int")
enum Status {
  UNKNOWN = (-1) /* -1 */,
  SUCCESS = 0,
  IO_ERROR,
  NO_SPACE,
  INVALID_FD,
}
