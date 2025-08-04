/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
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

package vendor.qti.hardware.ListenSoundModelAidl;
@Backing(type="int") @VintfStability
enum SmlConfigStructureId {
  SML_CONFIG_ID_VERSION,
  SML_CONFIG_ID_PARSING,
  SML_CONFIG_ID_ONLINE_VAD_INIT,
  SML_CONFIG_ID_ONLINE_VAD_REINIT,
  SML_CONFIG_ID_ONLINE_VAD_SET_PARAM,
  SML_CONFIG_ID_ONLINE_VAD_PROCESS,
  SML_CONFIG_ID_ONLINE_VAD_GET_RESULT,
  SML_CONFIG_ID_ONLINE_VAD_RELEASE,
  SML_CONFIG_ID_SET_PDK_PARAMS,
  SML_CONFIG_ID_SET_UDK_PARAMS,
  SML_CONFIG_ID_REQ_MEM_INFO,
  SML_CONFIG_ID_OTP,
  SML_CONFIG_ID_DEBUG,
  SML_CONFIG_ID_EAI_SCRATCH_MEM_INFO,
  SML_CONFIG_ID_GET_MODEL_SIZE,
  SML_CONFIG_ID_MODEL_SERIALIZE,
  SML_CONFIG_ID_RELEASE,
  SML_CONFIG_ID_COMPATIBILITY_CHECK,
}
