/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************/

#include <hardware/bluetooth.h>
#include <hardware/bt_apm.h>
#include "bt_trace.h"
#include "btif_acm_source.h"
#include "btif_ahim.h"
#include "btm_api.h"
#include "btm_int.h"
#include "btif_acm.h"
#include "btif_hf.h"
#include "osi/include/thread.h"
#include "btif/include/btif_vmcp.h"
#include "btif_bap_broadcast.h"

#define HCI_VSQC_CONTROLLER_A2DP_OPCODE 0x000A
#define VS_QHCI_USECASE_UPDATE 0x15

extern thread_t* get_worker_thread();

typedef struct {
  tA2DP_CTRL_CMD cmd;
  uint8_t direction;
}tLEA_CTRL_CMD;

#if AHIM_ENABLED

/* Context Types */
enum class LeAudioContextType : uint16_t {
  UNINITIALIZED = 0x0000,
  UNSPECIFIED = 0x0001,
  CONVERSATIONAL = 0x0002,
  MEDIA = 0x0004,
  GAME = 0x0008,
  INSTRUCTIONAL = 0x0010,
  VOICEASSISTANTS = 0x0020,
  LIVE = 0x0040,
  SOUNDEFFECTS = 0x0080,
  NOTIFICATIONS = 0x0100,
  RINGTONE = 0x0200,
  ALERTS = 0x0400,
  EMERGENCYALARM = 0x0800,
  RFU = 0x1000,
};

uint16_t LeAudioSrcContextToIntContent(LeAudioContextType context_type);
uint16_t LeAudioSnkContextToIntContent(LeAudioContextType context_type);
void btif_acm_source_profile_on_suspended(tA2DP_CTRL_ACK status,
                                           uint16_t sub_profile);

LeAudioContextType current_context_type_ = LeAudioContextType::MEDIA;
uint16_t current_profile_type_ = BAP;

LeAudioContextType current_snk_context_type_ = LeAudioContextType::LIVE;
LeAudioContextType current_src_context_type_ = LeAudioContextType::MEDIA;
LeAudioContextType default_context_type_ = LeAudioContextType::MEDIA;

extern bt_status_t set_active_acm_initiator(const RawAddress& peer_address,
                                             uint16_t profileType);
extern bt_status_t set_active_acm_initiator_internal(const RawAddress& peer_address,
                                              uint16_t profileType, bool ui_req);
void process_update_params(uint16_t delay, uint8_t mode);

extern bool btif_acm_check_in_call_tracker_timer_exist();

extern bool is_active_dev_streaming();
extern uint16_t btif_acm_get_previous_active_media_profile();
extern RawAddress btif_acm_initiator_music_active_peer(void);
extern RawAddress btif_acm_initiator_voice_active_peer(void);
bool reconfig_acm_initiator(const RawAddress& peer_address,
                                         uint16_t profileType, bool ui_req);
void btif_acm_aar4_vbc(const RawAddress& address, bool status);
bool getAAR4Status(const RawAddress& address);
RawAddress btif_acm_get_active_dev();
std::mutex acm_mutex_;
bool src_metadata_pending = false;
bool snk_metadata_pending = false;
bool is_set_active_pending = false;

extern std::mutex acm_set_active_wait_mutex_;
void vsc_cmd_callback(tBTM_VSC_CMPL *p_data) {
  uint8_t mode, *stream;
  uint16_t delay;
  if (p_data && (stream = (uint8_t*)p_data->p_param_buf) &&
      (p_data->opcode == HCI_VSQC_CONTROLLER_A2DP_OPCODE)) {
    STREAM_TO_UINT16(delay, stream);
    STREAM_TO_UINT8(mode, stream);
    process_update_params(delay, mode);
  }
}

void btif_acm_send_vs_cmd(const RawAddress& bd_addr, uint16_t content_type) {
  uint8_t param[4] = {0};
  tACL_CONN* p_acl = btm_bda_to_acl(bd_addr, BT_TRANSPORT_LE);
  if (p_acl == NULL) {
    BTIF_TRACE_ERROR("%s: acl not connected", __func__);
    return;
  }
  param[0] = VS_QHCI_USECASE_UPDATE;
  param[1] = (p_acl->hci_handle) & 0x00FF;
  param[2] = ((p_acl->hci_handle) & 0xFF00) >> 8;
  param[3] = (uint8_t)content_type;
  BTM_VendorSpecificCommand(HCI_VSQC_CONTROLLER_A2DP_OPCODE, 4, param, vsc_cmd_callback);
}

