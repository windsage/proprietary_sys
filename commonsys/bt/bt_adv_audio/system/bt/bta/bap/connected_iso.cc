/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

#include "bta_bap_uclient_api.h"
#include "btm_int.h"
#include <list>
#include <sstream>
#include "state_machine.h"
#include "stack/include/btm_ble_api_types.h"
#include "bt_trace.h"
#include "btif_util.h"
#include "osi/include/properties.h"
#include "btif/include/btif_vmcp.h"
#include "device/include/controller.h"
#include "btm_api.h"

namespace bluetooth {
namespace bap {
namespace cis {

using bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
using bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
using bluetooth::bap::ucast::CONTENT_TYPE_GAME;
using bluetooth::bap::ucast::CONTENT_TYPE_LIVE;
using bluetooth::bap::ucast::CONTENT_TYPE_NONE;

std::map<CodecIndex, std::vector<uint8_t>>codecindex_codec_id_map = {
    {CodecIndex::CODEC_INDEX_SOURCE_LC3,              {0x06, 0x00, 0x00, 0x00, 0x00}},
    {CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE, {0xFF, 0x0A, 0x00, 0x01, 0x00}}, // TO-DO: 0x01 to 0xAD? Comments - remote has not done it yet
    {CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4, {0xFF, 0x0A, 0x00, 0xAD, 0x01}}
};

constexpr uint8_t  LTV_TYPE_CONN_HANDLE    = 0X06;
constexpr uint8_t  LTV_TYPE_CODEC_ID       = 0X07;
constexpr uint8_t  LTV_TYPE_VER_NUM        = 0X00;
constexpr uint8_t  LTV_TYPE_FREQ           = 0X01;
constexpr uint8_t  LTV_TYPE_USE_CASE       = 0X02;

constexpr uint8_t  LTV_LEN_CONN_HANDLE    = 0X03;
constexpr uint8_t  LTV_LEN_CODEC_ID       = 0X06;
constexpr uint8_t  LTV_LEN_VER_NUM        = 0X02;
constexpr uint8_t  LTV_LEN_FREQ           = 0X02;
constexpr uint8_t  LTV_LEN_USE_CASE       = 0X02;

// usecase values which need to be sent to controller
constexpr uint8_t  HQ_AUDIO_USE_CASE        = 0X00;
constexpr uint8_t  GAMING_NO_VBC_USE_CASE   = 0X01;
constexpr uint8_t  GAMING_VBC_USE_CASE      = 0X02;
constexpr uint8_t  VOICE_USE_CASE           = 0X03;
constexpr uint8_t  STEREO_REC_USE_CASE      = 0X04;
//constexpr uint8_t  BROADCAST_USE_CASE       = 0X05;

constexpr uint8_t  LTV_VAL_SAMP_FREQ_8K         = 0X01;
//constexpr uint8_t  LTV_VAL_SAMP_FREQ_11K        = 0X02;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_16K        = 0X03;
//constexpr uint8_t  LTV_VAL_SAMP_FREQ_22K        = 0X04;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_24K        = 0X05;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_32K        = 0X06;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_441K       = 0X07;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_48K        = 0X08;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_882K       = 0X09;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_96K        = 0X0A;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_176K       = 0X0B;
constexpr uint8_t  LTV_VAL_SAMP_FREQ_192K       = 0X0C;
//constexpr uint8_t  LTV_VAL_SAMP_FREQ_384K       = 0X0D;

constexpr uint8_t  LTV_TYPE_MIN_FT                      = 0X00;
constexpr uint8_t  LTV_TYPE_MIN_BIT_RATE                = 0X01;
constexpr uint8_t  LTV_TYPE_MIN_MAX_ERROR_RESILIENCE    = 0X02;
constexpr uint8_t  LTV_TYPE_LATENCY_MODE                = 0X03;
constexpr uint8_t  LTV_TYPE_MAX_FT                      = 0X04;

constexpr uint8_t  LTV_LEN_MIN_FT                       = 0X01;
constexpr uint8_t  LTV_LEN_MIN_BIT_RATE                 = 0X01;
constexpr uint8_t  LTV_LEN_MIN_MAX_ERROR_RESILIENCE     = 0X01;
constexpr uint8_t  LTV_LEN_LATENCY_MODE                 = 0X01;
constexpr uint8_t  LTV_LEN_MAX_FT                       = 0X01;

constexpr uint8_t  ENCODER_LIMITS_SUB_OP                = 0x24;

std::map<CodecSampleRate, uint8_t> freq_to_ltv_map = {
  {CodecSampleRate::CODEC_SAMPLE_RATE_8000,   LTV_VAL_SAMP_FREQ_8K   },
  {CodecSampleRate::CODEC_SAMPLE_RATE_16000,  LTV_VAL_SAMP_FREQ_16K  },
  {CodecSampleRate::CODEC_SAMPLE_RATE_24000,  LTV_VAL_SAMP_FREQ_24K  },
  {CodecSampleRate::CODEC_SAMPLE_RATE_32000,  LTV_VAL_SAMP_FREQ_32K  },
  {CodecSampleRate::CODEC_SAMPLE_RATE_44100,  LTV_VAL_SAMP_FREQ_441K },
  {CodecSampleRate::CODEC_SAMPLE_RATE_48000,  LTV_VAL_SAMP_FREQ_48K  },
  {CodecSampleRate::CODEC_SAMPLE_RATE_88200,  LTV_VAL_SAMP_FREQ_882K },
  {CodecSampleRate::CODEC_SAMPLE_RATE_96000,  LTV_VAL_SAMP_FREQ_96K  },
  {CodecSampleRate::CODEC_SAMPLE_RATE_176400, LTV_VAL_SAMP_FREQ_176K },
  {CodecSampleRate::CODEC_SAMPLE_RATE_192000, LTV_VAL_SAMP_FREQ_192K }
};

typedef struct {
  uint8_t status;
  uint16_t cis_handle;
  uint8_t reason;
} tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM;

typedef struct {
  uint8_t status;
  uint16_t conn_handle;
} tBTM_BLE_CIS_DATA_PATH_EVT_PARAM;

typedef struct {
  uint8_t status;
  uint8_t opcode;
} tBTM_BLE_SET_ENCODER_LIMITS_EVT_PARAM;

typedef struct {
  uint8_t status;
} tBTM_BLE_CONFIGURE_DATA_PATH_EVT_PARAM;

typedef struct {
uint8_t status;
uint8_t cig_id;
} tBTM_BLE_SET_CIG_REMOVE_PARAM;

struct CIS;
class CisInterfaceCallbacks;
using bluetooth::bap::cis::CisInterfaceCallbacks;

struct tIsoSetUpDataPath {
  uint16_t conn_handle;
  uint8_t data_path_direction;
  uint8_t data_path_id;
  uint16_t audio_context;
  uint8_t codec_config_length;
  uint8_t *codec_config;
};

struct tIsoRemoveDataPath {
  uint16_t conn_handle;
  uint8_t data_path_direction;
};

struct tConfigureDataPath {
  uint8_t data_path_direction;
  uint8_t data_path_id;
  uint8_t codec_config_length;
  /*
    Conn handle LTV
    Codec ID LTV
    Codec version number LTV
    Frequency LTV
    Usecase LTV
  */
  std::vector<uint8_t> codec_config;
};

typedef struct {
  uint8_t cig_id;
  uint8_t cis_id;
  std::vector<uint8_t> encoder_params;
  uint8_t encoder_mode;
} tBTM_BLE_SET_ENCODER_LIMITS_PARAM;

enum IsoHciEvent {
  CIG_CONFIGURE_REQ = 0,
  CIG_CONFIGURED_EVT,
  CIS_CREATE_REQ,
  CIS_STATUS_EVT,
  CIS_ESTABLISHED_EVT,
  CIS_DISCONNECT_REQ,
  CIS_DISCONNECTED_EVT,
  CIG_REMOVE_REQ,
  CIG_REMOVED_EVT,
  SETUP_DATA_PATH_REQ,
  SETUP_DATA_PATH_DONE_EVT,
  REMOVE_DATA_PATH_REQ,
  REMOVE_DATA_PATH_DONE_EVT,
  CIS_CREATE_REQ_DUMMY,
  UPDATE_ENCODER_LIMITS,
  CIG_MULTI_CONFIGS_ADDED_EVT,
  CONFIGURE_DATA_PATH_REQ,
  CONFIGURE_DATA_PATH_DONE_EVT
};

struct DataPathNode {
  IsoHciEvent type;
  union {
    tIsoSetUpDataPath setup_datapath;
    tIsoRemoveDataPath rmv_datapath;
  };
};

struct ConfigureDataPathNode {
  IsoHciEvent type;
  tConfigureDataPath config_datapath;
};

class CisStateMachine : public bluetooth::common::StateMachine {
 public:
  enum {
    kStateIdle,
    kStateSettingDataPath,
    kStateReady,
    kStateEstablishing,
    kStateDestroying,
    kStateEstablished,
  };

