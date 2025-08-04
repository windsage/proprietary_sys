/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.qti.hardware.wifi.wifilearner;
@Backing(type="int") @VintfStability
enum ChannelSurveyInfoMask {
  SURVEY_HAS_NOISE = (1 << 0) /* 1 */,
  SURVEY_HAS_CHAN_TIME = (1 << 1) /* 2 */,
  SURVEY_HAS_CHAN_TIME_BUSY = (1 << 2) /* 4 */,
  SURVEY_HAS_CHAN_TIME_EXT_BUSY = (1 << 3) /* 8 */,
  SURVEY_HAS_CHAN_TIME_RX = (1 << 4) /* 16 */,
  SURVEY_HAS_CHAN_TIME_TX = (1 << 5) /* 32 */,
  SURVEY_HAS_CHAN_TIME_SCAN = (1 << 6) /* 64 */,
}