void btif_acm_process_request(tLEA_CTRL_CMD lea_cmd) {
  tA2DP_CTRL_ACK status = A2DP_CTRL_ACK_FAILURE;
  // update pending command
  btif_ahim_update_pending_command(lea_cmd.cmd, AUDIO_GROUP_MGR);

  BTIF_TRACE_IMP("%s: cmd %u", __func__, lea_cmd.cmd);
  BTIF_TRACE_IMP("%s: direction %u", __func__, lea_cmd.direction);
  BTIF_TRACE_IMP("%s: current_snk_context_type_ %d", __func__, current_snk_context_type_);
  BTIF_TRACE_IMP("%s: current_src_context_type_ %d", __func__, current_src_context_type_);
  switch (lea_cmd.cmd) {
    case A2DP_CTRL_CMD_START: {
      if (btif_ahim_is_aosp_aidl_hal_enabled()) {
          // ACM is in right state
          status = A2DP_CTRL_ACK_PENDING;
          if(lea_cmd.direction == FROM_AIR) {
            if(current_src_context_type_ == LeAudioContextType::GAME &&
               current_snk_context_type_ == LeAudioContextType::UNINITIALIZED) {
              current_snk_context_type_ = LeAudioContextType::LIVE;
            }
            if(current_snk_context_type_ == LeAudioContextType::LIVE) {
              // findout active peer, convert current_context_type_ to profileType
              uint16_t content_type = LeAudioSnkContextToIntContent(current_snk_context_type_);
              // push for reconfiguration to acm layer
              if (current_src_context_type_ == LeAudioContextType::GAME) {
                set_active_acm_initiator_internal(btif_acm_get_active_dev(), GCP_RX, false);
              } else {
                char wmcp_tx_rx[PROPERTY_VALUE_MAX] = "false";
                property_get("persist.vendor.btstack.wmcp_tx_rx", wmcp_tx_rx, "false");
                LOG(INFO) << __func__ << ": wmcp_tx_rx: " << wmcp_tx_rx;
                if (!strncmp("true", wmcp_tx_rx, 4)) {
                  set_active_acm_initiator_internal(btif_acm_get_active_dev(), WMCP_TX, false);
                } else if (current_src_context_type_ == LeAudioContextType::MEDIA) {
                  set_active_acm_initiator_internal(btif_acm_get_active_dev(), WMCP, false);
                }
              }
            }
          }

          if(current_src_context_type_ == LeAudioContextType::CONVERSATIONAL &&
             current_snk_context_type_ == LeAudioContextType::CONVERSATIONAL) {
            RawAddress voice_active_peer = btif_acm_initiator_voice_active_peer();
            if (voice_active_peer.IsEmpty()) {
              btif_ahim_ack_stream_started(A2DP_CTRL_ACK_FAILURE, AUDIO_GROUP_MGR);
              break;
            }
          }

          btif_acm_stream_start(lea_cmd.direction);
          btif_ahim_ack_stream_started(status, AUDIO_GROUP_MGR);
          break;
      } else {
        if (btif_acm_is_call_active() ||
            (!bluetooth::headset::btif_hf_is_call_vr_idle())) {
          BTIF_TRACE_IMP("%s: call active, return incall_failure", __func__);
          status = A2DP_CTRL_ACK_INCALL_FAILURE;
        } else {
          // ACM is in right state
          status = A2DP_CTRL_ACK_PENDING;
          btif_acm_stream_start(lea_cmd.direction);
        }
        btif_ahim_ack_stream_started(status, AUDIO_GROUP_MGR);
        break;
      }
    }

    case A2DP_CTRL_CMD_SUSPEND: {
      if (btif_ahim_is_aosp_aidl_hal_enabled()) {
        // ACM is in right state
        status = A2DP_CTRL_ACK_PENDING;
        if(lea_cmd.direction == FROM_AIR) {
          if(current_snk_context_type_ == LeAudioContextType::LIVE) {
            // push for reconfiguration to acm layer
            if (current_src_context_type_ == LeAudioContextType::GAME) {
              // ack back only rx path
              if (getAAR4Status(btif_acm_get_active_dev()) && false) {
                btif_acm_aar4_vbc(btif_acm_get_active_dev(), false);
              }
              /*btif_ahim_ack_stream_profile_suspended(A2DP_CTRL_ACK_SUCCESS,
                                                     AUDIO_GROUP_MGR, WMCP);
              break;*/
            } else {
              //Todo when 360REC implementation along with SHOs
              /*char wmcp_tx_rx[PROPERTY_VALUE_MAX] = "false";
              property_get("persist.vendor.btstack.wmcp_tx_rx", wmcp_tx_rx, "false");
              LOG(INFO) << __func__ << ": wmcp_tx_rx: " << wmcp_tx_rx;

              if (!strncmp("true", wmcp_tx_rx, 4) &&
                  current_src_context_type_ == LeAudioContextType::LIVE) {
                btif_ahim_ack_stream_profile_suspended(A2DP_CTRL_ACK_SUCCESS,
                                                       AUDIO_GROUP_MGR, WMCP_TX);
                break;
              }*/
            }
#if 0
            if(current_src_context_type_ == LeAudioContextType::GAME) {
              set_active_acm_initiator(btif_acm_get_active_dev(), GCP);
            } else {
              set_active_acm_initiator(btif_acm_get_active_dev(), WMCP);
            }
#endif
          } else if(current_src_context_type_ == LeAudioContextType::CONVERSATIONAL) {
            //reset the sink to LIVE
            current_snk_context_type_ = LeAudioContextType::LIVE;
            //Fake as WMCP to suspend decoder session only
            /*btif_ahim_ack_stream_profile_suspended(A2DP_CTRL_ACK_SUCCESS,
                                                   AUDIO_GROUP_MGR, WMCP);
            break;*/
          }
        } else if(lea_cmd.direction == TO_AIR) {
          if(current_src_context_type_ == LeAudioContextType::CONVERSATIONAL) {
            //reset the sink to LIVE
            current_snk_context_type_ = LeAudioContextType::LIVE;
          }
        }

        if (btif_acm_stream_suspend(lea_cmd.direction) == BT_STATUS_FAIL) {
          BTIF_TRACE_IMP("%s: stream suspend failed", __func__);
          btif_ahim_ack_stream_direction_suspended(A2DP_CTRL_ACK_SUCCESS,
                                                   AUDIO_GROUP_MGR, lea_cmd.direction);
        } else {
          btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
        }
        break;
      } else {
        if (btif_acm_is_call_active()) {
          BTIF_TRACE_IMP("%s: call active, return success", __func__);
          status = A2DP_CTRL_ACK_SUCCESS;
        } else {
          status = A2DP_CTRL_ACK_PENDING;
          btif_acm_stream_suspend(lea_cmd.direction);
        }
        btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
        break;
      }
    }

    case A2DP_CTRL_CMD_STOP: {
      status = A2DP_CTRL_ACK_SUCCESS;
      if (btif_ahim_is_aosp_aidl_hal_enabled()) {
        // ACM is in right state
        btif_acm_stream_stop();
        btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
        break;
      } else {
        if (btif_acm_is_call_active()) {
          BTIF_TRACE_IMP("%s: call active, return success", __func__);
        } else {
          btif_acm_stream_stop();
        }
        btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
        break;
      }
    }
    default:
      APPL_TRACE_ERROR("%s: unsupported command %d", __func__, lea_cmd.cmd);
      break;
  }
}

/*Copied below for the reference, from the file
 frameworks/proto_logging/stats/enums/media/audio/enums.proto */