  class StateIdle : public State {
   public:
    StateIdle(CisStateMachine& sm)
        : State(sm, kStateIdle), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "Idle"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  class StateSettingDataPath : public State {
   public:
    StateSettingDataPath(CisStateMachine& sm)
        : State(sm, kStateSettingDataPath), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "SettingDataPath"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  class StateReady : public State {
   public:
    StateReady(CisStateMachine& sm)
        : State(sm, kStateReady), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "Ready"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  class StateDestroying : public State {
   public:
    StateDestroying(CisStateMachine& sm)
        : State(sm, kStateDestroying), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "Destroying"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  class StateEstablishing : public State {
   public:
    StateEstablishing(CisStateMachine& sm)
        : State(sm, kStateEstablishing), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "Establishing"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  class StateEstablished : public State {
   public:
    StateEstablished(CisStateMachine& sm)
        : State(sm, kStateEstablished), cis_(sm.GetCis()) {}
    void OnEnter() override;
    void OnExit() override;
    const char* GetState() { return "Established"; }
    bool ProcessEvent(uint32_t event, void* p_data) override;

   private:
    CIS &cis_;
  };

  CisStateMachine(CIS &cis) :
       cis(cis) {
    state_idle_ = new StateIdle(*this);
    state_setting_data_path_ = new StateSettingDataPath(*this);
    state_ready_ = new StateReady(*this);
    state_destroying_ = new StateDestroying(*this);
    state_establishing_ = new StateEstablishing(*this);
    state_established_ = new StateEstablished(*this);

    AddState(state_idle_);
    AddState(state_setting_data_path_);
    AddState(state_ready_);
    AddState(state_destroying_);
    AddState(state_establishing_);
    AddState(state_established_);

    SetInitialState(state_idle_);
  }

  CIS  &GetCis() { return cis; }

  const char* GetEventName(uint32_t event) {
    switch (event) {
      CASE_RETURN_STR(CIG_CONFIGURE_REQ)
      CASE_RETURN_STR(CIG_CONFIGURED_EVT)
      CASE_RETURN_STR(CIS_CREATE_REQ)
      CASE_RETURN_STR(CIS_STATUS_EVT)
      CASE_RETURN_STR(CIS_ESTABLISHED_EVT)
      CASE_RETURN_STR(CIS_DISCONNECT_REQ)
      CASE_RETURN_STR(CIS_DISCONNECTED_EVT)
      CASE_RETURN_STR(CIG_REMOVE_REQ)
      CASE_RETURN_STR(CIG_REMOVED_EVT)
      CASE_RETURN_STR(SETUP_DATA_PATH_REQ)
      CASE_RETURN_STR(SETUP_DATA_PATH_DONE_EVT)
      CASE_RETURN_STR(REMOVE_DATA_PATH_REQ)
      CASE_RETURN_STR(REMOVE_DATA_PATH_DONE_EVT)
      CASE_RETURN_STR(CIS_CREATE_REQ_DUMMY)
      CASE_RETURN_STR(UPDATE_ENCODER_LIMITS)
      CASE_RETURN_STR(CONFIGURE_DATA_PATH_REQ)
      CASE_RETURN_STR(CONFIGURE_DATA_PATH_DONE_EVT)
      default:
       return "Unknown Event";
    }
  }

 private:
  CIS &cis;
  StateIdle *state_idle_;
  StateSettingDataPath *state_setting_data_path_;
  StateReady *state_ready_;
  StateDestroying *state_destroying_;
  StateEstablishing *state_establishing_;
  StateEstablished *state_established_;
};

struct CIS {
  uint8_t cig_id;
  uint8_t cis_id;
  uint16_t cis_handle;
  bool to_air_setup_done;
  bool from_air_setup_done;
  uint8_t datapath_status;
  uint8_t disc_direction;
  uint8_t direction; // input or output or both
  CISEstablishedEvtParamas cis_params;
  CisInterfaceCallbacks *cis_callback;
  RawAddress peer_bda;
  CISConfig cis_config;
  CisStateMachine cis_sm;
  CisState cis_state;
  std::list <DataPathNode> datapath_queue;
  CodecConfig to_air_codec_config;
  uint16_t to_air_audio_context;
  CodecConfig from_air_codec_config;
  uint16_t from_air_audio_context;

  CIS(uint8_t cig_id, uint8_t cis_id, uint8_t direction,
      CisInterfaceCallbacks* callback):
      cig_id(cig_id), cis_id(cis_id), direction(direction),
      cis_callback(callback),
      cis_sm(*this) {
      to_air_setup_done = false;
      from_air_setup_done = false;
  }
};

struct CreateCisNode {
  uint8_t cig_id;
  std::vector<uint8_t> cis_ids;
  std::vector<uint16_t> cis_handles;
  std::vector<RawAddress> peer_bda;
  bool mode;
};

struct CIG {
  CIGConfig cig_config;
  CigState cig_state;
  std::map<RawAddress, uint8_t> clients_list; // address and count
  std::map<uint8_t, CIS *> cis_list; // cis id to CIS
  std::vector<AltQosConfig> alt_qos_configs;
};

class CisInterfaceImpl;
CisInterfaceImpl *instance;
std::map<uint8_t, CIG *> cig_list_backup; // CIG backup

static void hci_cig_param_callback(tBTM_BLE_SET_CIG_RET_PARAM *param);
static void hci_cig_param_test_callback(tBTM_BLE_SET_CIG_PARAM_TEST_RET *param);
static void hci_cig_add_multi_configs_callback(tBTM_BLE_ADD_CIG_MULTI_CONFIGS_RET_PARAM *param);
static void hci_cig_remove_param_callback(uint8_t status, uint8_t cig_id);
static void hci_cis_create_status_callback( uint8_t status);
static void hci_cis_create_callback(tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *param);
static void hci_cis_setup_datapath_callback( uint8_t status,
                                              uint16_t conn_handle);
static void hci_cis_disconnect_callback(uint8_t status, uint16_t cis_handle,
                                         uint8_t reason);
static void btm_ble_set_encoder_limits_features_status_callback (tBTM_VSC_CMPL *param);
static void hci_cis_configure_datapath_callback(uint8_t status);

void CisStateMachine::StateIdle::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
}

void CisStateMachine::StateIdle::OnExit() {

}

static uint8_t audioContextToUseCase(uint16_t to_air_audio_context, uint16_t from_air_audio_context) {
    if(to_air_audio_context == CONTENT_TYPE_MEDIA)
        return HQ_AUDIO_USE_CASE;
    if(from_air_audio_context == CONTENT_TYPE_LIVE)
        return STEREO_REC_USE_CASE;
    if(to_air_audio_context == CONTENT_TYPE_GAME) {
        if(from_air_audio_context == CONTENT_TYPE_GAME)
            return GAMING_VBC_USE_CASE;
        else
            return GAMING_NO_VBC_USE_CASE;
    }
    if(to_air_audio_context == CONTENT_TYPE_CONVERSATIONAL
          && from_air_audio_context == CONTENT_TYPE_CONVERSATIONAL)
        return VOICE_USE_CASE;

    return HQ_AUDIO_USE_CASE; // [TBD]
}

uint8_t* PrepareSetEncoderLimitsPayload(tBTM_BLE_SET_ENCODER_LIMITS_PARAM *params,
                                        uint8_t *length, uint8_t *p) {
  uint8_t param_len = 0;
  uint8_t size = params->encoder_params.size();
  uint8_t num_limits = (size == 0) ? 1 : size;
  LOG(INFO) <<__func__  << "num_limits = "<<loghex(num_limits);
  LOG(INFO) <<__func__  << "params->cig_id = "<<loghex(params->cig_id);
  LOG(INFO) <<__func__  << "params->cis_id = "<<loghex(params->cis_id);
  UINT8_TO_STREAM(p, ENCODER_LIMITS_SUB_OP); //sub-opcode
  param_len++;
  UINT8_TO_STREAM(p, params->cig_id);
  param_len++;
  UINT8_TO_STREAM(p, params->cis_id);
  param_len++;
  UINT8_TO_STREAM(p, num_limits); //numlimits
  param_len++;

  if (params->encoder_params.empty()) {
    LOG(INFO) <<__func__  << " Call for mode update ";
    UINT8_TO_STREAM(p, LTV_TYPE_LATENCY_MODE);
    param_len++;
    UINT8_TO_STREAM(p, LTV_LEN_LATENCY_MODE);
    param_len++;
    UINT8_TO_STREAM(p, params->encoder_mode);
    param_len++;
    *length = param_len;
    LOG(INFO) <<__func__  << "param_len = "<<loghex(param_len);
    return p;
  }
  // Make a loop for this becaue encoder limits not fixed
  UINT8_TO_STREAM(p, LTV_TYPE_MIN_FT);
  param_len++;
  UINT8_TO_STREAM(p, LTV_LEN_MIN_FT);
  param_len++;
  UINT8_TO_STREAM(p, params->encoder_params[0]);
  param_len++;
  UINT8_TO_STREAM(p, LTV_TYPE_MIN_BIT_RATE);
  param_len++;
  UINT8_TO_STREAM(p, LTV_LEN_MIN_BIT_RATE);
  param_len++;
  UINT8_TO_STREAM(p, params->encoder_params[2]);
  param_len++;
  UINT8_TO_STREAM(p, LTV_TYPE_MIN_MAX_ERROR_RESILIENCE);
  param_len++;
  UINT8_TO_STREAM(p, LTV_LEN_MIN_MAX_ERROR_RESILIENCE);
  param_len++;
  UINT8_TO_STREAM(p, params->encoder_params[3]);
  param_len++;
  UINT8_TO_STREAM(p, LTV_TYPE_LATENCY_MODE);
  param_len++;
  UINT8_TO_STREAM(p, LTV_LEN_LATENCY_MODE);
  param_len++;
  UINT8_TO_STREAM(p, params->encoder_params[4]);
  param_len++;
  UINT8_TO_STREAM(p, LTV_TYPE_MAX_FT);
  param_len++;
  UINT8_TO_STREAM(p, LTV_LEN_MAX_FT);
  param_len++;
  UINT8_TO_STREAM(p, params->encoder_params[1]);
  param_len++;
  *length = param_len;
  LOG(INFO) <<__func__  << "param_len = "<<loghex(param_len);
  return p;
}

std::vector<uint8_t> PrepareCodecConfigPayload(CodecConfig *codec_config, uint16_t to_air_audio_context,
                                               uint16_t from_air_audio_context, uint8_t direction) {
    LOG(INFO) << __func__  << ": to_air_audio_context: " << to_air_audio_context
                           << ": from_air_audio_context: " << from_air_audio_context
                           << ": direction: " << direction;
    std::vector<uint8_t> codec_type;
    uint8_t version_number = 0;
    if(direction == DIR_TO_AIR) {
      LOG(INFO) << __func__  << ": direction is to air";
      CodecIndex codec_index = codec_config->codec_type;
      if(codec_index == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
        LOG(INFO) << __func__  << ": Codec type is Aptx Adaptive LE";
        version_number = GetVendorMetaDataCodecVerAptx(codec_config);
      } else {
        LOG(INFO) << __func__  << ": Codec type is Not Aptx Adaptive LE";
        version_number = GetVendorMetaDataCodecEncoderVer(codec_config);
      }
      LOG(INFO) << __func__  << ": version_number: " << version_number;
    } else if(direction == DIR_FROM_AIR) {
      LOG(INFO) << __func__  << ": direction is from air";
      version_number = GetVendorMetaDataCodecDecoderVer(codec_config);
    }
    LOG(INFO) << __func__  << " version_number: " << version_number;
    uint8_t len = LTV_LEN_VER_NUM;
    uint8_t type = LTV_TYPE_VER_NUM;
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    codec_type.insert(codec_type.end(), &version_number, &version_number + 1);

    for (auto it : freq_to_ltv_map) {
      if(codec_config->sample_rate == it.first) {
        len = LTV_LEN_FREQ;
        type = LTV_TYPE_FREQ;
        uint8_t sample_rate = it.second;
        codec_type.insert(codec_type.end(), &len, &len + 1);
        codec_type.insert(codec_type.end(), &type, &type + 1);
        codec_type.insert(codec_type.end(), &sample_rate, &sample_rate + 1);
        break;
      }
    }
    len = LTV_LEN_USE_CASE;
    type = LTV_TYPE_USE_CASE;
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    uint8_t usecase = audioContextToUseCase(to_air_audio_context, from_air_audio_context);
    codec_type.insert(codec_type.end(), &usecase, &usecase + 1);
    return codec_type;
}

std::vector<uint8_t> PrepareCodecConfigPayloadForConfigDatapath(CodecConfig *codec_config,
                                               uint16_t to_air_audio_context,
                                               uint16_t from_air_audio_context, uint8_t direction,
                                               uint16_t conn_handle) {
    LOG(INFO) << __func__  << ": to_air_audio_context: " << to_air_audio_context
                           << ": from_air_audio_context: " << from_air_audio_context
                           << ": codec_config->codec_type: " << uint8_t(codec_config->codec_type)
                           << ": direction: " << direction
                           << ": conn_handle:" << loghex(conn_handle);
    std::vector<uint8_t> codec_type;

    // Populate connection handle
    uint8_t len = LTV_LEN_CONN_HANDLE;
    uint8_t type = LTV_TYPE_CONN_HANDLE;
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    codec_type.insert(codec_type.end(), conn_handle);
    codec_type.insert(codec_type.end(), conn_handle << 8);

    // Populate codec ID
    len = LTV_LEN_CODEC_ID;
    type = LTV_TYPE_CODEC_ID;
    std::vector<uint8_t> codec_id = codecindex_codec_id_map[codec_config->codec_type];
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    codec_type.insert(codec_type.end(), &codec_id[0], &codec_id[0] + 5);

    // Populate codec version number
    uint8_t version_number = 0;
    if(direction == DIR_TO_AIR) {
      LOG(INFO) << __func__  <<": direction is to air ";
      CodecIndex codec_index = codec_config->codec_type;
      if(codec_index == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
        LOG(INFO) << __func__  << ": Codec type is Aptx Adaptive LE";
        version_number = GetVendorMetaDataCodecVerAptx(codec_config);
      } else {
        LOG(INFO) << __func__  << ": Codec type is Not Aptx Adaptive LE";
        version_number = GetVendorMetaDataCodecEncoderVer(codec_config);
      }
    } else if(direction == DIR_FROM_AIR) {
      LOG(INFO) << __func__  <<": direction is from air";
      version_number = GetVendorMetaDataCodecDecoderVer(codec_config);
    }
    LOG(INFO) << __func__  << " version_number: " << version_number;
    len = LTV_LEN_VER_NUM;
    type = LTV_TYPE_VER_NUM;
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    codec_type.insert(codec_type.end(), &version_number, &version_number + 1);

    // Populate frequency
    for (auto it : freq_to_ltv_map) {
      if(codec_config->sample_rate == it.first) {
        len = LTV_LEN_FREQ;
        type = LTV_TYPE_FREQ;
        uint8_t sample_rate = it.second;
        codec_type.insert(codec_type.end(), &len, &len + 1);
        codec_type.insert(codec_type.end(), &type, &type + 1);
        codec_type.insert(codec_type.end(), &sample_rate, &sample_rate + 1);
        break;
      }
    }

    // Populate usecase number
    len = LTV_LEN_USE_CASE;
    type = LTV_TYPE_USE_CASE;
    codec_type.insert(codec_type.end(), &len, &len + 1);
    codec_type.insert(codec_type.end(), &type, &type + 1);
    uint8_t usecase = audioContextToUseCase(to_air_audio_context, from_air_audio_context);
    codec_type.insert(codec_type.end(), &usecase, &usecase + 1);
    return codec_type;
}

bool checkIfPayloadRequired(CodecConfig *to_air_codec_config, CodecConfig *from_air_codec_config,
                            uint16_t to_air_audio_context, uint16_t from_air_audio_context, uint8_t direction) {
  LOG(INFO) << __func__;
  bool send_payload = false;
  CodecIndex codec_type = to_air_codec_config->codec_type;
  bool direction_to_air = ((direction & DIR_TO_AIR) != 0);
  bool direction_from_air = ((direction & DIR_FROM_AIR) != 0);
  bool is_game_usecase = (to_air_audio_context == CONTENT_TYPE_GAME) && direction_to_air;
  LOG(INFO) <<__func__  <<": is_game_usecase = " << loghex(is_game_usecase);
  bool is_game_vbc = (from_air_audio_context == CONTENT_TYPE_GAME) && direction_from_air;
  LOG(INFO) <<__func__  <<": is_game_vbc = " << loghex(is_game_vbc);
  switch (codec_type) {
    case CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE:
      if (direction_to_air && to_air_audio_context == CONTENT_TYPE_MEDIA)
        send_payload = true;
      break;
    case CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4:
      if (direction_to_air && to_air_audio_context == CONTENT_TYPE_MEDIA)
        send_payload = true;
      break;
    case CodecIndex::CODEC_INDEX_SOURCE_LC3: {
      uint8_t negotiated_version = GetVendorMetaDataCodecEncoderVer(to_air_codec_config);
      LOG(INFO) <<__func__  <<": negotiated_version = " << loghex(negotiated_version);
      bool is_ft_change_encoder_version = (negotiated_version == LC3Q_CODEC_FT_CHANGE_SUPPORTED_VERSION);
      if (is_ft_change_encoder_version && (is_game_usecase || is_game_vbc)) {
        send_payload = true;
      }
    } break;
    default:
      send_payload = false;
      break;
  }
  LOG(INFO) << __func__  <<": send_iso_codec config payload " << send_payload;
  return send_payload;
}

bool CisStateMachine::StateIdle::ProcessEvent(uint32_t event, void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  bool cis_status = true;
  switch (event) {
    case SETUP_DATA_PATH_REQ: {
      tIsoSetUpDataPath *data_path_info = (tIsoSetUpDataPath *) p_data;
      std::vector<uint8_t> codec_type;
      tBTM_BLE_SET_ISO_DATA_PATH_PARAM p_params;
      p_params.conn_handle = cis_.cis_handle;
      p_params.data_path_dir = data_path_info->data_path_direction >> 1;
      p_params.data_path_id = data_path_info->data_path_id;

      uint16_t audio_context_temp = 0;
      CodecConfig codec_config_temp;

      LOG(INFO) << __func__ << ": state = idle SETUP_DATA_PATH_REQ, cis_.to_air_audio_context = "
                            <<cis_.to_air_audio_context << " data_path_info->data_path_direction = "
                            <<loghex(data_path_info->data_path_direction);
      // payload needed when lc3 ver 2 for gaming/gaming+vbc and aptx for music
      if(data_path_info->data_path_direction == DIR_TO_AIR
          && checkIfPayloadRequired(&cis_.to_air_codec_config, &cis_.from_air_codec_config
                                    , cis_.to_air_audio_context, cis_.from_air_audio_context
                                    , data_path_info->data_path_direction)) {
        codec_type = PrepareCodecConfigPayload(&cis_.to_air_codec_config,
                                  cis_.to_air_audio_context,
                                  cis_.from_air_audio_context,
                                  data_path_info->data_path_direction);
        LOG(INFO) << __func__ << ": state = idle SETUP_DATA_PATH_REQ, direction is to air";
        p_params.codec_config_length = codec_type.size();
        if(p_params.codec_config_length != 0) {
          p_params.codec_config = &codec_type[0];
        }
      }
      else if(data_path_info->data_path_direction == DIR_FROM_AIR
              && checkIfPayloadRequired(&cis_.to_air_codec_config, &cis_.from_air_codec_config
                                        , cis_.to_air_audio_context, cis_.from_air_audio_context
                                        , data_path_info->data_path_direction)) {
        codec_type = PrepareCodecConfigPayload(&cis_.from_air_codec_config,
                                  cis_.to_air_audio_context,
                                  cis_.from_air_audio_context,
                                  data_path_info->data_path_direction);
        LOG(INFO) << __func__ << ": state = idle SETUP_DATA_PATH_REQ, direction is from air";
        p_params.codec_config_length = codec_type.size();
        if(p_params.codec_config_length != 0) {
          p_params.codec_config = &codec_type[0];
        }
      }
      else {
        p_params.codec_config_length = 0x00;
        p_params.codec_config = nullptr;
      }
      // now filling codec_id on basis of direction
      if(data_path_info->data_path_direction == DIR_TO_AIR) {
        std::vector<uint8_t> codec_id_temp = codecindex_codec_id_map[cis_.to_air_codec_config.codec_type];
        for (uint8_t i = 0; i < codec_id_temp.size(); i++) {
            p_params.codec_id[i] = codec_id_temp[i];
        }
      } else if(data_path_info->data_path_direction == DIR_FROM_AIR) {
        std::vector<uint8_t> codec_id_temp = codecindex_codec_id_map[cis_.from_air_codec_config.codec_type];
        for (uint8_t i = 0; i < codec_id_temp.size(); i++) {
            p_params.codec_id[i] = codec_id_temp[i];
        }
      }

      if(data_path_info->data_path_direction == DIR_TO_AIR) {
        audio_context_temp = cis_.to_air_audio_context;
        codec_config_temp = cis_.to_air_codec_config;
      } else if(data_path_info->data_path_direction == DIR_FROM_AIR) {
        audio_context_temp = cis_.from_air_audio_context;
        codec_config_temp = cis_.from_air_codec_config;
      }

      LOG(INFO) <<__func__  <<":  cig id, cis id = " <<loghex(cis_.cig_id) <<" "<<loghex(cis_.cis_id);

      memset(&p_params.cont_delay, 0x00, sizeof(p_params.cont_delay));

      p_params.p_cb = &hci_cis_setup_datapath_callback;
      if(BTM_BleSetIsoDataPath(&p_params) == HCI_SUCCESS) {
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateSettingDataPath);
        DataPathNode node = {
                             .type = SETUP_DATA_PATH_REQ,
                             .setup_datapath = {
                               .conn_handle = cis_.cis_handle,
                               .data_path_direction  =
                                    data_path_info->data_path_direction,
                               .data_path_id = data_path_info->data_path_id,
                               .audio_context = audio_context_temp,
                               .codec_config_length = uint8_t(codec_type.size()),
                             },
                            };
        if(node.setup_datapath.codec_config_length != 0) {
          node.setup_datapath.codec_config = &codec_type[0];
        }
        cis_.datapath_queue.push_back(node);
        LOG(INFO) << __func__
                  << ": cig id, cis id = " <<loghex(cis_.cig_id) <<" "<<loghex(cis_.cis_id)
                  <<" queue size = "<<loghex(size(cis_.datapath_queue));
      }
    } break;

    case CIS_CREATE_REQ: {
      char bap_pts[PROPERTY_VALUE_MAX] = "false";
      property_get("persist.vendor.btstack.bap_pts", bap_pts, "false");
      if (!strncmp("true", bap_pts, 4)) {
        LOG(INFO) <<__func__ <<": bap pts test prop enabled";
        tBTM_BLE_ISO_CREATE_CIS_CMD_PARAM cmd_data;
        CreateCisNode *pNode = (CreateCisNode *) p_data;
        cmd_data.cis_count = pNode->cis_ids.size();
        cmd_data.p_cb = &hci_cis_create_status_callback;
        cmd_data.p_evt_cb = &hci_cis_create_callback;

        for(uint8_t i = 0; i < pNode->cis_handles.size() ; i++) {
          tACL_CONN* acl = btm_bda_to_acl(
              pNode->peer_bda.at(i % pNode->peer_bda.size()), BT_TRANSPORT_LE);
          if(!acl) {
            BTIF_TRACE_DEBUG("%s create_cis return ", __func__);
            continue;
          }
          tBTM_BLE_CHANNEL_MAP map = {
                                     .cis_conn_handle = pNode->cis_handles.at(i),
                                     .acl_conn_handle = acl->hci_handle
                                     };
          cmd_data.link_conn_handles.push_back(map);
        }

        if(BTM_BleCreateCis(&cmd_data,
                            &hci_cis_disconnect_callback) == HCI_SUCCESS) {
          cis_.cis_sm.TransitionTo(CisStateMachine::kStateEstablishing);
        }
      }
    } break;
    case CIS_CREATE_REQ_DUMMY: {
      char bap_pts[PROPERTY_VALUE_MAX] = "false";
      property_get("persist.vendor.btstack.bap_pts", bap_pts, "false");
      if (!strncmp("true", bap_pts, 4)) {
        LOG(INFO) <<__func__ <<": bap pts test prop enabled";
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateEstablishing);
      }
    } break;
    default:
      cis_status = false;
      break;
  }
  return cis_status;
}

void CisStateMachine::StateSettingDataPath::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
}

void CisStateMachine::StateSettingDataPath::OnExit() {

}

bool CisStateMachine::StateSettingDataPath::ProcessEvent(uint32_t event,
                                                         void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  bool cis_status = true;
  switch (event) {
    case SETUP_DATA_PATH_REQ: {
      // add them to the queue
      tIsoSetUpDataPath *data_path_info = (tIsoSetUpDataPath *) p_data;
      std::vector<uint8_t> codec_type;
      LOG(INFO) << __func__
                << ": state = settingdatapath, SETUP_DATA_PATH_REQ, cis_.to_air_audio_context = "
                <<cis_.to_air_audio_context<<" data_path_info->data_path_direction = "
                <<loghex(data_path_info->data_path_direction);
      uint16_t audio_context_temp = 0;
      CodecConfig codec_config_temp;

      // payload needed when lc3 ver 2 for gaming/gaming+vbc and aptx for music
      if(data_path_info->data_path_direction == DIR_TO_AIR
           && checkIfPayloadRequired(&cis_.to_air_codec_config, &cis_.from_air_codec_config
                                     , cis_.to_air_audio_context, cis_.from_air_audio_context
                                     , data_path_info->data_path_direction)) {
        codec_type = PrepareCodecConfigPayload(&cis_.to_air_codec_config,
                                  cis_.to_air_audio_context,
                                  cis_.from_air_audio_context,
                                  data_path_info->data_path_direction);
      }
      else if(data_path_info->data_path_direction == DIR_FROM_AIR
                && checkIfPayloadRequired(&cis_.to_air_codec_config, &cis_.from_air_codec_config
                                          , cis_.to_air_audio_context, cis_.from_air_audio_context
                                          , data_path_info->data_path_direction)) {
        codec_type = PrepareCodecConfigPayload(&cis_.from_air_codec_config,
                                  cis_.to_air_audio_context,
                                  cis_.from_air_audio_context,
                                  data_path_info->data_path_direction);
      }
      if(data_path_info->data_path_direction == DIR_TO_AIR) {
        audio_context_temp = cis_.to_air_audio_context;
        codec_config_temp = cis_.to_air_codec_config;
      }
      else if(data_path_info->data_path_direction == DIR_FROM_AIR) {
        audio_context_temp = cis_.from_air_audio_context;
        codec_config_temp = cis_.from_air_codec_config;
      }

      DataPathNode node = {
                           .type = SETUP_DATA_PATH_REQ,
                           .setup_datapath = {
                           .conn_handle =  cis_.cis_handle,
                           .data_path_direction  =
                                data_path_info->data_path_direction,
                            .data_path_id = data_path_info->data_path_id,
                            .audio_context = audio_context_temp,
                            .codec_config_length = uint8_t(codec_type.size()),
                           }
                          };
      if(node.setup_datapath.codec_config_length != 0) {
        node.setup_datapath.codec_config = &codec_type[0];
      }
      cis_.datapath_queue.push_back(node);
      LOG(INFO) << __func__
                << ": cig id, cis id = " <<loghex(cis_.cig_id) <<" "<<loghex(cis_.cis_id)
                <<" queue size = "<<loghex(size(cis_.datapath_queue));
    } break;

    case SETUP_DATA_PATH_DONE_EVT: {
      tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *param =
                  (tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *) p_data;
      cis_.datapath_status = param->status;

      if(!cis_.datapath_queue.empty()) {
        if(cis_.datapath_status == ISO_HCI_SUCCESS) {
          DataPathNode node = cis_.datapath_queue.front();
          if(node.type == SETUP_DATA_PATH_REQ) {
            uint8_t direction = node.setup_datapath.data_path_direction;
            if(direction == DIR_TO_AIR) {
              cis_.to_air_setup_done = true;
            } else if( direction == DIR_FROM_AIR) {
              cis_.from_air_setup_done = true;
            }
          }
        }
        // remove the entry as it is processed
        cis_.datapath_queue.pop_front();
      }

      // check if there are any more entries in queue now
      // expect the queue entry to be of setup datapath only
      if(!cis_.datapath_queue.empty()) {
        DataPathNode node = cis_.datapath_queue.front();
        if(node.type == SETUP_DATA_PATH_REQ) {
          tBTM_BLE_SET_ISO_DATA_PATH_PARAM p_params;
          p_params.conn_handle = node.setup_datapath.conn_handle;
          p_params.data_path_dir = node.setup_datapath.data_path_direction >> 1;
          p_params.data_path_id = node.setup_datapath.data_path_id;
          p_params.codec_id[0] = 0x06; //CHECK
          memset(&p_params.codec_id[1], 0x00, sizeof(p_params.codec_id) - 1);
          memset(&p_params.cont_delay, 0x00, sizeof(p_params.cont_delay));
          // p_params.codec_config_length = 0x00;
          // p_params.codec_config = nullptr;
          p_params.codec_config_length = node.setup_datapath.codec_config_length;
          if(node.setup_datapath.codec_config_length != 0)
            p_params.codec_config = &node.setup_datapath.codec_config[0];
          p_params.p_cb = &hci_cis_setup_datapath_callback;
          if (BTM_BleSetIsoDataPath(&p_params) != HCI_SUCCESS) {
            LOG(ERROR) << "Setup Datapath Failed";
            cis_.datapath_queue.pop_front();
            cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
          }
        } else {
          LOG(ERROR) << "Unexpected entry";
        }
      } else {
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
      }
    } break;

    default:
      cis_status = false;
      break;
  }
  return cis_status;
}


void CisStateMachine::StateReady::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
  // update the ready state incase of transitioned from states except
  // setting up datapath as CIG state event is sufficient for transition
  // from setting up data path to ready.
  if(cis_.cis_sm.PreviousStateId() != CisStateMachine::kStateSettingDataPath &&
     cis_.cis_callback != nullptr) {
    cis_.cis_callback->OnCisState(cis_.cig_id, cis_.cis_id,
                                  cis_.direction, cis_.cis_params,
                                  CisState::READY);
  }
}

void CisStateMachine::StateReady::OnExit() {

}

bool CisStateMachine::StateReady::ProcessEvent(uint32_t event, void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  bool cis_status = true;
  switch (event) {
    case CIS_CREATE_REQ: {
      tBTM_BLE_ISO_CREATE_CIS_CMD_PARAM cmd_data;
      CreateCisNode *pNode = (CreateCisNode *) p_data;
      cmd_data.cis_count = pNode->cis_ids.size();
      cmd_data.p_cb = &hci_cis_create_status_callback;
      cmd_data.p_evt_cb = &hci_cis_create_callback;
      for(uint8_t i = 0; i < pNode->cis_handles.size() ; i++) {
        tACL_CONN* acl = btm_bda_to_acl(
            pNode->peer_bda.at(i % pNode->peer_bda.size()), BT_TRANSPORT_LE);
        if(!acl) {
          BTIF_TRACE_DEBUG("%s create_cis return ", __func__);
          continue;
        }
        tBTM_BLE_CHANNEL_MAP map = {
                                   .cis_conn_handle = pNode->cis_handles.at(i),
                                   .acl_conn_handle = acl->hci_handle
                                   };
        cmd_data.link_conn_handles.push_back(map);
      }
      if(BTM_BleCreateCis(&cmd_data, &hci_cis_disconnect_callback)
                          == HCI_SUCCESS)
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateEstablishing);
    } break;

    case CIS_CREATE_REQ_DUMMY: {
      cis_.cis_sm.TransitionTo(CisStateMachine::kStateEstablishing);
    } break;

    default:
      cis_status = false;
      break;
  }
  return cis_status;
}


void CisStateMachine::StateDestroying::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
  if(cis_.cis_callback != nullptr) {
    cis_.cis_callback->OnCisState(cis_.cig_id, cis_.cis_id,
                                  cis_.direction, cis_.cis_params,
                                  CisState::DESTROYING);
  }
}

void CisStateMachine::StateDestroying::OnExit() {

}

bool CisStateMachine::StateDestroying::ProcessEvent(uint32_t event,
                                                    void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  bool cis_status = true;
  switch (event) {
    case CIS_DISCONNECTED_EVT: {
      tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *param =
                    (tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *) p_data;
      if(param->status != ISO_HCI_SUCCESS) {
        LOG(ERROR) <<__func__  << " cis disconnection failed";
        cis_.cis_sm.TransitionTo(cis_.cis_sm.PreviousStateId());
      } else {
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
      }
    } break;
    default:
      cis_status = false;
      break;
  }
  return cis_status;
}


void CisStateMachine::StateEstablishing::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
  if(cis_.cis_callback != nullptr) {
    cis_.cis_callback->OnCisState(cis_.cig_id, cis_.cis_id,
                                  cis_.direction, cis_.cis_params,
                                  CisState::ESTABLISHING);
  }
}

