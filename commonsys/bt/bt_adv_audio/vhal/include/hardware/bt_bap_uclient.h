/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 * Not a contribution
 */

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_INCLUDE_BT_BAP_UCLIENT_H
#define ANDROID_INCLUDE_BT_BAP_UCLIENT_H

#include <hardware/bluetooth.h>
#include <hardware/bt_av.h>
#include <hardware/bt_pacs_client.h>


namespace bluetooth {
namespace bap {
namespace ucast {

#define BT_PROFILE_BAP_UCLIENT_ID "bt_bap_uclient"

using bluetooth::bap::pacs::CodecConfig;

constexpr uint8_t ASE_DIRECTION_SINK           = 0x01 << 0;
constexpr uint8_t ASE_DIRECTION_SRC            = 0x01 << 1;

constexpr uint8_t LATENCY_LOW                  = 0x01;
constexpr uint8_t LATENCY_BALANCED             = 0x02;
constexpr uint8_t LATENCY_HIGH                 = 0x03;

// Content types
constexpr uint16_t CONTENT_TYPE_UNSPECIFIED    = 0x0001; // Unspecified
constexpr uint16_t CONTENT_TYPE_CONVERSATIONAL = 0x0002; // Conversational

// Media(music playback, radio, podcast or movie soundtrack, or tv audio)
constexpr uint16_t CONTENT_TYPE_NONE           = 0x0000;
constexpr uint16_t CONTENT_TYPE_MEDIA          = 0x0004;
constexpr uint16_t CONTENT_TYPE_GAME           = 0x0008; // Game Audio
constexpr uint16_t CONTENT_TYPE_INSTRUCTIONAL  = 0x0010; // Instructional

// ManMachine(with voice recognition or virtual assistants)
constexpr uint16_t CONTENT_TYPE_MAN_MACHINE    = 0x0020;
constexpr uint16_t CONTENT_TYPE_LIVE           = 0x0040; // Live audio

// Sound Effects(including keyboard and touch feedback;
// menu and user interface sounds; and other system sounds)
constexpr uint16_t CONTENT_TYPE_SOUND_EFFECTS  = 0x0080;

// Notification and reminder sounds; attention-seeking audio,
//for example, in beeps signaling the arrival of a message
constexpr uint16_t CONTENT_TYPE_NOTIFICATIONS  = 0x0100;
constexpr uint16_t CONTENT_TYPE_RINGTONE       = 0x0200; // Ringtone
constexpr uint16_t CONTENT_TYPE_ALERT          = 0x0400; // ImmediateAlert
constexpr uint16_t CONTENT_TYPE_EMERGENCY      = 0x0800; // EmergencyAlert

// Audio locations
constexpr uint32_t AUDIO_LOC_LEFT              = 0x0001;
constexpr uint32_t AUDIO_LOC_RIGHT             = 0x0002;
constexpr uint32_t AUDIO_LOC_CENTER            = 0x0004;

constexpr uint8_t  LE_2M_PHY             = 0x02;
constexpr uint8_t  LE_QHS_PHY            = 0x80;

typedef uint8_t sdu_interval_t[3];
typedef uint8_t presentation_delay_t[3];
typedef uint8_t codec_type_t[5];
typedef uint8_t codec_config[255];

struct CISConfig {
  uint8_t cis_id;
  uint16_t max_sdu_m_to_s;
  uint16_t max_sdu_s_to_m;
  uint8_t phy_m_to_s;
  uint8_t phy_s_to_m;
  uint8_t rtn_m_to_s;
  uint8_t rtn_s_to_m;
};

struct CIGConfig {
  uint8_t cig_id;
  uint8_t cis_count;
  uint8_t packing;
  uint8_t framing;
  uint16_t max_tport_latency_m_to_s;
  uint16_t max_tport_latency_s_to_m;
  sdu_interval_t sdu_interval_m_to_s;
  sdu_interval_t sdu_interval_s_to_m;
};

// For ATL and Latency calculation usage
struct CISEstablishedEvtParamas {
  uint8_t status;
  uint8_t cig_id;
  uint8_t cis_id;
  uint16_t connection_handle;
  uint32_t transport_latency_m_to_s;
  uint32_t transport_latency_s_to_m;
  uint8_t ft_m_to_s;
  uint8_t ft_s_to_m;
  uint16_t iso_interval;
};

struct CISLatencyChangedEvt {
  uint8_t status;
  uint16_t conn_handle;
  uint8_t ft_c_to_p;
  uint8_t ft_p_to_c;
  uint8_t nse;
};

struct ASCSConfig {
  uint8_t cig_id;
  uint8_t cis_id;
  uint8_t target_latency;
  bool bi_directional;
  presentation_delay_t presentation_delay;
};

struct QosConfig {
  CIGConfig cig_config;
  std::vector<CISConfig> cis_configs; // individual CIS configs
  std::vector<ASCSConfig> ascs_configs;
};

struct AltQosConfig {
  uint8_t config_id;
  QosConfig qos_config;
};

enum class AseState {
  IDLE = 0,
  CODEC_CONFIGURED,
  QOS_CONFIGURED,
  ENABLING,
  STREAMING,
  DISABLING,
  RELEASING,
};

enum class StreamState {
  DISCONNECTED = 0,
  CONNECTING,
  CONNECTED,
  STARTING,
  STREAMING,
  STOPPING,
  DISCONNECTING,
  RECONFIGURING,
  UPDATING
};

enum class DeviceState {
  DISCONNECTED = 0,
  CONNECTING,
  CONNECTED
};

enum class StreamDiscReason {
  REASON_NONE,
  REASON_USER_DISC
};

struct CodecQosConfig {
  CodecConfig codec_config;
  QosConfig qos_config;
  std::vector<AltQosConfig> alt_qos_configs;
};

struct StreamType {
  uint16_t type;
  uint16_t audio_context;
  uint8_t  direction;
};

struct StreamConnect {
  StreamType stream_type;
  //CCID_List ccids; //TODO
  std::vector<CodecQosConfig> codec_qos_config_pair;
};

enum class StreamReconfigType {
  CODEC_CONFIG,
  QOS_CONFIG,
  APP_TRIGGERED_CONFIG
};

struct StreamReconfig {
  StreamType stream_type;
  StreamReconfigType reconf_type;
  std::vector<CodecQosConfig> codec_qos_config_pair;
};

enum class StreamUpdateType {
  STREAMING_CONTEXT,
  ENCODER_MODE_UPDATE,
};

struct StreamUpdate {
  StreamType stream_type;
  StreamUpdateType update_type;
  uint16_t update_value;
};

struct StreamLinkParams {
  uint8_t status;
  uint8_t cig_id;
  uint8_t cis_id;
  uint16_t connection_handle;
  uint32_t transport_latency_m_to_s;
  uint32_t transport_latency_s_to_m;
  uint8_t ft_m_to_s;
  uint8_t ft_s_to_m;
  uint16_t iso_interval;
};

struct StreamStateInfo {
  StreamType stream_type;
  StreamState stream_state;
  StreamDiscReason reason;
  StreamLinkParams stream_link_params;
};

struct StreamConfigInfo {
  StreamType stream_type;
  CodecConfig codec_config; // codec
  uint32_t audio_location; // location info of remote dev for the stream
  QosConfig qos_config; // current CIG, CISs configuration
  std::vector<CodecConfig> codecs_selectable; // pacs codec capabilities
};

class UcastClientCallbacks {
 public:
  virtual ~UcastClientCallbacks() = default;