/*========================================================================
// An enumeration from system/media/audio/include/system/audio-hal-enums.h
// audio_content_type_t, representing the content type of the AudioTrack.
enum ContentType {
  // Note: The first value in an enum must map to zero.
  // Mapping the first value to zero ensures the default behavior
  // is consistent between proto2 and proto3.
  CONTENT_TYPE_UNKNOWN = 0;
  CONTENT_TYPE_INVALID = -1;
  CONTENT_TYPE_SPEECH = 1;
  CONTENT_TYPE_MUSIC = 2;
  CONTENT_TYPE_MOVIE = 3;
  CONTENT_TYPE_SONIFICATION = 4;
}

enum Source {
  // Note: The first value in an enum must map to zero.
  // Mapping the first value to zero ensures the default behavior
  // is consistent between proto2 and proto3.
  AUDIO_SOURCE_DEFAULT = 0;  // may alias as UNKNOWN
  AUDIO_SOURCE_INVALID = -1;
  AUDIO_SOURCE_MIC = 1;
  AUDIO_SOURCE_VOICE_UPLINK = 2;
  AUDIO_SOURCE_VOICE_DOWNLINK = 3;
  AUDIO_SOURCE_VOICE_CALL = 4;
  AUDIO_SOURCE_CAMCORDER = 5;
  AUDIO_SOURCE_VOICE_RECOGNITION = 6;
  AUDIO_SOURCE_VOICE_COMMUNICATION = 7;
  AUDIO_SOURCE_REMOTE_SUBMIX = 8;
  AUDIO_SOURCE_UNPROCESSED = 9;
  AUDIO_SOURCE_VOICE_PERFORMANCE = 10;
  AUDIO_SOURCE_ECHO_REFERENCE = 1997;
  AUDIO_SOURCE_FM_TUNER = 1998;
  AUDIO_SOURCE_HOTWORD = 1999;
}

// An enumeration from system/media/audio/include/system/audio-hal-enums.h
// audio_usage_t, representing the use case for the AudioTrack.
enum Usage {
  AUDIO_USAGE_UNKNOWN = 0;
  AUDIO_USAGE_MEDIA = 1;
  AUDIO_USAGE_VOICE_COMMUNICATION = 2;
  AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING = 3;
  AUDIO_USAGE_ALARM = 4;
  AUDIO_USAGE_NOTIFICATION = 5;
  AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE = 6;
  AUDIO_USAGE_NOTIFICATION_EVENT = 10;
  AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY = 11;
  AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
  AUDIO_USAGE_ASSISTANCE_SONIFICATION = 13;
  AUDIO_USAGE_GAME = 14;
  AUDIO_USAGE_VIRTUAL_SOURCE = 15;
  AUDIO_USAGE_ASSISTANT = 16;
  AUDIO_USAGE_CALL_ASSISTANT = 17;
  AUDIO_USAGE_EMERGENCY = 1000;
  AUDIO_USAGE_SAFETY = 1001;
  AUDIO_USAGE_VEHICLE_STATUS = 1002;
  AUDIO_USAGE_ANNOUNCEMENT = 1003;
}
========================================================================*/

LeAudioContextType AudioContentToLeAudioContext(
    LeAudioContextType current_context_type,
    audio_content_type_t content_type,
    audio_source_t source_type, audio_usage_t usage) {

  switch (usage) {
    case AUDIO_USAGE_MEDIA:
      default_context_type_ = LeAudioContextType::MEDIA;
      return LeAudioContextType::MEDIA;
    case AUDIO_USAGE_VOICE_COMMUNICATION:
    case AUDIO_USAGE_CALL_ASSISTANT:
      default_context_type_ = LeAudioContextType::CONVERSATIONAL;
      return LeAudioContextType::CONVERSATIONAL;
    case AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
      return LeAudioContextType::VOICEASSISTANTS;
    case AUDIO_USAGE_ASSISTANCE_SONIFICATION:
      //When "usage" comes with touchtone from MM, handle below way
      //Check whether Call is active, if yes convert it into conversational.
      //else convert it into Media.
      LOG(INFO) << __func__ << ": current_context_type_: "
                << loghex(static_cast<uint16_t>(current_context_type_))
                << "current_profile_type_:" << current_profile_type_;
      if (btif_acm_get_is_inCall() ||
          btif_acm_check_in_call_tracker_timer_exist()) {
        LOG(INFO) << __func__ << ": Return conversational for touchtone or dial tone.";
        return LeAudioContextType::CONVERSATIONAL;
      } else if (current_context_type_ == LeAudioContextType::MEDIA &&
                 (current_profile_type_ == GCP ||
                  current_profile_type_ == GCP_RX)) {
        LOG(INFO) << __func__ << ": Return Game for touchtone or dial tone.";
        return LeAudioContextType::GAME;
      } else if (current_context_type_ == LeAudioContextType::MEDIA &&
                 current_profile_type_ == WMCP) {
        LOG(INFO) << __func__ << ": Return LIVE for touchtone or dial tone.";
        return LeAudioContextType::LIVE;
      } else {
        LOG(INFO) << __func__ << ": Return Media for touchtone or dial tone.";
        return LeAudioContextType::MEDIA;
      } break;
    case AUDIO_USAGE_GAME:
      default_context_type_ = LeAudioContextType::GAME;
      return LeAudioContextType::GAME;
    case AUDIO_USAGE_NOTIFICATION:
      return LeAudioContextType::NOTIFICATIONS;
    case AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE:
      return LeAudioContextType::RINGTONE;
    case AUDIO_USAGE_ALARM:
      return LeAudioContextType::ALERTS;
    case AUDIO_USAGE_EMERGENCY:
      return LeAudioContextType::EMERGENCYALARM;
    case AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
      return LeAudioContextType::INSTRUCTIONAL;
    default:
      break;
  }

  switch (source_type) {
    case AUDIO_SOURCE_MIC:
    case AUDIO_SOURCE_HOTWORD:
      if (source_type == AUDIO_SOURCE_HOTWORD) {
        char is_wake_word_detection_enabled[PROPERTY_VALUE_MAX] = "false";
        property_get("persist.vendor.btstack.wake_word_detection_enabled",
                                    is_wake_word_detection_enabled, "false");
        LOG(INFO) << __func__ << ": is_wake_word_detection_enabled: "
                              << is_wake_word_detection_enabled;
        if (!strncmp("true", is_wake_word_detection_enabled, 4)) {
          return LeAudioContextType::LIVE;
        }
        break;
      } else {
        return LeAudioContextType::LIVE;
      }
    case AUDIO_SOURCE_VOICE_CALL:
    case AUDIO_SOURCE_VOICE_COMMUNICATION:
      default_context_type_ = LeAudioContextType::CONVERSATIONAL;
      return LeAudioContextType::CONVERSATIONAL;
    default:
      break;
  }

  //When "usage" comes with some garbage from MM, handle below way
  //Check whether Call is active, if yes convert it into conversational.
  //else convert it into Media.
  if (btif_acm_get_is_inCall()) {
    LOG(INFO) << __func__ << ": Return conversational when in Call by default.";
    return LeAudioContextType::CONVERSATIONAL;
  } else {
    LOG(INFO) << __func__ << ": Return Media when not in call by default.";
    return LeAudioContextType::MEDIA;
  }
}

uint16_t LeAudioSrcContextToIntContent(LeAudioContextType context_type) {
  switch (context_type) {
    case LeAudioContextType::MEDIA:
      return bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
    case LeAudioContextType::GAME:
      return bluetooth::bap::ucast::CONTENT_TYPE_GAME;
    case LeAudioContextType::CONVERSATIONAL: //Fall through
    case LeAudioContextType::RINGTONE:
    case LeAudioContextType::VOICEASSISTANTS:
      return bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
    default:
      break;
  }
  return 0;
}

uint16_t LeAudioSnkContextToIntContent(LeAudioContextType context_type) {
  switch (context_type) {
    case LeAudioContextType::MEDIA:
      return bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
    case LeAudioContextType::GAME:
      return bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
    case LeAudioContextType::CONVERSATIONAL:
      return bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
    case LeAudioContextType::LIVE:
      return bluetooth::bap::ucast::CONTENT_TYPE_LIVE;
    default:
      break;
  }
  return 0;
}