void CisStateMachine::StateEstablishing::OnExit() {

}

bool CisStateMachine::StateEstablishing::ProcessEvent(uint32_t event,
                                                      void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  bool cis_status = true;
  switch (event) {
    case CIS_STATUS_EVT: {
      uint8_t status = *((uint8_t *)(p_data));
      LOG(INFO) <<__func__  <<": CIS Create status = " << loghex(status);
      if(status != ISO_HCI_SUCCESS) {
        cis_.cis_params.status = status;
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
      }
    } break;
    case CIS_ESTABLISHED_EVT: {
      tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *param =
                     (tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *) p_data;
      if(param->status != ISO_HCI_SUCCESS) {
        cis_.cis_params.status = param->status;
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
      } else {
        uint32_t transport_latency_m_to_s =
                static_cast<uint32_t>(param->transport_latency_m_to_s[0]) |
                static_cast<uint32_t>(param->transport_latency_m_to_s[1] << 8) |
                static_cast<uint32_t>(param->transport_latency_m_to_s[2] << 16);

        uint32_t transport_latency_s_to_m =
                static_cast<uint32_t>(param->transport_latency_s_to_m[0]) |
                static_cast<uint32_t>(param->transport_latency_s_to_m[1] << 8) |
                static_cast<uint32_t>(param->transport_latency_s_to_m[2] << 16);

        CISEstablishedEvtParamas params = { .status = param->status,
                                            .cig_id = cis_.cig_id,
                                            .cis_id = cis_.cis_id,
                                            .connection_handle = param->connection_handle,
                                            .transport_latency_m_to_s = transport_latency_m_to_s,
                                            .transport_latency_s_to_m = transport_latency_s_to_m,
                                            .ft_m_to_s = param->ft_m_to_s,
                                            .ft_s_to_m = param->ft_s_to_m,
                                            .iso_interval = param->iso_interval };
        cis_.cis_params = params;
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateEstablished);

      }
    } break;
    default:
      cis_status = false;
      break;
  }
  return cis_status;
}

