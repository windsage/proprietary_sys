/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above
         copyright notice, this list of conditions and the following
         disclaimer in the documentation and/or other materials provided
         with the distribution.
       * Neither the name of The Linux Foundation nor the names of its
         contributors may be used to endorse or promote products derived
         from this software without specific prior written permission.
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
 **************************************************************************/

#define LOG_TAG "btif_apm"

#include <base/logging.h>
#include <string.h>
#include <base/bind.h>
#include <base/callback.h>
#include "osi/include/thread.h"

#include <hardware/bluetooth.h>
#include <hardware/bt_apm.h>
#include <hardware/bt_bap_uclient.h>

#include "bt_common.h"
#include "btif_common.h"
#include "btif_ahim.h"
#include "btm_api.h"
#include "btif_acm_source.h"
#include <vector>
#include <map>

#define A2DP_PROFILE 0x0001
#define BROADCAST_BREDR 0x0400
#define BROADCAST_LE 0x0800
#define ACTIVE_VOICE_PROFILE_HFP 0x0002
using base::Bind;

std::mutex apm_mutex;
btapm_initiator_callbacks_t* callbacks_;

static bt_status_t init(btapm_initiator_callbacks_t* callbacks);

static void cleanup();
static bt_status_t update_active_device(const RawAddress& bd_addr, uint16_t profile, uint16_t audio_type);
static bt_status_t set_content_control_id(uint16_t content_control_id, uint16_t audio_type);
static bool apm_enabled = false;
bool is_qclea_enabled();
bool is_aospLea_enabled();
#define CHECK_BTAPM_INIT()                                                   \
  do {                                                                       \
    if (!apm_enabled) {                                                  \
      BTIF_TRACE_WARNING("%s: BTAV not initialized", __func__);              \
      return BT_STATUS_NOT_READY;                                            \
    }                                                                        \
  } while (0)

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

uint16_t LeAudioContextToIntContentInAPM(LeAudioContextType context_type);
//LeAudioContextType priority_context = LeAudioContextType::SOUNDEFFECTS;

typedef enum {
  BTIF_APM_AUDIO_TYPE_VOICE = 0x0,
  BTIF_APM_AUDIO_TYPE_MEDIA,

  BTIF_APM_AUDIO_TYPE_SIZE
} btif_av_state_t;

typedef struct {
  RawAddress peer_bda;
  int profile;
} btif_apm_device_profile_combo_t;

typedef struct {
  RawAddress peer_bda;
} btif_apm_get_active_profile;

enum CONTEXT_PRIORITY {
  SONIFICATION = 0,
  MEDIA,
  GAME,
  CONVERSATIONAL
};

enum METADATA_TYPE {
  SOURCE = 0,
  SINK
};

//int active_profile_info;
std::map<RawAddress, int> active_profile_info_map;

static btif_apm_device_profile_combo_t active_device_profile[BTIF_APM_AUDIO_TYPE_SIZE];
static uint16_t content_control_id[BTIF_APM_AUDIO_TYPE_SIZE];

static void btif_update_active_device(uint16_t audio_type, char* param);
void btif_get_active_device(btif_av_state_t audio_type, RawAddress* peer_bda);
static void btif_update_content_control(uint16_t audio_type, char* param);
uint16_t btif_get_content_control_id(btif_av_state_t audio_type);

static void btif_update_active_device(uint16_t audio_type, char* param) {
  btif_apm_device_profile_combo_t new_device_profile;
  if(audio_type != BTIF_APM_AUDIO_TYPE_MEDIA)
    return;

  memcpy(&new_device_profile, param, sizeof(new_device_profile));
  active_device_profile[audio_type].peer_bda = new_device_profile.peer_bda;
  active_device_profile[audio_type].profile = new_device_profile.profile;
  BTIF_TRACE_WARNING("%s() New Active Device: %s, Profile: %x\n", __func__,
                active_device_profile[audio_type].peer_bda.ToString().c_str(),
                active_device_profile[audio_type].profile);
  if(active_device_profile[audio_type].profile == A2DP_PROFILE) {
    btif_ahim_update_current_profile(A2DP);
  } else if(active_device_profile[audio_type].profile == BROADCAST_LE) {
    btif_ahim_update_current_profile(BROADCAST);
  } else {
    btif_ahim_update_current_profile(AUDIO_GROUP_MGR);
  }
}