  virtual void OnStreamState(const RawAddress &address,
                      std::vector<StreamStateInfo> streams_state_info) = 0;

  virtual void OnStreamConfig(const RawAddress &address,
                      std::vector<StreamConfigInfo> streams_config_info) = 0;

  virtual void OnStreamAvailable(const RawAddress &address,
                      uint16_t src_audio_contexts,
                      uint16_t sink_audio_contexts) = 0;

  virtual void OnStreamTimeOut(const RawAddress &address) = 0;
};

class UcastClientInterface {
 public:
  virtual ~UcastClientInterface() = default;

  /** Register the ucast client callbacks */
  virtual void Init(UcastClientCallbacks* callbacks) = 0;

  virtual void Connect(std::vector<RawAddress> &address, bool is_direct,
                       std::vector<StreamConnect> &streams) = 0;

  virtual void Disconnect(const RawAddress& address,
                       std::vector<StreamType> &streams) = 0;

  virtual void Start(std::vector<RawAddress> &address,
                     std::vector<StreamType> &streams,
                     bool is_mono_mic, bool mode) = 0;

  virtual void Stop(const RawAddress& address,
                    std::vector<StreamType> &streams, bool mode) = 0;

  virtual void Reconfigure(const RawAddress& address,
                           std::vector<StreamReconfig> &streams) = 0;

  virtual void UpdateStream(const RawAddress& address,
                            std::vector<StreamUpdate> &update_streams) = 0;

  /** Closes the interface. */
  virtual void Cleanup() = 0;
};

UcastClientInterface* btif_bap_uclient_get_interface();

}  // namespace ucast
}  // namespace bap
}  // namespace bluetooth

#endif /* ANDROID_INCLUDE_BT_BAP_UCLIENT_H */