void CisStateMachine::StateEstablished::OnEnter() {
  LOG(INFO) << __func__ << ": CIS State : " << GetState();
  cis_.disc_direction = cis_.direction;
  if(cis_.cis_callback != nullptr) {
    cis_.cis_callback->OnCisState(cis_.cig_id, cis_.cis_id,
                                  cis_.direction, cis_.cis_params,
                                  CisState::ESTABLISHED);
  }
}

void CisStateMachine::StateEstablished::OnExit() {

}

bool CisStateMachine::StateEstablished::ProcessEvent(uint32_t event,
                                                     void* p_data) {
  LOG(INFO) <<__func__  <<": CIS State = " << GetState()
                        <<": Event = " << cis_.cis_sm.GetEventName(event);
  LOG(INFO) <<__func__  <<": CIS Id = " << loghex(cis_.cis_id);
  LOG(INFO) <<__func__  <<": CIS Handle = " << loghex(cis_.cis_handle);

  switch (event) {
    case CIS_DISCONNECT_REQ:
      if(BTM_BleIsoCisDisconnect(cis_.cis_handle, 0x13 ,
                                 &hci_cis_disconnect_callback) ==
                                 HCI_SUCCESS) {
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateDestroying);
      }
      break;
    case CIS_DISCONNECTED_EVT: {
      tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *param =
                    (tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *) p_data;
      if(param->status != ISO_HCI_SUCCESS) {
        LOG(ERROR) <<__func__  << " cis disconnection failed";
        cis_.cis_sm.TransitionTo(cis_.cis_sm.PreviousStateId());
      } else {
        cis_.cis_params.status = param->status;
        cis_.cis_sm.TransitionTo(CisStateMachine::kStateReady);
      }
    } break;
    case UPDATE_ENCODER_LIMITS: {
      tBTM_BLE_SET_ENCODER_LIMITS_PARAM *encoder_limit_data =
          (tBTM_BLE_SET_ENCODER_LIMITS_PARAM *) p_data;
      LOG(INFO) <<__func__  << "encoder_limit_data->cig_id = "<<loghex(encoder_limit_data->cig_id);
      LOG(INFO) <<__func__  << "encoder_limit_data->cis_id = "<<loghex(encoder_limit_data->cis_id);
      if (controller_get_interface()->is_qbce_QLE_HCI_supported()) {
        uint8_t length = 0;
        uint8_t size = 1;
        if (encoder_limit_data->encoder_params.size())
          size = encoder_limit_data->encoder_params.size();
        uint16_t len = 4 + size * 3;
        LOG(INFO) <<__func__  << "len = "<<loghex(len);
        uint8_t param_arr[len];
        uint8_t *param = param_arr;
        PrepareSetEncoderLimitsPayload(encoder_limit_data, &length, param);
        BTM_VendorSpecificCommand(HCI_VS_QBCE_OCF, length, param,
                    btm_ble_set_encoder_limits_features_status_callback);
      }
    } break;
    default:
      break;
  }
  return true;
}

class CisInterfaceImpl : public CisInterface {
 public:
  CisInterfaceImpl(CisInterfaceCallbacks* callback):
     callbacks(callback)  { }

  ~CisInterfaceImpl() override = default;

  void CleanUp () {
    //Remove CIG
    LOG(WARNING) << __func__  << ": Checking if CIG remove is needed";
    cig_list_backup = cig_list;
    for (auto it : cig_list) {
      CIG *cig = it.second;
      if (cig->cig_state == CigState::CREATED) {
        for (auto cis_it : cig->cis_list) {
          CIS *cis = cis_it.second;
          if (cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            cis->cis_callback = nullptr;
            LOG(WARNING) << __func__
                         << ": remove CIS : "<<loghex(cis->cis_handle);
            BTM_BleIsoCisDisconnect(cis->cis_handle, 0x13 ,
                                        &hci_cis_disconnect_callback);
          }
        }
      }
    }
  }

  CigState GetCigState(const uint8_t &cig_id) override {
    CIG *cig = GetCig(cig_id);
    if (cig != nullptr) {
      return cig->cig_state;
    } else {
      return CigState::IDLE;
    }
  }

  CisState GetCisState(const uint8_t &cig_id, uint8_t cis_id) override {
    return CisState::READY;
  }

  uint8_t GetCisCount(const uint8_t &cig_id) override {
    return 0;
  }

  IsoHciStatus AddCigMultiConfigsInternal(
      uint8_t cig_id, const std::vector<AltQosConfig>& alt_qos_configs) {
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    std::stringstream config_ids;
    for (const auto& alt_qos_config : alt_qos_configs) {
      config_ids << +alt_qos_config.config_id << ",";
    }

    LOG(INFO) << __func__  << ": cig_id=" << loghex(cig_id)
              << ", config_ids=" << config_ids.str();

    tBTM_BLE_ISO_ADD_CIG_MULTI_CONFIGS_CMD_PARAM p_data;
    p_data.cig_id = cig_id;
    for (const auto& alt_qos_config : alt_qos_configs) {
      tBTM_BLE_ALT_CIG_CONFIG dst_config = {};
      dst_config.config_id = alt_qos_config.config_id;
      dst_config.mode_id = 0;
      memcpy(&dst_config.sdu_int_s_to_m, &cig->cig_config.sdu_interval_s_to_m,
              sizeof(dst_config.sdu_int_s_to_m));
      memcpy(&dst_config.sdu_int_m_to_s, &cig->cig_config.sdu_interval_m_to_s,
              sizeof(dst_config.sdu_int_m_to_s));

      dst_config.slave_clock_accuracy = 0x00;
      dst_config.packing = cig->cig_config.packing;
      dst_config.framing = cig->cig_config.framing;
      dst_config.max_transport_latency_m_to_s = cig->cig_config.max_tport_latency_m_to_s;
      dst_config.max_transport_latency_s_to_m = cig->cig_config.max_tport_latency_s_to_m;
      dst_config.low_latency = 0; // unused in controller
      dst_config.max_bandwidth = 0; // unused in controller

      for (auto it = cig->cis_list.begin(); it != cig->cis_list.end(); ++it) {
        CIS *cis = it->second;
        tBTM_BLE_CIS_CONFIG cis_config;
        memcpy(&cis_config, &(cis->cis_config), sizeof(tBTM_BLE_CIS_CONFIG));
        const std::vector<CISConfig> &alt_cis_configs =  alt_qos_config.qos_config.cis_configs;
        auto it_2 = std::find_if(alt_cis_configs.begin(), alt_cis_configs.end(),
            [cis] (const CISConfig &conf)
            { return conf.cis_id == cis->cis_id; });
        if (it_2 != alt_cis_configs.end()) {
          cis_config.max_sdu_m_to_s = it_2->max_sdu_m_to_s;
        } else {
          LOG(WARNING) << __func__  << ": cis config is not found in alt configs, cis_id="
                       << loghex(cis->cis_id);
        }
        dst_config.cis_configs.push_back(cis_config);
      }
      p_data.alt_cig_configs.push_back(dst_config);
    }

    p_data.p_cb = &hci_cig_add_multi_configs_callback;
    if (BTM_BleAddCigMultiConfigs(&p_data) == HCI_SUCCESS)
      return ISO_HCI_IN_PROGRESS;
    else
      return ISO_HCI_FAILED;
  }

