/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
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

package vendor.qti.hardware.radio.ims;
@Backing(type="int") @VintfStability
enum ConfigItem {
  INVALID = 0,
  NONE = 1,
  VOCODER_AMRMODESET = 2,
  VOCODER_AMRWBMODESET = 3,
  SIP_SESSION_TIMER = 4,
  MIN_SESSION_EXPIRY = 5,
  CANCELLATION_TIMER = 6,
  T_DELAY = 7,
  SILENT_REDIAL_ENABLE = 8,
  SIP_T1_TIMER = 9,
  SIP_T2_TIMER = 10,
  SIP_TF_TIMER = 11,
  VLT_SETTING_ENABLED = 12,
  LVC_SETTING_ENABLED = 13,
  DOMAIN_NAME = 14,
  SMS_FORMAT = 15,
  SMS_OVER_IP = 16,
  PUBLISH_TIMER = 17,
  PUBLISH_TIMER_EXTENDED = 18,
  CAPABILITIES_CACHE_EXPIRATION = 19,
  AVAILABILITY_CACHE_EXPIRATION = 20,
  CAPABILITIES_POLL_INTERVAL = 21,
  SOURCE_THROTTLE_PUBLISH = 22,
  MAX_NUM_ENTRIES_IN_RCL = 23,
  CAPAB_POLL_LIST_SUB_EXP = 24,
  GZIP_FLAG = 25,
  EAB_SETTING_ENABLED = 26,
  MOBILE_DATA_ENABLED = 27,
  VOICE_OVER_WIFI_ENABLED = 28,
  VOICE_OVER_WIFI_ROAMING = 29,
  VOICE_OVER_WIFI_MODE = 30,
  CAPABILITY_DISCOVERY_ENABLED = 31,
  EMERGENCY_CALL_TIMER = 32,
  SSAC_HYSTERESIS_TIMER = 33,
  VOLTE_USER_OPT_IN_STATUS = 34,
  LBO_PCSCF_ADDRESS = 35,
  KEEP_ALIVE_ENABLED = 36,
  REGISTRATION_RETRY_BASE_TIME_SEC = 37,
  REGISTRATION_RETRY_MAX_TIME_SEC = 38,
  SPEECH_START_PORT = 39,
  SPEECH_END_PORT = 40,
  SIP_INVITE_REQ_RETX_INTERVAL_MSEC = 41,
  SIP_INVITE_RSP_WAIT_TIME_MSEC = 42,
  SIP_INVITE_RSP_RETX_WAIT_TIME_MSEC = 43,
  SIP_NON_INVITE_REQ_RETX_INTERVAL_MSEC = 44,
  SIP_NON_INVITE_TXN_TIMEOUT_TIMER_MSEC = 45,
  SIP_INVITE_RSP_RETX_INTERVAL_MSEC = 46,
  SIP_ACK_RECEIPT_WAIT_TIME_MSEC = 47,
  SIP_ACK_RETX_WAIT_TIME_MSEC = 48,
  SIP_NON_INVITE_REQ_RETX_WAIT_TIME_MSEC = 49,
  SIP_NON_INVITE_RSP_RETX_WAIT_TIME_MSEC = 50,
  AMR_WB_OCTET_ALIGNED_PT = 51,
  AMR_WB_BANDWIDTH_EFFICIENT_PT = 52,
  AMR_OCTET_ALIGNED_PT = 53,
  AMR_BANDWIDTH_EFFICIENT_PT = 54,
  DTMF_WB_PT = 55,
  DTMF_NB_PT = 56,
  AMR_DEFAULT_MODE = 57,
  SMS_PSI = 58,
  VIDEO_QUALITY = 59,
  THRESHOLD_LTE1 = 60,
  THRESHOLD_LTE2 = 61,
  THRESHOLD_LTE3 = 62,
  THRESHOLD_1x = 63,
  THRESHOLD_WIFI_A = 64,
  THRESHOLD_WIFI_B = 65,
  T_EPDG_LTE = 66,
  T_EPDG_WIFI = 67,
  T_EPDG_1x = 68,
  VWF_SETTING_ENABLED = 69,
  VCE_SETTING_ENABLED = 70,
  RTT_SETTING_ENABLED = 71,
  SMS_APP = 72,
  VVM_APP = 73,
  VOICE_OVER_WIFI_ROAMING_MODE = 74,
  SET_AUTO_REJECT_CALL_MODE_CONFIG = 75,
  VOWIFI_FQDN = 76,
  MMTEL_CALL_COMPOSER_CONFIG = 77,
  VOWIFI_ENTITLEMENT_ID = 78,
}