LeAudioContextType ChooseContextType(
    std::vector<LeAudioContextType>& available_contents) {
  /* Mini policy.
   * Voice is prio 1,
   * Ringtone is prio 2
   * rec prio 3,
   * Game prio 4,
   * media prio 5 */
  auto iter = find(available_contents.begin(), available_contents.end(),
                   LeAudioContextType::CONVERSATIONAL);
  if (iter != available_contents.end())
    return LeAudioContextType::CONVERSATIONAL;

  iter = find(available_contents.begin(), available_contents.end(),
              LeAudioContextType::RINGTONE);
  if (iter != available_contents.end())
    return LeAudioContextType::CONVERSATIONAL;

  iter = find(available_contents.begin(), available_contents.end(),
              LeAudioContextType::LIVE);
  if (iter != available_contents.end()) return LeAudioContextType::LIVE;

  iter = find(available_contents.begin(), available_contents.end(),
              LeAudioContextType::GAME);
  if (iter != available_contents.end()) return LeAudioContextType::GAME;

  iter = find(available_contents.begin(), available_contents.end(),
              LeAudioContextType::MEDIA);
  if (iter != available_contents.end()) return LeAudioContextType::MEDIA;

  /*TODO do something smarter here */
  return available_contents[0];
}

void btif_acm_signal_metadata_complete() {
  if (src_metadata_pending) {
    btif_ahim_signal_src_metadata_complete();
    src_metadata_pending = false;
  }
  if (snk_metadata_pending) {
    btif_ahim_signal_snk_metadata_complete();
    snk_metadata_pending = false;
  }
}

void btif_acm_process_src_meta_request(source_metadata_t *p_param) {
  auto tracks = p_param->tracks;
  auto track_count = p_param->track_count;
  bt_status_t status = BT_STATUS_SUCCESS;
  //bool send_update_to_apm = true;
/*
  if (send_update_to_apm) {
    LOG(INFO) << __func__ << ": Metadata handled in Apps Layer";
    btif_ahim_signal_src_metadata_complete();
    return;
  }
*/
  LOG(INFO) << __func__ << ": is_set_active_pending:"
                        << logbool(is_set_active_pending);
  if (is_set_active_pending == true) {
    LOG(INFO) << __func__ << ": Set Active for device still in progress, return";
    btif_ahim_signal_src_metadata_complete();
    return;
  }

  std::vector<LeAudioContextType> contexts;

  while (track_count) {
    if (tracks->content_type == 0 && tracks->usage == 0) {
      --track_count;
      ++tracks;
      continue;
    }

    LOG(INFO) << __func__
              << ": usage=" << tracks->usage
              << ", content_type=" << tracks->content_type
              << ", gain=" << tracks->gain
              << ", current_context_type_: "
              << loghex(static_cast<uint16_t>(current_context_type_));

    auto new_context = AudioContentToLeAudioContext(
        current_context_type_, tracks->content_type, AUDIO_SOURCE_DEFAULT, tracks->usage);
    contexts.push_back(new_context);

    --track_count;
    ++tracks;
    LOG(INFO) << __func__
              << ": After Coverting AudioContentToLeAudioContext: "
              << static_cast<int>(new_context);
  }

  if (contexts.empty()) {
    LOG(INFO) << __func__ << ": invalid metadata update";
    btif_ahim_signal_src_metadata_complete();
    return;
  }
  auto new_context = ChooseContextType(contexts);
  uint16_t new_profile = 0;
  LOG(INFO) << __func__
            << ": new_context_type: " << static_cast<int>(new_context);
  if(new_context == LeAudioContextType::CONVERSATIONAL ||
     new_context == LeAudioContextType::RINGTONE ||
     new_context == LeAudioContextType::VOICEASSISTANTS) {
    new_profile = BAP_CALL;
  } else if(new_context == LeAudioContextType::MEDIA) {
    new_profile = BAP;
  } else if(new_context == LeAudioContextType::GAME) {
    new_profile = GCP;
  }

  LOG(INFO) << __func__ << ": current_context_type_: "
              << loghex(static_cast<uint16_t>(current_context_type_))
              << ", current_profile_type_: "<< current_profile_type_
              << ", new_profile: " << new_profile;

  //While SREC is going on, if Metadata request comes for Media again
  //ignore processing of Media Metadata request as SREC has the proprity
  //over Media.
  if ((current_context_type_ == LeAudioContextType::MEDIA) &&
      (current_profile_type_ == WMCP || current_profile_type_ == WMCP_TX) &&
      new_context == LeAudioContextType::MEDIA && new_profile == BAP &&
        is_active_dev_streaming()) {
    LOG(INFO) << __func__ << ": Ignore and suspend Media while SREC is ongoing.";
    // Suspend BAP because Some apps will start BAP TX stream while recording.
    //btif_acm_source_profile_on_suspended(A2DP_CTRL_ACK_STREAM_SUSPENDED, BAP);
    btif_ahim_signal_src_metadata_complete();
    return;
  }

  //While VBC is going on, if Metadata request comes for Game again
  //ignore processing of Game Metadata request.
  if((current_context_type_ == LeAudioContextType::MEDIA) &&
     (current_profile_type_ == GCP_RX) &&
     new_context == LeAudioContextType::GAME && new_profile == GCP) {
    LOG(INFO) << __func__ << ": Ignore Game while VBC is active.";
    btif_ahim_signal_src_metadata_complete();
    return;
  }

  if ((current_context_type_ == LeAudioContextType::MEDIA) &&
      (current_profile_type_ == GCP_RX || current_profile_type_ == GCP) &&
      (new_context == LeAudioContextType::MEDIA && new_profile == BAP)) {
    char ignore_media_while_gaming[PROPERTY_VALUE_MAX] = "false";
    property_get("persist.vendor.service.bt.ignore_media_while_gaming",
                  ignore_media_while_gaming, "false");
    if(!strncmp("true", ignore_media_while_gaming, 4)) {
      LOG(INFO) << __func__ << ": Ignore media while Gaming/VBC is active";
      btif_ahim_signal_src_metadata_complete();
      return;
    } else {
      LOG(INFO) << __func__ << ": Don't Ignore media while Gaming/VBC is active";
    }
  }

  char voip_lea_war[PROPERTY_VALUE_MAX] = "false";
  property_get("persist.enable.bluetooth.voipleawar", voip_lea_war, "false");
  if (!strncmp("true", voip_lea_war, 4)) {
    if (current_context_type_ == LeAudioContextType::CONVERSATIONAL &&
        current_profile_type_ == BAP_CALL &&
        btif_acm_get_is_inCall() &&
        new_context == LeAudioContextType::MEDIA && new_profile == BAP) {
      LOG(INFO) << __func__ << ": voip_lea_war enabled, Ignore media while call is still ongoing";
      btif_ahim_signal_src_metadata_complete();
      return;
    }

    if ((current_context_type_ == LeAudioContextType::MEDIA) &&
        (current_profile_type_ == BAP) &&
        (new_context == LeAudioContextType::VOICEASSISTANTS && new_profile == BAP_CALL)) {
      LOG(INFO) << __func__ << ": voip_lea_war enabled, Ignore VOICEASSISTANTS while BAP is active";
      btif_ahim_signal_src_metadata_complete();
      return;
    }
  }

  if(new_context == LeAudioContextType::CONVERSATIONAL ||
     new_context == LeAudioContextType::MEDIA ||
     new_context == LeAudioContextType::GAME ||
     new_context == LeAudioContextType::RINGTONE ||
     new_context == LeAudioContextType::VOICEASSISTANTS) {
    // findout active peer, convert current_context_type_ to profileType
    uint16_t content_type = LeAudioSrcContextToIntContent(new_context);
    LOG(INFO) << __func__ << ": content_type: " << content_type;

    {
      LOG(INFO) << __func__ << ": Wait for Mutex";
      std::unique_lock<std::mutex> guard(acm_set_active_wait_mutex_);
      LOG(INFO) << __func__ << ": Got Mutex";
    }
    LOG(INFO) << __func__ << ": Mutex scope completed.";

    if (getAAR4Status(btif_acm_get_active_dev())) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": AAR4  music active peer is empty, ignore metadata update";
        return;
      }
      LOG(INFO) << __func__ << ": send VSC Usecase Update for AAR4";
      btif_acm_send_vs_cmd(btif_acm_get_active_dev(), content_type);
    }
    // push for reconfiguration to acm layer
    if(content_type == bluetooth::bap::ucast::CONTENT_TYPE_MEDIA) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": music active peer is empty, ignore metadata update";
        btif_ahim_signal_src_metadata_complete();
        return;
      }
      char ignore_media_while_gaming[PROPERTY_VALUE_MAX] = "false";
      property_get("persist.vendor.service.bt.ignore_media_while_gaming",
                    ignore_media_while_gaming, "false");
      uint16_t previous_media_profile = btif_acm_get_previous_active_media_profile();
      if ((previous_media_profile & (GCP | GCP_RX)) &&
            !strncmp("true", ignore_media_while_gaming, 4)) {
        LOG(INFO) << __func__ << ": previous active media profile is "
                              << previous_media_profile;
        src_metadata_pending = true;
        status = set_active_acm_initiator_internal(music_active_peer, previous_media_profile, false);
      } else {
        LOG(INFO) << __func__ << ": Set active device for BAP";
        src_metadata_pending = true;
        status = set_active_acm_initiator_internal(music_active_peer, BAP, false);
      }
    } else if(content_type == bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL) {
      RawAddress voice_active_peer = btif_acm_initiator_voice_active_peer();
      if (voice_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": voice active peer is empty, ignore metadata update";
        btif_ahim_signal_src_metadata_complete();
        return;
      }
      src_metadata_pending = true;
      status = set_active_acm_initiator_internal(voice_active_peer, BAP_CALL, false);
    } else if(content_type == bluetooth::bap::ucast::CONTENT_TYPE_GAME) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": music active peer is empty, ignore metadata update";
        btif_ahim_signal_src_metadata_complete();
        return;
      }
      src_metadata_pending = true;
      status = set_active_acm_initiator_internal(music_active_peer, GCP, false);
    }
  }

  LOG(INFO) << __func__ << ": status: " << status;
  if (status != BT_STATUS_SUCCESS) {
    src_metadata_pending = false;
  }

  if (!src_metadata_pending) {
    btif_ahim_signal_src_metadata_complete();
  }
}