  IsoHciStatus CreateCig(RawAddress client_peer_bda, bool reconfig,
                         CIGConfig &cig_config,
                         std::vector<CISConfig> &cis_configs,
                         CodecConfig *codec_config,
                         uint16_t audio_context,
                         uint8_t stream_direction,
                         std::vector<AltQosConfig> &alt_qos_configs) override {
    LOG(INFO) << __func__  << " : stream_direction = " << loghex(stream_direction);
    LOG(INFO) << __func__  << " : audio_context = " << int(audio_context);
    LOG(INFO) << __func__  << " : codec_type = " << int(codec_config->codec_type);
    LOG(INFO) << __func__  << " : frequency = " << int(codec_config->sample_rate);

    /* For Config datapath */
    if(stream_direction & DIR_TO_AIR) {
        to_air_audio_context = audio_context;
    }
    if(stream_direction & DIR_FROM_AIR) {
        from_air_audio_context = audio_context;
    }
    codec_config_config_datapath = *codec_config;

    // check if CIG already exists
    LOG(INFO) << __func__  << " : CIG Id = " << loghex(cig_config.cig_id);
    CIG *cig = GetCig(cig_config.cig_id);
    if (cig != nullptr) {
      auto it = cig->clients_list.find(client_peer_bda);
      if (it == cig->clients_list.end()) {
        cig->clients_list.insert(std::make_pair(client_peer_bda, 0x01));
      } else {
        if(!reconfig) {
          // increment the count
          it->second++;
        }
      }

      auto it2 = cig->cis_list.begin();
      int i;
      for (it2 = cig->cis_list.begin(), i = 0; it2 != cig->cis_list.end() && cig_config.cis_count; it2++, i++) {
        CIS *cis = it2->second;
        cis->cis_config = cis_configs[i]; //remove?

        if(stream_direction & DIR_TO_AIR) {
            cis->to_air_codec_config = *codec_config;
            cis->to_air_audio_context = audio_context;
        }
        else if(stream_direction & DIR_FROM_AIR) {
            cis->from_air_codec_config = *codec_config;
            cis->from_air_audio_context = audio_context;
        }
      }
      // check if params are same for group requested
      // and for the group alredy exists
      if(cig->cig_state == CigState::CREATING) {
        return ISO_HCI_IN_PROGRESS;
      } else if(IsCigParamsSame(cig_config, cis_configs)) {
        if(cig->cig_state == CigState::CREATED) {
          return ISO_HCI_SUCCESS;
        }
      }
    }

    // check if the CIS vector length is same as cis count passed
    // in CIG confifuration
    if(cig_config.cis_count != cis_configs.size()) {
      return ISO_HCI_FAILED;
    }

    char value[PROPERTY_VALUE_MAX] = {0};
    bool create_cig = false;
    property_get("persist.vendor.btstack.get_cig_test_param", value, "");
    uint16_t ft_m_s, ft_s_m, iso_int, clk_accuracy, nse, pdu_m_s, pdu_s_m, bn_m_s, bn_s_m;
    int res = sscanf(value, "%hu,%hu,%hu,%hu,%hu,%hu,%hu,%hu,%hu", &ft_m_s, &ft_s_m, &iso_int,
                            &clk_accuracy, &nse, &pdu_m_s, &pdu_s_m, &bn_m_s, &bn_s_m);
    LOG(WARNING) << __func__<< ": FT_M_S: " << loghex(ft_m_s) << ", FT_S_M: " << loghex(ft_s_m)
                 << ", ISO_Interval: " << loghex(iso_int) << ", slave_clock: " << loghex(clk_accuracy)
                 << ", NSE: " << loghex(nse) << ", PDU_M_S:" << loghex(pdu_m_s)
                 << " PDU_S_M:" << loghex(pdu_s_m) << ", BN_M_S: " << loghex(bn_m_s)
                 << ", BN_S_M: " << loghex(bn_s_m);
    if (res == 9) {
      tBTM_BLE_SET_CIG_PARAM_TEST p_data_test;
      p_data_test.cig_id = cig_config.cig_id;
      memcpy(&p_data_test.sdu_int_s_to_m, &cig_config.sdu_interval_s_to_m,
              sizeof(p_data_test.sdu_int_s_to_m));

      memcpy(&p_data_test.sdu_int_m_to_s, &cig_config.sdu_interval_m_to_s,
              sizeof(p_data_test.sdu_int_m_to_s));

      p_data_test.ft_m_to_s = ft_m_s;
      p_data_test.ft_s_to_m = ft_s_m;
      p_data_test.iso_interval = iso_int;
      p_data_test.slave_clock_accuracy = clk_accuracy;
      p_data_test.packing = cig_config.packing;
      p_data_test.framing = cig_config.framing;
      p_data_test.cis_count = cig_config.cis_count;

      for (auto it = cis_configs.begin(); it != cis_configs.end();) {
        tBTM_BLE_CIS_TEST_CONFIG cis_config;
        cis_config.cis_id = it->cis_id;
        cis_config.nse = nse;
        cis_config.max_sdu_m_to_s = it->max_sdu_m_to_s;
        cis_config.max_sdu_s_to_m = it->max_sdu_s_to_m;
        cis_config.max_pdu_m_to_s = it->max_sdu_m_to_s;
        cis_config.max_pdu_s_to_m = it->max_sdu_s_to_m;
        cis_config.phy_m_to_s = it->phy_m_to_s;
        cis_config.phy_s_to_m = it->phy_s_to_m;
        cis_config.bn_m_to_s = bn_m_s;
        cis_config.bn_s_to_m = 0;
        if (cis_config.max_sdu_s_to_m > 0) {
          cis_config.bn_s_to_m = bn_s_m;
          if (cis_config.max_sdu_m_to_s > 0 && cis_config.nse > 13) {
            cis_config.nse = 13;
          }
        }
        p_data_test.cis_config.push_back(cis_config);
        it++;
      }
      p_data_test.p_cb = &hci_cig_param_test_callback;
      create_cig = (BTM_BleSetCigParametersTest(&p_data_test) == HCI_SUCCESS);
    } else {
      tBTM_BLE_ISO_SET_CIG_CMD_PARAM p_data;
      p_data.cig_id = cig_config.cig_id;
      memcpy(&p_data.sdu_int_s_to_m, &cig_config.sdu_interval_s_to_m,
              sizeof(p_data.sdu_int_s_to_m));

      memcpy(&p_data.sdu_int_m_to_s, &cig_config.sdu_interval_m_to_s,
              sizeof(p_data.sdu_int_m_to_s));

      p_data.slave_clock_accuracy = 0x00;
      p_data.packing = cig_config.packing;
      p_data.framing = cig_config.framing;
      p_data.max_transport_latency_m_to_s = cig_config.max_tport_latency_m_to_s;
      p_data.max_transport_latency_s_to_m = cig_config.max_tport_latency_s_to_m;
      p_data.cis_count = cig_config.cis_count;
      for (auto it = cis_configs.begin(); it != cis_configs.end();) {
        tBTM_BLE_CIS_CONFIG cis_config;
        memcpy(&cis_config, &(*it), sizeof(tBTM_BLE_CIS_CONFIG));
        p_data.cis_config.push_back(cis_config);
        it++;
      }
      p_data.p_cb = &hci_cig_param_callback;
      create_cig = (BTM_BleSetCigParam(&p_data) == HCI_SUCCESS);
    }
    if(create_cig) {
      // create new CIG and add it to the list
      if(cig == nullptr) {
        CIG *cig = new (CIG);
        cig_list.insert(std::make_pair(cig_config.cig_id, cig));
        cig->cig_config = cig_config;
        cig->cig_state = CigState::CREATING;
        cig->alt_qos_configs = alt_qos_configs;

        for(uint8_t i = 0; i < cig_config.cis_count; i++)  {
          uint8_t direction = 0;
          if(cis_configs[i].max_sdu_m_to_s) direction |= DIR_TO_AIR;
          if(cis_configs[i].max_sdu_s_to_m) direction |= DIR_FROM_AIR;
          CIS *cis = new CIS(cig_config.cig_id, cis_configs[i].cis_id,
                         direction, callbacks);
          cis->cis_config = cis_configs[i];

          if(stream_direction & DIR_TO_AIR) {
              cis->to_air_codec_config = *codec_config;
              cis->to_air_audio_context = audio_context;
          }
          else if(stream_direction & DIR_FROM_AIR) {
              cis->from_air_codec_config = *codec_config;
              cis->from_air_audio_context = audio_context;
          }

          cig->cis_list.insert(std::make_pair(cis_configs[i].cis_id, cis));
        }

        auto it = cig->clients_list.find(client_peer_bda);
        if (it == cig->clients_list.end()) {
          cig->clients_list.insert(std::make_pair(client_peer_bda, 0x01));
        } else {
          // increment the count
          it->second++;
          LOG(WARNING) << __func__  << "count " << loghex(it->second);
        }
      } else {
        cig->cig_config = cig_config;
        cig->cig_state = CigState::CREATING;
        cig->alt_qos_configs = alt_qos_configs;

        uint8_t i = 0;
        for (auto it = cig->cis_list.begin(); it != cig->cis_list.end();) {
          CIS *cis = it->second;
          cis->cis_config = cis_configs[i];
          cis->cis_sm.TransitionTo(CisStateMachine::kStateIdle);
          it++; i++;
        }

        auto it = cig->clients_list.find(client_peer_bda);
        if (it == cig->clients_list.end()) {
          cig->clients_list.insert(std::make_pair(client_peer_bda, 0x01));
        }
      }
      return ISO_HCI_IN_PROGRESS;
    } else {
      return ISO_HCI_FAILED;
    }
  }