LeAudioContextType AudioContentToLeAudioContextInAPM(
    audio_content_type_t content_type,
    audio_source_t source_type, audio_usage_t usage) {
  LOG(INFO) << __func__ << ": usage: " << usage;
  switch (usage) {
    case AUDIO_USAGE_MEDIA:
      return LeAudioContextType::MEDIA;
    case AUDIO_USAGE_VOICE_COMMUNICATION:
      return LeAudioContextType::CONVERSATIONAL;
    case AUDIO_USAGE_CALL_ASSISTANT:
      return LeAudioContextType::CONVERSATIONAL;
    case AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
      return LeAudioContextType::VOICEASSISTANTS;
    case AUDIO_USAGE_ASSISTANCE_SONIFICATION:
      return LeAudioContextType::SOUNDEFFECTS;
    case AUDIO_USAGE_GAME:
      return LeAudioContextType::GAME;
    case AUDIO_USAGE_NOTIFICATION:
      return LeAudioContextType::NOTIFICATIONS;
    case AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE:
      return LeAudioContextType::CONVERSATIONAL;
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
      return LeAudioContextType::CONVERSATIONAL;
    default:
      break;
  }

  LOG(INFO) << __func__ << ": Return Media when not in call by default.";
  return LeAudioContextType::MEDIA;
}

uint16_t LeAudioContextToIntContentInAPM(LeAudioContextType context_type) {
  switch (context_type) {
    case LeAudioContextType::MEDIA:
      return bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
    case LeAudioContextType::GAME:
      return bluetooth::bap::ucast::CONTENT_TYPE_GAME;
    case LeAudioContextType::CONVERSATIONAL: //Fall through
      return bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
    case LeAudioContextType::LIVE:
      return bluetooth::bap::ucast::CONTENT_TYPE_LIVE;
    case LeAudioContextType::RINGTONE:
      return bluetooth::bap::ucast::CONTENT_TYPE_RINGTONE;
    case LeAudioContextType::VOICEASSISTANTS:
      return bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
    case LeAudioContextType::SOUNDEFFECTS:
      return bluetooth::bap::ucast::CONTENT_TYPE_SOUND_EFFECTS;
    case LeAudioContextType::ALERTS:
      return bluetooth::bap::ucast::CONTENT_TYPE_ALERT;
    case LeAudioContextType::EMERGENCYALARM:
      return bluetooth::bap::ucast::CONTENT_TYPE_EMERGENCY;
    default:
      return bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
      break;
  }
  return 0;
}

int getPriority(LeAudioContextType context) {
  LOG(INFO) << __func__ << ": context type = "<<(uint16_t)context;
  switch (context) {
    case LeAudioContextType::MEDIA:
      return CONTEXT_PRIORITY::MEDIA;
    case LeAudioContextType::GAME:
      return CONTEXT_PRIORITY::GAME;
    case LeAudioContextType::CONVERSATIONAL:
      return CONTEXT_PRIORITY::CONVERSATIONAL;
    case LeAudioContextType::SOUNDEFFECTS:
      return CONTEXT_PRIORITY::SONIFICATION ;
    default:
      break;
  }
  return 0;
}

void btif_get_active_device(btif_av_state_t audio_type, RawAddress* peer_bda) {
  if(audio_type >= BTIF_APM_AUDIO_TYPE_SIZE)
    return;
  peer_bda = &active_device_profile[audio_type].peer_bda;
}