void btif_acm_process_snk_meta_request(sink_metadata_t *p_param) {
  auto tracks = p_param->tracks;
  auto track_count = p_param->track_count;
  bt_status_t status = BT_STATUS_SUCCESS;
  //bool send_update_to_apm = true;
/*
  if (send_update_to_apm) {
    LOG(INFO) << __func__ << ": Metadata handled in Apps Layer";
    btif_ahim_signal_src_metadata_complete();
    return;
  }
*/
  LOG(INFO) << __func__ << ": is_set_active_pending:"
                        << logbool(is_set_active_pending);
  if (is_set_active_pending == true) {
    LOG(INFO) << __func__ << ": Set Active for device still in progress, return";
    btif_ahim_signal_snk_metadata_complete();
    return;
  }

  std::vector<LeAudioContextType> contexts;

  while (track_count) {
    if (tracks->source == 0) {
      --track_count;
      ++tracks;
      continue;
    }

    LOG(INFO) << __func__
              << ": source =" << tracks->source
              << ", gain=" << tracks->gain
              << ", current_context_type_: "
              << loghex(static_cast<uint16_t>(current_context_type_));

    auto new_context = AudioContentToLeAudioContext(
        current_context_type_, AUDIO_CONTENT_TYPE_UNKNOWN,
        tracks->source, AUDIO_USAGE_UNKNOWN);
    contexts.push_back(new_context);

    --track_count;
    ++tracks;
    LOG(INFO) << __func__
              << ": After Coverting AudioContentToLeAudioContext: "
              << static_cast<int>(new_context);
  }

  if (contexts.empty()) {
    LOG(INFO) << __func__ << ": invalid metadata update";
    btif_ahim_signal_snk_metadata_complete();
    return;
  }

  auto context = ChooseContextType(contexts);

  {
    LOG(INFO) << __func__ << ": Wait for Mutex";
    std::unique_lock<std::mutex> guard(acm_set_active_wait_mutex_);
    LOG(INFO) << __func__ << ": Got Mutex";
  }
  LOG(INFO) << __func__ << ": Mutex scope completed.";

  if (context == LeAudioContextType::CONVERSATIONAL) {
    RawAddress voice_active_peer = btif_acm_initiator_voice_active_peer();
    if (voice_active_peer.IsEmpty()) {
      LOG(INFO) << __func__ << ": voice active peer is empty, ignore metadata update";
      btif_ahim_signal_snk_metadata_complete();
      return;
    }
  }
  current_snk_context_type_ = ChooseContextType(contexts);
  LOG(INFO) << __func__ << ": current_snk_context_type_: "
            << loghex(static_cast<uint16_t>(current_snk_context_type_));

  if (current_snk_context_type_ == LeAudioContextType::LIVE &&
    current_src_context_type_ == LeAudioContextType::GAME) {
    RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
    if (music_active_peer.IsEmpty()) {
      LOG(INFO) << __func__ << ": music active peer is empty, ignore sink metadata update";
      btif_ahim_signal_snk_metadata_complete();
      return;
    }
    if (getAAR4Status(btif_acm_get_active_dev()) && false) {
      if (default_context_type_ == LeAudioContextType::GAME) {
        LOG(INFO) << __func__ << ": AAR4 GCP_RX";
        status = set_active_acm_initiator_internal(music_active_peer, GCP_RX, false);
      } else {
        LOG(INFO) << __func__ << ": AAR4 WMCP";
        status = set_active_acm_initiator_internal(music_active_peer, WMCP, false);
      }
    } else if (current_snk_context_type_ == LeAudioContextType::LIVE) {
      status = set_active_acm_initiator_internal(music_active_peer, GCP_RX, false);
    }
    if (getAAR4Status(btif_acm_get_active_dev())) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": AAR4  music active peer is empty, ignore metadata update";
        return;
      }
      LOG(INFO) << __func__ << ": send VSC Usecase Update for AAR4";
      btif_acm_send_vs_cmd(btif_acm_get_active_dev(), bluetooth::bap::ucast::CONTENT_TYPE_LIVE);
    }
  } else if (current_snk_context_type_ == LeAudioContextType::LIVE) {
    RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
    if (music_active_peer.IsEmpty()) {
      LOG(INFO) << __func__ << ": music active peer is empty, ignore sink metadata update";
      btif_ahim_signal_snk_metadata_complete();
      return;
    }
    if (btif_bap_broadcast_is_active()) {
      btif_bap_broadcast_to_unicast_switch();
    }
    snk_metadata_pending = true;
    char wmcp_tx_rx[PROPERTY_VALUE_MAX] = "false";
    property_get("persist.vendor.btstack.wmcp_tx_rx", wmcp_tx_rx, "false");
    LOG(INFO) << __func__ << ": wmcp_tx_rx: " << wmcp_tx_rx;

    if (!strncmp("true", wmcp_tx_rx, 4)) {
      status = set_active_acm_initiator_internal(music_active_peer, WMCP_TX, false);
    } else {
      status = set_active_acm_initiator_internal(music_active_peer, WMCP, false);
    }
    if (getAAR4Status(btif_acm_get_active_dev())) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": AAR4  music active peer is empty, ignore metadata update";
        return;
      }
      LOG(INFO) << __func__ << ": send VSC Usecase Update for AAR4";
      btif_acm_send_vs_cmd(btif_acm_get_active_dev(), bluetooth::bap::ucast::CONTENT_TYPE_LIVE);
    }
  } else if (current_snk_context_type_ == LeAudioContextType::CONVERSATIONAL) {
    RawAddress voice_active_peer = btif_acm_initiator_voice_active_peer();
    if (voice_active_peer.IsEmpty()) {
      LOG(INFO) << __func__ << ": voice active peer is empty, ignore sink metadata update";
      snk_metadata_pending = false;
      btif_ahim_signal_snk_metadata_complete();
      return;
    }
    LOG(INFO) << __func__ << ": calling set_active_acm_initiator_internal for BAP_CALL";
    snk_metadata_pending = true;
    status = set_active_acm_initiator_internal(voice_active_peer, BAP_CALL, false);
    if (getAAR4Status(btif_acm_get_active_dev())) {
      RawAddress music_active_peer = btif_acm_initiator_music_active_peer();
      if (music_active_peer.IsEmpty()) {
        LOG(INFO) << __func__ << ": AAR4  music active peer is empty, ignore metadata update";
        return;
      }
      LOG(INFO) << __func__ << ": send VSC Usecase Update for AAR4";
      btif_acm_send_vs_cmd(btif_acm_get_active_dev(), bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL);
    }
  }

  LOG(INFO) << __func__ << ": status: " << status;
  if (status != BT_STATUS_SUCCESS) {
    snk_metadata_pending = false;
  }

  if (!snk_metadata_pending) {
    btif_ahim_signal_snk_metadata_complete();
  }