  IsoHciStatus RemoveCig(RawAddress client_peer_bda, uint8_t cig_id) override {
    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id);
    // check if the CIG exists
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state == CigState::IDLE ||
       cig->cig_state == CigState::CREATING) {
      return ISO_HCI_FAILED;
    } else if(cig->cig_state == CigState::CREATED) {

      auto it = cig->clients_list.find(client_peer_bda);
      if (it == cig->clients_list.end()) {
        return ISO_HCI_FAILED;
      } else {
        // decrement the count
        it->second--;
        LOG(WARNING) << __func__  << ": Count : " << loghex(it->second);
      }

      // check if all clients have voted off then go for CIG removal
      uint8_t vote_on_count = 0;
      for (auto it = cig->clients_list.begin();
                                it != cig->clients_list.end();) {
        vote_on_count += it->second;
        it++;
      }

      if(vote_on_count) {
        LOG(WARNING) << __func__  << " : Vote On Count : "
                                  << loghex(vote_on_count);
        return ISO_HCI_SUCCESS;
      }

      // check if any of the CIS are in established/streaming state
      // if so return false as it is not allowed
      if(IsCisActive(cig_id, 0xFF)) return ISO_HCI_FAILED;

      if(BTM_BleRemoveCig(cig_id, &hci_cig_remove_param_callback)
              == HCI_SUCCESS) {
        cig->cig_state = CigState::REMOVING;
        return ISO_HCI_IN_PROGRESS;
      } else return ISO_HCI_FAILED;
    }
    return ISO_HCI_FAILED;
  }

  IsoHciStatus CreateCis(uint8_t cig_id, std::vector<uint8_t> cis_ids,
                         std::vector<RawAddress> peer_bda,
                         bool mode) override  {
    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id);
    LOG(INFO) <<__func__  << ": No. of CISes = " << loghex(cis_ids.size());

    IsoHciStatus ret = ISO_HCI_FAILED;
    uint32_t cur_state;
    // check if the CIG exists
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state != CigState::CREATED) {
      return ISO_HCI_FAILED;
    }

    bool cis_created = false;
    CreateCisNode param;
    param.cig_id = cig_id;
    param.cis_ids = cis_ids;
    param.peer_bda = peer_bda;
    param.mode = mode;
    std::vector<uint16_t> cis_handles;

    for (auto i: cis_ids) {
      CIS *cis = GetCis(cig_id, i);
      if (cis == nullptr) {
        return ISO_HCI_FAILED;
      }
      cis_handles.push_back(cis->cis_handle);
    }
    param.cis_handles = cis_handles;

    for (auto i: cis_ids) {
      LOG(INFO) <<__func__ << ": CIS Id = " << loghex(i);
      // check if CIS ID mentioned is present as part of CIG
      CIS *cis = GetCis(cig_id, i);
      if (cis == nullptr) {
        ret = ISO_HCI_FAILED;
        break;
      }

      cur_state = cis->cis_sm.StateId();

      // check if CIS is already created or in progress
      if(cur_state == CisStateMachine::kStateEstablishing) {
        ret = ISO_HCI_IN_PROGRESS;
        break;
      } else if(cur_state == CisStateMachine::kStateEstablished) {
        if (mode && cis->cis_callback) {
              LOG(INFO) <<__func__ << ": AAR4 CIS Established sent ";
              cis->cis_callback->OnCisState(cis->cig_id, cis->cis_id,
                                     cis->direction, cis->cis_params,
                                              CisState::ESTABLISHED);
        }
        ret = ISO_HCI_SUCCESS;
        break;
      } else if(cur_state == CisStateMachine::kStateDestroying) {
        ret = ISO_HCI_FAILED;
        break;
      }
      if (cis_created == false) {
        // queue it if there is pending create CIS
        if (cis_queue.size()) {
          // hand it over to the CIS module
          // check if the new request is already exists
          // as the head entry in the list
          CreateCisNode& head = cis_queue.front();
          if(head.cig_id == cig_id && head.cis_ids == cis_ids &&
             head.peer_bda == peer_bda) {
             if(cis->cis_sm.ProcessEvent(
                          IsoHciEvent::CIS_CREATE_REQ, &param)) {
               ret = ISO_HCI_IN_PROGRESS;
             } else {
               ret = ISO_HCI_FAILED;
               break;
             }
          } else {
            cis_queue.push_back(param);
            ret = ISO_HCI_IN_PROGRESS;
          }
        } else {
          cis_queue.push_back(param);
          if(cis->cis_sm.ProcessEvent(IsoHciEvent::CIS_CREATE_REQ,
                                          &param)) {
            ret = ISO_HCI_IN_PROGRESS;
          } else {
            ret = ISO_HCI_FAILED;
            break;
          }
        }
        cis_created = true;
      } else {
        if(cis->cis_sm.ProcessEvent(IsoHciEvent::CIS_CREATE_REQ_DUMMY,
                                        &peer_bda)) {
          ret = ISO_HCI_IN_PROGRESS;
        } else {
          ret = ISO_HCI_FAILED;
          break;
        }
      }
    }
    return ret;
  }

  IsoHciStatus DisconnectCis(uint8_t cig_id, uint8_t cis_id,
                             uint8_t direction) override {
    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id)
                          << ": CIS Id = " << loghex(cis_id);

    uint32_t cur_state;
    // check if the CIG exists
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state != CigState::CREATED) {
      return ISO_HCI_FAILED;
    }

    // check if CIS ID mentioned is present as part of CIG
    CIS *cis = GetCis(cig_id, cis_id);
    if (cis == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cis->disc_direction & direction) {
       // remove the direction bit form disc direciton
       cis->disc_direction &= ~direction;
    }

    if(cis->disc_direction) return ISO_HCI_SUCCESS;

    // if all directions are voted off go for CIS disconneciton
    cur_state = cis->cis_sm.StateId();

    // check if CIS is not created or in progress
    if(cur_state == CisStateMachine::kStateReady) {
      return ISO_HCI_SUCCESS;
    } else if(cur_state == CisStateMachine::kStateEstablishing) {
      return ISO_HCI_FAILED;
    } else if(cur_state == CisStateMachine::kStateDestroying) {
      return ISO_HCI_IN_PROGRESS;
    }

    LOG(INFO) <<__func__  << " Request issued to CIS SM";
    // hand it over to the CIS module
    if(cis->cis_sm.ProcessEvent(
              IsoHciEvent::CIS_DISCONNECT_REQ, nullptr)) {
      return ISO_HCI_IN_PROGRESS;
    } else return ISO_HCI_FAILED;
  }

  IsoHciStatus SetupDataPath(uint8_t cig_id, uint8_t cis_id,
          uint8_t data_path_direction, uint8_t data_path_id)  override {
    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id)
                          << ": CIS Id = " << loghex(cis_id);

    uint32_t cur_state;
    // check if the CIG exists
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state != CigState::CREATED) {
      return ISO_HCI_FAILED;
    }

    // check if CIS ID mentioned is present as part of CIG
    CIS *cis = GetCis(cig_id, cis_id);
    if (cis == nullptr) {
      return ISO_HCI_FAILED;
    }

    cur_state = cis->cis_sm.StateId();

    // check if CIS is not created or in progress
    if(cur_state == CisStateMachine::kStateReady ||
       cur_state == CisStateMachine::kStateEstablishing ||
       cur_state == CisStateMachine::kStateDestroying) {
      return ISO_HCI_FAILED;
    } else if(cur_state == CisStateMachine::kStateEstablished) {
      // return success as it is already created
      return ISO_HCI_SUCCESS;
    }

    // hand it over to the CIS module
    tIsoSetUpDataPath data_path_info;
    data_path_info.data_path_direction = data_path_direction;
    data_path_info.data_path_id = data_path_id;

    if(cis->cis_sm.ProcessEvent(
        IsoHciEvent::SETUP_DATA_PATH_REQ, &data_path_info)) {
      return ISO_HCI_IN_PROGRESS;
    } else return ISO_HCI_FAILED;
  }

  IsoHciStatus RemoveDataPath(uint8_t cig_id, uint8_t cis_id,
                      uint8_t data_path_direction) override {
    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id)
                          << ": CIS Id = " << loghex(cis_id);

    uint32_t cur_state;
    // check if the CIG exists
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state != CigState::CREATED) {
      return ISO_HCI_FAILED;
    }

    // check if CIS ID mentioned is present as part of CIG
    CIS *cis = GetCis(cig_id, cis_id);
    if (cis == nullptr) {
      return ISO_HCI_FAILED;
    }

    cur_state = cis->cis_sm.StateId();

    // check if CIS is not created or in progress
    if(cur_state == CisStateMachine::kStateReady ||
       cur_state == CisStateMachine::kStateEstablishing ||
       cur_state == CisStateMachine::kStateDestroying ||
       cur_state == CisStateMachine::kStateEstablished) {
      return ISO_HCI_FAILED;
    }

    // hand it over to the CIS module
    if(cis->cis_sm.ProcessEvent(
           IsoHciEvent::REMOVE_DATA_PATH_REQ, &data_path_direction)) {
      return ISO_HCI_SUCCESS;
    } else return ISO_HCI_FAILED;
  }


  IsoHciStatus UpdateEncoderParams(uint8_t cig_id, uint8_t cis_id,
                                   std::vector<uint8_t> encoder_limit_params,
                                   uint8_t encoder_mode) {

    LOG(INFO) <<__func__  << ": CIG Id = " << loghex(cig_id)
                          << ": CIS Id = " << loghex(cis_id);
    CIG *cig = GetCig(cig_id);
    if (cig == nullptr) {
      return ISO_HCI_FAILED;
    }

    if(cig->cig_state != CigState::CREATED) {
      return ISO_HCI_FAILED;
    }
    CIS *cis = GetCis(cig_id, cis_id);
    if (cis == nullptr) {
      return ISO_HCI_FAILED;
    }

    tBTM_BLE_SET_ENCODER_LIMITS_PARAM encoder_params = {
                                         .cig_id = cig_id,
                                         .cis_id = cis_id,
                                         .encoder_params = encoder_limit_params,
                                         .encoder_mode = encoder_mode};

    uint32_t cur_state = cis->cis_sm.StateId();
    if (cur_state == CisStateMachine::kStateReady ||
        cur_state == CisStateMachine::kStateEstablishing ||
        cur_state == CisStateMachine::kStateDestroying ||
        cur_state == CisStateMachine::kStateEstablished) {

      if (cis->cis_sm.ProcessEvent(
             IsoHciEvent::UPDATE_ENCODER_LIMITS, &encoder_params)) {
        return ISO_HCI_SUCCESS;
      } else return ISO_HCI_FAILED;
    }
    return ISO_HCI_FAILED;
  }

  const char* GetEventName(uint32_t event) {
    switch (event) {
      CASE_RETURN_STR(CIG_CONFIGURED_EVT)
      CASE_RETURN_STR(CIS_STATUS_EVT)
      CASE_RETURN_STR(CIS_ESTABLISHED_EVT)
      CASE_RETURN_STR(CIS_DISCONNECTED_EVT)
      CASE_RETURN_STR(CIG_REMOVED_EVT)
      CASE_RETURN_STR(SETUP_DATA_PATH_DONE_EVT)
      CASE_RETURN_STR(REMOVE_DATA_PATH_DONE_EVT)
      CASE_RETURN_STR(CIG_MULTI_CONFIGS_ADDED_EVT)
      CASE_RETURN_STR(CONFIGURE_DATA_PATH_REQ)
      CASE_RETURN_STR(CONFIGURE_DATA_PATH_DONE_EVT)
      default:
       return "Unknown Event";
    }
  }

  IsoHciStatus ProcessEvent (uint32_t event, void* p_data) {
    LOG(INFO) << __func__ << ": Event = " << GetEventName(event);
    switch (event) {
      case CIG_CONFIGURED_EVT: {
        conn_handles.clear();
        config_datapath_queue.clear();
        tBTM_BLE_SET_CIG_RET_PARAM *param =
                            (tBTM_BLE_SET_CIG_RET_PARAM *) p_data;

        LOG(INFO) << __func__ << ": CIG Id = " << loghex(param->cig_id)
                  << ": status = " << loghex(param->status);
        auto it = cig_list.find(param->cig_id);
        if (it == cig_list.end()) {
          return ISO_HCI_FAILED;
        }
        bool to_air_config_datapath_added = false;
        bool from_air_config_datapath_added = false;

        if(!param->status) {
          uint8_t i = 0;
          CIG *cig = it->second;
          LOG(INFO) << __func__ << ": cig->cis_list.size(): " << loghex(cig->cis_list.size());
          for (auto it = cig->cis_list.begin();
                    it != cig->cis_list.end(); it++) {
            CIS *cis = it->second;
            cis->cis_handle = *(param->conn_handle + i++);
            conn_handles.push_back(cis->cis_handle);
            LOG(INFO) << __func__ << ": cis->cis_handle: " << loghex(cis->cis_handle)
                                  << ": cis->direction: " << loghex(cis->direction);

            if((cis->direction & DIR_TO_AIR) && !to_air_config_datapath_added) {
              tConfigureDataPath config_data_path_info_tx;
              config_data_path_info_tx.data_path_direction = DIR_TO_AIR;
              config_data_path_info_tx.data_path_id = 0x01;
              std::vector<uint8_t> codec_config_payload;
              codec_config_payload = PrepareCodecConfigPayloadForConfigDatapath(
                                         &codec_config_config_datapath, to_air_audio_context,
                                         from_air_audio_context, DIR_TO_AIR, cis->cis_handle);
              LOG(INFO) <<__func__ <<": Printing config data path payload Tx";
              for(uint8_t i = 0; i < codec_config_payload.size(); i++) {
                  LOG(INFO) <<__func__ << " byte " << loghex(i) << ": "
                                       << loghex(codec_config_payload[i]);
                  config_data_path_info_tx.codec_config.push_back(codec_config_payload[i]);
              }
              config_data_path_info_tx.codec_config_length = codec_config_payload.size();
              ConfigureDataPathNode node = {.type = CONFIGURE_DATA_PATH_REQ,
                                            .config_datapath = config_data_path_info_tx};
              config_datapath_queue.push_back(node);
              to_air_config_datapath_added = true;
            }
            if((cis->direction & DIR_FROM_AIR) && !from_air_config_datapath_added) {
              tConfigureDataPath config_data_path_info_rx;
              config_data_path_info_rx.data_path_direction = DIR_FROM_AIR;
              config_data_path_info_rx.data_path_id = 0x01;
              std::vector<uint8_t> codec_config_payload;
              codec_config_payload = PrepareCodecConfigPayloadForConfigDatapath(
                                         &codec_config_config_datapath, to_air_audio_context,
                                         from_air_audio_context, DIR_FROM_AIR, cis->cis_handle);
              LOG(INFO) <<__func__ <<": Printing config data path payload Rx";
              for(uint8_t i = 0; i < codec_config_payload.size(); i++) {
                  LOG(INFO) <<__func__ << " byte " << loghex(i) << ": "
                                       << loghex(codec_config_payload[i]);
                  config_data_path_info_rx.codec_config.push_back(codec_config_payload[i]);
              }
              config_data_path_info_rx.codec_config_length = codec_config_payload.size();
              ConfigureDataPathNode node = {.type = CONFIGURE_DATA_PATH_REQ,
                                            .config_datapath = config_data_path_info_rx};
              config_datapath_queue.push_back(node);
              from_air_config_datapath_added = true;
            }
          }

          LOG(INFO) <<__func__ << ":config_datapath_queue.size(): " << config_datapath_queue.size();
          cig_ret_param_status = param->status;
          cig_ret_param_cig_id = param->cig_id;
          cig_ret_param_cis_count = param->cis_count;
          cig_ret_param_conn_handles = &conn_handles[0];
          LOG(INFO) << __func__ << ": set cig_ret_param ";

          if(!config_datapath_queue.empty()) {
              LOG(INFO) << __func__ <<": config_datapath_queue is not empty";
              ConfigureDataPathNode node = config_datapath_queue.front();
              tBTM_CFG_DATA_PATH_PARAM p_params;
              p_params.data_path_dir = node.config_datapath.data_path_direction >> 1;
              p_params.data_path_id = node.config_datapath.data_path_id;
              p_params.vs_config_len = node.config_datapath.codec_config_length;
              LOG(INFO) << __func__ <<" p_params.vs_config_len: "<<p_params.vs_config_len;
              if(p_params.vs_config_len != 0) {
                  p_params.vs_config = &node.config_datapath.codec_config[0];
              }
              LOG(INFO) << __func__ <<" p_params.vs_config[0]: "<< loghex(p_params.vs_config[0]);
              p_params.p_cb = &hci_cis_configure_datapath_callback;
              BTM_ConfigureDataPath(&p_params);
              /* Wait for hci_cis_configure_datapath_callback*/
          }
        } else {
          // maybe CIG was not removed in the last stream stop properly?
          if (param->status == HCI_ERR_COMMAND_DISALLOWED) {
             LOG(INFO) << __func__ <<": removing cig as command disallowed received";
             BTM_BleRemoveCig(param->cig_id, NULL);
          }
          // delete CIG and CIS
          CIG *cig = it->second;
          cig->cig_state = CigState::IDLE;

          while (!cig->cis_list.empty()) {
            auto it = cig->cis_list.begin();
            CIS * cis = it->second;
            cig->cis_list.erase(it);
            delete cis;
          }
          callbacks->OnCigState(param->cig_id, CigState::IDLE);
          cig_list.erase(it);
          delete cig;
        }

      } break;
      case CONFIGURE_DATA_PATH_DONE_EVT: {

        tBTM_BLE_CONFIGURE_DATA_PATH_EVT_PARAM *param =
                              (tBTM_BLE_CONFIGURE_DATA_PATH_EVT_PARAM *) p_data;
        LOG(INFO) <<__func__ << ": CONFIGURE_DATA_PATH_DONE_EVT config_datapath_queue.size()"
                             << loghex(config_datapath_queue.size())
                             << " status: " << loghex(param->status);
        if(!config_datapath_queue.empty()) {
            config_datapath_queue.pop_front();
        }

        if(!config_datapath_queue.empty()) {
            LOG(INFO) <<__func__ << ": config_datapath_queue is not empty ";
            ConfigureDataPathNode node = config_datapath_queue.front();
            tBTM_CFG_DATA_PATH_PARAM p_params;
            p_params.data_path_dir = node.config_datapath.data_path_direction >> 1;
            p_params.data_path_id = node.config_datapath.data_path_id;
            p_params.vs_config_len = node.config_datapath.codec_config_length;
            if(p_params.vs_config_len != 0) {
                p_params.vs_config = &node.config_datapath.codec_config[0];
            }
            p_params.p_cb = &hci_cis_configure_datapath_callback;
            BTM_ConfigureDataPath(&p_params);
            return ISO_HCI_SUCCESS;
        }
        if(config_datapath_queue.empty()) {
          tBTM_BLE_SET_CIG_RET_PARAM cig_ret_param = {.status = cig_ret_param_status,
                                                       .cig_id = cig_ret_param_cig_id,
                                                       .cis_count = cig_ret_param_cis_count,
                                                       .conn_handle = cig_ret_param_conn_handles,
                                                      };
          LOG(INFO) << __func__ << " CONFIGURE_DATA_PATH_DONE_EVT: cig_ret_param.status: "
                                << loghex(cig_ret_param.status)
                                << " cig_ret_param.cig_id: " << loghex(cig_ret_param.cig_id)
                                << " cig_ret_param.cis_count: " << loghex(cig_ret_param.cis_count);

          LOG(INFO) <<__func__ << ": CONFIGURE_DATA_PATH_DONE_EVT config_datapath_queue is empty ";
          auto it = cig_list.find(cig_ret_param.cig_id);
                  LOG(INFO) << __func__ << ": cig_ret_param CIG Id = "
                                        << loghex(cig_ret_param.cig_id)
                                        << ": status = " << loghex(cig_ret_param.status)
                                        << ": cis_count: " << loghex(cig_ret_param.cis_count);

          if (it == cig_list.end()) {
            LOG(INFO) <<__func__ << ": cig ID not found ";
            return ISO_HCI_FAILED;
          }

          uint8_t i = 0;
          CIG *cig = it->second;
          tIsoSetUpDataPath data_path_info;

          char bap_pts[PROPERTY_VALUE_MAX] = "flase";
          property_get("persist.vendor.btstack.bap_pts", bap_pts, "false");

          if (!strncmp("true", bap_pts, 4)) {
            LOG(INFO) <<__func__ <<": bap pts test prop enabled";
            for (auto it = cig->cis_list.begin();
                      it != cig->cis_list.end(); it++) {
              CIS *cis = it->second;
              cis->cis_handle = *(cig_ret_param.conn_handle + i++);
              cis->cis_sm.Start();
            }
            cig->cig_state = CigState::CREATED;
            callbacks->OnCigState(cig_ret_param.cig_id, CigState::CREATED);
          } else {
            bool cig_configs_added = false;
            if (!cig->alt_qos_configs.empty()) {
              cig_configs_added = AddCigMultiConfigsInternal(cig_ret_param.cig_id,
                  cig->alt_qos_configs) == ISO_HCI_IN_PROGRESS;
            LOG(INFO) <<__func__ <<": bap pts test prop not enabled ";
            }
            LOG(INFO) <<__func__ <<": cig_configs_added " << cig_configs_added;
            for (auto it = cig->cis_list.begin();
                      it != cig->cis_list.end(); it++) {
              LOG(INFO) <<__func__ <<": looping on cis_list";
              CIS *cis = it->second;
              cis->cis_handle = *(cig_ret_param.conn_handle + i++);
              cis->cis_sm.Start();
              if (!cig_configs_added) {
                if(cis->direction & DIR_TO_AIR) {
                  LOG(INFO) <<__func__ <<": direction is to air for setup data path req";
                  data_path_info.data_path_direction = DIR_TO_AIR;
                  data_path_info.data_path_id = 0x01;
                  cis->cis_sm.ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_REQ,
                                            &data_path_info);
                }
                if(cis->direction & DIR_FROM_AIR) {
                  LOG(INFO) <<__func__ <<": direction is from air for setup data path req";
                  data_path_info.data_path_direction = DIR_FROM_AIR;
                  data_path_info.data_path_id = 0x01;
                  cis->cis_sm.ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_REQ,
                                            &data_path_info);
                }
              }
            }
          }
        }
      } break;
      case CIG_MULTI_CONFIGS_ADDED_EVT: {
        tBTM_BLE_ADD_CIG_MULTI_CONFIGS_RET_PARAM *param =
                              (tBTM_BLE_ADD_CIG_MULTI_CONFIGS_RET_PARAM *)p_data;

        LOG(INFO) << __func__ << ": cig_id=" << loghex(param->cig_id)
                  << ", status=" << loghex(param->status)
                  << ", added_num=" << param->config_ids_added.size();
        if (!param->config_ids_added.empty()) {
          std::stringstream config_ids;
          for (const uint8_t& id : param->config_ids_added) {
            config_ids << +id << ",";
          }
          LOG(INFO) << __func__ << ": config_ids_added=" << config_ids.str();
        }

        auto cig_it = cig_list.find(param->cig_id);
        if (cig_it == cig_list.end()) {
          LOG(ERROR) << __func__ << ": cannot find cig_id=" << loghex(param->cig_id);
          return ISO_HCI_FAILED;
        }
        CIG *cig = cig_it->second;

        if (cig->alt_qos_configs.size() != param->config_ids_added.size()) {
          LOG(ERROR) << __func__ << ": total config num=" << cig->alt_qos_configs.size()
                     << ", but added num=" << param->config_ids_added.size();
        }

        LOG(INFO) << __func__ << ", cig_state="
                  << static_cast<std::underlying_type<CigState>::type>(cig->cig_state);

        if (cig->cig_state == CigState::CREATING) {
          tIsoSetUpDataPath data_path_info;
          for (auto it = cig->cis_list.begin();
                    it != cig->cis_list.end(); ++it) {
            CIS *cis = it->second;
            if(cis->direction & DIR_TO_AIR) {
              data_path_info.data_path_direction = DIR_TO_AIR;
              data_path_info.data_path_id = 0x01;
              cis->cis_sm.ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_REQ,
                                        &data_path_info);
            }
            if(cis->direction & DIR_FROM_AIR) {
              data_path_info.data_path_direction = DIR_FROM_AIR;
              data_path_info.data_path_id = 0x01;
              cis->cis_sm.ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_REQ,
                                        &data_path_info);
            }
          }
        }
      } break;
      case CIG_REMOVED_EVT: {
        tBTM_BLE_SET_CIG_REMOVE_PARAM *param =
                                (tBTM_BLE_SET_CIG_REMOVE_PARAM *) p_data;
        auto it = cig_list.find(param->cig_id);
        if (it == cig_list.end()) {
          if (callbacks) {
            LOG(INFO) << __func__  << " calling onCigState";
            callbacks->OnCigState(param->cig_id, CigState::IDLE);
          }
          return ISO_HCI_FAILED;
        } else {
          // delete CIG and CIS
          CIG *cig = it->second;
          while (!cig->cis_list.empty()) {
            auto it = cig->cis_list.begin();
            CIS * cis = it->second;
            cig->cis_list.erase(it);
            delete cis;
          }
          if (cis_queue.size() > 0) {
            LOG(INFO) << __func__  << "cleanup cis_queue";
            std::list<CreateCisNode>::iterator itr = cis_queue.begin();
            while (itr != cis_queue.end()) {
              if (itr->cig_id == param->cig_id) {
                itr = cis_queue.erase(itr);
              } else {
                itr++;
              }
            }
          }
          cig->cig_state = CigState::IDLE;
          cig_list.erase(it);
          callbacks->OnCigState(param->cig_id, CigState::IDLE);
          delete cig;
        }
      } break;
      case CIS_STATUS_EVT: {
        // clear the first entry from cis queue and send the next
        // CIS creation request queue it if there is pending create CIS
        if (cis_queue.size()) {
            CreateCisNode &head = cis_queue.front();
            LOG(INFO) <<__func__  << ": No. of CISes = " << loghex(head.cis_ids.size());
            for (auto i: head.cis_ids) {
              CIS *cis = GetCis(head.cig_id, i);
              if(cis) {
                cis->cis_sm.ProcessEvent(IsoHciEvent::CIS_STATUS_EVT, p_data);
              }
            }
        }
        uint8_t status = *((uint8_t *)(p_data));
        LOG(INFO) <<__func__  << ": status = " << loghex(status);
        if (status != ISO_HCI_SUCCESS) {
          bool cis_status = false;
          if (cis_queue.size()) {
            cis_queue.pop_front();
          }
          while(cis_queue.size() && !cis_status) {
            CreateCisNode &head = cis_queue.front();
            CIS *cis = GetCis(head.cig_id, head.cis_ids[0]);
            if(cis == nullptr ||
               cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
              // remove the entry
              cis_queue.pop_front();
            } else if(cis) {
              IsoHciStatus hci_status =  CreateCis(head.cig_id, head.cis_ids,
                                                   head.peer_bda, head.mode);
              if(hci_status == ISO_HCI_SUCCESS ||
                 hci_status == ISO_HCI_IN_PROGRESS) {
                cis_status = true;
              } else {
                // remove the entry
                cis_queue.pop_front();
              }
            }
          }
        }
      } break;
      case CIS_ESTABLISHED_EVT: {
        tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *param =
                       (tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *) p_data;
        LOG(INFO) << __func__  << ": CIS handle = "
                                  << loghex(param->connection_handle)
                                  << ": Status = " << loghex(param->status);
        CIS *cis = GetCis(param->connection_handle);
        if (cis == nullptr) {
          return ISO_HCI_FAILED;
        } else {
          cis->cis_sm.ProcessEvent(IsoHciEvent::CIS_ESTABLISHED_EVT, p_data);
        }
        bool cis_status = false;
        if (cis_queue.size()) {
          cis_queue.pop_front();
        }
        while(cis_queue.size() && !cis_status) {
          CreateCisNode &head = cis_queue.front();
          CIS *cis = GetCis(head.cig_id, head.cis_ids[0]);
          if(cis == nullptr ||
             cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            // remove the entry
            cis_queue.pop_front();
          } else if(cis) {
            IsoHciStatus hci_status =  CreateCis(head.cig_id, head.cis_ids,
                                                 head.peer_bda, head.mode);
            if(hci_status == ISO_HCI_SUCCESS ||
               hci_status == ISO_HCI_IN_PROGRESS) {
              cis_status = true;
            } else {
              // remove the entry
              cis_queue.pop_front();
            }
          }
        }
      } break;
      case CIS_DISCONNECTED_EVT: {
        tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *param =
                      (tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM *) p_data;
        CIS *cis = GetCis(param->cis_handle);
        if (cis == nullptr) {
          return ISO_HCI_FAILED;
        } else {
          cis->cis_sm.ProcessEvent(IsoHciEvent::CIS_DISCONNECTED_EVT, p_data);
        }
      } break;
      case SETUP_DATA_PATH_DONE_EVT: {
        tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *param =
                   (tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *) p_data;

        CIS *cis = GetCis(param->conn_handle);
        CIG *cig = nullptr;
        if (cis == nullptr) {
          return ISO_HCI_FAILED;
        } else {
          cis->cis_sm.ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_DONE_EVT,
                                                  p_data);
        }
        uint8_t cig_id = cis->cig_id;

        auto it = cig_list.find(cig_id);
        if (it == cig_list.end()) {
          break;
        } else {
          // delete CIG and CIS
          cig = it->second;
        }

        uint8_t num_cis_is_ready = 0;
        for(auto it = cig->cis_list.begin(); it != cig->cis_list.end(); it++) {
          CIS *cis = it->second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateReady) {
            num_cis_is_ready++;
          }
        }

        // check if all setup data paths are completed
        if(num_cis_is_ready == cig->cis_list.size()) {
          cig->cig_state = CigState::CREATED;
          callbacks->OnCigState(cig_id, CigState::CREATED);
        }
      } break;
      case REMOVE_DATA_PATH_DONE_EVT: {
        tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *param =
                   (tBTM_BLE_CIS_DATA_PATH_EVT_PARAM *) p_data;
        CIS *cis = GetCis(param->conn_handle);
        if (cis == nullptr) {
            return ISO_HCI_FAILED;
        } else {
          cis->cis_sm.ProcessEvent(IsoHciEvent::REMOVE_DATA_PATH_DONE_EVT,
                                                  p_data);
        }
      } break;
      default:
        break;
    }
    return ISO_HCI_SUCCESS;
  }

 private:
  std::map<uint8_t, CIG *> cig_list; // cig id to CIG structure
  std::list <CreateCisNode> cis_queue;
  CisInterfaceCallbacks *callbacks;
  uint16_t to_air_audio_context = CONTENT_TYPE_NONE; //for config datapath
  uint16_t from_air_audio_context = CONTENT_TYPE_NONE; //for config datapath
  CodecConfig codec_config_config_datapath; //for config datapath
  uint8_t cig_ret_param_status;
  uint8_t cig_ret_param_cig_id;
  uint8_t cig_ret_param_cis_count;
  uint16_t *cig_ret_param_conn_handles;
  vector<uint16_t> conn_handles;
  std::list <ConfigureDataPathNode> config_datapath_queue;
  // 0xFF will be passed for cis id in case search is for any of the
  // CIS part of that group
  bool IsCisActive(uint8_t cig_id, uint8_t cis_id)  {
    bool is_cis_active = false;
    auto it = cig_list.find(cig_id);
    if (it == cig_list.end()) {
      return is_cis_active;
    } else {
      CIG *cig = it->second;
      if(cis_id != 0XFF) {
        auto it = cig->cis_list.find(cis_id);
        if (it != cig->cis_list.end()) {
          CIS *cis = it->second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_active = true;
          }
        }
      } else {
        for (auto it : cig->cis_list) {
          CIS *cis = it.second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_active = true;
            break;
          }
        }
      }
    }
    return is_cis_active;
  }

  bool IsCigParamsSame(CIGConfig &cig_config,
                       std::vector<CISConfig> &cis_configs)  {
    CIG *cig = GetCig(cig_config.cig_id);
    bool is_params_same = true;
    uint8_t i = 0;

    if(cig == nullptr || (cis_configs.size() != cig->cig_config.cis_count)) {
      LOG(WARNING) << __func__  << ": Count is different ";
      return false;
    }

    if(cig->cig_config.cig_id != cig_config.cig_id ||
       cig->cig_config.cis_count != cig_config.cis_count ||
       cig->cig_config.packing !=  cig_config.packing ||
       cig->cig_config.framing != cig_config.framing ||
       cig->cig_config.max_tport_latency_m_to_s !=
                          cig_config.max_tport_latency_m_to_s ||
       cig->cig_config.max_tport_latency_s_to_m !=
                            cig_config.max_tport_latency_s_to_m ||
       cig->cig_config.sdu_interval_m_to_s[0] !=
                         cig_config.sdu_interval_m_to_s[0] ||
       cig->cig_config.sdu_interval_m_to_s[1] !=
                         cig_config.sdu_interval_m_to_s[1] ||
       cig->cig_config.sdu_interval_m_to_s[2] !=
                         cig_config.sdu_interval_m_to_s[2] ||
       cig->cig_config.sdu_interval_s_to_m[0] !=
                         cig_config.sdu_interval_s_to_m[0] ||
       cig->cig_config.sdu_interval_s_to_m[1] !=
                         cig_config.sdu_interval_s_to_m[1] ||
       cig->cig_config.sdu_interval_s_to_m[2] !=
                         cig_config.sdu_interval_s_to_m[2]) {
      LOG(WARNING) << __func__  << " cig params are different ";
      return false;
    }

    for (auto it = cig->cis_list.begin(); it != cig->cis_list.end();) {
      CIS *cis = it->second;
      if(cis->cis_config.cis_id  ==  cis_configs[i].cis_id &&
         cis->cis_config.max_sdu_m_to_s == cis_configs[i].max_sdu_m_to_s &&
         cis->cis_config.max_sdu_s_to_m == cis_configs[i].max_sdu_s_to_m &&
         cis->cis_config.phy_m_to_s == cis_configs[i].phy_m_to_s  &&
         cis->cis_config.phy_s_to_m == cis_configs[i].phy_s_to_m  &&
         cis->cis_config.rtn_m_to_s == cis_configs[i].rtn_m_to_s  &&
         cis->cis_config.rtn_s_to_m == cis_configs[i].rtn_s_to_m) {
        it++; i++;
      } else {
        is_params_same = false;
        break;
      }
    }
    LOG(WARNING) << __func__  << ": is_params_same : "
                              << loghex(is_params_same);
    return is_params_same;
  }

  bool IsCisExists(uint8_t cig_id, uint8_t cis_id)  {
    bool is_cis_exists = false;
    auto it = cig_list.find(cig_id);
    if (it != cig_list.end()) {
      CIG *cig = it->second;
      auto it = cig->cis_list.find(cis_id);
      if (it != cig->cis_list.end()) {
        is_cis_exists = true;
      }
    }
    return is_cis_exists;
  }

  CIS *GetCis(uint8_t cig_id, uint8_t cis_id)  {
    auto it = cig_list.find(cig_id);
    if (it != cig_list.end()) {
      CIG *cig = it->second;
      auto it = cig->cis_list.find(cis_id);
      if (it != cig->cis_list.end()) {
        return it->second;
      }
    }
    return nullptr;
  }

  CIG *GetCig(uint8_t cig_id)  {
    auto it = cig_list.find(cig_id);
    if (it != cig_list.end()) {
      return it->second;
    }
    return nullptr;
  }

  CIS *GetCis(uint16_t cis_handle)  {
    bool cis_found = false;
    CIS *cis = nullptr;
    for (auto it : cig_list) {
      CIG *cig = it.second;
      if(cig->cig_state == CigState::CREATED ||
         cig->cig_state == CigState::CREATING) {
        for (auto it : cig->cis_list) {
          cis = it.second;
          if(cis->cis_handle == cis_handle) {
            cis_found = true;
            break;
          }
        }
      }
      if(cis_found) return cis;
    }
    return nullptr;
  }

  // TODO to remove if there is no need
  bool IsCisEstablished(uint8_t cig_id, uint8_t cis_id) {
    bool is_cis_established = false;
    auto it = cig_list.find(cig_id);
    if (it == cig_list.end()) {
      return false;
    } else {
      CIG *cig = it->second;
      if(cis_id != 0XFF) {
        auto it = cig->cis_list.find(cis_id);
        if (it != cig->cis_list.end()) {
          CIS *cis = it->second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_established = true;
          }
        }
      } else {
        for (auto it : cig->cis_list) {
          CIS *cis = it.second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_established = true;
            break;
          }
        }
      }
    }
    return is_cis_established;
  }

  // TODO to remove if there is no need
  bool IsCisStreaming(uint8_t cig_id, uint8_t cis_id)  {
    bool is_cis_streaming = false;
    auto it = cig_list.find(cig_id);
    if (it == cig_list.end()) {
      return false;
    } else {
      CIG *cig = it->second;
      if(cis_id != 0XFF) {
        auto it = cig->cis_list.find(cis_id);
        if (it != cig->cis_list.end()) {
          CIS *cis = it->second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_streaming = true;
          }
        }
      } else {
        for (auto it : cig->cis_list) {
          CIS *cis = it.second;
          if(cis->cis_sm.StateId() == CisStateMachine::kStateEstablished) {
            is_cis_streaming = true;
            break;
          }
        }
      }
    }
    return is_cis_streaming;
  }
};