static void btif_update_content_control(uint16_t audio_type, char* param) {
  if(audio_type >= BTIF_APM_AUDIO_TYPE_SIZE)
    return;
  uint16_t cc_id = (uint16_t)(*param);
  content_control_id[audio_type] = cc_id;
  /*Update ACM here*/
}

uint16_t btif_get_content_control_id(btif_av_state_t audio_type) {
  if(audio_type >= BTIF_APM_AUDIO_TYPE_SIZE)
    return 0;
  return content_control_id[audio_type];
}

static const bt_apm_interface_t bt_apm_interface = {
    sizeof(bt_apm_interface_t),
    init,
    update_active_device,
    set_content_control_id,
    is_qclea_enabled,
    is_aospLea_enabled,
    cleanup,
};

const bt_apm_interface_t* btif_apm_get_interface(void) {
  BTIF_TRACE_EVENT("%s", __func__);
  return &bt_apm_interface;
}

static bt_status_t init(btapm_initiator_callbacks_t* callbacks) {
  BTIF_TRACE_EVENT("%s", __func__);
  callbacks_  = callbacks;
  apm_enabled = true;

  return BT_STATUS_SUCCESS;
}

static void cleanup() {
  BTIF_TRACE_EVENT("%s", __func__);
  apm_enabled = false;
}

static bt_status_t update_active_device(const RawAddress& bd_addr, uint16_t profile, uint16_t audio_type) {
  BTIF_TRACE_EVENT("%s", __func__);
  CHECK_BTAPM_INIT();
  btif_apm_device_profile_combo_t new_device_profile;
  new_device_profile.peer_bda = bd_addr;
  new_device_profile.profile = profile;

  std::unique_lock<std::mutex> guard(apm_mutex);

  return btif_transfer_context(btif_update_active_device, (uint8_t)audio_type,
            (char *)&new_device_profile, sizeof(btif_apm_device_profile_combo_t), NULL);
}

static bt_status_t set_content_control_id(uint16_t content_control_id, uint16_t audio_type) {
  BTIF_TRACE_EVENT("%s", __func__);
  CHECK_BTAPM_INIT();

  std::unique_lock<std::mutex> guard(apm_mutex);

  return btif_transfer_context(btif_update_content_control,
     (uint8_t)audio_type, (char *)&content_control_id, sizeof(content_control_id), NULL);
}

void call_active_profile_info(const RawAddress& bd_addr, uint16_t audio_type) {
  if (apm_enabled == true) {
     BTIF_TRACE_WARNING("%s", __func__);
     int active_profile_info = callbacks_->active_profile_cb(bd_addr, audio_type);
     BTIF_TRACE_WARNING("%s: profile info is %d", __func__, active_profile_info);
     std::map<RawAddress, int>::iterator iter = active_profile_info_map.find(bd_addr);
     if (iter != active_profile_info_map.end()) {
       iter->second = active_profile_info;
     } else {
       active_profile_info_map.insert(std::make_pair(bd_addr, active_profile_info));
     }
  }
}

int get_active_profile(const RawAddress& bd_addr, uint16_t audio_type) {
  if (apm_enabled == true) {
     std::map<RawAddress, int>::iterator iter = active_profile_info_map.find(bd_addr);
     if (iter != active_profile_info_map.end()) {
       int active_profile_info = iter->second;
       BTIF_TRACE_WARNING("%s: active profile is %d ", __func__, active_profile_info);
       return active_profile_info;
     } else {
       BTIF_TRACE_WARNING("%s: Active profile has not been fetched, returning 0", __func__);
       return 0;
     }
  }
  else {
     BTIF_TRACE_WARNING("%s: APM is not enabled, returning HFP as active profile %d ",
                                     __func__, ACTIVE_VOICE_PROFILE_HFP);
     return ACTIVE_VOICE_PROFILE_HFP;
  }
}