#if 0
  auto new_context = ChooseContextType(contexts);
  auto actual_context = new_context;
  uint16_t new_profile = 0;
  LOG(INFO) << __func__
             << " new_context_type: " << static_cast<int>(new_context);


  if(new_context == LeAudioContextType::CONVERSATIONAL) {
    new_profile = BAP_CALL;
  } else if(new_context == LeAudioContextType::LIVE) {
    new_context = LeAudioContextType::MEDIA;
    new_profile = WMCP;
  }

  if(new_context == LeAudioContextType::CONVERSATIONAL ||
     new_context == LeAudioContextType::MEDIA) {
    // findout active peer, convert current_context_type_ to profileType
    uint16_t content_type = LeAudioSnkContextToIntContent(actual_context);
    // push for reconfiguration to acm layer
    if(content_type == bluetooth::bap::ucast::CONTENT_TYPE_LIVE) {
      set_active_acm_initiator(btif_acm_get_active_dev(), WMCP);
    } else if(content_type == bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL) {
      set_active_acm_initiator(btif_acm_get_active_dev(), BAP_CALL);
    }
  }
#endif
}

bool btif_acm_update_acm_context(uint16_t acm_context, uint16_t acm_profile,
                                 uint16_t curr_acm_profile,
                                 const RawAddress& peer_address) {
  std::unique_lock<std::mutex> guard(acm_mutex_);
  uint16_t current_acm_context = CONTEXT_TYPE_MUSIC;

  LOG(INFO) << __func__ << ": current_context_type_: "
            << loghex(static_cast<uint16_t>(current_context_type_))
            << ", current_profile_type_: "<< current_profile_type_;

  if (current_context_type_ == LeAudioContextType::CONVERSATIONAL) {
    if (acm_profile == current_profile_type_ &&
        acm_context == CONTEXT_TYPE_VOICE) {
      //Below check is, for different LE-A to LE-A device
      //or null to LE-A. In both cases need to skip returning false.
      //But need to update all variables.
      if (peer_address == btif_acm_initiator_voice_active_peer()) {
        LOG(INFO) << __func__
                  << ": Voice profie did not changed, return false.";
        btif_acm_signal_metadata_complete();
        return false;
      }
    } else if (btif_acm_get_is_inCall()) {
      LOG(INFO) << __func__ << ": Return conversational when in Call is active";
      btif_acm_signal_metadata_complete();
      return false;
    }
    current_acm_context = CONTEXT_TYPE_VOICE;
  }

  if (acm_context == current_acm_context &&
      acm_profile == current_profile_type_ &&
      acm_profile == curr_acm_profile) {
    //Below check is, for different LE-A to LE-A device
    //or null to LE-A. In both cases need to skip returning false.
    //But need to update all variables.
    if (peer_address == btif_acm_initiator_music_active_peer()) {
      LOG(INFO) << __func__
                << ": Context and profie did not changed, return false.";
      btif_acm_signal_metadata_complete();
      return false;
    }
  }

  if (acm_context == CONTEXT_TYPE_MUSIC) {
    LOG(INFO) << __func__
              << ": ACM Context changed to media, profile type: "
              << current_profile_type_;
    current_context_type_ = LeAudioContextType::MEDIA;
    current_profile_type_ = acm_profile;
    if(current_profile_type_ == BAP) {
      current_src_context_type_ = LeAudioContextType::MEDIA;
    } else if(current_profile_type_ == WMCP) {
      current_src_context_type_ = current_context_type_;
      current_snk_context_type_ = LeAudioContextType::LIVE;
    } else if(current_profile_type_ == GCP) {
      current_src_context_type_ = LeAudioContextType::GAME;
      current_snk_context_type_ = LeAudioContextType::UNINITIALIZED;
    } else if(current_profile_type_ == GCP_RX) {
      current_src_context_type_ = LeAudioContextType::GAME;
      current_snk_context_type_ = LeAudioContextType::LIVE;
    } else if(current_profile_type_ == WMCP_TX) {
      current_src_context_type_ = LeAudioContextType::LIVE;
      current_snk_context_type_ = LeAudioContextType::LIVE;
    }
  } else if(acm_context == CONTEXT_TYPE_VOICE) {
    LOG(INFO) << __func__ << ": ACM Context changed to Call";
    current_context_type_ = LeAudioContextType::CONVERSATIONAL;
    current_profile_type_ = acm_profile;
    current_src_context_type_ = current_context_type_;
    current_snk_context_type_ = current_context_type_;
  }

  LOG(INFO) << __func__ << ": updated current_context_type_: "
            << loghex(static_cast<uint16_t>(current_context_type_))
            << ", updated current_profile_type_: "<< current_profile_type_;

  return true;
}