void CisInterface::Initialize(
                   CisInterfaceCallbacks* callbacks) {
  if (instance) {
    LOG(ERROR) << "Already initialized!";
  } else {
    instance = new CisInterfaceImpl(callbacks);
  }
}

void CisInterface::CleanUp() {

  CisInterfaceImpl* ptr = instance;
  ptr->CleanUp();
  instance = nullptr;
  delete ptr;
}

CisInterface* CisInterface::Get() {
  CHECK(instance);
  return instance;
}

static void hci_cig_param_callback(tBTM_BLE_SET_CIG_RET_PARAM *param) {
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIG_CONFIGURED_EVT, param);
  }
}

static void hci_cig_add_multi_configs_callback(tBTM_BLE_ADD_CIG_MULTI_CONFIGS_RET_PARAM *param) {
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIG_MULTI_CONFIGS_ADDED_EVT, param);
  }
}

static void hci_cig_param_test_callback(tBTM_BLE_SET_CIG_PARAM_TEST_RET *param) {
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIG_CONFIGURED_EVT, param);
  }
}

static void hci_cig_remove_param_callback(uint8_t status, uint8_t cig_id) {
  tBTM_BLE_SET_CIG_REMOVE_PARAM param = { .status = status,
                                          .cig_id = cig_id };
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIG_REMOVED_EVT, &param);
  }
}