void metadata_update_apm_jni(uint16_t context) {
  LOG(INFO) << __func__ ;
  callbacks_->update_metadata_cb(context);
}

void set_latency_mode_apm_jni(bool is_low_latency) {
  LOG(INFO) << __func__ ;
  callbacks_->set_latency_mode_cb(is_low_latency);
}

int context_contention_src(const source_metadata_t& source_metadata) {
  BTIF_TRACE_WARNING("%s: Choosing Context by Priority", __func__);
  auto tracks = source_metadata.tracks;
  auto track_count = source_metadata.track_count;
  LeAudioContextType current_context = LeAudioContextType::MEDIA;
  auto current_priority = -1;

  LOG(INFO) << __func__ << ": tracks count: " << track_count;
  if(!track_count) {
    return 0;
  }

  while (track_count) {
    auto context_priority = 0;
    if (tracks->content_type == 0 && tracks->usage == 0) {
      --track_count;
      LOG(INFO) << __func__ << ": tracks count: " << track_count;
      ++tracks;
      continue;
    }

    LOG(INFO) << __func__
              << ": usage=" << tracks->usage
              << ", content_type=" << tracks->content_type
              << ", gain=" << tracks->gain;
    LeAudioContextType context_type = AudioContentToLeAudioContextInAPM(tracks->content_type,
                                                    AUDIO_SOURCE_DEFAULT, tracks->usage);
    context_priority = getPriority(context_type);
    if (context_priority > current_priority)
      {
        current_priority = context_priority;
        current_context = context_type;
      }
    --track_count;
    ++tracks;
  }
  uint16_t val = LeAudioContextToIntContentInAPM(current_context);
  return val;
}

void btif_report_a2dp_src_metadata_update(const source_metadata_t& source_metadata) {
  if (apm_enabled == true) {
    BTIF_TRACE_WARNING("%s: Update Metadata in APM", __func__);
    do_in_jni_thread(FROM_HERE, Bind(&metadata_update_apm_jni,
                                context_contention_src(source_metadata)));
  }
}

void btif_report_a2dp_snk_metadata_update(const sink_metadata_t& sink_metadata) {
  if (apm_enabled == true) {
    BTIF_TRACE_WARNING("%s: Update Metadata in APM", __func__);
    LeAudioContextType context_type = AudioContentToLeAudioContextInAPM(
                                          AUDIO_CONTENT_TYPE_UNKNOWN, sink_metadata.tracks->source,
                                          AUDIO_USAGE_UNKNOWN);
    uint16_t context = LeAudioContextToIntContentInAPM(context_type);
    BTIF_TRACE_WARNING("%s: Context Type: %d", __func__,context);
    do_in_jni_thread(FROM_HERE, Bind(&metadata_update_apm_jni, context));
  }
}

void btif_apm_set_latency_mode(bool is_low_latency) {
  if (apm_enabled == true) {
    BTIF_TRACE_WARNING("%s: Set Latency Mode in APM", __func__);
    do_in_jni_thread(FROM_HERE, Bind(&set_latency_mode_apm_jni, is_low_latency));
  }
}

bool is_qclea_enabled() {
  // call the AHIM API to get hal enabled Status
  BTIF_TRACE_EVENT("%s", __func__);
  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    BTIF_TRACE_EVENT("%s: false ", __func__);
    return false;
  } else if (btif_ahim_is_qc_lea_enabled()) {
    BTIF_TRACE_EVENT("%s: true ", __func__);
    return true;
  }
  return false;
}

bool is_aospLea_enabled() {
  // call the AHIM API to get hal enabled Status
  BTIF_TRACE_EVENT("%s", __func__);
  if(btif_ahim_is_aosp_aidl_hal_enabled()) {
    BTIF_TRACE_EVENT("%s: true ", __func__);
    return true;
  } else if (btif_ahim_is_qc_lea_enabled()) {
    BTIF_TRACE_EVENT("%s: false ", __func__);
    return false;
  }
  return false;
}