void btif_acm_handle_event(uint16_t event, char* p_param) {
  LOG(INFO) << __func__ << ": event: " << event;
  switch(event) {
    case BTIF_ACM_PROCESS_HIDL_REQ_EVT:
      btif_acm_process_request(*((tLEA_CTRL_CMD *) p_param));
      break;

    case BTIF_ACM_PROCESS_SRC_META_REQ_EVT:
      btif_acm_process_src_meta_request((source_metadata_t *) p_param);
      break;

    case BTIF_ACM_PROCESS_SNK_META_REQ_EVT:
      btif_acm_process_snk_meta_request((sink_metadata_t *) p_param);
      break;

    default:
      BTIF_TRACE_IMP("%s: unhandled event", __func__);
      break;
  }
}

void process_hidl_req_acm(tA2DP_CTRL_CMD cmd, uint8_t direction) {
  tLEA_CTRL_CMD lea_cmd;

  lea_cmd.cmd = cmd;
  lea_cmd.direction = direction;

  btif_transfer_context(btif_acm_handle_event,
      BTIF_ACM_PROCESS_HIDL_REQ_EVT, (char*)&lea_cmd, sizeof(lea_cmd), NULL);
}

void process_src_metadata(const source_metadata_t& src_metadata) {
  auto track_count = src_metadata.track_count;
  auto usage = src_metadata.tracks->usage;

  LOG(INFO) << __func__ << ": track_count: " << track_count
                        << ", usage: " << usage;

  uint16_t param_len = 0;
  param_len += sizeof(source_metadata_t);
  param_len += (src_metadata.track_count * sizeof(playback_track_metadata));

  LOG(INFO) << __func__ << ": param_len: " << param_len;
  LOG(INFO) << __func__ << ": Sending Metadata to APM";
  //src_meta_update_apm(src_metadata);
  btif_transfer_context(btif_acm_handle_event,
                       BTIF_ACM_PROCESS_SRC_META_REQ_EVT,
                       (char*)&src_metadata, param_len, src_metadata_copy_cb);

}

void process_snk_metadata(const sink_metadata_t& snk_metadata) {
  auto track_count = snk_metadata.track_count;
  auto source = snk_metadata.tracks->source;

  LOG(INFO) << __func__ << ": track_count: " << track_count
                         << ", source: " << source;

  uint16_t param_len = 0;
  param_len += sizeof(sink_metadata_t);
  param_len += (snk_metadata.track_count * sizeof(record_track_metadata));

  LOG(INFO) << __func__ << ": param_len: " << param_len;
  //snk_meta_update_apm(snk_metadata);
  btif_transfer_context(btif_acm_handle_event,
                        BTIF_ACM_PROCESS_SNK_META_REQ_EVT,
                        (char*)&snk_metadata, param_len, sink_metadata_copy_cb);
}

void src_metadata_copy_cb(uint16_t event, char* p_dest, char* p_src) {

  LOG(INFO) << __func__ << ": event: " << event;
  source_metadata_t* p_dest_data = (source_metadata_t*)p_dest;
  source_metadata_t* p_src_data = (source_metadata_t*)p_src;

  if (!p_src) {
    LOG(INFO) << __func__ << ": p_src is null";
    return;
  }

  maybe_non_aligned_memcpy(p_dest_data, p_src_data, sizeof(*p_src_data));

  switch (event) {
    case BTIF_ACM_PROCESS_SRC_META_REQ_EVT: {
      p_dest_data->tracks =
          (playback_track_metadata*)(p_dest + sizeof(source_metadata_t));
      memcpy(p_dest_data->tracks, p_src_data->tracks,
             p_src_data->track_count * sizeof(playback_track_metadata));
    } break;
  }
}

void sink_metadata_copy_cb(uint16_t event, char* p_dest, char* p_src) {

  LOG(INFO) << __func__ << ": event: " << event;

  sink_metadata_t* p_dest_data = (sink_metadata_t*)p_dest;
  sink_metadata_t* p_src_data = (sink_metadata_t*)p_src;

  if (!p_src) {
    LOG(INFO) << __func__ << ": p_src is null";
    return;
  }

  maybe_non_aligned_memcpy(p_dest_data, p_src_data, sizeof(*p_src_data));

  switch (event) {
    case BTIF_ACM_PROCESS_SNK_META_REQ_EVT: {
      p_dest_data->tracks =
          (record_track_metadata*)(p_dest + sizeof(sink_metadata_t));
      memcpy(p_dest_data->tracks, p_src_data->tracks,
             p_src_data->track_count * sizeof(record_track_metadata));
    } break;
  }
}