static void hci_cis_create_status_callback ( uint8_t status) {
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIS_STATUS_EVT, &status);
  }
}

static void hci_cis_create_callback (
              tBTM_BLE_CIS_ESTABLISHED_EVT_PARAM *param) {
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIS_ESTABLISHED_EVT, param);
  }
}

static void hci_cis_setup_datapath_callback (uint8_t status,
                                             uint16_t conn_handle) {
  tBTM_BLE_CIS_DATA_PATH_EVT_PARAM param = { .status = status,
                                             .conn_handle = conn_handle };
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::SETUP_DATA_PATH_DONE_EVT, &param);
  }
}

static void hci_cis_configure_datapath_callback (uint8_t status) {
  LOG(INFO) << __func__ ;
  tBTM_BLE_CONFIGURE_DATA_PATH_EVT_PARAM param = { .status = status};
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CONFIGURE_DATA_PATH_DONE_EVT, &param);
  }
}

static void hci_cis_disconnect_callback (uint8_t status, uint16_t cis_handle,
                                         uint8_t reason) {
  tBTM_BLE_CIS_DISCONNECTED_EVT_PARAM param = { .status = status,
                                                .cis_handle = cis_handle,
                                                .reason = reason
                                             };
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::CIS_DISCONNECTED_EVT, &param);
   } else {
    //Remove CIG
    LOG(ERROR) << __func__ << ": Checking if CIG remove is needed";
    for (auto it : cig_list_backup) {
      bool is_cis_active = false;
      CIG *cig = it.second;
      for (auto it : cig->cis_list) {
        CIS *cis = it.second;
        LOG(ERROR) <<__func__  << " cis list hdl: "<<cis->cis_handle<<" input hdl:"<<cis_handle;
        if(cis->cis_handle == cis_handle) {
          if(status != ISO_HCI_SUCCESS) {
            LOG(ERROR) <<__func__  << " cis disconnection failed";
            cis->cis_sm.TransitionTo(cis->cis_sm.PreviousStateId());
          } else {
            cis->cis_sm.TransitionTo(CisStateMachine::kStateReady);
          }
        }

        if((cis->cis_sm.StateId() == CisStateMachine::kStateEstablished)
          ||(cis->cis_sm.StateId() == CisStateMachine::kStateDestroying)
          ||(cis->cis_sm.StateId() == CisStateMachine::kStateEstablishing)){
            LOG(ERROR) <<__func__  << " active cis hdl: "<<cis->cis_handle;
            is_cis_active = true;
        }
      }

      LOG(ERROR) << __func__ << ": is_cis_active: " << is_cis_active;
      if (cig->cig_state == CigState::CREATED &&
          is_cis_active == false) {
        if (BTM_BleRemoveCig(cig->cig_config.cig_id,
                             &hci_cig_remove_param_callback) == HCI_SUCCESS) {
          LOG(WARNING) << __func__
                       << ": remove CIG: "<< loghex(cig->cig_config.cig_id);
          cig->cig_state = CigState::REMOVING;
        }
      }
    }
  }
}

#if 0
static void hci_cis_remove_datapath_callback ( uint8_t status,
                                              uint16_t conn_handle) {
  tBTM_BLE_CIS_DATA_PATH_EVT_PARAM param = { .status = status,
                                             .conn_handle = conn_handle };
  if (instance) {
    instance->ProcessEvent(IsoHciEvent::REMOVE_DATA_PATH_DONE_EVT, &param);
  }
}
#endif

static void btm_ble_set_encoder_limits_features_status_callback (tBTM_VSC_CMPL *param) {
  // tBTM_BLE_SET_ENCODER_LIMITS_EVT_PARAM param = {.status = status,    //commented because unused variable error
                                                 // .opcode = opcode };
  // TO-DO: Check handling required ??

}

}  // namespace ucast
}  // namespace bap
}  // namespace bluetooth