static btif_ahim_client_callbacks_t sAhimAcmCallbacks = {
  1, // mode
  process_hidl_req_acm,
  btif_acm_get_sample_rate,
  btif_acm_get_ch_mode,
  btif_acm_get_bitrate,
  btif_acm_get_octets,
  btif_acm_get_framelength,
  btif_acm_get_ch_count,
  nullptr,
  btif_acm_get_current_active_profile,
  btif_acm_get_codec_type,
  btif_acm_is_codec_type_lc3q,
  btif_acm_codec_encoder_version,
  btif_acm_codec_decoder_version,
  btif_acm_get_min_sup_frame_dur,
  btif_acm_get_feature_map,
  btif_acm_lc3_blocks_per_sdu,
  btif_acm_get_audio_location,
  process_src_metadata,
  process_snk_metadata,
  btif_acm_get_aar4_mode,
  btif_acm_get_frame_duration,
  process_update_params,
  btif_acm_codec_version_aptx
};

void btif_register_cb() {
  reg_cb_with_ahim(AUDIO_GROUP_MGR, &sAhimAcmCallbacks);
}

bt_status_t btif_acm_source_setup_codec() {
  APPL_TRACE_EVENT("%s: ## setup_codec ##", __func__);

  bt_status_t status = BT_STATUS_FAIL;

  btif_ahim_setup_codec(AUDIO_GROUP_MGR);

  // TODO: check the status
  return status;
}

bool btif_acm_source_start_session(const RawAddress& peer_address) {
  bt_status_t status = BT_STATUS_FAIL;
  APPL_TRACE_DEBUG("%s: starting session for BD addr %s",__func__,
        peer_address.ToString().c_str());

  // initialize hal.
  btif_ahim_init_hal(get_worker_thread(), AUDIO_GROUP_MGR);

  status = btif_acm_source_setup_codec();

  btif_ahim_start_session(AUDIO_GROUP_MGR);

  return true;
}

bool btif_acm_source_end_session(const RawAddress& peer_address) {
  APPL_TRACE_DEBUG("%s: ending session for BD addr %s",__func__,
        peer_address.ToString().c_str());

  btif_ahim_end_session(AUDIO_GROUP_MGR);

  return true;
}

bool btif_acm_source_restart_session(const RawAddress& old_peer_address,
                                      const RawAddress& new_peer_address) {
  bool is_streaming = btif_ahim_is_streaming();
  SessionType session_type = btif_ahim_get_session_type(AUDIO_GROUP_MGR);

  APPL_TRACE_IMP("%s: old_peer_address: %s, new_peer_address: %s,"
                 " is_streaming: %d, session_type: %d", __func__,
                 old_peer_address.ToString().c_str(),
                 new_peer_address.ToString().c_str(),
                 is_streaming, session_type);

  //LOG(INFO) << __func__ << ": SessionType=" << toString(session_type);

  // TODO: do we need to check for new empty address
  //CHECK(!new_peer_address.IsEmpty());

  // If the old active peer was valid or if session is not
  // unknown, end the old session.
  if(!btif_ahim_is_aosp_aidl_hal_enabled()) {
    if (!old_peer_address.IsEmpty() ||
      session_type != SessionType::UNKNOWN) {
      btif_acm_source_end_session(old_peer_address);
    }
  }
  btif_acm_source_start_session(new_peer_address);

  return true;
}

bool btif_acm_update_sink_latency_change(uint16_t sink_latency) {
  APPL_TRACE_DEBUG("%s: update_sink_latency %d for active session ",__func__,
                                sink_latency);
  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_set_remote_delay(sink_latency, AUDIO_GROUP_MGR);
  } else {
    // In QTI LE-AUDIO, LE-A is going over A2DP session, and the unit
    // of delay report for A2DP session is (1/10) ms. Need convert the
    // sink latency of ACM from ms to (1/10) ms.
    btif_ahim_set_remote_delay(sink_latency * 10, AUDIO_GROUP_MGR);
  }

  return true;
}

void btif_acm_source_command_ack(tA2DP_CTRL_CMD cmd, tA2DP_CTRL_ACK status) {
  switch (cmd) {
    case A2DP_CTRL_CMD_START:
      btif_ahim_ack_stream_started(status, AUDIO_GROUP_MGR);
      break;
    case A2DP_CTRL_CMD_SUSPEND:
    case A2DP_CTRL_CMD_STOP:
      btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
      break;
    default:
      break;
  }
}

void btif_acm_source_on_stopped(tA2DP_CTRL_ACK status) {
  APPL_TRACE_EVENT("%s: status: %u", __func__, status);
  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
    btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
  } else {
    btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
  }
}

void btif_acm_source_on_suspended(tA2DP_CTRL_ACK status) {
  APPL_TRACE_EVENT("%s: status: %u", __func__, status);

  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
    btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
  } else {
    btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
  }
}

void btif_acm_source_direction_on_suspended(tA2DP_CTRL_ACK status, uint8_t direction) {
  APPL_TRACE_EVENT("%s: status: %u, dir: %d", __func__, status, direction);

  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR, direction);
    btif_ahim_ack_stream_direction_suspended(status, AUDIO_GROUP_MGR, direction);
  } else {
    btif_ahim_ack_stream_suspended(status, AUDIO_GROUP_MGR);
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
  }
}

void btif_acm_source_profile_on_suspended(tA2DP_CTRL_ACK status,
                                          uint16_t sub_profile) {

  APPL_TRACE_EVENT("%s: status: %u, sub_profile: %d",
                          __func__, status, sub_profile);

  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
    btif_ahim_ack_stream_profile_suspended(status, AUDIO_GROUP_MGR, sub_profile);
  } else {
    btif_ahim_ack_stream_profile_suspended(status, AUDIO_GROUP_MGR, sub_profile);
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
  }
}

bool btif_acm_on_started(tA2DP_CTRL_ACK status) {
  APPL_TRACE_EVENT("%s: status %u", __func__, status);
  bool retval = true;
  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
    btif_ahim_ack_stream_started(status, AUDIO_GROUP_MGR);
  } else {
    btif_ahim_ack_stream_started(status, AUDIO_GROUP_MGR);
    btif_ahim_reset_pending_command(AUDIO_GROUP_MGR);
  }
  return retval;
}

void process_update_params(uint16_t delay, uint8_t mode){
  APPL_TRACE_EVENT("%s: delay %u mode %u", __func__, delay, mode);
  bool is_delay_updated = false;
  if (delay != 0xFFFF && delay != btif_ahim_get_remote_delay()) {
    btif_acm_update_sink_latency_change(delay);
    is_delay_updated = true;
  }
  if (mode != 0xFF && mode != btif_acm_get_aar4_mode()) {
    btif_acm_set_aar4_mode(mode);
  }
  if (is_delay_updated) {
    btif_ahim_update_audio_config();
  }
}
#endif // AHIM_ENABLED
