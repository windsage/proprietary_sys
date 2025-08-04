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
#include "ucast_client_int.h"
#include "bt_trace.h"
#include "btif/include/btif_bap_codec_utils.h"
#include "osi/include/properties.h"
#include "uclient_alarm.h"
#include "btm_int.h"
#include "btif/include/btif_bap_config.h"
#include "btcore/include/device_features.h"
#include "device/include/controller.h"
#include "l2c_api.h"

namespace bluetooth {
namespace bap {
namespace ucast {

using bluetooth::bap::pacs::CodecIndex;
using bluetooth::bap::pacs::CodecBPS;
using bluetooth::bap::pacs::CodecConfig;
using bluetooth::bap::pacs::ConnectionState;
using bluetooth::bap::pacs::CodecSampleRate;
using bluetooth::bap::pacs::CodecChannelMode;
using bluetooth::bap::pacs::PacsClient;
using bluetooth::bap::cis::CisInterface;
using bluetooth::bap::ascs::AseCodecConfigOp;
using bluetooth::bap::ascs::AseQosConfigOp;
using bluetooth::bap::ascs::AseEnableOp;
using bluetooth::bap::ascs::AseStartReadyOp;
using bluetooth::bap::ascs::AseStopReadyOp;
using bluetooth::bap::ascs::AseDisableOp;
using bluetooth::bap::ascs::AseReleaseOp;
using bluetooth::bap::ascs::AseUpdateMetadataOp;
using bluetooth::bap::pacs::CodecDirection;

using cis::IsoHciStatus;
using bluetooth::bap::alarm::BapAlarm;

using bluetooth::bap::ucast::StreamUpdateType;
using bluetooth::bap::ucast::CONTENT_TYPE_MEDIA;
using bluetooth::bap::ucast::CONTENT_TYPE_CONVERSATIONAL;
using bluetooth::bap::ucast::CONTENT_TYPE_UNSPECIFIED;
using bluetooth::bap::ucast::CONTENT_TYPE_GAME;

constexpr uint8_t  LTV_TYPE_SAMP_FREQ           = 0X01;
constexpr uint8_t  LTV_TYPE_FRAME_DUR           = 0x02;
constexpr uint8_t  LTV_TYPE_CHNL_ALLOC          = 0x03;
constexpr uint8_t  LTV_TYPE_OCTS_PER_FRAME      = 0X04;
constexpr uint8_t  LTV_TYPE_FRAMES_PER_SDU      = 0X05;
constexpr uint8_t  LTV_TYPE_STRM_AUDIO_CONTEXTS = 0x02;
constexpr uint8_t  LTV_TYPE_CCID_LIST           = 0x05;

constexpr uint8_t  LTV_TYPE_VS_METADATA         = 0xFF;
constexpr uint8_t  LTV_TYPE_VS_METADATA_FE      = 0xFE;

constexpr uint8_t  LTV_TYPE_SAMP_FREQ_AAR3      = 0X81;
constexpr uint8_t  LTV_TYPE_CHNL_ALLOC_AAR3     = 0x83;
constexpr uint8_t  LTV_TYPE_SAMP_FREQ_AAR4      = 0X81;
constexpr uint8_t  LTV_TYPE_CHNL_ALLOC_AAR4     = 0x83;

constexpr uint8_t  LTV_LEN_SAMP_FREQ            = 0X02;
constexpr uint8_t  LTV_LEN_FRAME_DUR            = 0x02;
constexpr uint8_t  LTV_LEN_CHNL_ALLOC           = 0x05;
constexpr uint8_t  LTV_LEN_OCTS_PER_FRAME       = 0X03;
constexpr uint8_t  LTV_LEN_FRAMES_PER_SDU       = 0X02;
constexpr uint8_t  LTV_LEN_STRM_AUDIO_CONTEXTS  = 0x03;

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

constexpr uint8_t  LC3_CODEC_ID          = 0x06;
constexpr uint8_t  ASCS_CLIENT_ID        = 0x01;

constexpr uint8_t  TGT_LOW_LATENCY       = 0x01;
constexpr uint8_t  TGT_BAL_LATENCY       = 0x02;
constexpr uint8_t  TGT_HIGH_RELIABLE     = 0x03;

constexpr uint8_t  REMOTE_USER_TERMINATED_CONN = 0x13;
constexpr uint8_t  CIS_DISCONN_SUCCESS         = 0x00;

bool isRemoteMora = true;

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

std::list<uint8_t> directions = {
  cis::DIR_FROM_AIR,
  cis::DIR_TO_AIR
};

std::vector<uint32_t> locations = {
  AUDIO_LOC_LEFT,
  AUDIO_LOC_RIGHT
};

std::vector<RawAddress> bap_to_remote_bd_addrs;

void bap_print_codec_parameters(CodecConfig config) {
  uint8_t frame = GetFrameDuration(&config);
  uint8_t lc3blockspersdu = GetLc3BlocksPerSdu(&config);
  uint16_t octets = GetOctsPerFrame(&config);
  bool vendormetadatalc3qpref = GetCapaVendorMetaDataLc3QPref(&config);
  uint8_t vendormetadataencoderver = GetCapaVendorMetaDataCodecEncoderVer(&config);
  uint8_t vendormetadatadecoderver = GetCapaVendorMetaDataCodecDecoderVer(&config);

  LOG(WARNING) << __func__
    << ": codec_type: " << static_cast<uint32_t>(config.codec_type)
    << ", codec_priority: " << static_cast<uint32_t>(config.codec_priority)
    << ", sample_rate: " << static_cast<uint32_t>(config.sample_rate)
    << ", bits_per_sample: " << static_cast<uint32_t>(config.bits_per_sample)
    << ", channel_mode: " << static_cast<uint16_t>(config.channel_mode)
    << ", frame_duration: " << static_cast<uint32_t>(frame)
    << ", lc3_blocks_per_SDU: " << static_cast<uint32_t>(lc3blockspersdu)
    << ", octets_per_frame: " << static_cast<uint32_t>(octets)
    << ", vendormetadatalc3qpref: "
    << static_cast<uint32_t>(vendormetadatalc3qpref)
    << ", vendormetadataencoderqver: "
    << static_cast<uint32_t>(vendormetadataencoderver)
    << ", vendormetadatadecoderqver: "
    << static_cast<uint32_t>(vendormetadatadecoderver);
}

void bap_print_qos_parameters(QosConfig qos) {
  LOG(WARNING) << __func__
    << ": CIG --> cig_id=: " << static_cast<uint32_t>(qos.cig_config.cig_id)
    << ", cis_count: " << static_cast<uint32_t>(qos.cig_config.cis_count)
    << ", packing: " << static_cast<uint32_t>(qos.cig_config.packing)
    << ", framing: " << static_cast<uint32_t>(qos.cig_config.framing)
    << ", max_tport_latency_m_to_s: "
    << static_cast<uint32_t>(qos.cig_config.max_tport_latency_m_to_s)
    << ", max_tport_latency_s_to_m: "
    << static_cast<uint32_t>(qos.cig_config.max_tport_latency_s_to_m)
    << ", sdu_interval_m_to_s[0]: "
    << loghex(qos.cig_config.sdu_interval_m_to_s[0])
    << ", sdu_interval_m_to_s[1]: "
    << loghex(qos.cig_config.sdu_interval_m_to_s[1])
    << ", sdu_interval_m_to_s[2]: "
    << loghex(qos.cig_config.sdu_interval_m_to_s[2]);

  for (auto it = qos.cis_configs.begin();
                   it != qos.cis_configs.end(); ++it) {
    LOG(WARNING) << __func__
       << ": CIS --> cis_id: " << static_cast<uint32_t>(it->cis_id)
       << ", max_sdu_m_to_s: " << static_cast<uint32_t>(it->max_sdu_m_to_s)
       << ", max_sdu_s_to_m: " << static_cast<uint32_t>(it->max_sdu_s_to_m)
       << ", phy_m_to_s: " << static_cast<uint32_t>(it->phy_m_to_s)
       << ", phy_s_to_m: " << static_cast<uint32_t>(it->phy_s_to_m)
       << ", rtn_m_to_s: " << static_cast<uint32_t>(it->rtn_m_to_s)
       << ", rtn_s_to_m: " << static_cast<uint32_t>(it->rtn_s_to_m);
  }

  for (auto it = qos.ascs_configs.begin();
                   it != qos.ascs_configs.end(); ++it) {
    LOG(WARNING) << __func__
      << ": ASCS --> cig_id: " << static_cast<uint32_t>(it->cig_id)
      << ", cis_id: " << static_cast<uint32_t>(it->cis_id)
      << ", target_latency: " << static_cast<uint32_t>(it->target_latency)
      << ", bi_directional: " << static_cast<uint32_t>(it->bi_directional)
      << ", presentation_delay[0]: " << loghex(it->presentation_delay[0])
      << ", presentation_delay[1]: " << loghex(it->presentation_delay[1])
      << ", presentation_delay[2]: " << loghex(it->presentation_delay[2]);
  }
}

void AddRemoteToBapTimeoutDevices(const RawAddress& address) {
  LOG(WARNING) << __func__ << ": address: " << address;
  bap_to_remote_bd_addrs.push_back(address);
  return;
}

bool IsRemoteMatchForBapTimeoutDevices(const RawAddress& address) {
  LOG(WARNING) << __func__ << ": address: " << address;
  for (auto itr = bap_to_remote_bd_addrs.begin();
                itr != bap_to_remote_bd_addrs.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    if ((*itr) == address) {
      LOG(WARNING) << __func__ << ": address is matched.";
      return true;
    }
  }
  return false;
}

void RemoveRemoteFromBapTimeoutDevices(const RawAddress& address) {
  LOG(WARNING) << __func__ << ": address: " << address;
  bap_to_remote_bd_addrs.erase(std::remove(bap_to_remote_bd_addrs.begin(),
                                           bap_to_remote_bd_addrs.end(), address),
                                              bap_to_remote_bd_addrs.end());
  return;
}

void ClearAllBapTimeoutDevices() {
  LOG(WARNING) << __func__;
  bap_to_remote_bd_addrs.clear();
  return;
}

// common functions used from Stream tracker state Handlers
uint8_t StreamTracker::ChooseBestCodec(StreamType stream_type,
                              std::vector<CodecQosConfig> *codec_qos_configs,
                              PacsDiscovery *pacs_discovery) {
  bool codec_found = false;
  uint8_t index = 0;

  // check the stream direction, based on direction look for
  // matching record from preferred list of upper layer and
  // remote device sink or src pac records
  std::vector<CodecConfig> *pac_records = nullptr;
  if(stream_type.direction == ASE_DIRECTION_SINK) {
    pac_records = &pacs_discovery->sink_pac_records;
  } else if(stream_type.direction == ASE_DIRECTION_SRC) {
    pac_records = &pacs_discovery->src_pac_records;
  }

  if (!pac_records) {
     LOG(ERROR) << __func__ << ": pac_records is null";
     return 0xFF;
  }

  uint32_t supp_contexts_ = 0;
  btif_bap_get_supp_contexts(pacs_discovery->bd_addr, &supp_contexts_);
  LOG(WARNING) << __func__ << ": supp_contexts_: " << supp_contexts_;

  DeviceType dev_type;
  btif_bap_get_device_type(pacs_discovery->bd_addr, &dev_type,
                          static_cast <CodecDirection> (stream_type.direction));

  for (auto i = codec_qos_configs->begin(); i != codec_qos_configs->end()
                                          ; i++, index++) {
    LOG(WARNING) << __func__
                 << ": dev_type: " << loghex(static_cast<uint8_t>(dev_type))
                 << ", stream direction: " << loghex(stream_type.direction);

    if(dev_type == DeviceType::EARBUD ||
       dev_type == DeviceType::EARBUD_NO_CSIP ||
       dev_type == DeviceType::HEADSET_STEREO) {
      if((*i).qos_config.ascs_configs.size() != 1) continue;
    } else if(dev_type == DeviceType::HEADSET_SPLIT_STEREO) {
      if((*i).qos_config.ascs_configs.size() != 2) continue;
    }

    for (auto j = pac_records->begin();
                    j != pac_records->end();j++) {
      LOG(WARNING) << __func__ << ": Pacs record traversing";
      CodecConfig *src = &((*i).codec_config);
      CodecConfig *dst = &(*j);

      if (IsCodecConfigEqual(src,dst, supp_contexts_, stream_type.direction)) {
        LOG(WARNING) << __func__ << ": Checking for matching Codec";

        if (src->codec_type == CodecIndex::CODEC_INDEX_SOURCE_LC3) {
          if (GetLc3QPreference(src) &&
              GetCapaVendorMetaDataLc3QPref(dst)) {
            LOG(INFO) << __func__ << ": Matching Codec LC3Q Found "
                     << ", for direction: " << loghex(stream_type.direction);

            uint8_t lc3q_Encoder_Ver = GetCapaVendorMetaDataCodecEncoderVer(dst);
            uint8_t lc3q_Decoder_Ver = GetCapaVendorMetaDataCodecDecoderVer(dst);
            LOG(INFO) << __func__ << ": lc3q_Encoder_Ver = " << loghex(lc3q_Encoder_Ver)
                     << ", lc3q_Decoder_Ver = " << loghex(lc3q_Decoder_Ver);

            UpdateVendorMetaDataLc3QPref(src, true);
            isRemoteMora = true;

            LOG(INFO) << __func__
                      << ": GetCapaVendorMetaDataCodecEncoderVer(dst) = "
                      << loghex(GetCapaVendorMetaDataCodecEncoderVer(dst))
                      << ", GetVendorMetaDataCodecEncoderVer(src) "
                      << loghex(GetVendorMetaDataCodecEncoderVer(src))
                      << ": GetCapaVendorMetaDataCodecDecoderVer(dst) = "
                      << loghex(GetCapaVendorMetaDataCodecDecoderVer(dst))
                      << ", GetVendorMetaDataCodecDecoderVer(src) "
                      << loghex(GetVendorMetaDataCodecDecoderVer(src));
          } else {
            LOG(INFO) << __func__ << ": LC3Q not prefered, going with LC3 "
                      << "for direction: " << loghex(stream_type.direction);
            isRemoteMora = false;
          }
        } else if (src->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
          LOG(INFO) << __func__ << ": Matching Codec AptX Adaptive LE found ";
          isRemoteMora = true;
        } else if (src->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
          LOG(INFO) << __func__ << ": Matching Codec AptX Adaptive R4 found ";
          isRemoteMora = true;
        }
        codec_found = true;
        break;
      }
    }
    if(codec_found) break;
  }

  if (codec_found) {
    LOG(INFO) << __func__
              << ": Codec found on Index: " << loghex(index)
              << ", for direction: " << loghex(stream_type.direction);
    return index;
  } else {
    return 0xFF;
  }
}

// fine tuning the PD based on all remote device preferences
bool StreamTracker::ChooseBestPD() {
  std::vector<StreamType> streams;
  switch(StateId()) {
    case StreamTracker::kStateConnecting: {
      std::vector<StreamConnect> *conn_streams = GetConnStreams();

      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
    } break;
    case StreamTracker::kStateReconfiguring: {
      std::vector<StreamReconfig> *reconf_streams = GetReconfStreams();

      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
    } break;
  }

  for (auto itr = streams.begin(); itr != streams.end(); itr++) {
    // initialize the final pd value to 0
    presentation_delay_t final_pd = { 0, 0, 0 };
    for (auto itr_2 = bd_addrs_.begin(); itr_2 != bd_addrs_.end(); itr_2++) {
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr_2);
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr_2);
      StreamContext *context = contexts->FindOrAddByType(*itr);
      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        if(!stream) continue;

        ascs::AseCodecConfigParams *rem_qos_prefs = &stream->pref_qos_params;

        uint32_t rem_pd_min = (rem_qos_prefs->pd_min[0] |
                               rem_qos_prefs->pd_min[1] << 8 |
                               rem_qos_prefs->pd_min[2] << 16);

        uint32_t rem_pd_max = (rem_qos_prefs->pd_max[0] |
                               rem_qos_prefs->pd_max[1] << 8 |
                               rem_qos_prefs->pd_max[2] << 16);

        uint32_t rem_pref_pd_min = (rem_qos_prefs->pref_pd_min[0] |
                                    rem_qos_prefs->pref_pd_min[1] << 8 |
                                    rem_qos_prefs->pref_pd_min[2] << 16);

        uint32_t rem_pref_pd_max = (rem_qos_prefs->pref_pd_max[0] |
                                    rem_qos_prefs->pref_pd_max[1] << 8 |
                                    rem_qos_prefs->pref_pd_max[2] << 16);

        uint32_t pd = (final_pd[0] | final_pd[1] << 8 | final_pd[2] << 16);

        // make sure to choose max of min values of all similar streams
        if(rem_pref_pd_min && rem_pref_pd_max) {
          if(rem_pref_pd_min > pd) {
            memcpy(&final_pd, &rem_qos_prefs->pref_pd_min, sizeof(final_pd));
          }
        } else {
          if(rem_pd_min > pd) {
            memcpy(&final_pd, &rem_qos_prefs->pd_min, sizeof(final_pd));
          }
        }

        pd = (final_pd[0] | final_pd[1] << 8 | final_pd[2] << 16);
        LOG(INFO) << __func__ << " final pd: " << loghex(pd)
                     << ", rem_pd_min: " << loghex(rem_pd_min)
                     << ", rem_pd_max: " << loghex(rem_pd_max)
                     << ", rem_pref_pd_min: " << loghex(rem_pref_pd_min)
                     << ", rem_pref_pd_max: " << loghex(rem_pref_pd_max);
      }
    }

    // now apply final pd value to all similar streams of all devices
    for (auto itr_2 = bd_addrs_.begin(); itr_2 != bd_addrs_.end(); itr_2++) {
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr_2);
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr_2);
      StreamContext *context = contexts->FindOrAddByType(*itr);
      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        if(!stream) continue;
        QosConfig *dst_config = &stream->qos_config;
        memcpy(&dst_config->ascs_configs[0].presentation_delay,
               &final_pd, sizeof(final_pd));
      }
    }
  }
  return true;
}

// fine tuning the QOS params (RTN, MTL) based on
// remote device preferences
bool StreamTracker::ChooseBestQos(RawAddress addr,
                                  QosConfig *src_config,
                                  ascs::AseCodecConfigParams *rem_qos_prefs,
                                  QosConfig *dst_config,
                                  int stream_state,
                                  uint8_t stream_direction) {
  if(!src_config) {
    LOG(ERROR) << __func__
               << "src_config is NULL, returning false." ;
    return false;
  }
  uint8_t final_rtn = 0xFF;
  uint16_t final_mtl = 0xFFFF;
  bool is_config_type = false;
  uint8_t add_on_features_size = 0;

  LOG(WARNING) << __func__
               << ": stream_state: " << loghex(stream_state);

  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(addr);
  std::vector<UcastAudioStream *> streams = audio_strms->FindByCigId(
                                 src_config->ascs_configs[0].cig_id,
                                 stream_state);

  const bt_device_soc_add_on_features_t *add_on_features_list =
      controller_get_interface()->get_soc_add_on_features(&add_on_features_size);
  is_config_type = HCI_ISO_CIG_PARAMETER_CALCULATOR(add_on_features_list->as_array);
  LOG(INFO) << __func__
            << ": ISO Config Calculator sent the value: " << is_config_type;
  if(src_config)
    bap_print_qos_parameters(*src_config);
  // check if the RTN and MTL is with in the limits
  if(stream_direction == ASE_DIRECTION_SINK) {
    LOG(WARNING) << __func__
                 << ": stream_direction: " << loghex(stream_direction)
                 << ", remote_pref_rtn: " << loghex(rem_qos_prefs->pref_rtn)
                 << ", remote_pref_mtl: " << rem_qos_prefs->mtl
                 << ", isRemoteMora: " << isRemoteMora;
    if (isRemoteMora) {
      if (src_config && !(is_config_type || src_config->cis_configs.empty())) {
        if(src_config->cis_configs[0].rtn_m_to_s > rem_qos_prefs->pref_rtn) {
          LOG(INFO) << __func__ << ": Setting the RTN for Mora";
          final_rtn = rem_qos_prefs->pref_rtn;
        }
      }
    } else {
      if (src_config && !(src_config->cis_configs.empty())) {
        if(src_config->cis_configs[0].rtn_m_to_s > rem_qos_prefs->pref_rtn) {
          LOG(INFO) << __func__ << ": Setting the RTN for Non-Mora";
          final_rtn = rem_qos_prefs->pref_rtn;
        }
      }
    }
    if(src_config && src_config->cig_config.max_tport_latency_m_to_s > rem_qos_prefs->mtl) {
      final_mtl = rem_qos_prefs->mtl;
    }

  } else if(stream_direction == ASE_DIRECTION_SRC) {
    LOG(WARNING) << __func__
                 << ": stream_direction: " << loghex(stream_direction)
                 << ", remote_pref_rtn: " << loghex(rem_qos_prefs->pref_rtn)
                 << ", remote_pref_mtl: " << rem_qos_prefs->mtl
                 << ", isRemoteMora: " << isRemoteMora;
    if (isRemoteMora) {
      if (src_config && !(is_config_type || src_config->cis_configs.empty())) {
        if(src_config->cis_configs[0].rtn_s_to_m > rem_qos_prefs->pref_rtn) {
          LOG(INFO) << __func__ << ": Setting the RTN for Mora";
          final_rtn = rem_qos_prefs->pref_rtn;
        }
      }
    } else {
      if (src_config && !(src_config->cis_configs.empty())) {
        if(src_config->cis_configs[0].rtn_s_to_m > rem_qos_prefs->pref_rtn) {
          LOG(INFO) << __func__ << ": Setting the RTN for Non-Mora";
          final_rtn = rem_qos_prefs->pref_rtn;
        }
      }
    }
    if(src_config && src_config->cig_config.max_tport_latency_s_to_m > rem_qos_prefs->mtl) {
      final_mtl = rem_qos_prefs->mtl;
    }
  }

  // check if anything to be updated for all streams
  if(final_rtn == 0xFF && final_mtl == 0XFFFF) {
    LOG(WARNING) << __func__ << ": No fine tuning for QOS params";
    return true;
  }

  if(final_rtn != 0xFF) {
    LOG(WARNING) << __func__ << ": Updated RTN to: " << loghex(final_rtn);
  }

  if(final_mtl != 0XFFFF) {
    LOG(WARNING) << __func__ << ": Updated MTL to: " << loghex(final_mtl);
  }

  for (auto i = streams.begin(); i != streams.end();i++) {
    UcastAudioStream *stream = (*i);
    LOG(WARNING) << __func__ << ": stream_direction: " << loghex(stream_direction);
    if(stream_direction == ASE_DIRECTION_SINK) {
      if(final_mtl != 0xFFFF) {
        stream->qos_config.cig_config.max_tport_latency_m_to_s = final_mtl;
      }
      if(final_rtn != 0xFF) {
        for (auto it = stream->qos_config.cis_configs.begin();
                          it != stream->qos_config.cis_configs.end(); it++) {
          (*it).rtn_m_to_s = final_rtn;
        }
      }
    } else if(stream_direction == ASE_DIRECTION_SRC) {
      if(final_mtl != 0xFFFF) {
        stream->qos_config.cig_config.max_tport_latency_s_to_m = final_mtl;
      }
      if(final_rtn != 0xFF) {
        for (auto it = stream->qos_config.cis_configs.begin();
                          it != stream->qos_config.cis_configs.end(); it++) {
          (*it).rtn_s_to_m = final_rtn;
        }
      }
    }
    LOG(INFO) << __func__ << ": print qos after updating RTN and MTL values";
    bap_print_qos_parameters(stream->qos_config);
  }
  return true;
}

bool StreamTracker::HandlePacsConnectionEvent(void *p_data) {
  PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
  LOG(WARNING) << __func__
               << ": pacs_state: " << static_cast<int>(pacs_state->state);
  if(pacs_state->state == ConnectionState::CONNECTED) {
    LOG(INFO) << __func__ << ": PACS server connected";
  } else if(pacs_state->state == ConnectionState::DISCONNECTED) {
    HandleInternalDisconnect(false);
  }
  return true;
}

bool StreamTracker::HandlePacsAudioContextEvent(
                               PacsAvailableContexts *pacs_contexts) {
  std::vector<StreamUpdate> *update_streams = GetMetaUpdateStreams();
  uint8_t contexts_supported = 0;

  // check if supported audio contexts has required contexts
  for(auto it = update_streams->begin(); it != update_streams->end(); it++) {
    if(it->update_type == StreamUpdateType::STREAMING_CONTEXT) {
      if(it->stream_type.direction == ASE_DIRECTION_SINK) {
        if(it->update_value & pacs_contexts->available_contexts) {
          contexts_supported++;
        }
      } else if(it->stream_type.direction == ASE_DIRECTION_SRC) {
        if((static_cast<uint64_t>(it->update_value) << 16) &
             pacs_contexts->available_contexts) {
          contexts_supported++;
        }
      }
    }
  }

  if(contexts_supported != update_streams->size()) {
    LOG(ERROR) << __func__ << ": No Matching available Contexts found";
    return false;
  } else {
    return true;
  }
}

bool StreamTracker::HandleCisEventsInStreaming(void* p_data) {
  CisStreamState *data = (CisStreamState *) p_data;
  bool stream_found = false;
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
  if(data->state == CisState::READY) {
    for(auto it = directions.begin(); it != directions.end(); ++it) {
      if(data->direction & *it) {
        // find out the stream here
        UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                   (data->cig_id, data->cis_id, *it);
        if(stream) {
          stream->cis_state = data->state;
          stream->cis_pending_cmd = CisPendingCmd::NONE;
          // change the overall state to stopping
          stream->overall_state = StreamTracker::kStateStopping;
          stream_found = true;
        }
      }
    }
    if(stream_found) {
      TransitionTo(StreamTracker::kStateStopping);
    }
  }
  return true;
}

bool StreamTracker::HandleAscsConnectionEvent(void *p_data) {
  AscsConnectionState *ascs_state =  (AscsConnectionState *) p_data;
  LOG(WARNING) << __func__
               << ": ascs_state: " << static_cast<int>(ascs_state->state);
  if(ascs_state->state == GattState::CONNECTED) {
    LOG(INFO) << __func__ << ": ASCS server connected";
  } else if(ascs_state->state == GattState::DISCONNECTED) {
    // make all streams ASE state ot idle so that further processing
    // can happen
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                                ascs_state->bd_addr);
    std::vector<UcastAudioStream *> *strms_list = audio_strms->GetAllStreams();

    for (auto it = strms_list->begin(); it != strms_list->end(); it++) {
      (*it)->ase_state = ascs::ASE_STATE_IDLE;
      (*it)->ase_pending_cmd = AscsPendingCmd::NONE;
      (*it)->overall_state = StreamTracker::kStateIdle;
    }
    HandleInternalDisconnect(false);
  }
  return true;
}

bool StreamTracker::ValidateAseUpdate(void* p_data,
                                      IntStrmTrackers *int_strm_trackers,
                                      int exp_strm_state) {
  AscsState *ascs =  ((AscsState *) p_data);

  uint8_t ase_id = ascs->ase_params.ase_id;

  // check if current stream tracker is interested in this ASE update
  if(int_strm_trackers->FindByAseId(ase_id)
                             == nullptr) {
    LOG(INFO) << __func__ << ": Not intended for this tracker";
    return false;
  }

  // find out the stream for the given ase id
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);

  UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);

  if (stream == nullptr) {
    LOG(WARNING) << __func__ << ": No Audio Stream found";
    return false;
  }

  LOG(INFO) << __func__ << ": Streams Size = " << audio_strms->size()
                        << ", ASE Id = " << loghex(ase_id)
                        << ", stream->overall_state: " << loghex(stream->overall_state);

  if (stream->overall_state != exp_strm_state) {
    LOG(WARNING) << __func__ << ": Stream overall_state is not expected_strm_state";
    return false;
  }

  stream->ase_state = ascs->ase_params.ase_state;
  stream->ase_params = ascs->ase_params;
  stream->ase_pending_cmd = AscsPendingCmd::NONE;
  return true;
}

bool StreamTracker::HandleRemoteDisconnect(uint32_t event,
                                           void* p_data, int cur_state) {
  UpdateControlType(StreamControlType::Disconnect);
  std::vector<StreamType> streams;

  switch(cur_state) {
    case StreamTracker::kStateConnecting: {
      std::vector<StreamConnect> *conn_streams = GetConnStreams();

      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
      UpdateStreams(&streams);
    } break;
    case StreamTracker::kStateReconfiguring: {
      std::vector<StreamReconfig> *reconf_streams = GetReconfStreams();

      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
      UpdateStreams(&streams);
    } break;
  }

  // update the state to disconnecting
  TransitionTo(StreamTracker::kStateDisconnecting);
  ProcessEvent(event, p_data);
  return true;
}

bool StreamTracker::StreamCanbeDisconnected(RawAddress bd_addr,
                                            StreamContext *cur_context,
                                            uint8_t ase_id) {
  bool can_be_disconnected = false;
  StreamContexts *contexts = strm_mgr_->GetStreamContexts(bd_addr);
  StreamAttachedState state = (StreamAttachedState)
               (static_cast<uint8_t> (StreamAttachedState::PHYSICAL) |
                static_cast<uint8_t> (StreamAttachedState::IDLE_TO_PHY) |
                static_cast<uint8_t> (StreamAttachedState::VIR_TO_PHY));

  std::vector<StreamContext *> attached_contexts =
                 contexts->FindByAseAttachedState(ase_id, state);

  LOG(INFO) << __func__
            << ": attached_contexts: size : " << attached_contexts.size()
            << ", cur_context->attached_state: "
            << loghex(static_cast<uint16_t>(cur_context->attached_state));
  if(cur_context->attached_state == StreamAttachedState::PHYSICAL ||
     cur_context->attached_state == StreamAttachedState::IDLE_TO_PHY ||
     cur_context->attached_state == StreamAttachedState::VIR_TO_PHY ) {
    can_be_disconnected = true;
  }
  return can_be_disconnected;
}

bool StreamTracker::HandleInternalDisconnect(bool release) {

  UpdateControlType(StreamControlType::Disconnect);

  std::vector<StreamType> streams;

  int cur_state = StateId();
  LOG(WARNING) << __func__ << ": cur_state: " << cur_state
                           << ", release: " << release;
  switch(cur_state) {
    case StreamTracker::kStateConnecting: {
      std::vector<StreamConnect> *conn_streams = GetConnStreams();

      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
      UpdateStreams(&streams);
    } break;
    case StreamTracker::kStateReconfiguring: {
      std::vector<StreamReconfig> *reconf_streams = GetReconfStreams();

      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end(); it++) {
        StreamType type = it->stream_type;
        streams.push_back(type);
      }
      UpdateStreams(&streams);
    } break;
  }

  if (release) {
    for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
      std::vector<AseReleaseOp> ase_ops;
      std::vector<StreamType> *disc_streams = GetStreams();

      for (auto it = disc_streams->begin(); it != disc_streams->end(); it++) {
        StreamContext *context = contexts->FindOrAddByType(*it);

        for (auto id = context->stream_ids.begin();
                           id != context->stream_ids.end(); id++) {

          UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
          bool can_be_disconnected = StreamCanbeDisconnected(*itr,
                                                      context, id->ase_id);
          if (can_be_disconnected &&
              stream && stream->overall_state == cur_state &&
              stream->ase_state != ascs::ASE_STATE_IDLE &&
              stream->ase_state != ascs::ASE_STATE_RELEASING &&
              stream->ase_pending_cmd != AscsPendingCmd::RELEASE_ISSUED) {
            LOG(WARNING) << __func__
                         << ": ASE State : " << loghex(stream->ase_state);
            AseReleaseOp release_op = {
                                        .ase_id = stream->ase_id
                                      };
            ase_ops.push_back(release_op);
            stream->ase_pending_cmd = AscsPendingCmd::RELEASE_ISSUED;
            // change the overall state to Disconnecting
            stream->overall_state = StreamTracker::kStateDisconnecting;
          }
        }
      }

      // send consolidated release command to ASCS client
      if(ase_ops.size()) {
        LOG(WARNING) << __func__ << ": Going For ASCS Release op";
        ascs_client->Release(ASCS_CLIENT_ID, *itr, ase_ops);
      }
    }
  }

  // update the state to disconnecting
  TransitionTo(StreamTracker::kStateDisconnecting);
  return true;
}

bool StreamTracker::HandleRemoteStop(uint32_t event,
                                           void* p_data, int cur_state) {
  AscsState *ascs =  ((AscsState *) p_data);
  uint8_t ase_id = ascs->ase_params.ase_id;
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);
  UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);
  std::vector<StreamType> *stop_streams = GetStreams();
  StreamContexts *contexts = strm_mgr_->GetStreamContexts(ascs->bd_addr);
  if(!stream) return false;

  if(stream->direction & cis::DIR_TO_AIR &&
     ascs->ase_params.ase_state == ascs::ASE_STATE_DISABLING) {
    LOG(ERROR) << __func__ << ": Invalid State transition to Disabling"
               << ": ASE Id = " << loghex(ase_id);
    return false;
  }

  UpdateControlType(StreamControlType::Stop);

  if(cur_state != StreamTracker::kStateStarting &&
     cur_state != StreamTracker::kStateStreaming) {
    return false;
  }

  // update the state to stopping
  for (auto it = stop_streams->begin();
                       it != stop_streams->end(); it++) {
    StreamContext *context = contexts->FindOrAddByType(*it);
    for (auto id = context->stream_ids.begin();
              id != context->stream_ids.end(); id++) {
      UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
      if (stream != nullptr) {
        // change the overall state to stopping
        stream->overall_state = StreamTracker::kStateStopping;
      }
    }
  }
  TransitionTo(StreamTracker::kStateStopping);
  ProcessEvent(event, p_data);
  return true;
}

bool StreamTracker::HandleAbruptStop(uint32_t event, void* p_data) {
  AscsState *ascs =  ((AscsState *) p_data);
  uint8_t ase_id = ascs->ase_params.ase_id;
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);
  UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);

  if(!stream) return false;

  stream->ase_pending_cmd = AscsPendingCmd::NONE;
  stream->overall_state = StreamTracker::kStateStopping;

  UpdateControlType(StreamControlType::Stop);

  // update the state to stopping
  TransitionTo(StreamTracker::kStateStopping);
  return true;
}

bool StreamTracker::HandleRemoteReconfig(uint32_t event,
                                           void* p_data, int cur_state) {
  UpdateControlType(StreamControlType::Reconfig);
  std::vector<StreamType> streams;

  if(cur_state != StreamTracker::kStateConnected) {
    return false;
  }
  // update the state to Reconfiguring
  TransitionTo(StreamTracker::kStateReconfiguring);
  ProcessEvent(event, p_data);
  return true;
}

void StreamTracker::HandleAseOpFailedEvent(void *p_data) {
  AscsOpFailed *ascs_op =  ((AscsOpFailed *) p_data);
  std::vector<ascs::AseOpStatus> *ase_list = &ascs_op->ase_list;
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs_op->bd_addr);

  if(ascs_op->ase_op_id == ascs::AseOpId::CODEC_CONFIG) {
    // treat it like internal failure
    for (auto i = ase_list->begin(); i != ase_list->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((i)->ase_id);
      if(stream) {
        stream->ase_pending_cmd = AscsPendingCmd::NONE;
        stream->overall_state = StreamTracker::kStateIdle;
      }
    }
    HandleInternalDisconnect(false);
  } else if (ascs_op->ase_op_id == ascs::AseOpId::ENABLE) {
    int cur_state = StateId();
    LOG(WARNING) << __func__ << ": cur_state: " << cur_state;
    HandleStop(nullptr, cur_state);
  } else {
    HandleInternalDisconnect(true);
  }
}

void StreamTracker::HandleAseStateEvent(void *p_data,
                  StreamControlType control_type,
                  std::map<RawAddress, IntStrmTrackers> *int_strm_trackers) {
  // check the state and if the state is codec configured for all ASEs
  // then proceed with group creation
  AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);

  uint8_t ase_id = ascs->ase_params.ase_id;

  // check if current stream tracker is interested in this ASE update
  if((*int_strm_trackers)[ascs->bd_addr].FindByAseId(ase_id) == nullptr) {
    LOG(INFO) << __func__ << ": Not intended for this tracker";
    return;
  }

  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);

  UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);

  if(stream == nullptr) {
    return;
  } else {
    stream->ase_state = ascs->ase_params.ase_state;
    stream->ase_params = ascs->ase_params;
    stream->ase_pending_cmd = AscsPendingCmd::NONE;
  }

  if(ascs->ase_params.ase_state == ascs::ASE_STATE_CODEC_CONFIGURED) {
    stream->pref_qos_params = ascs->ase_params.codec_config_params;
    // find out the stream for the given ase id
    LOG(INFO) << __func__ << ": Total Num Streams = " << audio_strms->size()
                          << ", ASE Id = " << loghex(ase_id);

    // decide on best QOS params by comparing the upper layer prefs
    // and remote dev's preferences
    int state = StreamTracker::kStateIdle;

    if(control_type == StreamControlType::Connect) {
      state = StreamTracker::kStateConnecting;
    } else if(control_type == StreamControlType::Reconfig) {
      state = StreamTracker::kStateReconfiguring;
    }

    // check for all trackers codec is configured or not
    uint8_t num_codec_configured = 0;
    uint8_t num_trackers = 0;
    for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
      std::vector<IntStrmTracker *> *all_trackers =
                        (*int_strm_trackers)[*itr].GetTrackerList();
      for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
        UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
        if(stream && stream->ase_pending_cmd == AscsPendingCmd::NONE &&
           (stream->ase_state == ascs::ASE_STATE_CODEC_CONFIGURED ||
            (control_type == StreamControlType::Reconfig &&
             stream->ase_state == ascs::ASE_STATE_QOS_CONFIGURED))) {
          num_codec_configured++;
        }
        num_trackers++;
      }
    }

    if(num_trackers != num_codec_configured) {
      LOG(WARNING) << __func__ << ": Codec Not Configured For All Streams";
      return;
    }

    // check for all streams together so that final group params
    // will be decided
    for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
      std::vector<IntStrmTracker *> *all_trackers =
                        (*int_strm_trackers)[*itr].GetTrackerList();
      for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
        UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
        if (stream) {
          ChooseBestQos(*itr,
                        &stream->req_qos_config, &stream->pref_qos_params,
                        &stream->qos_config, state, stream->direction);
        }
      }
    }

    ChooseBestPD();

    for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
      CheckAndSendQosConfig(*itr, &(*int_strm_trackers)[*itr]);
    }

  } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
    // TODO update the state as connected using callbacks
    // make the state transition to connected

    // check for all trackers QOS is configured or not
    // if so update it as streams are connected
    std::vector<IntStrmTracker *> *all_trackers =
                        (*int_strm_trackers)[ascs->bd_addr].GetTrackerList();
    uint8_t num_qos_configured = 0;
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && stream->ase_state == ascs::ASE_STATE_QOS_CONFIGURED &&
         stream->ase_pending_cmd == AscsPendingCmd::NONE) {
        num_qos_configured++;
      }
    }

    if((*int_strm_trackers)[ascs->bd_addr].size() != num_qos_configured) {
      LOG(WARNING) << __func__ << ": QOS Not Configured For All Streams";
      return;
    }

    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (!stream) {
        LOG(ERROR) << __func__ << ": stream is null";
        continue;
      }
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(ascs->bd_addr);
      StreamContext *context = contexts->FindOrAddByType(
                                         (*i)->strm_type);
      if(context->attached_state == StreamAttachedState::IDLE_TO_PHY ||
         context->attached_state == StreamAttachedState::VIR_TO_PHY) {
        context->attached_state = StreamAttachedState::PHYSICAL;
        LOG(INFO) << __func__ << ": Attached state made physical";
      }
      stream->overall_state = kStateConnected;
    }

    uint16_t num_trackers = 0;
    uint16_t total_strms_connected = 0;
    for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
       UcastAudioStreams *audio_strms =
                               strm_mgr_->GetAudioStreams(*itr);
      std::vector<IntStrmTracker *> *all_trackers =
                        (*int_strm_trackers)[*itr].GetTrackerList();
      for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
        UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
        if(stream && stream->overall_state == kStateConnected) {
          total_strms_connected++;
        }
        num_trackers++;
      }
    }
    // update the state to connected
    if(num_trackers == total_strms_connected) {
      TransitionTo(StreamTracker::kStateConnected);
    }

  } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
    HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());
  }
}

bool StreamTracker::HandleStreamUpdate (int cur_state) {
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

    std::vector<AseUpdateMetadataOp> ase_meta_ops;
    std::vector<StreamUpdate> *update_streams = GetMetaUpdateStreams();

    for (auto it = update_streams->begin();
                         it != update_streams->end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(it->stream_type);

      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        std::vector<uint8_t> meta_data;
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        if(stream && stream->ase_state != ascs::ASE_STATE_ENABLING &&
           stream->ase_state != ascs::ASE_STATE_STREAMING) {
          continue;
        }
        if(it->update_type == StreamUpdateType::STREAMING_CONTEXT) {
          uint8_t len = LTV_LEN_STRM_AUDIO_CONTEXTS;
          uint8_t type = LTV_TYPE_STRM_AUDIO_CONTEXTS;
          uint16_t value = it->update_value;
          if(stream) stream->audio_context = value;
          meta_data.insert(meta_data.end(), &len, &len + 1);
          meta_data.insert(meta_data.end(), &type, &type + 1);
          meta_data.insert(meta_data.end(), ((uint8_t *)&value),
                                ((uint8_t *)&value) + sizeof(uint16_t));
        }

        AseUpdateMetadataOp meta_op = {
                              .ase_id = id->ase_id,
                              .meta_data_len =
                               static_cast <uint8_t> (meta_data.size()),
                              .meta_data = meta_data // media or voice
                            };
        ase_meta_ops.push_back(meta_op);
      }
    }

    // send consolidated update meta command to ASCS client
    if(ase_meta_ops.size()) {
      LOG(WARNING) << __func__ << ": Going For ASCS Update MetaData op";
      ascs_client->UpdateStream(ASCS_CLIENT_ID, *itr, ase_meta_ops);
    } else {
      return false;
    }

    if(cur_state == StreamTracker::kStateUpdating) {
      for (auto it = update_streams->begin();
                           it != update_streams->end(); it++) {
        StreamContext *context = contexts->FindOrAddByType(it->stream_type);
        for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
          UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
          if (stream != nullptr) {
            // change the connection state to disable issued
            stream->ase_pending_cmd = AscsPendingCmd::UPDATE_METADATA_ISSUED;
            // change the overall state to Updating
            stream->overall_state = StreamTracker::kStateUpdating;
          }
        }
      }
    }
  }
  return true;
}

bool StreamTracker::HandleStop(void* p_data, int cur_state) {

  LOG(WARNING) << __func__ << ": cur_state: " << cur_state;

  if(p_data != nullptr) {
    BapStop *evt_data = (BapStop *) p_data;
    UpdateStreams(&evt_data->streams);
  }
  UpdateControlType(StreamControlType::Stop);

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

    std::vector<AseDisableOp> ase_ops;
    std::vector<StreamType> *stop_streams = GetStreams();

    for (auto it = stop_streams->begin();
                         it != stop_streams->end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(*it);
      bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                          (it->direction == ASE_DIRECTION_SRC) &&
                          (it->audio_context == CONTENT_TYPE_GAME);
      for (auto id = context->stream_ids.begin();
                           id != context->stream_ids.end(); id++) {
        AseDisableOp disable_op = {
                            .ase_id = id->ase_id
        };
        ase_ops.push_back(disable_op);
        if (is_gaming_rx && context->is_mono_config) {
          LOG(ERROR) << __func__ << ": Defer sending disable to another ASE";
          break;
        }
      }
    }

    // send consolidated disable command to ASCS client
    if(ase_ops.size()) {
      LOG(WARNING) << __func__ << ": Going For ASCS Disable op";
      ascs_client->Disable(ASCS_CLIENT_ID, *itr, ase_ops);
    }

    for (auto it = stop_streams->begin();
                         it != stop_streams->end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(*it);
      for (auto id = context->stream_ids.begin();
                    id != context->stream_ids.end(); id++) {
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        if (stream != nullptr && stream->overall_state == cur_state) {
          // change the connection state to disable issued
          stream->ase_pending_cmd = AscsPendingCmd::DISABLE_ISSUED;
          // change the overall state to stopping
          stream->overall_state = StreamTracker::kStateStopping;
        }
      }
    }
  }
  TransitionTo(StreamTracker::kStateStopping);
  return true;
}

bool StreamTracker::HandleDisconnect(void* p_data, int cur_state) {
  // expect the disconnection for same set of streams connection
  // has initiated ex: if connect is issued for media (tx), voice(tx & rx)
  // then disconnect is expected for media (tx), voice(tx & rx).
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

    BapDisconnect *evt_data = (BapDisconnect *) p_data;

    UpdateControlType(StreamControlType::Disconnect);

    UpdateStreams(&evt_data->streams);

    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);

    std::vector<AseReleaseOp> ase_ops;
    std::vector<StreamType> *disc_streams = GetStreams();

    for (auto it = disc_streams->begin(); it != disc_streams->end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(*it);
      LOG(WARNING) << __func__ << ": Checking for streams to be disconnected.";

      for (auto id = context->stream_ids.begin();
                    id != context->stream_ids.end(); id++) {
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        bool can_be_disconnected = StreamCanbeDisconnected(*itr,
                                         context, id->ase_id);
        LOG(WARNING) << __func__ << ": can_be_disconnected: " << can_be_disconnected;

        if (stream) {
          LOG(WARNING) << __func__
                       << ": stream->overall_state: " << loghex(stream->overall_state);
        }

        if(can_be_disconnected &&
           stream && stream->overall_state != StreamTracker::kStateIdle &&
           stream->overall_state != StreamTracker::kStateDisconnecting &&
           stream->ase_pending_cmd != AscsPendingCmd::RELEASE_ISSUED) {
          AseReleaseOp release_op = {
                                      .ase_id = id->ase_id
                                    };
          ase_ops.push_back(release_op);
          stream->ase_pending_cmd = AscsPendingCmd::RELEASE_ISSUED;
          // change the overall state to starting
          stream->overall_state = StreamTracker::kStateDisconnecting;
        }
      }
    }

    LOG(INFO) << __func__ << ": ase_ops size: " << ase_ops.size();
    // send consolidated release command to ASCS client
    if(ase_ops.size() && (ascs_client != nullptr)) {
      LOG(WARNING) << __func__ << ": Going For ASCS Release op";
      ascs_client->Release(ASCS_CLIENT_ID, *itr, ase_ops);
    }
  }

  TransitionTo(StreamTracker::kStateDisconnecting);
  return true;
}

void StreamTracker::CheckAndSendQosConfig(RawAddress bd_addr,
                                          IntStrmTrackers *int_strm_trackers) {

  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(bd_addr);
  AscsClient *ascs_client = strm_mgr_->GetAscsClient(bd_addr);
  // check for all trackers CIG is created or not
  // if so proceed with QOS config operaiton
  std::vector<IntStrmTracker *> *all_trackers =
                      int_strm_trackers->GetTrackerList();

  std::vector<AseQosConfigOp> ase_ops;
  for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
    UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
    QosConfig *qos_config = &stream->qos_config;
    if(!stream || stream->ase_pending_cmd != AscsPendingCmd::NONE ||
       !qos_config || qos_config->cis_configs.empty()) {
      continue;
    }
    if(stream->direction & cis::DIR_TO_AIR) {

      AseQosConfigOp qos_config_op = {
       .ase_id = (*i)->ase_id,
       .cig_id = stream->cig_id,
       .cis_id = stream->cis_id,
       .sdu_interval = { qos_config->cig_config.sdu_interval_m_to_s[0],
                         qos_config->cig_config.sdu_interval_m_to_s[1],
                         qos_config->cig_config.sdu_interval_m_to_s[2] },
       .framing = qos_config->cig_config.framing,
       .phy = LE_2M_PHY,
       .max_sdu_size = qos_config->cis_configs[(*i)->cis_id].max_sdu_m_to_s,
       .retrans_number = qos_config->cis_configs[(*i)->cis_id].rtn_m_to_s,
       .trans_latency = qos_config->cig_config.max_tport_latency_m_to_s,
       .present_delay = {qos_config->ascs_configs[0].presentation_delay[0],
                         qos_config->ascs_configs[0].presentation_delay[1],
                         qos_config->ascs_configs[0].presentation_delay[2]}
      };
      ase_ops.push_back(qos_config_op);

    } else if(stream->direction & cis::DIR_FROM_AIR) {
      AseQosConfigOp qos_config_op = {
       .ase_id = (*i)->ase_id,
       .cig_id = stream->cig_id,
       .cis_id = stream->cis_id,
       .sdu_interval = { qos_config->cig_config.sdu_interval_s_to_m[0],
                         qos_config->cig_config.sdu_interval_s_to_m[1],
                         qos_config->cig_config.sdu_interval_s_to_m[2] },
       .framing = qos_config->cig_config.framing,
       .phy = LE_2M_PHY,
       .max_sdu_size = qos_config->cis_configs[(*i)->cis_id].max_sdu_s_to_m,
       .retrans_number = qos_config->cis_configs[(*i)->cis_id].rtn_s_to_m,
       .trans_latency = qos_config->cig_config.max_tport_latency_s_to_m,
       .present_delay = {qos_config->ascs_configs[0].presentation_delay[0],
                         qos_config->ascs_configs[0].presentation_delay[1],
                         qos_config->ascs_configs[0].presentation_delay[2]}
      };
      ase_ops.push_back(qos_config_op);
    }
  }

  if(ase_ops.size()) {
    LOG(WARNING) << __func__ << ": Going For ASCS QosConfig op";
    ascs_client->QosConfig(ASCS_CLIENT_ID, bd_addr, ase_ops);

    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && stream->ase_pending_cmd == AscsPendingCmd::NONE)
        stream->ase_pending_cmd = AscsPendingCmd::QOS_CONFIG_ISSUED;
    }
  }
}


void StreamTracker::CheckAndSendEnable(RawAddress bd_addr,
                                       IntStrmTrackers *int_strm_trackers) {

  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(bd_addr);
  AscsClient *ascs_client = strm_mgr_->GetAscsClient(bd_addr);
  std::vector<StreamType> *start_streams = GetStreams();
  StreamContexts *contexts = strm_mgr_->GetStreamContexts(bd_addr);
  std::vector<AseEnableOp> ase_ops;
  // check for all trackers CIG is created or not
  // if so proceed with QOS config operaiton
  std::vector<IntStrmTracker *> *all_trackers =
                      int_strm_trackers->GetTrackerList();

  uint8_t num_cig_created = 0;

  for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
    UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
    if(stream && stream->cig_state == CigState::CREATED) {
      num_cig_created++;
    }
  }

  if(int_strm_trackers->size() != num_cig_created) {
    LOG(WARNING) << __func__ << ": All CIGs are not created";
    return;
  }

  for(auto it = start_streams->begin(); it != start_streams->end(); it++) {
    bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                        (it->direction == ASE_DIRECTION_SRC) &&
                        (it->audio_context == CONTENT_TYPE_GAME);
    StreamContext *context = contexts->FindOrAddByType(*it);
    for (auto id = context->stream_ids.begin();
              id != context->stream_ids.end(); id++) {
      std::vector<uint8_t> meta_data;
      UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
      uint8_t len = LTV_LEN_STRM_AUDIO_CONTEXTS;
      uint8_t type = LTV_TYPE_STRM_AUDIO_CONTEXTS;
      uint16_t value = (*it).audio_context;
      if(stream) stream->audio_context = value;
      meta_data.insert(meta_data.end(), &len, &len + 1);
      meta_data.insert(meta_data.end(), &type, &type + 1);
      meta_data.insert(meta_data.end(), ((uint8_t *)&value),
                              ((uint8_t *)&value) + sizeof(uint16_t));

      char bap_pts_send_ccid[PROPERTY_VALUE_MAX] = {'\0'};
      uint8_t ccid_for_pts = 0;
      property_get("persist.vendor.btstack.bap_pts_send_ccid", bap_pts_send_ccid, "0");

      int res = sscanf(bap_pts_send_ccid, "%hhu", &ccid_for_pts);

      LOG(INFO) <<__func__ <<": res: " << res;
      LOG(INFO) <<__func__ <<": ccid_for_pts: " << ccid_for_pts;

      std::vector<uint8_t> ccid_values;
      if ((res == 1) && (ccid_for_pts != 0)) {
        LOG(INFO) <<__func__ <<": bap pts test  enabled";
        if (ccid_for_pts == 1) {
          len = 2; //Single CCID
          type = LTV_TYPE_CCID_LIST;
          if(value == CONTENT_TYPE_MEDIA) {
            LOG(INFO) <<__func__ <<": value == CONTENT_TYPE_MEDIA";
            ccid_values = {0}; //Dummy value, TBD
          } else if(value == CONTENT_TYPE_CONVERSATIONAL) {
            LOG(INFO) <<__func__ <<": value == CONTENT_TYPE_CONVERSATIONAL";
            ccid_values = {1}; //Dummy value, TBD
          } else {
            LOG(INFO) <<__func__ <<": default CCID";
            ccid_values = {0}; //Dummy value, TBD
          }
        } else if (ccid_for_pts == 2) {
          len = 3; //Multi CCID
          type = LTV_TYPE_CCID_LIST;
          if(value == CONTENT_TYPE_MEDIA) {
            LOG(INFO) <<__func__ <<": value == CONTENT_TYPE_MEDIA";
            ccid_values = {0, 0}; //Dummy value, TBD
          } else if(value == CONTENT_TYPE_CONVERSATIONAL) {
            LOG(INFO) <<__func__ <<": value == CONTENT_TYPE_CONVERSATIONAL";
            ccid_values = {1, 1}; //Dummy value, TBD
          } else {
            LOG(INFO) <<__func__ <<": default CCID";
            ccid_values = {0, 0}; //Dummy value, TBD
          }
        }
        meta_data.insert(meta_data.end(), &len, &len + 1);
        meta_data.insert(meta_data.end(), &type, &type + 1);
        meta_data.insert(meta_data.end(), ((uint8_t *)&ccid_values.front()),
                         ((uint8_t *)&ccid_values.front()) + ccid_values.size() * sizeof(uint8_t));
      }

      AseEnableOp enable_op = {
                            .ase_id = id->ase_id,
                            .meta_data_len =
                             static_cast <uint8_t> (meta_data.size()),
                            .meta_data = meta_data // media or voice
                          };
      ase_ops.push_back(enable_op);
      if (is_gaming_rx && context->is_mono_config) {
        LOG(ERROR) << __func__ << ": Defer sending enable to another ASE";
        break;
      }
    }
  }

  // send consolidated enable command to ASCS client
  if(ase_ops.size()) {
    LOG(WARNING) << __func__ << ": Going For ASCS Enable op";
    ascs_client->Enable(ASCS_CLIENT_ID, bd_addr, ase_ops);

    for (auto it = start_streams->begin(); it != start_streams->end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(*it);
      bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                          (it->direction == ASE_DIRECTION_SRC) &&
                          (it->audio_context == CONTENT_TYPE_GAME);
      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
        if (stream != nullptr && stream->overall_state ==
                               StreamTracker::kStateConnected) {
          // change the connection state to enable issued
          stream->ase_pending_cmd = AscsPendingCmd::ENABLE_ISSUED;
          // change the overall state to starting
          stream->overall_state = StreamTracker::kStateStarting;
        }
        if (getMode()) {
          if (is_gaming_rx && context->is_mono_config) {
            LOG(ERROR) << __func__  << ": Defer setting overall_state Starting";
            break;
          }
        }
      }
    }
  }
}

void StreamTracker::HandleCigStateEvent(uint32_t event, void *p_data,
                                        IntStrmTrackers *int_strm_trackers) {
  // check if the associated CIG state is created
  // if so go for Enable Operation
  CisGroupState *data = ((CisGroupState *) p_data);

  // check if current stream tracker is interested in this CIG update
  std::vector<IntStrmTracker *> int_trackers =
                        int_strm_trackers->FindByCigId(data->cig_id);
  if(int_trackers.empty()) {
    LOG(INFO) << __func__ << ": Not intended for this tracker";
    return;
  }

  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
  if(data->state == CigState::CREATED) {
    for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream) {
        stream->cis_pending_cmd = CisPendingCmd::NONE;
        stream->cig_state = data->state;
        stream->cis_state = CisState::READY;
      }
    }
    CheckAndSendEnable(data->bd_addr, int_strm_trackers);
  } else if(data->state == CigState::IDLE) {
    // CIG state is idle means there is some failure
    LOG(ERROR) << __func__ << ": CIG Creation Failed";
    for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream) {
        stream->cig_state = CigState::INVALID;
        stream->cis_state = CisState::INVALID;
        stream->cis_pending_cmd = CisPendingCmd::NONE;;
      }
    }
    //HandleInternalDisconnect(false);
    //Don't disconnect, instead move to connected state.
    TransitionTo(StreamTracker::kStateConnected);
    return;
  }
}

bool StreamTracker::PrepareCodecConfigPayload(
                                 std::vector<AseCodecConfigOp> *ase_ops,
                                 UcastAudioStream *stream) {
  std::vector<uint8_t> codec_params;
  uint8_t tgt_latency = TGT_HIGH_RELIABLE;
  // check for sampling freq
  for (auto it : freq_to_ltv_map) {
    if(stream->codec_config.sample_rate == it.first) {
      uint8_t len = LTV_LEN_SAMP_FREQ;
      uint8_t type;
      if (stream->codec_config.codec_type ==
                   CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
        type = LTV_TYPE_SAMP_FREQ_AAR3;
      } else if (stream->codec_config.codec_type ==
                   CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
        type = LTV_TYPE_SAMP_FREQ_AAR4;
      } else {
        type = LTV_TYPE_SAMP_FREQ; // default LC3
      }
      uint8_t rate = it.second;
      codec_params.insert(codec_params.end(), &len, &len + 1);
      codec_params.insert(codec_params.end(), &type, &type + 1);
      codec_params.insert(codec_params.end(), &rate, &rate + 1);
      break;
    }
  }

  if (stream->codec_config.codec_type == CodecIndex::CODEC_INDEX_SOURCE_LC3) {
    LOG(INFO) << __func__ << ": Codec type is LC3 ";
    // check for frame duration and fetch 5th byte
    uint8_t frame_duration = GetFrameDuration(&stream->codec_config);
    LOG(INFO) << __func__ << ": frame_duration: " << loghex(frame_duration);
    uint8_t len = LTV_LEN_FRAME_DUR;
    uint8_t type = LTV_TYPE_FRAME_DUR;
    codec_params.insert(codec_params.end(), &len, &len + 1);
    codec_params.insert(codec_params.end(), &type, &type + 1);
    codec_params.insert(codec_params.end(), &frame_duration,
                                            &frame_duration + 1);
  }

  // audio chnl allcation
  if(stream->audio_location) {
    uint8_t len = LTV_LEN_CHNL_ALLOC;
    uint8_t type;
    if (stream->codec_config.codec_type ==
                  CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
      type = LTV_TYPE_CHNL_ALLOC_AAR3 ;
    } else if (stream->codec_config.codec_type ==
                  CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
      type = LTV_TYPE_CHNL_ALLOC_AAR4 ;
    } else {
      type = LTV_TYPE_CHNL_ALLOC; // default LC3
    }
    LOG(INFO) << __func__ << ": audio_location: " << stream->audio_location;
    uint32_t value = stream->audio_location & (AUDIO_LOC_LEFT | AUDIO_LOC_RIGHT);
    codec_params.insert(codec_params.end(), &len, &len + 1);
    codec_params.insert(codec_params.end(), &type, &type + 1);
    codec_params.insert(codec_params.end(), ((uint8_t *)&value),
                           ((uint8_t *)&value) + sizeof(uint32_t));
  }

  if (stream->codec_config.codec_type == CodecIndex::CODEC_INDEX_SOURCE_LC3) {
    LOG(INFO) << __func__ << ": Codec type is LC3 ";
    // octets per frame
    uint8_t len = LTV_LEN_OCTS_PER_FRAME;
    uint8_t type = LTV_TYPE_OCTS_PER_FRAME;
    uint16_t octs_per_frame = GetOctsPerFrame(&stream->codec_config);
    LOG(INFO) << __func__ << ": octs_per_frame: " << octs_per_frame;
    codec_params.insert(codec_params.end(), &len, &len + 1);
    codec_params.insert(codec_params.end(), &type, &type + 1);
    codec_params.insert(codec_params.end(), ((uint8_t *)&octs_per_frame),
                          ((uint8_t *)&octs_per_frame) + sizeof(uint16_t));

    // blocks per SDU
    len = LTV_LEN_FRAMES_PER_SDU;
    type = LTV_TYPE_FRAMES_PER_SDU;
    uint8_t blocks_per_sdu = GetLc3BlocksPerSdu(&stream->codec_config);
    LOG(INFO) << __func__ << ": blocks_per_sdu: " << loghex(blocks_per_sdu);
    // initialize it to 1 if it doesn't exists
    if(!blocks_per_sdu) {
      blocks_per_sdu = 1;
    }
    codec_params.insert(codec_params.end(), &len, &len + 1);
    codec_params.insert(codec_params.end(), &type, &type + 1);
    codec_params.insert(codec_params.end(), &blocks_per_sdu,
                                            &blocks_per_sdu + 1);

  } else if (stream->codec_config.codec_type ==
                CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
    LOG(INFO) << __func__ << ": Codec type is AptX Adaptive LE ";
  } else if (stream->codec_config.codec_type ==
                CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
    LOG(INFO) << __func__ << ": Codec type is AptX Adaptive R4 ";
  }

  // TBD for AAR3
  LOG(INFO) << __func__ << ": audio_context: " << loghex(stream->audio_context);
  if(stream->audio_context == CONTENT_TYPE_MEDIA) {
    tgt_latency = TGT_HIGH_RELIABLE;
  } else if(stream->audio_context == CONTENT_TYPE_CONVERSATIONAL) {
    tgt_latency = TGT_BAL_LATENCY;
  } else if(stream->audio_context == CONTENT_TYPE_GAME) {
    tgt_latency = TGT_LOW_LATENCY;
  }

  AseCodecConfigOp codec_config_op;
  if (stream->codec_config.codec_type ==
            CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
    codec_config_op = {
                       .ase_id = stream->ase_id,
                       .tgt_latency =  tgt_latency,
                       .tgt_phy = LE_2M_PHY,
                       .codec_id = {0xFF, 0x0A, 0, 0x01, 0x00},// Check
                       .codec_params_len =
                           static_cast <uint8_t> (codec_params.size()),
                       .codec_params = codec_params
    };
  } else if (stream->codec_config.codec_type ==
            CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
    codec_config_op = {
                       .ase_id = stream->ase_id,
                       .tgt_latency =  tgt_latency,
                       .tgt_phy = LE_2M_PHY,
                       .codec_id = {0xFF, 0x0A, 0x00, 0xAD, 0x01},// Check
                       .codec_params_len =
                           static_cast <uint8_t> (codec_params.size()),
                       .codec_params = codec_params
    };
  } else {  //default LC3
    codec_config_op = {
                       .ase_id = stream->ase_id,
                       .tgt_latency =  tgt_latency,
                       .tgt_phy = LE_2M_PHY,
                       //.codec_id = {LC3_CODEC_ID, 0x75, 0x00, 0x01, 0x00},
                       .codec_id = {LC3_CODEC_ID, 0, 0, 0, 0},
                       .codec_params_len =
                           static_cast <uint8_t> (codec_params.size()),
                       .codec_params = codec_params
    };
  }

  ase_ops->push_back(codec_config_op);
  return true;
}

alarm_t* StreamTracker::SetTimer(const char* alarmname,
                  BapTimeout* timeout, TimeoutReason reason, uint64_t ms) {
  alarm_t* timer = nullptr;

  timeout->bd_addr = bd_addrs_[0];
  timeout->tracker = this;
  timeout->reason = reason;
  timeout->transition_state = StateId();

  BapAlarm* bap_alarm = strm_mgr_->GetBapAlarm(bd_addrs_[0]);
  if (bap_alarm != nullptr) {
    timer = bap_alarm->Create(alarmname);
    if (timer == nullptr) {
      LOG(ERROR) << __func__ << ": Not able to create alarm";
      return nullptr;
    }
    LOG(INFO) << __func__ << ": starting " << alarmname;
    bap_alarm->Start(timer, ms, timeout);
  }
  return timer;
}

void StreamTracker::ClearTimer(alarm_t* timer, const char* alarmname) {
  BapAlarm* bap_alarm = strm_mgr_->GetBapAlarm(bd_addrs_[0]);

  if (bap_alarm != nullptr && bap_alarm->IsScheduled(timer)) {
    LOG(INFO) << __func__ << ": clear " << alarmname;
    bap_alarm->Stop(timer);
  }
}

void StreamTracker::OnTimeout(void* data) {
  BapTimeout* timeout = (BapTimeout *)data;
  if (timeout == nullptr) {
    LOG(INFO) << __func__ << ": timeout data null, return ";
    return;
  }

  bool isReleaseNeeded = false;
  int stream_tracker_id = timeout->transition_state;
  LOG(INFO) << __func__ << ": stream_tracker_ID: " << stream_tracker_id
            << ", timeout reason: " << static_cast<int>(timeout->reason);

  if (timeout->reason == TimeoutReason::STATE_TRANSITION) {
    if (stream_tracker_id == StreamTracker::kStateConnecting) {
      for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
        StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
        std::vector<StreamConnect> *conn_streams = GetConnStreams();
        UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

        LOG(WARNING) << __func__ << ": audio_strms: " << audio_strms->size()
                                 << ", conn_streams: " << conn_streams->size();

        for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
          StreamContext *context = contexts->FindOrAddByType(it->stream_type);
          LOG(INFO) << __func__ << ": connection_state: "
                                << static_cast<int>(context->connection_state);

          if(context->connection_state == IntConnectState::ASCS_DISCOVERED) {
            for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
              UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
              if (stream && stream->ase_state != ascs::ASE_STATE_IDLE &&
                 stream->ase_state != ascs::ASE_STATE_RELEASING) {
                LOG(WARNING) << __func__
                             << ": ascs state is neither idle nor releasing";
                isReleaseNeeded = true;
                break;
              }
            }
          }
        }
      }
      LOG(INFO) << __func__ << ": isReleaseNeeded: " << isReleaseNeeded;
      HandleInternalDisconnect(isReleaseNeeded);
    } else if (stream_tracker_id != StreamTracker::kStateDisconnecting) {
      HandleInternalDisconnect(true);
      for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
        LOG(INFO) << __func__ << ": BD Addr = " << (*itr);

        UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks((*itr));
        if (callbacks) {
          callbacks->OnStreamTimeOut((*itr));
        }
        AddRemoteToBapTimeoutDevices(*itr);
        //L2CA_CancelBleConnectReq(*itr);
      }
    }
  }
  LOG(INFO) << __func__ << ": Exit";
}

void StreamTracker::HandleSingleDeviceRemoteDisconnetFromGroup(uint32_t event,
                                                               void* p_data) {
  PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
  UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks((pacs_state->bd_addr));
  StreamContexts *contexts = strm_mgr_->GetStreamContexts((pacs_state->bd_addr));
  std::vector<StreamStateInfo> strms;
  std::vector<StreamType> *disc_streams = GetStreams();
  LOG(WARNING) << __func__ << ": Disc Streams Size: "
                           << disc_streams->size();
  for (auto it = disc_streams->begin(); it != disc_streams->end(); it++) {
    StreamStateInfo state;
    memset(&state, 0, sizeof(state));
    state.stream_type = *it;
    state.stream_state = StreamState::DISCONNECTED;
    strms.push_back(state);
    StreamContext *context = contexts->FindOrAddByType(*it);
    context->stream_state = StreamState::DISCONNECTED;
    context->attached_state = StreamAttachedState::IDLE;
    LOG(INFO) << __func__ << ": Attached state made idle";
    context->stream_ids.clear();
  }

  if (callbacks) {
    callbacks->OnStreamState((pacs_state->bd_addr), strms);
  }
  return;
}

void StreamTracker::StateIdle::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);

    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks((*itr));
    StreamContexts *contexts = strm_mgr_->GetStreamContexts((*itr));
    std::vector<StreamStateInfo> strms;
    StreamControlType control_type = tracker_.GetControlType();

    if(control_type != StreamControlType::Disconnect &&
       control_type != StreamControlType::Connect) {
      return;
    }

    if(control_type == StreamControlType::Disconnect) {
      std::vector<StreamType> *disc_streams = tracker_.GetStreams();
      LOG(WARNING) << __func__ << ": Disc Streams Size: "
                               << disc_streams->size();
      for (auto it = disc_streams->begin(); it != disc_streams->end(); it++) {
        StreamStateInfo state;
        memset(&state, 0, sizeof(state));
        state.stream_type = *it;
        state.stream_state = StreamState::DISCONNECTED;
        strms.push_back(state);
        StreamContext *context = contexts->FindOrAddByType(*it);
        context->stream_state = StreamState::DISCONNECTED;
        context->attached_state = StreamAttachedState::IDLE;
        LOG(INFO) << __func__ << ": Attached state made idle";
        context->stream_ids.clear();
      }
    } else if(control_type == StreamControlType::Connect) {
      std::vector<StreamConnect> *conn_streams = tracker_.GetConnStreams();
      uint32_t prev_state = tracker_.PreviousStateId();
      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamStateInfo state;
        memset(&state, 0, sizeof(state));
        StreamContext *context = contexts->FindOrAddByType(it->stream_type);
        context->stream_state = StreamState::DISCONNECTED;
        context->attached_state = StreamAttachedState::IDLE;
        LOG(INFO) << __func__ << ": Attached state made idle";
        context->stream_ids.clear();
        if(prev_state == StreamTracker::kStateConnecting) {
          LOG(INFO) << __func__ << ": connecting request";
          /*if ((it->stream_type.direction == ASE_DIRECTION_SRC) &&
             (it->stream_type.audio_context == CONTENT_TYPE_UNSPECIFIED) &&
             (it->stream_type.type == CONTENT_TYPE_MEDIA)) {
            it->stream_type.audio_context = CONTENT_TYPE_LIVE;
            LOG(INFO) << __func__ << ": Updating the content type to Live";
          }*/
          state.stream_type = it->stream_type;
          state.stream_state = StreamState::DISCONNECTED;
          strms.push_back(state);
        }
      }
    }
    if(callbacks) {
      callbacks->OnStreamState((*itr), strms);
    }
  }
}

void StreamTracker::StateIdle::OnExit() {

}

bool StreamTracker::StateIdle::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_CONNECT_REQ_EVT: {
      BapConnect *evt_data = (BapConnect *) p_data;
      for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
        GattPendingData *gatt_pending_data =
                             strm_mgr_->GetGattPendingData(*itr);
        // check if the PACS client is connected to remote device
        PacsClient *pacs_client = strm_mgr_->GetPacsClient(*itr);
        if(!gatt_pending_data || !pacs_client) {
          LOG(WARNING) << __func__
                       << ": gatt_pending_data or pacs_client is null";
          continue;
        }
        uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(*itr);
        LOG(WARNING) << __func__
                     << ": pacs_client_id: " << pacs_client_id;

        ConnectionState pacs_state = strm_mgr_->GetPacsState(*itr);
        LOG(WARNING) << __func__
                     << ": pacs_state: " << static_cast<int>(pacs_state);

        if(pacs_state == ConnectionState::DISCONNECTED ||
              pacs_state == ConnectionState::DISCONNECTING ||
              pacs_state == ConnectionState::CONNECTING) {
          // move the state to connecting and initiate pacs connection
          pacs_client->Connect(pacs_client_id, *itr, evt_data->is_direct);
          if(gatt_pending_data->pacs_pending_cmd == GattPendingCmd::NONE) {
            gatt_pending_data->pacs_pending_cmd =
                                GattPendingCmd::GATT_CONN_PENDING;
          }
        } else if(pacs_state == ConnectionState::CONNECTED) {
          // pacs is already connected so initiate
          // pacs service discovry now and move the state to connecting
          pacs_client->StartDiscovery(pacs_client_id, *itr);
        }
      }
      tracker_.TransitionTo(StreamTracker::kStateConnecting);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      LOG(WARNING) << __func__ << ": Stable state, return false.";
      return false;
    } break;

    default:
      LOG(WARNING) << __func__ << ": Unhandled Event" << loghex(event);
      break;
  }
  return true;
}

void StreamTracker::StateConnecting::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  std::vector<StreamConnect> *conn_streams = tracker_.GetConnStreams();
  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));

    StreamControlType control_type = tracker_.GetControlType();

    if(control_type != StreamControlType::Connect) return;

    ConnectionState pacs_state = strm_mgr_->GetPacsState(*itr);

    LOG(INFO) << __func__ << ": Conn Streams Size: " << conn_streams->size();

    for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      StreamContext *context = contexts->FindOrAddByType(it->stream_type);
      context->stream_state = StreamState::CONNECTING;
      if(pacs_state == ConnectionState::DISCONNECTED ||
         pacs_state == ConnectionState::CONNECTING) {
        context->connection_state = IntConnectState::PACS_CONNECTING;
      } else if(pacs_state == ConnectionState::CONNECTED) {
        context->connection_state = IntConnectState::PACS_DISCOVERING;
      }
      /*if ((it->stream_type.direction == ASE_DIRECTION_SRC) &&
         (it->stream_type.audio_context == CONTENT_TYPE_UNSPECIFIED) &&
         (it->stream_type.type == CONTENT_TYPE_MEDIA)) {
         it->stream_type.audio_context = CONTENT_TYPE_LIVE;
         LOG(INFO) << __func__ << ": Updating the content type to Live";
      }*/
      state.stream_type = it->stream_type;
      state.stream_state = StreamState::CONNECTING;
      strms.push_back(state);
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }

  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateConnectingTimer",
                       &timeout, reason, ((conn_streams->size()) *
                       (static_cast<uint64_t>(TimeoutVal::ConnectingTimeout))));
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": StateConnecting: Alarm not allocated.";
    return;
  }
}

void StreamTracker::StateConnecting::OnExit() {
  tracker_.ClearTimer(state_transition_timer, "StateConnectingTimer");
}

bool StreamTracker::StateConnecting::AttachStreamsToContext(
                                 RawAddress bd_addr,
                                 std::vector<IntStrmTracker *> *all_trackers,
                                 std::vector<UcastAudioStream *> *streams,
                                 uint8_t cis_count,
                                 std::vector<AseCodecConfigOp> *ase_ops) {
  PacsDiscovery *pacs_discovery_ = tracker_.GetPacsDiscovery(bd_addr);
  if (!pacs_discovery_) {
    return false;
  }
  StreamContexts *contexts = strm_mgr_->GetStreamContexts(bd_addr);
  for(uint8_t i = 0; i < all_trackers->size()/cis_count ; i++) {
    for(uint8_t j = 0; j < cis_count ; j++) {
      IntStrmTracker *tracker = all_trackers->at(i*cis_count + j);
      UcastAudioStream *stream = streams->at((i*cis_count + j)% streams->size());
      StreamContext *context = contexts->FindOrAddByType(
                                         tracker->strm_type);
      if (stream) {
        if(stream->overall_state == StreamTracker::kStateIdle) {
          stream->audio_context = tracker->strm_type.audio_context;
          stream->control_type = StreamControlType::Connect;
          stream->ase_pending_cmd = AscsPendingCmd::NONE;
          stream->cis_pending_cmd = CisPendingCmd::NONE;
          stream->codec_config = tracker->codec_config;
          stream->req_qos_config = tracker->qos_config;
          stream->qos_config = tracker->qos_config;
          stream->cig_id = tracker->cig_id;
          stream->cis_id = tracker->cis_id;

          stream->cig_state = CigState::INVALID;
          stream->cis_state = CisState::INVALID;
          stream->overall_state = StreamTracker::kStateConnecting;

          if (stream->direction == ASE_DIRECTION_SINK) {
            if (cis_count > 1) {
              stream->audio_location =
                    pacs_discovery_->sink_locations & locations.at(j);
            } else {
              stream->audio_location = pacs_discovery_->sink_locations;
            }
          } else if (stream->direction == ASE_DIRECTION_SRC) {
            if (cis_count > 1) {
              stream->audio_location =
                    pacs_discovery_->src_locations & locations.at(j);
            } else {
              //Set below prop on need basis if aud_location giving by PTS
              // as 0. Mostly in some of the use-cases of TMAP/CAP.
              //But for TMAP config B and C this should be false, as PTS is
              //giving right audio locations.
              char pts_tmap_cap_aud_loc[PROPERTY_VALUE_MAX] = "false";
              property_get("persist.vendor.btstack.pts_tmap_cap_aud_loc",
                                                pts_tmap_cap_aud_loc, "false");
              LOG(WARNING) << __func__
                           << ": pts_tmap_cap_aud_loc: " << pts_tmap_cap_aud_loc;

              if (!strncmp("true", pts_tmap_cap_aud_loc, 4)) {
                stream->audio_location = 1;
              } else {
                stream->audio_location = pacs_discovery_->src_locations;
              }
            }
          }
          tracker_.PrepareCodecConfigPayload(ase_ops, stream);
          tracker->attached_state = context->attached_state =
                                  StreamAttachedState::IDLE_TO_PHY;
          LOG(INFO) << __func__
                     << ": Physically  attached";
        } else {
          LOG(INFO) << __func__
                     << ": Virtually attached";
          tracker->attached_state = context->attached_state =
                                  StreamAttachedState::VIRTUAL;
        }
        tracker->ase_id = stream->ase_id;

        StreamIdType id = {
                  .ase_id = stream->ase_id,
                  .ase_direction = stream->direction,
                  .virtual_attach = false,
                  .cig_id = tracker->cig_id,
                  .cis_id = tracker->cis_id
        };
        context->stream_ids.push_back(id);
      }
    }
  }
  return true;
}

bool StreamTracker::StateConnecting::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  std::vector<StreamConnect> *conn_streams = tracker_.GetConnStreams();

  uint8_t num_conn_streams = 0;
  if(conn_streams) {
     num_conn_streams = conn_streams->size();
  }

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {

      // expect the disconnection for same set of streams connection
      // has initiated ex: if connect is issued for media (tx), voice(tx & rx)
      // then disconnect is expected for media (tx), voice(tx & rx).

      // based on connection state, issue the relevant commands and move
      // the state to disconnecting
      // issue the release opertion if for any stream ASE operation is
      // initiated

      // upate the control type and streams also
      for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
        LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
        BapDisconnect *evt_data = (BapDisconnect *) p_data;

        tracker_.UpdateControlType(StreamControlType::Disconnect);

        tracker_.UpdateStreams(&evt_data->streams);
        StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
        AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);
        UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

        std::vector<AseReleaseOp> ase_ops;
        std::vector<StreamType> *disc_streams = tracker_.GetStreams();

        LOG(WARNING) << __func__ << ": disc_streams: " << disc_streams->size();

        for (auto it = disc_streams->begin(); it != disc_streams->end(); it++){
          StreamContext *context = contexts->FindOrAddByType(*it);
          if(context->connection_state == IntConnectState::ASCS_DISCOVERED) {
            for (auto id = context->stream_ids.begin();
                    id != context->stream_ids.end(); id++) {
              UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
              if (!stream) {
                LOG(ERROR) << __func__ << "stream is null";
                continue;
              }
              bool can_be_disconnected = tracker_.StreamCanbeDisconnected(*itr,
                                                   context, id->ase_id);
              if(can_be_disconnected &&
                 stream->ase_state == ascs::ASE_STATE_CODEC_CONFIGURED &&
                 stream->ase_pending_cmd != AscsPendingCmd::RELEASE_ISSUED) {
                AseReleaseOp release_op = {
                                           .ase_id = stream->ase_id
                                          };
                ase_ops.push_back(release_op);
                stream->ase_pending_cmd = AscsPendingCmd::RELEASE_ISSUED;
                // change the overall state to starting
                stream->overall_state = StreamTracker::kStateDisconnecting;
              }
            }
          }
        }

        LOG(INFO) << __func__ << ": ase_ops size: " << ase_ops.size();

        // send consolidated release command to ASCS client
        if(ase_ops.size()) {
          LOG(WARNING) << __func__ << ": Going For ASCS Release op";
          ascs_client->Release(ASCS_CLIENT_ID, *itr, ase_ops);
        }
      }
      tracker_.TransitionTo(StreamTracker::kStateDisconnecting);
    } break;
    case PACS_CONNECTION_STATE_EVT: {
      PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                     pacs_state->bd_addr);
      GattPendingData *gatt_pending_data = strm_mgr_->
                                     GetGattPendingData(pacs_state->bd_addr);
      PacsClient *pacs_client = strm_mgr_->GetPacsClient(pacs_state->bd_addr);
      uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(pacs_state->bd_addr);

      LOG(WARNING) << __func__
                   << ": pacs_state: " << static_cast<int>(pacs_state->state);
      if(pacs_state->state == ConnectionState::CONNECTED) {
        // now send the PACS discovery
        gatt_pending_data->pacs_pending_cmd = GattPendingCmd::NONE;
        pacs_client->StartDiscovery(pacs_client_id, pacs_state->bd_addr);
      } else if(pacs_state->state == ConnectionState::DISCONNECTED) {
        gatt_pending_data->pacs_pending_cmd = GattPendingCmd::NONE;
        tracker_.HandleInternalDisconnect(false);
        return false;
      }

      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        StreamContext *context = contexts->FindOrAddByType(
                                           conn_stream.stream_type);
        if(pacs_state->state == ConnectionState::CONNECTED) {
          context->connection_state = IntConnectState::PACS_DISCOVERING;
        }
      }
    } break;

    case PACS_DISCOVERY_RES_EVT: {
      PacsDiscovery pacs_discovery_ =  *((PacsDiscovery *) p_data);
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(
                                     pacs_discovery_.bd_addr);
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                     pacs_discovery_.bd_addr);
      GattState ascs_state = strm_mgr_->GetAscsState(pacs_discovery_.bd_addr);
      GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData
                                                    (pacs_discovery_.bd_addr);
      bool process_pacs_results = false;

      // check if this tracker already passed the pacs discovery stage
      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        StreamContext *context = contexts->FindOrAddByType(
                                           conn_stream.stream_type);
        if(context->connection_state == IntConnectState::PACS_DISCOVERING) {
          process_pacs_results = true;
          break;
        }
      }

      if(!process_pacs_results) break;

      bool context_supported = false;
      // check the status
      if(pacs_discovery_.status) {
        tracker_.HandleInternalDisconnect(false);
        LOG(ERROR) << __func__ << ": PACS discovery failed";
        return false;
      }

      tracker_.UpdatePacsDiscovery(pacs_discovery_.bd_addr, pacs_discovery_);

      // check if supported audio contexts has required contexts
      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamType stream = it->stream_type;
        if(stream.direction == ASE_DIRECTION_SINK) {
          if(stream.audio_context & pacs_discovery_.supported_contexts) {
            context_supported = true;
          }
        } else if(stream.direction == ASE_DIRECTION_SRC) {
          if((static_cast<uint64_t>(stream.audio_context) << 16) &
               pacs_discovery_.supported_contexts) {
            context_supported = true;
          }
        }
      }

      if(!context_supported) {
        LOG(ERROR) << __func__ << ": No Matching Supported Contexts found";
        tracker_.HandleInternalDisconnect(false);
        break;
      }

      // if not present send the BAP callback as disconnected
      // compare the codec configs from upper layer to remote dev
      // sink or src PACS records/capabilities.

      // go for ASCS discovery only when codec configs are decided
      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        // TODO for now will pick directly first set of Codec and QOS configs

        uint8_t index = tracker_.ChooseBestCodec(conn_stream.stream_type,
                                             &conn_stream.codec_qos_config_pair,
                                             &pacs_discovery_);
        if(index != 0XFF) {
          CodecQosConfig entry = conn_stream.codec_qos_config_pair.at(index);
          CodecConfig codec_config = entry.codec_config;
          QosConfig qos_config = entry.qos_config;

          StreamContext *context = contexts->FindOrAddByType(
                                             conn_stream.stream_type);
          for (auto ascs_config = qos_config.ascs_configs.begin();
                ascs_config != qos_config.ascs_configs.end(); ascs_config++) {
            uint8_t cis_id;
            if(pacs_discovery_.bd_addr != *(bd_addrs_.begin())) {
              cis_id = ascs_config->cis_id + 1;
            } else {
              cis_id = ascs_config->cis_id;
            }
            int_strm_trackers_[pacs_discovery_.bd_addr].
                           FindOrAddBytrackerType(conn_stream.stream_type,
                               0x00, ascs_config->cig_id,
                               cis_id,
                               codec_config, qos_config);
          }
          context->codec_config = codec_config;
          context->req_qos_config = qos_config;
          context->req_alt_qos_configs = entry.alt_qos_configs;
        } else {
          LOG(ERROR) << __func__ << ": No Matching Codec Found For Stream";
        }
      }

      // check if any match between upper layer codec and remote dev's
      // pacs records
      if(!int_strm_trackers_[pacs_discovery_.bd_addr].size()) {
        LOG(WARNING) << __func__ << ": No Matching codec found for all streams";
        tracker_.HandleInternalDisconnect(false);
        return false;
      }

      LOG(WARNING) << __func__ << ": ascs_state: "
                               << loghex(static_cast<int>(ascs_state));
      if(ascs_state == GattState::CONNECTED) {
        LOG(WARNING) << __func__ << ": Going For ASCS Service Discovery";
        // now send the ASCS discovery
        ascs_client->StartDiscovery(ASCS_CLIENT_ID, pacs_discovery_.bd_addr);
      } else if(ascs_state == GattState::DISCONNECTED) {
        LOG(WARNING) << __func__ << ": Going For ASCS Conneciton";
        ascs_client->Connect(ASCS_CLIENT_ID, pacs_discovery_.bd_addr, true);
        if(gatt_pending_data->ascs_pending_cmd == GattPendingCmd::NONE) {
          gatt_pending_data->ascs_pending_cmd =
                              GattPendingCmd::GATT_CONN_PENDING;
        }
      }

      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        StreamContext *context = contexts->FindOrAddByType(
                                             conn_stream.stream_type);
        if(ascs_state == GattState::CONNECTED) {
          context->connection_state = IntConnectState::ASCS_DISCOVERING;
        } else if(ascs_state == GattState::DISCONNECTED) {
          context->connection_state = IntConnectState::ASCS_CONNECTING;
        }
      }
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      AscsConnectionState *ascs_state =  (AscsConnectionState *) p_data;
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                     ascs_state->bd_addr);
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(ascs_state->bd_addr);
      GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData
                                                    (ascs_state->bd_addr);
      if(ascs_state->state == GattState::CONNECTED) {
        LOG(INFO) << __func__ << " ASCS server connected";
        // now send the ASCS discovery
        gatt_pending_data->ascs_pending_cmd = GattPendingCmd::NONE;
        ascs_client->StartDiscovery(ASCS_CLIENT_ID, ascs_state->bd_addr);
      } else if(ascs_state->state == GattState::DISCONNECTED) {
        LOG(INFO) << __func__ << " ASCS server Disconnected";
        gatt_pending_data->ascs_pending_cmd = GattPendingCmd::NONE;
        tracker_.HandleInternalDisconnect(false);
        return false;
      }

      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        StreamContext *context = contexts->FindOrAddByType(
                                             conn_stream.stream_type);
        if(ascs_state->state == GattState::CONNECTED) {
          context->connection_state = IntConnectState::ASCS_DISCOVERING;
        }
      }
    } break;

    case ASCS_DISCOVERY_RES_EVT: {
      AscsDiscovery ascs_discovery_ =  *((AscsDiscovery *) p_data);
      std::vector<AseCodecConfigOp> ase_ops;
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                     ascs_discovery_.bd_addr);
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(
                                           ascs_discovery_.bd_addr);
      std::vector<AseParams> sink_ase_list = ascs_discovery_.sink_ases_list;
      std::vector<AseParams> src_ase_list = ascs_discovery_.src_ases_list;
      // check the status
      if(ascs_discovery_.status) {
        tracker_.HandleInternalDisconnect(false);
        return false;
      }

      for (uint8_t i = 0; i < num_conn_streams ; i++) {
        StreamConnect conn_stream = conn_streams->at(i);
        StreamContext *context = contexts->FindOrAddByType(
                                             conn_stream.stream_type);
        context->connection_state = IntConnectState::ASCS_DISCOVERED;
      }

      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                                  ascs_discovery_.bd_addr);
      // create the UcastAudioStream for each ASEs (ase id)
      // check if the entry is present, if not create and add it to list
      // find out number of ASEs which are in IDLE state
      for (auto & ase : sink_ase_list) {
        audio_strms->FindOrAddByAseId(ase.ase_id,
                                      ase.ase_state, ASE_DIRECTION_SINK);
      }

      for (auto & ase : src_ase_list) {
        audio_strms->FindOrAddByAseId(ase.ase_id,
                                      ase.ase_state, ASE_DIRECTION_SRC);
      }

      LOG(INFO) << __func__ << ": total num of audio strms: "
                            << audio_strms->size();

      std::vector<IntStrmTracker *> sink_int_trackers =
                   int_strm_trackers_[ascs_discovery_.bd_addr].
                   GetTrackerListByDir(ASE_DIRECTION_SINK);

      std::vector<IntStrmTracker *> src_int_trackers =
                   int_strm_trackers_[ascs_discovery_.bd_addr].
                   GetTrackerListByDir(ASE_DIRECTION_SRC);

      std::vector<int> state_ids = { StreamTracker::kStateIdle };

      std::vector<UcastAudioStream *> idle_sink_streams =
                                   audio_strms->GetStreamsByStates(state_ids,
                                   ASE_DIRECTION_SINK);
      std::vector<UcastAudioStream *> idle_src_streams =
                                   audio_strms->GetStreamsByStates(state_ids,
                                   ASE_DIRECTION_SRC);
      LOG(INFO) << __func__ << ": Num of Sink Idle Streams = "
                                <<  idle_sink_streams.size()
                                << ": Num of Src Idle Streams = "
                                <<  idle_src_streams.size();

      LOG(INFO) << __func__ << ": Num of Sink Internal Trackers = "
                                <<  sink_int_trackers.size()
                                << ": Num of Src Internal Trackers = "
                                <<  src_int_trackers.size();

      LOG(INFO) << __func__ << ": Num of Conn Streams "
                                <<  loghex(num_conn_streams);

      // check how many stream connections are requested and
      // how many streams(ASEs) are available for processing
      // check if we have sufficient number of streams(ASEs) for
      // the given set of connection requirement
      DeviceType dev_type;
      btif_bap_get_device_type(ascs_discovery_.bd_addr, &dev_type,
                               CodecDirection::CODEC_DIR_SINK);
      uint8_t cis_count = 0;
      if(dev_type == DeviceType::EARBUD ||
         dev_type == DeviceType::EARBUD_NO_CSIP ||
         dev_type == DeviceType::HEADSET_STEREO) {
        cis_count = 1;
      } else if(dev_type == DeviceType::HEADSET_SPLIT_STEREO) {
        cis_count = 2;
      }

      std::vector<int> valid_state_ids = {
                                       StreamTracker::kStateConnecting,
                                       StreamTracker::kStateConnected,
                                       StreamTracker::kStateStreaming,
                                       StreamTracker::kStateReconfiguring,
                                       StreamTracker::kStateDisconnecting,
                                       StreamTracker::kStateStarting,
                                       StreamTracker::kStateStopping
                                    };

      if(sink_int_trackers.size()) {
        if(idle_sink_streams.size() >= sink_int_trackers.size()) {
          AttachStreamsToContext(ascs_discovery_.bd_addr,
                                 &sink_int_trackers, &idle_sink_streams,
                                 cis_count, &ase_ops);
        } else {
          std::vector<IntStrmTracker *> sink_int_trackers_1,
                                        sink_int_trackers_2;
            // split the sink_int_trackers into 2 lists now, one list
            // is equal to idle_sink_streams as physical and other
            // list as virtually attached
          if(idle_sink_streams.size()) { // less num of free ASEs
            for (uint8_t i = 0; i < idle_sink_streams.size() ; i++) {
              IntStrmTracker *tracker = sink_int_trackers.at(i);
              sink_int_trackers_1.push_back(tracker);
            }
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &sink_int_trackers_1, &idle_sink_streams,
                                   cis_count, &ase_ops);
            for (uint8_t i = idle_sink_streams.size();
                         i < sink_int_trackers.size() ; i++) {
              IntStrmTracker *tracker = sink_int_trackers.at(i);
              sink_int_trackers_2.push_back(tracker);
            }
          }

          std::vector<UcastAudioStream *> all_active_sink_streams =
                                audio_strms->GetStreamsByStates(valid_state_ids,
                                ASE_DIRECTION_SINK);

          if(sink_int_trackers_2.size()) {
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &sink_int_trackers_2,
                                   &all_active_sink_streams,
                                   cis_count, &ase_ops);
          } else if(sink_int_trackers.size()) {
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &sink_int_trackers,
                                   &all_active_sink_streams,
                                   cis_count, &ase_ops);
          }
        }
      }

      btif_bap_get_device_type(ascs_discovery_.bd_addr, &dev_type,
                               CodecDirection::CODEC_DIR_SRC);
      if(dev_type == DeviceType::EARBUD ||
         dev_type == DeviceType::EARBUD_NO_CSIP ||
         dev_type == DeviceType::HEADSET_STEREO) {
        cis_count = 1;
      } else if(dev_type == DeviceType::HEADSET_SPLIT_STEREO) {
        cis_count = 2;
      }

      // do the same procedure for src trackers as well
      if(src_int_trackers.size()) {
        if(idle_src_streams.size() >= src_int_trackers.size()) {
          AttachStreamsToContext(ascs_discovery_.bd_addr,
                                 &src_int_trackers, &idle_src_streams,
                                 cis_count, &ase_ops);
        } else {
          std::vector<IntStrmTracker *> src_int_trackers_1,
                                        src_int_trackers_2;
            // split the src_int_trackers into 2 lists now, one list
            // is equal to idle_src_streams as physical and other
            // list as virtually attached
          if(idle_src_streams.size()) { // less num of free ASEs
            for (uint8_t i = 0; i < idle_src_streams.size() ; i++) {
              IntStrmTracker *tracker = src_int_trackers.at(i);
              src_int_trackers_1.push_back(tracker);
            }
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &src_int_trackers_1, &idle_src_streams,
                                   cis_count, &ase_ops);
            for (uint8_t i = idle_src_streams.size();
                         i < src_int_trackers.size() ; i++) {
              IntStrmTracker *tracker = src_int_trackers.at(i);
              src_int_trackers_2.push_back(tracker);
            }
          }

          std::vector<UcastAudioStream *> all_active_src_streams =
                              audio_strms->GetStreamsByStates(valid_state_ids,
                              ASE_DIRECTION_SRC);

          if(src_int_trackers_2.size()) {
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &src_int_trackers_2, &all_active_src_streams,
                                   cis_count, &ase_ops);
          } else if(src_int_trackers.size()) {
            AttachStreamsToContext(ascs_discovery_.bd_addr,
                                   &src_int_trackers, &all_active_src_streams,
                                   cis_count, &ase_ops);
          }
        }
      }

      // remove all duplicate internal stream trackers
      int_strm_trackers_[ascs_discovery_.bd_addr].
                                  RemoveVirtualAttachedTrackers();

      // if the int strm trackers size is 0 then return as
      // connected immediately
      if(!int_strm_trackers_[ascs_discovery_.bd_addr].size()) {
        // update the state to connected
        TransitionTo(StreamTracker::kStateConnected);
        break;
      }

      if(!ase_ops.empty()) {
        LOG(WARNING) << __func__ << ": Going For ASCS CodecConfig op";
        ascs_client->CodecConfig(ASCS_CLIENT_ID, ascs_discovery_.bd_addr,
                                 ase_ops);
      } else {
        tracker_.HandleInternalDisconnect(false);
        break;
      }

      // refresh the sink and src trackers
      sink_int_trackers =  int_strm_trackers_[ascs_discovery_.bd_addr].
                           GetTrackerListByDir(ASE_DIRECTION_SINK);

      src_int_trackers = int_strm_trackers_[ascs_discovery_.bd_addr].
                         GetTrackerListByDir(ASE_DIRECTION_SRC);

      LOG(INFO) << __func__ << ": Num of new Sink Internal Trackers = "
                             <<  sink_int_trackers.size()
                             << ": Num of new Src Internal Trackers = "
                             <<  src_int_trackers.size();

      LOG(INFO) << __func__ << ": Num of new Sink Idle Streams = "
                             <<  idle_sink_streams.size()
                             << ": Num of new Src Idle Streams = "
                             <<  idle_src_streams.size();

      // update the states to connecting or other internal states
      if(sink_int_trackers.size()) {
        for (uint8_t i = 0; i < sink_int_trackers.size() ; i++) {
          UcastAudioStream *stream = idle_sink_streams.at(i);
          stream->ase_pending_cmd = AscsPendingCmd::CODEC_CONFIG_ISSUED;
        }
      }
      if(src_int_trackers.size()) {
        for (uint8_t i = 0; i < src_int_trackers.size() ; i++) {
          UcastAudioStream *stream = idle_src_streams.at(i);
          stream->ase_pending_cmd = AscsPendingCmd::CODEC_CONFIG_ISSUED;
        }
      }
    } break;

    case ASCS_ASE_STATE_EVT: {
      tracker_.HandleAseStateEvent(p_data, StreamControlType::Connect,
                                   &int_strm_trackers_);
    } break;

    case ASCS_ASE_OP_FAILED_EVT: {
      tracker_.HandleAseOpFailedEvent(p_data);
    } break;

    case BAP_TIME_OUT_EVT: {
      tracker_.OnTimeout(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}


void StreamTracker::StateConnected::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamConfigInfo> stream_configs;
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    PacsDiscovery *pacs_discovery_ = tracker_.GetPacsDiscovery(*itr);
    StreamControlType control_type = tracker_.GetControlType();
    std::vector<StreamType> conv_streams;

    if(control_type != StreamControlType::Connect &&
       control_type != StreamControlType::Stop &&
       control_type != StreamControlType::Start &&
       control_type != StreamControlType::Reconfig) {
      return;
    }

    if(control_type == StreamControlType::Connect) {
      std::vector<StreamConnect> *conn_streams = tracker_.GetConnStreams();
      for (auto it = conn_streams->begin(); it != conn_streams->end(); it++) {
        StreamType type = it->stream_type;
        conv_streams.push_back(type);
      }
      LOG(WARNING) << __func__ << ": Conn Streams Size " << conn_streams->size();
    } else if(control_type == StreamControlType::Reconfig) {
      std::vector<StreamReconfig> *reconf_streams = tracker_.GetReconfStreams();
      for (auto it = reconf_streams->begin(); it != reconf_streams->end();it++) {
        StreamType type = it->stream_type;
        conv_streams.push_back(type);
      }
      LOG(WARNING) << __func__ << ": Reconfig Streams size "
                                << reconf_streams->size();
    } else {
      conv_streams =  *tracker_.GetStreams();
    }

    if(control_type == StreamControlType::Connect ||
       control_type == StreamControlType::Reconfig) {
      for (auto it = conv_streams.begin(); it != conv_streams.end(); it++) {
        StreamContext *context = contexts->FindOrAddByType(*it);
        UcastAudioStream *stream = audio_strms->FindByStreamType(
                  (*it).audio_context, (*it).direction);
        // avoid duplicate updates
        if(context && pacs_discovery_ &&
           context->stream_state != StreamState::CONNECTED) {
          StreamConfigInfo config;
          memset(&config, 0, sizeof(config));
          config.stream_type = *it;
          if(stream) {
            config.codec_config = stream->codec_config;
            config.qos_config = stream->qos_config;
            context->qos_config = stream->qos_config;
          } else {
            config.codec_config = context->codec_config;
            config.qos_config = context->req_qos_config;
            context->qos_config = context->req_qos_config;
          }

          // Keeping bits_per_sample as 24 always for all LC3/R3/R4
          config.codec_config.bits_per_sample =
                                CodecBPS::CODEC_BITS_PER_SAMPLE_24;

          if(config.stream_type.direction == ASE_DIRECTION_SINK) {
            config.audio_location = pacs_discovery_->sink_locations;
            config.codecs_selectable = pacs_discovery_->sink_pac_records;
          } else if(config.stream_type.direction == ASE_DIRECTION_SRC) {
            config.audio_location = pacs_discovery_->src_locations;
            config.codecs_selectable = pacs_discovery_->src_pac_records;
          }
          stream_configs.push_back(config);
        }
      }
      if(stream_configs.size() && callbacks) {
        callbacks->OnStreamConfig(*itr, stream_configs);
      }
    }

    for (auto it = conv_streams.begin(); it != conv_streams.end(); it++) {
      StreamContext *context = contexts->FindOrAddByType(*it);
      // avoid duplicate updates
      if( context->stream_state != StreamState::CONNECTED) {
        StreamStateInfo state;
        memset(&state, 0, sizeof(state));
        state.stream_type = *it;
        state.stream_state = StreamState::CONNECTED;
        context->stream_state = StreamState::CONNECTED;
        strms.push_back(state);
      }
    }

    if(strms.size() && callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }
}

void StreamTracker::StateConnected::OnExit() {

}

bool StreamTracker::StateConnected::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ": Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateConnected);
    } break;

    case BAP_START_REQ_EVT: {
      for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
        PacsClient *pacs_client = strm_mgr_->GetPacsClient(*itr);
        uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(*itr);

        BapStart *evt_data = (BapStart *) p_data;
        bool isMonoMic = evt_data->is_mono_mic;
        tracker_.setMode(evt_data->mode);
        StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
        tracker_.UpdateControlType(StreamControlType::Start);

        tracker_.UpdateStreams(&evt_data->streams);
        std::vector<StreamType> *start_streams = tracker_.GetStreams();
        LOG(INFO) << __func__ << ": isMono config = " << isMonoMic;
        for (auto it = start_streams->begin(); it != start_streams->end(); it++) {
          StreamContext *context = contexts->FindOrAddByType(*it);
          bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                              (it->direction == ASE_DIRECTION_SRC) &&
                              (it->audio_context == CONTENT_TYPE_GAME);
          if (is_gaming_rx)
            context->is_mono_config = isMonoMic;
        }
        if(pacs_client) {
          pacs_client->GetAudioAvailability(pacs_client_id, *itr);
        }
      }
      tracker_.TransitionTo(StreamTracker::kStateStarting);
    } break;

    case BAP_RECONFIG_REQ_EVT: {
      BapReconfig *evt_data = (BapReconfig *) p_data;

      std::vector<StreamReconfig> reconf_streams_ = evt_data->streams;
      std::vector<StreamType> streams;

      //Logging to check passed streams from ACM to BAP
      for (auto it = reconf_streams_.begin();
                                it != reconf_streams_.end(); it ++) {
        CodecQosConfig entry = it->codec_qos_config_pair.at(0);
        CodecConfig codec_config = entry.codec_config;
        QosConfig qos_config = entry.qos_config;
        bap_print_codec_parameters(codec_config);
        bap_print_qos_parameters(qos_config);
      }

      tracker_.UpdateControlType(StreamControlType::Reconfig);
      tracker_.UpdateReconfStreams(&evt_data->streams);

      // check if codec reconfiguration or qos reconfiguration
      PacsClient *pacs_client = strm_mgr_->GetPacsClient(evt_data->bd_addr);
      uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(evt_data->bd_addr);

      // pacs is already connected so initiate
      // pacs service discovry now and move the state to reconfiguring
      if(pacs_client) {
        pacs_client->StartDiscovery(pacs_client_id, evt_data->bd_addr);
      }
      tracker_.TransitionTo(StreamTracker::kStateReconfiguring);

    } break;

    case PACS_CONNECTION_STATE_EVT: {
      PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
      LOG(WARNING) << __func__
               << ": pacs_state: " << static_cast<int>(pacs_state->state);
      if (pacs_state->state == ConnectionState::DISCONNECTED) {
        LOG(WARNING) << __func__ << ": traker bd_addr Size: "
                                 << tracker_.bd_addrs_.size();
        if (tracker_.bd_addrs_.size() == 1) {
           tracker_.HandlePacsConnectionEvent(p_data);
        } else {
          tracker_.HandleSingleDeviceRemoteDisconnetFromGroup(event, p_data);
          LOG(WARNING) << __func__ << "removing address: " << pacs_state->bd_addr;
          bd_addrs_.erase(std::remove(bd_addrs_.begin(), bd_addrs_.end(), pacs_state->bd_addr),
                                                                   bd_addrs_.end());
          tracker_.bd_addrs_.erase(std::remove(tracker_.bd_addrs_.begin(),
                                              tracker_.bd_addrs_.end(), pacs_state->bd_addr),
                                                             tracker_.bd_addrs_.end());
        }
      } else {
        tracker_.HandlePacsConnectionEvent(p_data);
      }
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case ASCS_ASE_STATE_EVT: {
      AscsState *ascs =  ((AscsState *) p_data);
      if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
        tracker_.HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());
      } else if(ascs->ase_params.ase_state ==
                              ascs::ASE_STATE_CODEC_CONFIGURED) {
        tracker_.HandleRemoteReconfig(ASCS_ASE_STATE_EVT, p_data, StateId());
      }
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      LOG(WARNING) << __func__ << ": Stable state, return false.";
      return false;
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}


void StreamTracker::StateStarting::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  uint64_t tout;
  uint8_t num_ases = 0;
  DeviceType dev_type = DeviceType::NONE;

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamType> *start_streams = tracker_.GetStreams();
    LOG(WARNING) << __func__ << ": Start Streams Size: "
                              << start_streams->size();

    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));

    StreamControlType control_type = tracker_.GetControlType();

    if(control_type != StreamControlType::Start) return;

    for (auto it = start_streams->begin(); it != start_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      state.stream_type = *it;
      state.stream_state = StreamState::STARTING;
      strms.push_back(state);
      StreamContext *context = contexts->FindOrAddByType(*it);
      context->stream_state = StreamState::STARTING;
      bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                          (it->direction == ASE_DIRECTION_SRC) &&
                          (it->audio_context == CONTENT_TYPE_GAME);
      if(dev_type == DeviceType::NONE) {
        btif_bap_get_device_type(*itr, &dev_type,
                            static_cast <CodecDirection> (it->direction));
      }

      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        int_strm_trackers_[*itr].FindOrAddBytrackerType(*it,
                                  id->ase_id, id->cig_id, id->cis_id,
                                  context->codec_config, context->qos_config);
        if (is_gaming_rx && context->is_mono_config) {
          LOG(ERROR) << __func__ << ": StateStarting: Alarm not allocated.";
          break;
        }
      }
      num_ases += context->stream_ids.size();
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }

  if(dev_type == DeviceType::EARBUD ||
     dev_type == DeviceType::EARBUD_NO_CSIP ||
     dev_type == DeviceType::HEADSET_STEREO) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::StartingTimeout);
  } else {
    tout = num_ases * (static_cast<uint64_t>(TimeoutVal::StartingTimeout));
  }
  if(!tout) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::StartingTimeout);
  }
  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateStartingTimer",
        &timeout, reason, tout);
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": StateStarting: Alarm not allocated.";
    return;
  }
}

void StreamTracker::StateStarting::OnExit() {
  StreamControlType control_type = tracker_.GetControlType();
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                      int_strm_trackers_[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms =
                             strm_mgr_->GetAudioStreams(*itr);
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      while(stream != NULL && !stream->vs_meta_data_queue.empty()) {
        LOG(INFO) << __func__
                  << ": StateStarting::OnExit(), stream->ase_id = "
                  << loghex(stream->ase_id);
        LOG(INFO) << __func__
                  << ": StateStarting::OnExit(), meta_data_queue length = "
                  << stream->vs_meta_data_queue.size();

        CigCisVSMetadata cigCisVSMetadata = stream->vs_meta_data_queue.front();
        CisInterface *cis_intf = strm_mgr_->GetCisInterface(cigCisVSMetadata.bd_addr);

        LOG(INFO) << __func__
                  << ": StateStarting::OnExit(), cigCisVSMetadata.cig_id = "
                  << loghex(cigCisVSMetadata.cig_id)
                  << "cis_id = " << loghex(cigCisVSMetadata.cis_id);
        LOG(INFO) << __func__
                  << ": cigCisVSMetadata.vs_meta_data.size() = "
                  << cigCisVSMetadata.vs_meta_data.size();

         // only UpdateEncoderParams when control type is start,
         // in other cases just empty the queue
        if(control_type == StreamControlType::Start && cis_intf != NULL) {
          LOG(INFO) << __func__ << ": control_type = StreamControlType::Start";
          cis_intf->UpdateEncoderParams(cigCisVSMetadata.cig_id,
                                        cigCisVSMetadata.cis_id,
                                        cigCisVSMetadata.vs_meta_data,
                                        cigCisVSMetadata.encoder_mode);
        }
        stream->vs_meta_data_queue.pop();
      }
    }
  }

  tracker_.ClearTimer(state_transition_timer, "StateStartingTimer");
}

bool StreamTracker::CheckAndUpdateStreamingState(
                    std::map<RawAddress, IntStrmTrackers> *int_strm_trackers) {
  //  to check for all internal trackers are moved to
  // streaming state then update it upper layers
  uint8_t num_strms_in_streaming = 0;
  uint8_t num_trackers = 0;
  bool pending_cmds = false;

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(WARNING) << __func__ << ": iterator address: " << *itr;
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    std::vector<IntStrmTracker *> *all_trackers =
                      (*int_strm_trackers)[*itr].GetTrackerList();
    // check if any pending commands are present
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && (stream->cis_pending_cmd != CisPendingCmd::NONE ||
                    stream->ase_pending_cmd != AscsPendingCmd::NONE)) {
        LOG(WARNING) << __func__ << ": cis_pending_cmd "
                     << loghex(static_cast <uint8_t>(stream->cis_pending_cmd));
        LOG(WARNING) << __func__ << ": ase_pending_cmd "
                     << loghex(static_cast <uint8_t>(stream->ase_pending_cmd));
        pending_cmds = true;
        break;
      }
    }
    if(pending_cmds) break;
  }

  if(pending_cmds) {
    LOG(WARNING) << __func__ << ": ASCS/CIS Pending commands left";
    return false;
  }

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    std::vector<IntStrmTracker *> *all_trackers =
                      (*int_strm_trackers)[*itr].GetTrackerList();
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && stream->ase_state == ascs::ASE_STATE_STREAMING &&
         stream->cis_state == CisState::ESTABLISHED) {
        num_strms_in_streaming++;
      }
      num_trackers++;
    }
  }

  if(num_trackers != num_strms_in_streaming) {
    LOG(WARNING) << __func__ << ": Not all streams moved to streaming";
    return false;
  }

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    std::vector<IntStrmTracker *> *all_trackers =
                      (*int_strm_trackers)[*itr].GetTrackerList();
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (stream) {
        stream->overall_state = StreamTracker::kStateStreaming;
      }
    }
  }

  // all streams are moved to streaming state
  TransitionTo(StreamTracker::kStateStreaming);
  return true;
}

bool StreamTracker::CheckAndCreateCis(std::map<RawAddress, IntStrmTrackers> *int_strm_trackers) {
  //check for Enabling notification is received for all ASEs and CIS state is ready, then create CIS
  CisInterface *cis_intf = nullptr;
  uint8_t num_enabling_notify = 0;
  uint16_t num_trackers = 0;
  bool cis_ready_to_create = true;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                (*int_strm_trackers)[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms =
                strm_mgr_->GetAudioStreams(*itr);
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && stream->ase_state == ascs::ASE_STATE_ENABLING) {
        num_enabling_notify++;
        cis_intf = strm_mgr_->GetCisInterface(*itr);
        if (stream->cis_state != CisState::READY ||
            stream->cis_pending_cmd != CisPendingCmd::NONE) {
          cis_ready_to_create = false;
        }
      }
      num_trackers++;
    }
  }

  LOG(INFO) << __func__ << ": num_enabling_notify = "<<loghex(num_enabling_notify)
            << ", num_trackers = "<<loghex(num_trackers)
            << ", cis_ready_to_create = " << cis_ready_to_create;

  if (!cis_ready_to_create) {
    LOG(INFO) << __func__ << ": cis is not ready for creation";
    return false;
  }

  if(num_trackers != num_enabling_notify) {
    LOG(WARNING) << __func__
                 << ": Enabling notification is not received for all strms";
    return false;
  } else {
    LOG(INFO) << __func__
              << ": Enabling notification is recevied for all strms, proceed to CIS Creation";
  }

  IsoHciStatus status = IsoHciStatus::ISO_HCI_FAILED;
  std::vector<uint8_t> cis_ids;
  uint8_t cigId;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                      (*int_strm_trackers)[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms =
                             strm_mgr_->GetAudioStreams(*itr);
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (stream != nullptr && std::find(cis_ids.begin(), cis_ids.end(),
                    stream->cis_id) == cis_ids.end()) {
        cis_ids.push_back(stream->cis_id);
        cigId = stream->cig_id;
      }
    }
  }

  if(cis_ids.size() && cis_intf != nullptr) {
    LOG(WARNING) << __func__ << ": Going For CIS Creation ";
    status = cis_intf->CreateCis(cigId, cis_ids, bd_addrs_, getMode());
  }

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                      (*int_strm_trackers)[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms =
                             strm_mgr_->GetAudioStreams(*itr);
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (!stream) {
        LOG(ERROR) << __func__ << ": stream is null";
        continue;
      }
      stream->cis_retry_count = 0;
      if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
        stream->cis_state = CisState::ESTABLISHED;
      } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
        // change the connection state to CIS create issued
        stream->cis_pending_cmd = CisPendingCmd::CIS_CREATE_ISSUED;
      } else {
        LOG(WARNING) << __func__ << ": CIS create Failed";
      }
    }
  }
  return true;
}

bool isRecordReadable(uint8_t total_len, uint8_t processed_len,
                      uint8_t req_len) {
  LOG(WARNING) << __func__ << ": total_len: " << loghex(total_len)
                           << ", processed_len: " << loghex(processed_len)
                           << ", req_len: " << loghex(req_len);
  if((total_len > processed_len) &&
     ((total_len - processed_len) >= req_len)) {
    return true;
  } else {
    return false;
  }
}

void parseVSMetadata(uint8_t total_len, AseGenericParams *gen_params,
                     bool isStreaming, CisInterface *cis_intf, RawAddress bd_addr,
                     UcastAudioStream *audio_stream) {
  LOG(INFO) << __func__ ;
  uint8_t* p = &gen_params->meta_data[0];
  uint8_t ltv_len, ltv_type;
  uint8_t processed_len = 0;
  std::vector<uint8_t> vs_meta_data;
  uint8_t meta_data_len = total_len;

  while(meta_data_len > 0) {
    STREAM_TO_UINT8(ltv_len, p);
    processed_len++;

    if(!ltv_len || !isRecordReadable(total_len, processed_len, ltv_len)) {
      LOG(INFO) << __func__ << ": Data is not readable ";
      break;
    }

    STREAM_TO_UINT8(ltv_type, p);
    processed_len++;

    if(ltv_type == LTV_TYPE_VS_METADATA) {
      uint16_t company_id;
      uint8_t vs_meta_data_len, vs_meta_data_type;
      STREAM_TO_UINT16(company_id, p);
      LOG(INFO) << ": company_id = " << loghex(company_id);
      processed_len += 2;
      ltv_len -= 3; //company id and ltv_type
      while(ltv_len) {
        STREAM_TO_UINT8(vs_meta_data_len, p);
        LOG(INFO) << __func__ << ": vs_meta_data_len = " << loghex(vs_meta_data_len);
        processed_len++;

        if(!vs_meta_data_len || !isRecordReadable(total_len, processed_len, vs_meta_data_len)) {
          LOG(INFO) << __func__ << ": Data is not readable ";
          break;
        }

        STREAM_TO_UINT8(vs_meta_data_type, p);
        LOG(INFO) << __func__ << ": vs_meta_data_type = " << loghex(vs_meta_data_type);
        processed_len++;
        if(vs_meta_data_type == LTV_TYPE_VS_METADATA_FE) {
          vs_meta_data.resize(vs_meta_data_len - 1);
          STREAM_TO_ARRAY(vs_meta_data.data(), p, vs_meta_data_len - 1); // "ltv_len - 1" because 1B for type
          LOG(INFO) << __func__ << ": STREAM_TO_ARRAY done ";
          processed_len += static_cast<int> (sizeof(vs_meta_data));
          if(!isStreaming) {// Push into the queue as vs metadata will be sent later
            CigCisVSMetadata cigCisVSMetadata;
            cigCisVSMetadata.cig_id = gen_params->cig_id;
            cigCisVSMetadata.cis_id = gen_params->cis_id;
            cigCisVSMetadata.vs_meta_data = vs_meta_data;
            cigCisVSMetadata.encoder_mode = 0xFF;
            cigCisVSMetadata.bd_addr = bd_addr;
            audio_stream->vs_meta_data_queue.push(cigCisVSMetadata);
          } else { //right away call UpdateEncoderParams
            LOG(INFO) << __func__ << ": straight away call UpdateEncoderParams ";
            if(cis_intf != NULL) {
              cis_intf->UpdateEncoderParams(gen_params->cig_id, gen_params->cis_id,
                                            vs_meta_data, 0xFF);
            } else {
              LOG(INFO) << __func__ << ": cis_intf is NULL ";
            }
          }
          vs_meta_data.clear();
        } else {
          (p) += (vs_meta_data_len - 1); //just ignore and increase pointer
          processed_len += (vs_meta_data_len - 1);
        }
        ltv_len -= (vs_meta_data_len + 1);
      }
    } else {
      (p) += (ltv_len - 1);
    }
    meta_data_len -= (ltv_len + 1);
  }
}

bool StreamTracker::StateStarting::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateStarting);
    } break;

    case BAP_STOP_REQ_EVT: {
      tracker_.HandleStop(p_data, StreamTracker::kStateStarting);
    } break;

    case BAP_STREAM_UPDATE_REQ_EVT: {
      BapStreamUpdate *evt_data = (BapStreamUpdate *) p_data;
      tracker_.UpdateMetaUpdateStreams(&evt_data->update_streams);
      if(tracker_.HandlePacsAudioContextEvent(&pacs_contexts)) {
        tracker_.HandleStreamUpdate(StreamTracker::kStateStarting);
      }
    } break;

    case PACS_CONNECTION_STATE_EVT: {
      PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
      LOG(WARNING) << __func__
               << ": pacs_state: " << static_cast<int>(pacs_state->state);
      if (pacs_state->state == ConnectionState::DISCONNECTED) {
        LOG(WARNING) << __func__ << ": traker bd_addr Size: "
                                 << tracker_.bd_addrs_.size();
        if (tracker_.bd_addrs_.size() == 1) {
           tracker_.HandlePacsConnectionEvent(p_data);
        } else {
          tracker_.HandleSingleDeviceRemoteDisconnetFromGroup(event, p_data);
          LOG(WARNING) << __func__ << ": removing address: " << pacs_state->bd_addr;
          bd_addrs_.erase(std::remove(bd_addrs_.begin(), bd_addrs_.end(), pacs_state->bd_addr),
                                                                   bd_addrs_.end());
          tracker_.bd_addrs_.erase(std::remove(tracker_.bd_addrs_.begin(),
                                              tracker_.bd_addrs_.end(), pacs_state->bd_addr),
                                                             tracker_.bd_addrs_.end());

          // check and update streaming state after single device remote disconnection
          if (tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_)) {
            LOG(INFO) << __func__ << ": update streaming state after single remote disconnected";
            break;
          }

          // check for Enabling notification is received for all ASEs after single device remote disconnection
          if (tracker_.CheckAndCreateCis(&int_strm_trackers_)) {
            LOG(INFO) << __func__ << ": create CIS after single single remote disconnected";
          }
        }
      } else {
        tracker_.HandlePacsConnectionEvent(p_data);
      }
    } break;

    case PACS_AUDIO_CONTEXT_RES_EVT: {
      // check for all stream start requests, stream contexts are
      // part of available contexts
      pacs_contexts = *((PacsAvailableContexts *) p_data);
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                            pacs_contexts.bd_addr);
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                           pacs_contexts.bd_addr);
      std::vector<IntStrmTracker *> *all_trackers =
                 int_strm_trackers_[pacs_contexts.bd_addr].GetTrackerList();
      bool ignore_event = false;

      std::vector<StreamType> *start_streams = tracker_.GetStreams();
      uint8_t contexts_supported = 0;

      // check if supported audio contexts has required contexts
      for(auto it = start_streams->begin(); it != start_streams->end(); it++) {
        LOG(ERROR) << __func__
                   << ": it->direction: " << loghex(it->direction)
                   << ", it->audio_context: " << loghex(it->audio_context)
                   << ", available_contexts: " << loghex(pacs_contexts.available_contexts);
        if(it->direction == ASE_DIRECTION_SINK) {
          if(it->audio_context & pacs_contexts.available_contexts) {
            contexts_supported++;
          } else if(1 & pacs_contexts.available_contexts) {
            contexts_supported++;
            LOG(INFO) << __func__ << ": Sink direction unspecified context: "
                                  << it->audio_context;
          }
        } else if(it->direction == ASE_DIRECTION_SRC) {
          if((static_cast<uint64_t>(it->audio_context) << 16) &
               pacs_contexts.available_contexts) {
            contexts_supported++;
          } else if(1 & pacs_contexts.available_contexts) {
            contexts_supported++;
            LOG(INFO) << __func__ << ": src direction unspecified context: "
                                  << it->audio_context;
          }
        }
      }

      if(contexts_supported != start_streams->size()) {
        LOG(ERROR) << __func__ << ": No Matching available Contexts found";
        tracker_.TransitionTo(StreamTracker::kStateConnected);
        break;
      }

      for(auto it = start_streams->begin(); it != start_streams->end(); it++) {
        StreamContext *context = contexts->FindOrAddByType(*it);
        for (auto id = context->stream_ids.begin();
                  id != context->stream_ids.end(); id++) {
          UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
          if (stream != nullptr && stream->overall_state ==
                                 StreamTracker::kStateStarting) {
            ignore_event = true;
            break;
          }
        }
      }

      if(ignore_event) break;

      // Now create the groups
      for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
        StreamContext *context = contexts->FindByType((*i)->strm_type);
        UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
        if (!stream) {
          LOG(ERROR) << __func__ << "stream is null";
          continue;
        }
        QosConfig *qos_config = &stream->qos_config;
        CodecConfig *codec_config = &stream->codec_config;
        bap_print_codec_parameters(*codec_config);
        bap_print_qos_parameters(*qos_config);
        uint16_t audio_context = stream->audio_context;
        LOG(INFO) << __func__
                  << ": stream->direction = " << loghex(stream->direction)
                  << ", stream->audio_context = "<<loghex(stream->audio_context);
        CisInterface *cis_intf = strm_mgr_->GetCisInterface(
                                            pacs_contexts.bd_addr);
        for (auto it = stream->qos_config.cis_configs.begin();
                       it != stream->qos_config.cis_configs.end(); it++) {
        }
        IsoHciStatus status =  cis_intf->CreateCig(pacs_contexts.bd_addr,
                                                   false,
                                                   qos_config->cig_config,
                                                   qos_config->cis_configs,
                                                   codec_config,
                                                   audio_context,
                                                   stream->direction,
                                                   context->req_alt_qos_configs);
        LOG(WARNING) << __func__ << ": status: "
                     << loghex(static_cast<uint8_t>(status));
        if(status == IsoHciStatus::ISO_HCI_SUCCESS) {
          stream->cig_state = CigState::CREATED;
          stream->cis_state = CisState::READY;
        } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
          stream->cis_pending_cmd = CisPendingCmd::CIG_CREATE_ISSUED;
        } else {
          LOG(ERROR) << __func__ << ": CIG Creation Failed";
        }
      }
      tracker_.CheckAndSendEnable(pacs_contexts.bd_addr,
                                  &int_strm_trackers_[pacs_contexts.bd_addr]);
    } break;

    case CIS_GROUP_STATE_EVT: {
      CisGroupState *cig =  ((CisGroupState *) p_data);
      tracker_.HandleCigStateEvent(event, p_data,
                                  &int_strm_trackers_[cig->bd_addr]);
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case ASCS_ASE_STATE_EVT: {
      // to handle remote driven operations
      // check the state and if the state is Enabling
      // proceed with cis creation
      LOG(INFO) << __func__ << ": statestarting ASCS_ASE_STATE_EVT";
      AscsState *ascs =  ((AscsState *) p_data);

      if(!tracker_.ValidateAseUpdate(p_data, &int_strm_trackers_[ascs->bd_addr],
                                     StreamTracker::kStateStarting)) {
        break;
      }

      if (ascs->ase_params.ase_state == ascs::ASE_STATE_ENABLING) {
        // change the connection state to ENABLING
        CisInterface *cis_intf = strm_mgr_->GetCisInterface(ascs->bd_addr);

        // check for Enabling notification is received for all ASEs
        uint8_t num_enabling_notify = 0;
        uint16_t num_trackers = 0;
        for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
          std::vector<IntStrmTracker *> *all_trackers =
                            int_strm_trackers_[*itr].GetTrackerList();
          UcastAudioStreams *audio_strms =
                                   strm_mgr_->GetAudioStreams(*itr);
          for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
            UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
            if((*i)->ase_id == ascs->ase_params.ase_id) {
              AseGenericParams *gen_params = &ascs->ase_params.generic_params;
              uint8_t meta_data_len = gen_params->meta_data_len;
              LOG(INFO) << __func__ << ": ase_id = "<<loghex((*i)->ase_id)
                                     << ", cig_id = "<< loghex((*i)->cig_id)
                                     << ", cis_id = "<<loghex((*i)->cis_id);

              //have to put some state value in 3rd argument instead of bool
              parseVSMetadata(meta_data_len, &ascs->ase_params.generic_params,
                              false, cis_intf, ascs->bd_addr, stream);
            }
            if(stream && stream->ase_state == ascs::ASE_STATE_ENABLING) {
              num_enabling_notify++;
            }
            num_trackers++;
          }
        }

        if(num_trackers != num_enabling_notify) {
          LOG(WARNING) << __func__
                       << ": Enabling notification is not received for all strms";
          break;
        }

        // As it single group use cases, always single group start request
        // will come to BAP layer
        IsoHciStatus status = IsoHciStatus::ISO_HCI_FAILED;
        std::vector<uint8_t> cis_ids;
        uint8_t cigId;
        for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
          std::vector<IntStrmTracker *> *all_trackers =
                            int_strm_trackers_[*itr].GetTrackerList();
          UcastAudioStreams *audio_strms =
                                   strm_mgr_->GetAudioStreams(*itr);
          for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
            UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
            if (std::find(cis_ids.begin(), cis_ids.end(),
                          stream->cis_id) == cis_ids.end()) {
              cis_ids.push_back(stream->cis_id);
              cigId = stream->cig_id;
            }
          }
        }

        if(cis_ids.size()) {
          LOG(WARNING) << __func__ << ": Going For CIS Creation ";
          status = cis_intf->CreateCis(cigId, cis_ids, bd_addrs_, tracker_.getMode());
        }

        for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
          std::vector<IntStrmTracker *> *all_trackers =
                            int_strm_trackers_[*itr].GetTrackerList();
          UcastAudioStreams *audio_strms =
                                   strm_mgr_->GetAudioStreams(*itr);
          for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
            UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
            if (!stream) {
              LOG(ERROR) << __func__ << ": stream is null";
              continue;
            }
            stream->cis_retry_count = 0;
            if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
              stream->cis_state = CisState::ESTABLISHED;
            } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
              // change the connection state to CIS create issued
              stream->cis_pending_cmd = CisPendingCmd::CIS_CREATE_ISSUED;
            } else {
              LOG(WARNING) << __func__ << ": CIS create Failed";
            }
          }
        }
      } else if (ascs->ase_params.ase_state == ascs::ASE_STATE_STREAMING) {
        CisInterface *cis_intf = strm_mgr_->GetCisInterface(ascs->bd_addr);

        for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
          std::vector<IntStrmTracker *> *all_trackers =
                            int_strm_trackers_[*itr].GetTrackerList();
          UcastAudioStreams *audio_strms =
                                   strm_mgr_->GetAudioStreams(*itr);

          for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
            if((*i)->ase_id == ascs->ase_params.ase_id) {
              UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
              AseGenericParams *gen_params = &ascs->ase_params.generic_params;
              uint8_t meta_data_len = gen_params->meta_data_len;
              LOG(INFO) << __func__ << ": ase_id = "<<loghex((*i)->ase_id)
                                     << ", cig_id = "<< loghex((*i)->cig_id)
                                     << ", cis_id = "<<loghex((*i)->cis_id);

              //have to put some state value in 3rd argument instead of bool
              parseVSMetadata(meta_data_len, &ascs->ase_params.generic_params,
                              false, cis_intf, ascs->bd_addr, stream);
            }
          }
        }
        tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_);

      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
        tracker_.HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());

      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_DISABLING ||
                ascs->ase_params.ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
        tracker_.HandleRemoteStop(ASCS_ASE_STATE_EVT, p_data, StateId());
      }
    } break;

    case ASCS_ASE_OP_FAILED_EVT: {
      tracker_.HandleAseOpFailedEvent(p_data);
    } break;

    case CIS_STATE_EVT: {
      CisStreamState *data = (CisStreamState *) p_data;
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
      // check if current stream tracker is interested in this CIG update
      std::vector<IntStrmTracker *> int_trackers =
                   int_strm_trackers_[data->bd_addr].
                   FindByCisId(data->cig_id, data->cis_id);
      if(int_trackers.empty()) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }

      if(data->state == CisState::ESTABLISHED) {
        // find out the CIS is bidirectional or from air direction
        // cis, send Receiver start ready as set up data path
        // is already completed during CIG creation
        if(data->direction & cis::DIR_FROM_AIR) {
          // setup the datapath for RX
          // find out the stream here
          UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                     (data->cig_id, data->cis_id,
                                      cis::DIR_FROM_AIR);
          LOG(WARNING) << __func__ << ": DIR_FROM_AIR: "
                       << loghex(static_cast <uint8_t> (cis::DIR_FROM_AIR));

          if(stream && int_strm_trackers_[data->bd_addr].
                               FindByAseId(stream->ase_id)) {
            LOG(INFO) << __func__ << ": Stream Direction: "
                      << loghex(static_cast <uint8_t> (stream->direction));

            LOG(INFO) << __func__ << ": Stream ASE Id: "
                      << loghex(static_cast <uint8_t> (stream->ase_id));

            AscsClient *ascs_client = strm_mgr_->GetAscsClient(data->bd_addr);
            AseStartReadyOp start_ready_op = {
                                              .ase_id = stream->ase_id
                                             };
            std::vector<AseStartReadyOp> ase_ops;
            ase_ops.push_back(start_ready_op);
            ascs_client->StartReady(ASCS_CLIENT_ID,
                         data->bd_addr, ase_ops);
            stream->cis_state = data->state;
            stream->cis_pending_cmd = CisPendingCmd::NONE;
            stream->ase_pending_cmd = AscsPendingCmd::START_READY_ISSUED;
          }
        }

        if(data->direction & cis::DIR_TO_AIR) {
          UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                     (data->cig_id, data->cis_id,
                                      cis::DIR_TO_AIR);
          if(stream) {
            stream->cis_state = data->state;
            stream->cis_pending_cmd = CisPendingCmd::NONE;
          }
        }

        std::vector<int> ids = { StreamTracker::kStateStarting };
        uint8_t dir = 0;
        if (data->direction & cis::DIR_TO_AIR)
          dir |= ASE_DIRECTION_SINK;
        if (data->direction & cis::DIR_FROM_AIR)
          dir |= ASE_DIRECTION_SRC;

        std::vector<UcastAudioStream *> streams = audio_strms->GetStreamsByStates(ids, dir);
        for (auto it = streams.begin(); it != streams.end(); it++) {
          LOG(WARNING) << __func__ << ": Assign CIS Established Params to streams ";
          UcastAudioStream *stream = *it;
          stream->cis_establised_params = data->param;
        }

        tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_);

      } else if (data->state == CisState::READY) { // CIS creation failed
        CisInterface *cis_intf = strm_mgr_->GetCisInterface(data->bd_addr);
        UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                   (data->cig_id, data->cis_id,
                                    data->direction);
        if(stream && stream->cis_retry_count < 2 &&
           data->param.status != REMOTE_USER_TERMINATED_CONN &&
           data->param.status != CIS_DISCONN_SUCCESS) {
          std::vector<uint8_t> cisIds = {stream->cis_id};
          LOG(WARNING) << __func__ << ": Going For Retrial of CIS Creation ";
          std::vector<RawAddress> bd_addrs;
          bd_addrs.push_back(data->bd_addr);
          IsoHciStatus status =  cis_intf->CreateCis(
                                           stream->cig_id,
                                           cisIds,
                                           bd_addrs,
                                           tracker_.getMode());

          if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
            stream->cis_state = CisState::ESTABLISHED;
            tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_);
          } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
            // change the connection state to CIS create issued
            stream->cis_retry_count++;
            stream->cis_pending_cmd = CisPendingCmd::CIS_CREATE_ISSUED;
          } else {
            stream->cis_retry_count = 0;
            LOG(WARNING) << __func__ << ": CIS create Failed";
          }
        } else {
          if (btif_bap_is_device_supports_csip(data->bd_addr)) {
            LOG(WARNING) << __func__
                         << ": Removing address: " << data->bd_addr;
            bd_addrs_.erase(std::remove(bd_addrs_.begin(),
                                        bd_addrs_.end(), data->bd_addr),bd_addrs_.end());
            tracker_.bd_addrs_.erase(std::remove(tracker_.bd_addrs_.begin(),
                                                 tracker_.bd_addrs_.end(), data->bd_addr),
                                                                tracker_.bd_addrs_.end());
            //Todo
            //If remote gives status as other than 0x13 need to send connected
            //to acm, and do corresponding chnages in acm.
            tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_);
          } else {
            tracker_.HandleStop(nullptr, StreamTracker::kStateStarting);
          }
          if(stream) {
            stream->cis_retry_count = 0;
            stream->cis_state = data->state;
            stream->cis_pending_cmd = CisPendingCmd::NONE;
          }
        }
      } else {  // transient states
        UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                   (data->cig_id, data->cis_id,
                                    data->direction);
        if(stream) stream->cis_state = data->state;
      }
    } break;

    case BAP_TIME_OUT_EVT: {
      tracker_.OnTimeout(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

void StreamTracker::StateUpdating::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  uint8_t num_ases = 0;

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamUpdate> *update_streams = tracker_.GetMetaUpdateStreams();

    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));
    LOG(WARNING) << __func__ << ": Start Streams Size "
                              << update_streams->size();

    StreamControlType control_type = tracker_.GetControlType();

    if(control_type != StreamControlType::UpdateStream) return;

    for (auto it = update_streams->begin(); it != update_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      state.stream_type = it->stream_type;
      state.stream_state = StreamState::UPDATING;
      strms.push_back(state);
      StreamContext *context = contexts->FindOrAddByType(it->stream_type);
      context->stream_state = StreamState::UPDATING;
      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        int_strm_trackers_[*itr].FindOrAddBytrackerType(it->stream_type,
                                  id->ase_id, id->cig_id,
                                  id->cis_id,
                                  context->codec_config, context->qos_config);
      }
      num_ases += context->stream_ids.size();
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }

  uint64_t tout = num_ases *
                    (static_cast<uint64_t>(TimeoutVal::UpdatingTimeout));
  if(!tout) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::UpdatingTimeout);
  }
  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateUpdatingTimer",
                                             &timeout, reason, tout);
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": StateUpdating: Alarm not allocated.";
    return;
  }
}

void StreamTracker::StateUpdating::OnExit() {
  tracker_.ClearTimer(state_transition_timer, "StateUpdatingTimer");
}

bool StreamTracker::StateUpdating::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateUpdating);
    } break;

    case BAP_STOP_REQ_EVT: {
      tracker_.HandleStop(p_data, StreamTracker::kStateUpdating);
    } break;

    case PACS_CONNECTION_STATE_EVT: {
      tracker_.HandlePacsConnectionEvent(p_data);
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case PACS_AUDIO_CONTEXT_RES_EVT: {
      // check for all stream start requests, stream contexts are
      // part of available contexts
      PacsAvailableContexts *pacs_contexts = (PacsAvailableContexts *) p_data;
      if(!tracker_.HandlePacsAudioContextEvent(pacs_contexts) ||
         !tracker_.HandleStreamUpdate(StreamTracker::kStateUpdating)) {
        tracker_.TransitionTo(StreamTracker::kStateStreaming);
      }
    } break;

    case ASCS_ASE_STATE_EVT: {
      LOG(INFO) << __func__ << ": stateupdating ASCS_ASE_STATE_EVT";
      AscsState *ascs =  ((AscsState *) p_data);
      if(!tracker_.ValidateAseUpdate(p_data, &int_strm_trackers_[ascs->bd_addr],
                                     StreamTracker::kStateUpdating)) {
        break;
      }

      if(ascs->ase_params.ase_state == ascs::ASE_STATE_STREAMING) {
        tracker_.CheckAndUpdateStreamingState(&int_strm_trackers_);
      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
        tracker_.HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());
      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_DISABLING) {
        tracker_.HandleRemoteStop(ASCS_ASE_STATE_EVT, p_data, StateId());
      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
        // this can happen when CIS is lost and detected on remote side
        // first so it will immediately transition to QOS configured.
        tracker_.HandleAbruptStop(ASCS_ASE_STATE_EVT, p_data);
      }
    } break;

    case CIS_STATE_EVT: {
      // handle sudden CIS Disconnection
      tracker_.HandleCisEventsInStreaming(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

void StreamTracker::StateStreaming::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamType> *start_streams = tracker_.GetStreams();
    std::vector<StreamUpdate> *update_streams = tracker_.GetMetaUpdateStreams();

    LOG(WARNING) << __func__ << ": Start Streams Size: "
                              << start_streams->size();

    LOG(WARNING) << __func__ << ": Update Streams Size: "
                              << update_streams->size();

    StreamControlType control_type = tracker_.GetControlType();
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

    if(control_type == StreamControlType::Start) {
      for (auto it = start_streams->begin(); it != start_streams->end(); it++) {
        StreamStateInfo state;
        memset(&state, 0, sizeof(state));
        state.stream_type = *it;
        state.stream_state = StreamState::STREAMING;
        UcastAudioStream *stream =
            audio_strms->FindByStreamType(it->audio_context, it->direction);
        if(stream != NULL) {
          state.stream_link_params = { .status = stream->cis_establised_params.status,
                                       .cig_id = stream->cis_establised_params.cig_id,
                                       .cis_id = stream->cis_establised_params.cis_id,
                                       .connection_handle =
                                           stream->cis_establised_params.connection_handle,
                                       .transport_latency_m_to_s =
                                         stream->cis_establised_params.transport_latency_m_to_s,
                                       .transport_latency_s_to_m =
                                           stream->cis_establised_params.transport_latency_s_to_m,
                                       .ft_m_to_s = stream->cis_establised_params.ft_m_to_s,
                                       .ft_s_to_m = stream->cis_establised_params.ft_s_to_m,
                                       .iso_interval = stream->cis_establised_params.iso_interval
                                     };
          strms.push_back(state);
        }
        StreamContext *context = contexts->FindOrAddByType(*it);
        context->stream_state = StreamState::STREAMING;
      }
    } else if(control_type == StreamControlType::UpdateStream) {
      for (auto it = update_streams->begin(); it != update_streams->end();
                                              it++) {
        StreamStateInfo state;
        memset(&state, 0, sizeof(state));
        state.stream_type = it->stream_type;
        state.stream_state = StreamState::STREAMING;
        UcastAudioStream *stream = audio_strms->FindByStreamType(
                it->stream_type.audio_context, it->stream_type.direction);
        if(stream != NULL) {
          state.stream_link_params = { .status = stream->cis_establised_params.status,
                                       .cig_id = stream->cis_establised_params.cig_id,
                                       .cis_id = stream->cis_establised_params.cis_id,
                                       .connection_handle =
                                           stream->cis_establised_params.connection_handle,
                                       .transport_latency_m_to_s =
                                           stream->cis_establised_params.transport_latency_m_to_s,
                                       .transport_latency_s_to_m =
                                           stream->cis_establised_params.transport_latency_s_to_m,
                                       .ft_m_to_s = stream->cis_establised_params.ft_m_to_s,
                                       .ft_s_to_m = stream->cis_establised_params.ft_s_to_m,
                                       .iso_interval = stream->cis_establised_params.iso_interval
                                     };
          strms.push_back(state);
        }

        StreamContext *context = contexts->FindOrAddByType(it->stream_type);
        context->stream_state = StreamState::STREAMING;
      }
    }
    if(strms.size() && callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }
}

void StreamTracker::StateStreaming::OnExit() {

}

bool StreamTracker::StateStreaming::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateStreaming);
    } break;

    case BAP_STOP_REQ_EVT: {
      tracker_.setMode(((BapStop *) p_data)->mode);
      tracker_.HandleStop(p_data, StreamTracker::kStateStreaming);
    } break;

    case BAP_STREAM_UPDATE_REQ_EVT: {
      BapStreamUpdate *evt_data = (BapStreamUpdate *) p_data;
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(evt_data->bd_addr);
      std::vector<StreamUpdate> update_streams = evt_data->update_streams;
      bool is_mode_switch_update = false;
      for (auto it = update_streams.begin(); it != update_streams.end(); ++it) {
        StreamUpdate stream = *it;
        if (stream.update_type == bluetooth::bap::ucast::StreamUpdateType::ENCODER_MODE_UPDATE) {
          UcastAudioStream *strm = audio_strms->FindByStreamType(
                  stream.stream_type.audio_context, stream.stream_type.direction);
          if (strm != nullptr) {
            CisInterface *cis_intf = strm_mgr_->GetCisInterface(evt_data->bd_addr);
            if(cis_intf != NULL) {
              std::vector<uint8_t> param;
              cis_intf->UpdateEncoderParams(strm->cig_id, strm->cis_id,
                                          param, stream.update_value);
            }
          }
          is_mode_switch_update = true;
        }
      }

      if (is_mode_switch_update) break;

      PacsClient *pacs_client = strm_mgr_->GetPacsClient(evt_data->bd_addr);
      uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(evt_data->bd_addr);
      tracker_.UpdateControlType(StreamControlType::UpdateStream);
      tracker_.UpdateMetaUpdateStreams(&evt_data->update_streams);
      if(pacs_client) {
        pacs_client->GetAudioAvailability(pacs_client_id, evt_data->bd_addr);
      }
      tracker_.TransitionTo(StreamTracker::kStateUpdating);
    } break;

    case PACS_CONNECTION_STATE_EVT: {
      tracker_.HandlePacsConnectionEvent(p_data);
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case ASCS_ASE_STATE_EVT: {
      LOG(INFO) << __func__ << ": statestreaming ASCS_ASE_STATE_EVT";
      AscsState *ascs =  ((AscsState *) p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      // find out the stream for the given ase id
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);
      UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);
      if (stream) {
        stream->ase_state = ascs->ase_params.ase_state;
        stream->ase_params = ascs->ase_params;
        stream->ase_pending_cmd = AscsPendingCmd::NONE;
        if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
          tracker_.HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());
        } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_DISABLING) {
          tracker_.HandleRemoteStop(ASCS_ASE_STATE_EVT, p_data, StateId());
        } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_QOS_CONFIGURED){
          // this can happen when CIS is lost and detected on remote side
          // first so it will immediately transition to QOS configured.
          tracker_.HandleAbruptStop(ASCS_ASE_STATE_EVT, p_data);
        }
      }

      AseGenericParams *gen_params = &ascs->ase_params.generic_params;
      uint8_t meta_data_len = gen_params->meta_data_len;
      CisInterface *cis_intf = strm_mgr_->GetCisInterface(ascs->bd_addr);
      parseVSMetadata(meta_data_len, &ascs->ase_params.generic_params, true, cis_intf, ascs->bd_addr, stream); //have to put some state value in 3rd argument instead of bool
    } break;

    case CIS_STATE_EVT: {
      // handle sudden CIS Disconnection
      tracker_.HandleCisEventsInStreaming(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      LOG(WARNING) << __func__ << ": Stable state, return false.";
      return false;
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

void StreamTracker::StateStopping::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  uint8_t num_ases = 0;
  uint64_t tout;
  DeviceType dev_type = DeviceType::NONE;

  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamType> *stop_streams = tracker_.GetStreams();

    LOG(WARNING) << __func__ << ": Stop Streams Size : "
                              << stop_streams->size();
    StreamControlType control_type = tracker_.GetControlType();
    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));

    if(control_type != StreamControlType::Stop) return;

    for (auto it = stop_streams->begin();
                         it != stop_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      state.stream_type = *it;
      state.stream_state = StreamState::STOPPING;
      strms.push_back(state);
      StreamContext *context = contexts->FindOrAddByType(*it);
      context->stream_state = StreamState::STOPPING;
      bool is_gaming_rx = (it->type == CONTENT_TYPE_MEDIA) &&
                          (it->direction == ASE_DIRECTION_SRC) &&
                          (it->audio_context == CONTENT_TYPE_GAME);

      if(dev_type == DeviceType::NONE) {
        btif_bap_get_device_type(*itr, &dev_type,
                            static_cast <CodecDirection> (it->direction));
      }
      for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
        int_strm_trackers_[*itr].FindOrAddBytrackerType(*it,
                                  id->ase_id, id->cig_id,
                                  id->cis_id,
                                  context->codec_config, context->qos_config);
        if (is_gaming_rx && context->is_mono_config) {
          context->is_mono_config = false;
          LOG(ERROR) << __func__ << ": Defer allocating int tracker for another ASE";
          break;
        }
      }
      num_ases += context->stream_ids.size();
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }
  if(dev_type == DeviceType::EARBUD ||
     dev_type == DeviceType::EARBUD_NO_CSIP ||
     dev_type == DeviceType::HEADSET_STEREO) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::StoppingTimeout);
  } else {
    tout = num_ases * (static_cast<uint64_t>(TimeoutVal::StoppingTimeout));
  }

  if(!tout) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::StoppingTimeout);
  }

  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateStoppingTimer",
                &timeout, reason, tout);
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": StateStopping: Alarm not allocated.";
    return;
  }
}

void StreamTracker::StateStopping::OnExit() {
  tracker_.ClearTimer(state_transition_timer, "StateStoppingTimer");
}

bool StreamTracker::StateStopping::TerminateCisAndCig(RawAddress bd_addr,
                                                  UcastAudioStream *stream) {

  CisInterface *cis_intf = strm_mgr_->GetCisInterface(bd_addr);
  uint8_t num_strms_in_qos_configured = 0;
  UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(bd_addr);

  std::vector<IntStrmTracker *> all_trackers =
                      int_strm_trackers_[bd_addr].FindByCigIdAndDir(stream->cig_id,
                                                           stream->direction);

  for(auto i = all_trackers.begin(); i != all_trackers.end();i++) {
    UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
    if (!stream) {
      LOG(ERROR) << __func__ << "stream is null";
      continue;
    }
    if(stream->ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
      num_strms_in_qos_configured++;
    }
  }

  if(all_trackers.size() != num_strms_in_qos_configured) {
    LOG(WARNING) << __func__ << "Not All Streams Moved to QOS Configured";
    return false;
  }

  for (auto i = all_trackers.begin(); i != all_trackers.end();i++) {
    UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
    if (!stream) {
      LOG(ERROR) << __func__ << "stream is null";
      continue;
    }
    if(stream->cis_pending_cmd == CisPendingCmd::NONE &&
       stream->cis_state == CisState::ESTABLISHED &&
       stream->ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
      LOG(WARNING) << __func__ << ": Going For CIS Disconnect ";
      IsoHciStatus status;
      if (tracker_.getMode()) {
        LOG(WARNING) << __func__  << ": AAR4 2 CISes Disconnect ";
        status = cis_intf->DisconnectCis(stream->cig_id,
                                         stream->cis_id,
                                         cis::DIR_FROM_AIR);
        status = cis_intf->DisconnectCis(stream->cig_id,
                                         stream->cis_id,
                                         cis::DIR_TO_AIR);
      } else {
        status = cis_intf->DisconnectCis(stream->cig_id,
                                         stream->cis_id,
                                         stream->direction);
      }
      if(status == IsoHciStatus::ISO_HCI_SUCCESS) {
        stream->cis_pending_cmd = CisPendingCmd::NONE;
        stream->cis_state = CisState::READY;
      } else if(status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
        stream->cis_pending_cmd = CisPendingCmd::CIS_DESTROY_ISSUED;
      } else {
        LOG(WARNING) << __func__ << ": CIS Disconnect Failed";
      }
    }

    if(stream->cis_state == CisState::READY) {
      if(stream->cig_state == CigState::CREATED &&
         stream->cis_pending_cmd == CisPendingCmd::NONE) {
        LOG(WARNING) << __func__ << ": Going For CIG Removal";
        IsoHciStatus status = cis_intf->RemoveCig(bd_addr,
                                stream->cig_id);
        if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
          stream->cig_state = CigState::INVALID;
          stream->cis_state = CisState::INVALID;
        } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
          stream->cis_pending_cmd = CisPendingCmd::CIG_REMOVE_ISSUED;
        } else {
          LOG(WARNING) << __func__ << ": CIG removal Failed";
        }
      }
    }
  }
  return true;
}

bool StreamTracker::StateStopping::CheckAndUpdateStoppedState() {
  //  to check for all internal trackers are moved to
  // cis destroyed state then update the callback
  uint8_t num_strms_in_stopping = 0;
  uint8_t num_trackers = 0;
  bool pending_cmds = false;

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                        int_strm_trackers_[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);

    // check if any pending commands are present
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && (stream->cis_pending_cmd != CisPendingCmd::NONE ||
                    stream->ase_pending_cmd != AscsPendingCmd::NONE)) {
        LOG(WARNING) << __func__ << ": cis_pending_cmd "
                     << loghex(static_cast <uint8_t>(stream->cis_pending_cmd));
        LOG(WARNING) << __func__ << ": ase_pending_cmd "
                     << loghex(static_cast <uint8_t>(stream->ase_pending_cmd));
        pending_cmds = true;
        break;
      }
    }
    if(pending_cmds) break;
  }

  LOG(WARNING) << __func__ << "Not All Streams Moved to Stopped State" << pending_cmds;
  if(pending_cmds) return false;

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                        int_strm_trackers_[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    for(auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && (stream->cig_state == CigState::IDLE ||
         stream->cig_state == CigState::INVALID) &&
         (stream->cis_state == CisState::READY ||
          stream->cis_state == CisState::INVALID) &&
         stream->ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {
        num_strms_in_stopping++;
      }
      num_trackers++;
    }
  }

  if(num_trackers != num_strms_in_stopping) {
    LOG(WARNING) << __func__ << "Not All Streams Moved to Stopped State";
    return false;
  }

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                        int_strm_trackers_[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (stream) {
        stream->overall_state = StreamTracker::kStateConnected;
      }
    }
  }

  tracker_.TransitionTo(StreamTracker::kStateConnected);
  return true;
}

bool StreamTracker::StateStopping::ProcessEvent(uint32_t event, void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT:{
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateStopping);
    } break;

    case PACS_CONNECTION_STATE_EVT: {
      tracker_.HandlePacsConnectionEvent(p_data);
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case ASCS_ASE_STATE_EVT: {
      LOG(INFO) << __func__ << ": statestopping ASCS_ASE_STATE_EVT";
      // to handle remote driven operations
      AscsState *ascs =  ((AscsState *) p_data);

      if(!tracker_.ValidateAseUpdate(p_data, &int_strm_trackers_[ascs->bd_addr],
                                     StreamTracker::kStateStopping)) {
        break;
      }

      // find out the stream for the given ase id
      uint8_t ase_id = ascs->ase_params.ase_id;
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);

      UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);
      if (!stream) {
        LOG(ERROR) << __func__ << ": stream is null";
        break;
      }

      if(ascs->ase_params.ase_state == ascs::ASE_STATE_DISABLING) {
        if(stream->direction & cis::DIR_FROM_AIR) {
          LOG(INFO) << __func__ << ": Sending Stop Ready ";
          AscsClient *ascs_client = strm_mgr_->GetAscsClient(ascs->bd_addr);
          AseStopReadyOp stop_ready_op = {
                                .ase_id = stream->ase_id
          };
          std::vector<AseStopReadyOp> ase_ops;
          ase_ops.push_back(stop_ready_op);
          ascs_client->StopReady(ASCS_CLIENT_ID,
                       ascs->bd_addr, ase_ops);
          stream->ase_pending_cmd = AscsPendingCmd::STOP_READY_ISSUED;
        } else {
          LOG(ERROR) << __func__ << ": Invalid State transition to Disabling"
                     << ": ASE Id = " << loghex(ase_id);
        }

      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_QOS_CONFIGURED) {

        stream->ase_pending_cmd = AscsPendingCmd::NONE;
        // stopped state then issue CIS disconnect
        if ((stream->direction == cis::DIR_FROM_AIR) && tracker_.getMode()){
          tracker_.TransitionTo(StreamTracker::kStateConnected);
          stream->overall_state = StreamTracker::kStateConnected;
          break;
        }
        TerminateCisAndCig(ascs->bd_addr, stream);
        CheckAndUpdateStoppedState();

      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
        tracker_.HandleRemoteDisconnect(ASCS_ASE_STATE_EVT, p_data, StateId());
      }
    } break;

    case ASCS_ASE_OP_FAILED_EVT: {
      tracker_.HandleAseOpFailedEvent(p_data);
    } break;

    case CIS_STATE_EVT: {
      CisStreamState *data = (CisStreamState *) p_data;
      CisInterface *cis_intf = strm_mgr_->GetCisInterface(data->bd_addr);
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
      // check if current stream tracker is interested in this CIG update
      std::vector<IntStrmTracker *> int_trackers =
                   int_strm_trackers_[data->bd_addr].FindByCisId(
                   data->cig_id, data->cis_id);
      if(int_trackers.empty()) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }
      if(data->state == CisState::ESTABLISHED) {
        for(auto it = directions.begin(); it != directions.end(); ++it) {
          if(data->direction & *it) {
            // find out the stream here
            UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                       (data->cig_id, data->cis_id, *it);
            if(stream) {
              stream->cis_state = data->state;
              stream->cis_pending_cmd = CisPendingCmd::NONE;
              if(int_strm_trackers_[data->bd_addr].FindByAseId(stream->ase_id)){
                TerminateCisAndCig(data->bd_addr, stream);
              }
            }
          }
        }
        CheckAndUpdateStoppedState();

      } else if(data->state == CisState::READY) {
        for(auto it = directions.begin(); it != directions.end(); ++it) {
          if(data->direction & *it) {
            // find out the stream here
            UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                       (data->cig_id, data->cis_id, *it);
            if(stream) {
              stream->cis_state = data->state;
              stream->cis_pending_cmd = CisPendingCmd::NONE;
              if(stream->cig_state == CigState::CREATED &&
                 stream->cis_pending_cmd == CisPendingCmd::NONE) {
                IsoHciStatus status = cis_intf->RemoveCig(
                                        data->bd_addr,
                                        stream->cig_id);
                if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
                  stream->cig_state = CigState::INVALID;
                  stream->cis_state = CisState::INVALID;
                } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
                  stream->cis_pending_cmd = CisPendingCmd::CIG_REMOVE_ISSUED;
                } else {
                  LOG(WARNING) << __func__ << ": CIG removal Failed";
                }
              }
            }
          }
        }
        CheckAndUpdateStoppedState();
      } else {  // transient states
        for(auto it = directions.begin(); it != directions.end(); ++it) {
          if(data->direction & *it) {
            // find out the stream here
            UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                       (data->cig_id, data->cis_id, *it);
            if(stream) stream->cis_state = data->state;
          }
        }
      }
    } break;

    case CIS_GROUP_STATE_EVT: {
      CisGroupState *data = ((CisGroupState *) p_data);
      // check if current stream tracker is interested in this CIG update
      std::vector<IntStrmTracker *> int_trackers =
                            int_strm_trackers_[data->bd_addr].
                                   FindByCigId(data->cig_id);
      if(int_trackers.empty()) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }

      for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
        uint8_t ase_id = ((*i)->ase_id);
        LOG(WARNING) << __func__ << ": ASE ID: " << loghex(ase_id);
      }
      LOG(WARNING) << __func__ << ": Tracker size: " << int_trackers.size();
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
      if(data->state == CigState::CREATED) {
        for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
          if (stream) {
            // check if this is a CIG created event due to CIG create
            // issued during starting state
            stream->cis_pending_cmd = CisPendingCmd::NONE;
            stream->cig_state = data->state;
            stream->cis_state = CisState::READY;
            TerminateCisAndCig(data->bd_addr, stream);
          }
        }
        CheckAndUpdateStoppedState();

      } else if(data->state == CigState::IDLE) {
        for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
          if (stream) {
            stream->cig_state = CigState::INVALID;
            stream->cis_state = CisState::INVALID;
            stream->cis_pending_cmd = CisPendingCmd::NONE;
          }
        }
        CheckAndUpdateStoppedState();
      }
    } break;

    case BAP_TIME_OUT_EVT: {
      tracker_.OnTimeout(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

bool StreamTracker::StateDisconnecting::TerminateGattConnection() {
  bool disc_issued = false;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamContext *> *all_contexts = contexts->GetAllContexts();
    bool any_context_active = false;
    std::vector<int> ids = { StreamTracker::kStateIdle };
    std::vector<UcastAudioStream *> idle_streams =
                                 audio_strms->GetStreamsByStates(
                                 ids, ASE_DIRECTION_SINK | ASE_DIRECTION_SRC);

    LOG(WARNING) << __func__ << ": Total Streams size: " << audio_strms->size()
                             << ", Idle Streams size: " << idle_streams.size();

    if(!gatt_pending_data) continue;

    // check if any of the contexts stream state is connected
    for (auto it = all_contexts->begin(); it != all_contexts->end(); it++) {
      if((*it)->stream_state != StreamState::DISCONNECTING &&
         (*it)->stream_state != StreamState::DISCONNECTED) {
        LOG(INFO) << __func__ << ": Other contexts are active,not to disc Gatt";
        any_context_active = true;
        break;
      }
    }
    if(!any_context_active &&
       (!audio_strms->size() || audio_strms->size() == idle_streams.size())) {

      // check if gatt connection can be tear down for ascs & pacs clients
      // all streams are in idle state
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(*itr);
      PacsClient *pacs_client = strm_mgr_->GetPacsClient(*itr);
      uint16_t pacs_client_id = strm_mgr_->GetPacsClientId(*itr);

      ConnectionState pacs_state = strm_mgr_->GetPacsState(*itr);
      if((pacs_state == ConnectionState::CONNECTED &&
          gatt_pending_data->pacs_pending_cmd == GattPendingCmd::NONE) ||
         (gatt_pending_data->pacs_pending_cmd ==
                                  GattPendingCmd::GATT_CONN_PENDING)) {
        if(pacs_client) {
          LOG(WARNING) << __func__ << ": Issue PACS server disconnect ";
          pacs_client->Disconnect(pacs_client_id, *itr);
        }
        gatt_pending_data->pacs_pending_cmd = GattPendingCmd::GATT_DISC_PENDING;
        disc_issued = true;
      }

      GattState ascs_state = strm_mgr_->GetAscsState(*itr);
      if((ascs_state == GattState::CONNECTED &&
          gatt_pending_data->ascs_pending_cmd == GattPendingCmd::NONE) ||
         (gatt_pending_data->ascs_pending_cmd ==
                                  GattPendingCmd::GATT_CONN_PENDING)) {
        if(ascs_client) {
          LOG(WARNING) << __func__ << ": Issue ASCS server disconnect ";
          ascs_client->Disconnect(ASCS_CLIENT_ID, *itr);
        }
        gatt_pending_data->ascs_pending_cmd = GattPendingCmd::GATT_DISC_PENDING;
        disc_issued = true;
      }
    }
  }
  return disc_issued;
}

void StreamTracker::StateDisconnecting::OnEnter() {
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  // check the previous state i.e connecting, starting, stopping
  // or reconfiguring
  uint8_t num_ases = 0;
  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;

    std::vector<StreamType> *disc_streams = tracker_.GetStreams();
    LOG(WARNING) << __func__ << ": Disconection Streams Size: "
                              << disc_streams->size();

    StreamControlType control_type = tracker_.GetControlType();

    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));

    if(control_type != StreamControlType::Disconnect) {
      return;
    }

    for (auto it = disc_streams->begin(); it != disc_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      state.stream_type = *it;
      state.stream_state = StreamState::DISCONNECTING;
      strms.push_back(state);
      StreamContext *context = contexts->FindOrAddByType(*it);
      context->stream_state = StreamState::DISCONNECTING;
      if(context->connection_state == IntConnectState::ASCS_DISCOVERED) {
        for (auto id = context->stream_ids.begin();
                id != context->stream_ids.end(); id++) {
          bool can_be_disconnected = tracker_.
                         StreamCanbeDisconnected(*itr, context, id->ase_id);
          if(can_be_disconnected) {
            int_strm_trackers_[*itr].FindOrAddBytrackerType(*it,
                               id->ase_id, id->cig_id,
                               id->cis_id,
                               context->codec_config, context->qos_config);
          }
        }
      }
      num_ases += context->stream_ids.size();
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }
  uint64_t tout = num_ases *
                    (static_cast<uint64_t>(TimeoutVal::DisconnectingTimeout));
  if(!tout ||tout > static_cast<uint64_t>(MaxTimeoutVal::DisconnectingTimeout)) {
    tout = static_cast<uint64_t>(MaxTimeoutVal::DisconnectingTimeout);
  }

  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateDisconnectingTimer",
                                          &timeout, reason, tout);
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": StateDisconnecting: Alarm not allocated.";
    return;
  }

  bool gatt_disc_pending = TerminateGattConnection();
  // check if there are no internal stream trackers, then update to
  // upper layer as completely disconnected
  if(!int_strm_trackers_.size() && !gatt_disc_pending) {
    tracker_.TransitionTo(StreamTracker::kStateIdle);
  }
}

void StreamTracker::StateDisconnecting::ContinueDisconnection
                               (RawAddress bd_addr, UcastAudioStream *stream) {

 tACL_CONN* acl_up = btm_bda_to_acl(bd_addr, BT_TRANSPORT_LE);
 if (!acl_up) {
   if (stream->cis_pending_cmd != CisPendingCmd::NONE)
     LOG(WARNING) << __func__ << ": acl down clear cis pending command";
   stream->cis_pending_cmd = CisPendingCmd::NONE;
 }
  // check ase state, return if state is not releasing or
  if(stream->ase_state != ascs::ASE_STATE_IDLE &&
     stream->ase_state != ascs::ASE_STATE_CODEC_CONFIGURED &&
     stream->ase_state != ascs::ASE_STATE_RELEASING) {
    LOG(WARNING) << __func__ << ": Return as ASE is not moved to Right state";
    return;
  }

  CisInterface *cis_intf = strm_mgr_->GetCisInterface(bd_addr);

  // check if there is no pending CIS command then issue relevant
  // CIS command based on CIS state
  if(stream->cis_pending_cmd != CisPendingCmd::NONE) {
    LOG(INFO) << __func__ << ": cis_pending_cmd is not NONE ";
    return;
  }

  if(!cis_intf) {
    LOG(INFO) << __func__ << ": CIS interface not available ";
    return;
  }

  if(stream->cis_state == CisState::ESTABLISHED) {
    LOG(WARNING) << __func__ << ": Going For CIS disconnect ";
    IsoHciStatus status;
    if (tracker_.getMode()) {
      LOG(WARNING) << __func__  << ": AAR4 2 CISes Disconnect ";
      status = cis_intf->DisconnectCis(stream->cig_id,
                                       stream->cis_id,
                                       cis::DIR_FROM_AIR);
      status = cis_intf->DisconnectCis(stream->cig_id,
                                       stream->cis_id,
                                       cis::DIR_TO_AIR);
    } else {
      status = cis_intf->DisconnectCis(stream->cig_id,
                                       stream->cis_id,
                                       stream->direction);
    }
    if(status == IsoHciStatus::ISO_HCI_SUCCESS) {
      stream->cis_pending_cmd = CisPendingCmd::NONE;
      stream->cis_state = CisState::READY;
    } else if(status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
      stream->cis_pending_cmd = CisPendingCmd::CIS_DESTROY_ISSUED;
    } else {
      LOG(WARNING) << __func__ << ": CIS Disconnect Failed";
    }
  } else {
    if (!acl_up)
      stream->cis_state = CisState::READY;
  }

  if(stream->cis_state == CisState::READY) {
    if(stream->cig_state == CigState::CREATED &&
       stream->cis_pending_cmd == CisPendingCmd::NONE) {
      LOG(WARNING) << __func__ << ": Going For CIG Removal";
      IsoHciStatus status = cis_intf->RemoveCig(bd_addr, stream->cig_id);
      if( status == IsoHciStatus::ISO_HCI_SUCCESS) {
        stream->cig_state = CigState::INVALID;
        stream->cis_state = CisState::INVALID;
      } else if (status == IsoHciStatus::ISO_HCI_IN_PROGRESS) {
        stream->cis_pending_cmd = CisPendingCmd::CIG_REMOVE_ISSUED;
      } else {
        LOG(WARNING) << __func__ << ": CIG removal Failed";
      }
    }
  }
}

bool StreamTracker::StateDisconnecting::CheckAndUpdateDisconnectedState() {
  bool pending_cmds = false;
  uint8_t num_trackers = 0;
  uint8_t num_strms_disconnected = 0;

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    std::vector<IntStrmTracker *> *all_trackers =
                        int_strm_trackers_[*itr].GetTrackerList();
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    // check if any pending commands are present
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && (stream->cis_pending_cmd != CisPendingCmd::NONE ||
                    stream->ase_pending_cmd != AscsPendingCmd::NONE)) {
        pending_cmds = true;
        break;
      }
    }
    if(pending_cmds) break;
  }

  if(pending_cmds) {
    LOG(WARNING) << __func__ << ": Pending ASCS/CIS cmds present ";
    return false;
  }

  TerminateGattConnection();

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
  // check it needs to wait for ASCS & PACS disconnection also
    GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData(*itr);
    if(gatt_pending_data &&
       (gatt_pending_data->ascs_pending_cmd != GattPendingCmd::NONE ||
        gatt_pending_data->pacs_pending_cmd != GattPendingCmd::NONE)) {
      pending_cmds = true;
      break;
    }
    if(pending_cmds) break;
  }

  if(pending_cmds) {
    LOG(WARNING) << __func__ << ": Pending Gatt disc present ";
    return false;
  }

  // check for all trackers moved to idle and
  // CIG state is idle if so update it as streams are disconnected
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    std::vector<IntStrmTracker *> *all_trackers =
                                 int_strm_trackers_[*itr].GetTrackerList();
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if(stream && (stream->ase_state == ascs::ASE_STATE_IDLE ||
         stream->ase_state == ascs::ASE_STATE_CODEC_CONFIGURED) &&
         (stream->cig_state == CigState::IDLE ||
         stream->cig_state == CigState::INVALID) &&
         (stream->cis_state == CisState::READY ||
          stream->cis_state == CisState::INVALID)) {
        num_strms_disconnected++;
      }
      num_trackers++;
    }
  }

  if(num_trackers != num_strms_disconnected) {
    LOG(WARNING) << __func__ << ": Not disconnected for all streams";
    return false;
  } else {
    LOG(ERROR) << __func__ << ": Disconnected for all streams";
  }

  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
    std::vector<IntStrmTracker *> *all_trackers =
                       int_strm_trackers_[*itr].GetTrackerList();
    for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
      UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
      if (stream) {
        stream->overall_state = StreamTracker::kStateIdle;
      }
    }
  }

  // update the state to idle
  tracker_.TransitionTo(StreamTracker::kStateIdle);
  return true;
}

void StreamTracker::StateDisconnecting::OnExit() {
  tracker_.ClearTimer(state_transition_timer, "StateDisconnectingTimer");
}

bool StreamTracker::StateDisconnecting::ProcessEvent(uint32_t event,
                                                     void* p_data) {
  for (auto it = bd_addrs_.begin(); it != bd_addrs_.end(); it++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*it);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  switch (event) {
    case PACS_CONNECTION_STATE_EVT: {
      PacsConnectionState *pacs_state =  (PacsConnectionState *) p_data;
      GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData(
                                                      pacs_state->bd_addr);
      if(pacs_state->state == ConnectionState::DISCONNECTED &&
         gatt_pending_data) {
        gatt_pending_data->pacs_pending_cmd = GattPendingCmd::NONE;
      }
      CheckAndUpdateDisconnectedState();
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      AscsConnectionState *ascs_state =  (AscsConnectionState *) p_data;
      GattPendingData *gatt_pending_data = strm_mgr_->GetGattPendingData(
                                                      ascs_state->bd_addr);
      if(ascs_state->state == GattState::DISCONNECTED &&
         gatt_pending_data) {
        // make all streams ASE state to idle so that further processing
        // can happen
        gatt_pending_data->ascs_pending_cmd = GattPendingCmd::NONE;
        UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                                    ascs_state->bd_addr);
        std::vector<UcastAudioStream *> *strms_list =
                                             audio_strms->GetAllStreams();

        for (auto it = strms_list->begin(); it != strms_list->end(); it++) {

          (*it)->ase_state = ascs::ASE_STATE_IDLE;
          (*it)->ase_pending_cmd = AscsPendingCmd::NONE;
          (*it)->overall_state = StreamTracker::kStateIdle;
          ContinueDisconnection(ascs_state->bd_addr, *it);
        }
      }
      CheckAndUpdateDisconnectedState();
    } break;

    case ASCS_ASE_STATE_EVT: {  // to handle remote driven operations

      // check for state releasing
      // based on prev state do accordingly
      AscsState *ascs =  ((AscsState *) p_data);

      uint8_t ase_id = ascs->ase_params.ase_id;

      // check if current stream tracker is interested in this ASE update
      if(int_strm_trackers_[ascs->bd_addr].FindByAseId(ase_id)
                                 == nullptr) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }

      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);
      UcastAudioStream *stream = audio_strms->FindByAseId(ase_id);

      if(stream == nullptr) {
        break;
      } else {
        stream->ase_state = ascs->ase_params.ase_state;
        stream->ase_params = ascs->ase_params;
      }

      if(ascs->ase_params.ase_state == ascs::ASE_STATE_RELEASING) {
        // find out the stream for the given ase id
        LOG(WARNING) << __func__ << ": ASE Id: " << loghex(ase_id);
        stream->ase_pending_cmd = AscsPendingCmd::NONE;
        ContinueDisconnection(ascs->bd_addr, stream);

      } else if( ascs->ase_params.ase_state ==
                                    ascs::ASE_STATE_CODEC_CONFIGURED) {
        // check if this is a codec config notification due to codec config
        // issued during connecting state
        if((tracker_.PreviousStateId() == StreamTracker::kStateConnecting ||
            tracker_.PreviousStateId() == StreamTracker::kStateReconfiguring) &&
           stream->ase_pending_cmd == AscsPendingCmd::CODEC_CONFIG_ISSUED &&
           stream->ase_state == ascs::ASE_STATE_CODEC_CONFIGURED) {
          // mark int conn state as codec configured and issue release command
          std::vector<AseReleaseOp> ase_ops;
          AscsClient *ascs_client = strm_mgr_->GetAscsClient(ascs->bd_addr);
          AseReleaseOp release_op = {
                                       .ase_id = stream->ase_id
                                    };
          ase_ops.push_back(release_op);
          stream->ase_pending_cmd = AscsPendingCmd::RELEASE_ISSUED;
          ascs_client->Release(ASCS_CLIENT_ID, ascs->bd_addr, ase_ops);
          break; // break the switch case
        } else {
          stream->ase_pending_cmd = AscsPendingCmd::NONE;
          stream->overall_state = StreamTracker::kStateIdle;
          ContinueDisconnection(ascs->bd_addr, stream);
          CheckAndUpdateDisconnectedState();
        }
      } else if(ascs->ase_params.ase_state == ascs::ASE_STATE_IDLE) {
        // check for all trackers moved to idle and
        // CIG state is idle if so update it as streams are disconnected
        stream->ase_pending_cmd = AscsPendingCmd::NONE;
        stream->overall_state = StreamTracker::kStateIdle;
        ContinueDisconnection(ascs->bd_addr, stream);
        CheckAndUpdateDisconnectedState();
      }
    } break;

    case ASCS_ASE_OP_FAILED_EVT: {
      AscsOpFailed *ascs_op =  ((AscsOpFailed *) p_data);
      std::vector<ascs::AseOpStatus> *ase_list = &ascs_op->ase_list;
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                                  ascs_op->bd_addr);

      if(ascs_op->ase_op_id == ascs::AseOpId::RELEASE) {
        // treat it like internal failure
        for (auto i = ase_list->begin(); i != ase_list->end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((i)->ase_id);
          if(stream) {
            stream->ase_state = ascs::ASE_STATE_IDLE;
            stream->ase_pending_cmd = AscsPendingCmd::NONE;
            stream->overall_state = StreamTracker::kStateIdle;
            ContinueDisconnection(ascs_op->bd_addr, stream);
          }
        }
        CheckAndUpdateDisconnectedState();
      }
    } break;

    case CIS_GROUP_STATE_EVT: {
      // check if the associated CIG state is created
      // if so go for QOS config operation
      CisGroupState *data = ((CisGroupState *) p_data);

      // check if current stream tracker is interested in this CIG update
      std::vector<IntStrmTracker *> int_trackers =
                 int_strm_trackers_[data->bd_addr].FindByCigId(data->cig_id);
      if(int_trackers.empty()) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }

      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
      if(data->state == CigState::CREATED) {
        for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
          if (stream) {
            stream->cis_pending_cmd = CisPendingCmd::NONE;
            stream->cig_state = data->state;
            stream->cis_state = CisState::READY;
            // check if this is a CIG created event due to CIG create
            // issued during starting state
            ContinueDisconnection(data->bd_addr, stream);
          }
        }
        CheckAndUpdateDisconnectedState();

      } else if(data->state == CigState::IDLE) {
        for (auto i = int_trackers.begin(); i != int_trackers.end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
          if (!stream) {
            LOG(ERROR) << __func__ << ": stream is null";
            continue;
          }
          stream->cig_state = CigState::INVALID;
          stream->cis_state = CisState::INVALID;
          stream->cis_pending_cmd = CisPendingCmd::NONE;
        }
        CheckAndUpdateDisconnectedState();
      }
    } break;

    case CIS_STATE_EVT: {
      CisStreamState *data = (CisStreamState *) p_data;
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(data->bd_addr);
      // check if current stream tracker is interested in this CIG update
      std::vector<IntStrmTracker *> int_trackers =
                   int_strm_trackers_[data->bd_addr].
                   FindByCisId(data->cig_id, data->cis_id);
      if(int_trackers.empty()) {
        LOG(INFO) << __func__ << ": Not intended for this tracker";
        break;
      }

      // go for CIS destroy or CIG removal based on CIS state
      if(data->state == CisState::ESTABLISHED) {
        for(auto it = directions.begin(); it != directions.end(); ++it) {
          if(data->direction & *it) {
            UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                       (data->cig_id, data->cis_id, *it);
            if(stream) {
              stream->cis_state = data->state;
              stream->cis_pending_cmd = CisPendingCmd::NONE;
              ContinueDisconnection(data->bd_addr, stream);
            }
          }
        }
      } else if(data->state == CisState::READY) {
        for(auto it = directions.begin(); it != directions.end(); ++it) {
          if(data->direction & *it) {
            UcastAudioStream *stream = audio_strms->FindByCisIdAndDir
                                       (data->cig_id, data->cis_id, *it);
            if(stream) {
              stream->cis_state = data->state;
              stream->cis_pending_cmd = CisPendingCmd::NONE;
              ContinueDisconnection(data->bd_addr, stream);
            }
          }
        }
        CheckAndUpdateDisconnectedState();
      }
    } break;

    case BAP_TIME_OUT_EVT: {
      BapTimeout* timeout = static_cast <BapTimeout *> (p_data);
      if (timeout == nullptr) {
        LOG(INFO) << __func__ << ": timeout data null, return ";
        break;
      }

      int stream_tracker_id = timeout->transition_state;
      LOG(INFO) << __func__ << ": stream_tracker_ID: " << stream_tracker_id
                << ", timeout reason: " << static_cast<int>(timeout->reason);

      if (timeout->reason == TimeoutReason::STATE_TRANSITION) {
        for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
          std::vector<IntStrmTracker *> *int_trackers =
                      int_strm_trackers_[*itr].GetTrackerList();
          UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(*itr);
          for (auto i = int_trackers->begin(); i != int_trackers->end();i++) {
            UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
            if(stream) {
              stream->ase_state = ascs::ASE_STATE_IDLE;
              stream->ase_pending_cmd = AscsPendingCmd::NONE;
              stream->overall_state = StreamTracker::kStateIdle;
              ContinueDisconnection(*itr, stream);
            }
          }
          CheckAndUpdateDisconnectedState();
        }
      }
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

void StreamTracker::StateReconfiguring::OnEnter() {
  uint8_t num_ases = 0;
  LOG(INFO) << __func__ << ": StreamTracker State: " << GetState();
  bd_addrs_ = tracker_.bd_addrs_;
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
    UcastClientCallbacks* callbacks = strm_mgr_->GetUclientCbacks(*itr);
    StreamContexts *contexts = strm_mgr_->GetStreamContexts(*itr);
    std::vector<StreamStateInfo> strms;
    std::vector<StreamReconfig> *reconfig_streams = tracker_.GetReconfStreams();

    LOG(WARNING) << __func__ << ": Reconfig Streams Size: "
                              << reconfig_streams->size();

    StreamControlType control_type = tracker_.GetControlType();

    IntStrmTrackers int_strm_trackers;
    int_strm_trackers_.insert(std::make_pair(*itr, int_strm_trackers));

    if(control_type != StreamControlType::Reconfig) return;

    for (auto it = reconfig_streams->begin();
                         it != reconfig_streams->end(); it++) {
      StreamStateInfo state;
      memset(&state, 0, sizeof(state));
      state.stream_type = it->stream_type;
      state.stream_state = StreamState::RECONFIGURING;
      strms.push_back(state);
      StreamContext *context = contexts->FindOrAddByType(it->stream_type);
      context->connection_state = IntConnectState::PACS_DISCOVERING;
      context->stream_state = StreamState::RECONFIGURING;
      num_ases += context->stream_ids.size();
    }
    if(callbacks) {
      callbacks->OnStreamState(*itr, strms);
    }
  }
  uint64_t tout = num_ases *
                    (static_cast<uint64_t>(TimeoutVal::ReconfiguringTimeout));
  if(!tout ||tout > static_cast<uint64_t>(MaxTimeoutVal::ReconfiguringTimeout)){
    tout = static_cast<uint64_t>(MaxTimeoutVal::ReconfiguringTimeout);
  }

  TimeoutReason reason = TimeoutReason::STATE_TRANSITION;
  state_transition_timer = tracker_.SetTimer("StateReconfiguringTimer",
            &timeout, reason, tout);
  if (state_transition_timer == nullptr) {
    LOG(ERROR) << __func__ << ": state_transition_timer: Alarm not allocated.";
    return;
  }
}

void StreamTracker::StateReconfiguring::OnExit() {
  tracker_.ClearTimer(state_transition_timer, "StateReconfiguringTimer");
}

bool StreamTracker::StateReconfiguring::ProcessEvent(uint32_t event,
                                                     void* p_data) {
  for (auto itr = bd_addrs_.begin(); itr != bd_addrs_.end(); itr++) {
    LOG(INFO) << __func__ << ": BD Addr = " << (*itr);
  }
  LOG(INFO) << __func__ << ": State = " << GetState()
                        << ", Event = " << tracker_.GetEventName(event);

  std::vector<StreamReconfig> *reconf_streams = tracker_.GetReconfStreams();
  uint8_t num_reconf_streams = 0;
  if(reconf_streams) {
     num_reconf_streams = reconf_streams->size();
  }
  LOG(INFO) << __func__ << ": num_reconf_streams = " << loghex(num_reconf_streams);

  switch (event) {
    case BAP_DISCONNECT_REQ_EVT: {
      tracker_.HandleDisconnect(p_data, StreamTracker::kStateReconfiguring);
    } break;

    case PACS_DISCOVERY_RES_EVT: {
      PacsDiscovery pacs_discovery_ =  *((PacsDiscovery *) p_data);
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(
                                         pacs_discovery_.bd_addr);
      AscsClient *ascs_client = strm_mgr_->GetAscsClient(
                                         pacs_discovery_.bd_addr);
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(
                                            pacs_discovery_.bd_addr);
      GattState ascs_state = strm_mgr_->GetAscsState(
                                         pacs_discovery_.bd_addr);
      uint8_t qos_reconfigs = 0;

      bool process_pacs_results = false;

      // check if this tracker already passed the pacs discovery stage
      for (auto it = reconf_streams->begin();
                        it != reconf_streams->end(); it++) {
        StreamContext *context = contexts->FindOrAddByType(it->stream_type);
        if (context->connection_state == IntConnectState::PACS_DISCOVERING) {
          context->connection_state = IntConnectState::ASCS_DISCOVERED;
          process_pacs_results = true;
        }
      }

      if(!process_pacs_results) break;

      // check the status
      if(pacs_discovery_.status) {
        // send the BAP callback as connected as discovery failed
        // during reconfiguring
        tracker_.TransitionTo(StreamTracker::kStateConnected);
        return false;
      }

      tracker_.UpdatePacsDiscovery(pacs_discovery_.bd_addr, pacs_discovery_);

      // check if supported audio contexts has required contexts
      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end();) {
        LOG(INFO) << __func__
                  << ": check if supported audio contexts has required contexts.";
        bool context_supported = false;
        StreamType stream = it->stream_type;
        if(stream.direction == ASE_DIRECTION_SINK) {
          if(stream.audio_context & pacs_discovery_.supported_contexts) {
            context_supported = true;
            LOG(INFO) << __func__ << ": Sink direction audio_context: "
                                  << stream.audio_context;
          }
          if(1 & pacs_discovery_.supported_contexts) {
            context_supported = true;
            LOG(INFO) << __func__ << ": Sink direction unspecified context: "
                                  << stream.audio_context;
          }
        } else if(stream.direction == ASE_DIRECTION_SRC) {
          if((static_cast<uint64_t>(stream.audio_context) << 16) &
               pacs_discovery_.supported_contexts) {
            context_supported = true;
            LOG(INFO) << __func__ << ": Source direction audio_context: "
                                  << stream.audio_context;
          }
          if(1 & pacs_discovery_.supported_contexts) {
            context_supported = true;
            LOG(INFO) << __func__ << ": Source direction unspecified context: "
                                  << stream.audio_context;
          }
        }

        if(context_supported) {
          it++;
        } else {
          it = reconf_streams->erase(it);
          // TODO to update the disconnected callback
        }
      }

      if(reconf_streams->empty()) {
        LOG(ERROR) << __func__ << ": No Matching Sup Contexts found";
        LOG(ERROR) << __func__ << ": Moving back to Connected state";
        tracker_.TransitionTo(StreamTracker::kStateConnected);
        break;
      }

      // check physical allocation for all reconfig requests
      uint8_t num_phy_attached = 0;
      uint8_t num_same_config_applied = 0;
      // if not present send the BAP callback as disconnected
      // compare the codec configs from upper layer to remote dev
      // sink or src PACS records/capabilities.
      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end(); it++) {
        LOG(INFO) << __func__ << ": Process to choose for best codec.";
        uint8_t index = tracker_.ChooseBestCodec(it->stream_type,
                                 &it->codec_qos_config_pair,
                                 &pacs_discovery_);
        if(index != 0XFF) {
          CodecQosConfig entry = it->codec_qos_config_pair.at(index);
          StreamContext *context = contexts->FindOrAddByType(
                                             it->stream_type);
          LOG(INFO) << __func__ << ": Checking Context attached state: "
                                << static_cast<uint16_t>(context->attached_state);
          if(context->attached_state == StreamAttachedState::PHYSICAL) {
            num_phy_attached++;
            // check if same config is already applied
            bap_print_codec_parameters(context->codec_config);
            bap_print_qos_parameters(context->req_qos_config);
            bap_print_codec_parameters(entry.codec_config);
            bap_print_qos_parameters(entry.qos_config);

            uint32_t sup_contexts = 0;
            btif_bap_get_supp_contexts(pacs_discovery_.bd_addr, &sup_contexts);
            LOG(WARNING) << __func__ << ": sup_contexts: " << sup_contexts;

            if(IsCodecConfigEqual(&context->codec_config, &entry.codec_config,
                                  sup_contexts, it->stream_type.direction) &&
               IsQosConfigEqual(&context->req_qos_config, &entry.qos_config)) {
              num_same_config_applied++;
            }
          }
        } else {
          LOG(ERROR) << __func__ << ": Matching Codec not found";
        }
      }

      if(reconf_streams->size() == num_phy_attached &&
         num_phy_attached == num_same_config_applied) {
        // update the state to connected
        LOG(INFO) << __func__ << ": Making state to Connected as Nothing to do";
        TransitionTo(StreamTracker::kStateConnected);
        break;
      }

      if(ascs_state != GattState::CONNECTED) {
        break;
      }

      for (auto it = reconf_streams->begin();
                           it != reconf_streams->end(); it++) {
        LOG(INFO) << __func__
                  << ": Process to choose for best codec, before attach convert.";
        uint8_t index = tracker_.ChooseBestCodec(it->stream_type,
                                 &it->codec_qos_config_pair,
                                 &pacs_discovery_);
        if(index != 0XFF) {
          CodecQosConfig entry = it->codec_qos_config_pair.at(index);
          StreamContext *context = contexts->FindOrAddByType(
                                             it->stream_type);
          CodecConfig codec_config = entry.codec_config;
          QosConfig qos_config = entry.qos_config;
          bap_print_codec_parameters(codec_config);
          bap_print_qos_parameters(qos_config);

          LOG(INFO) << __func__ << ": Context attached state: "
                                << static_cast<uint16_t>(context->attached_state);
          if(context->attached_state == StreamAttachedState::VIRTUAL) {
            std::vector<StreamContext *> phy_attached_contexts;
            for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
              std::vector<StreamContext *> phy_attached_contexts;
              phy_attached_contexts = contexts->FindByAseAttachedState(
                                    id->ase_id, StreamAttachedState::PHYSICAL);
              for (auto context_id = phy_attached_contexts.begin();
                        context_id != phy_attached_contexts.end();
                        context_id++) {
                LOG(INFO) << __func__ << ": Attached state made virtual";
                (*context_id)->attached_state = StreamAttachedState::VIRTUAL;
              }
            }
            LOG(INFO) << __func__ << ": Attached state made virtual to phy";
            context->attached_state = StreamAttachedState::VIR_TO_PHY;
            context->codec_config = codec_config;
            context->req_qos_config = qos_config;
            context->req_alt_qos_configs = entry.alt_qos_configs;
          } else if (context->attached_state == StreamAttachedState::PHYSICAL) {
            LOG(INFO) << __func__ << ": Attached state is physical";
            context->codec_config = codec_config;
            context->req_qos_config = qos_config;
            context->req_alt_qos_configs = entry.alt_qos_configs;
          }

          uint8_t ascs_config_index = 0;
          for (auto id = context->stream_ids.begin();
                    id != context->stream_ids.end(); id++) {
            int_strm_trackers_[pacs_discovery_.bd_addr].
                        FindOrAddBytrackerType(it->stream_type,
                        id->ase_id,
                        qos_config.ascs_configs[ascs_config_index].cig_id,
                        qos_config.ascs_configs[ascs_config_index].cis_id,
                        codec_config,
                        qos_config);
            UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
            if (!stream) {
              LOG(ERROR) << __func__ << ": stream is null";
              continue;
            }
            stream->cig_id = id->cig_id =
                            qos_config.ascs_configs[ascs_config_index].cig_id;
            stream->cis_id = id->cis_id =
                            qos_config.ascs_configs[ascs_config_index].cis_id;
            stream->cig_state = CigState::INVALID;
            stream->cis_state = CisState::INVALID;
            stream->codec_config = codec_config;
            stream->req_qos_config = qos_config;
            stream->qos_config = qos_config;
            stream->audio_context = it->stream_type.audio_context;
            std::vector<CISConfig> cis_configs_temp = stream->qos_config.cis_configs;
            ascs_config_index++;
          }
        } else {
          LOG(ERROR) << __func__ << ": Matching Codec not found";
        }
      }
      for (auto it = reconf_streams->begin();
                it != reconf_streams->end(); it++) {
        if (it->reconf_type == StreamReconfigType::QOS_CONFIG) {
          qos_reconfigs++;
        }
      }

      LOG(INFO) << __func__ << ": qos_reconfigs: " << qos_reconfigs;

      if(qos_reconfigs == num_reconf_streams) {
        // now create the group
        std::vector<IntStrmTracker *> *all_trackers =
                   int_strm_trackers_[pacs_discovery_.bd_addr].GetTrackerList();
        // check for all streams together so that final group params
        // will be decided.
        for (auto i = all_trackers->begin(); i != all_trackers->end();i++) {
          UcastAudioStream *stream = audio_strms->FindByAseId((*i)->ase_id);
          if (!stream) {
            LOG(ERROR) << __func__ << ": stream is null";
            continue;
          }
          tracker_.ChooseBestQos(pacs_discovery_.bd_addr,
                                 &stream->req_qos_config,
                                 &stream->pref_qos_params,
                                 &stream->qos_config,
                                 StreamTracker::kStateReconfiguring,
                                 stream->direction);
        }

        tracker_.ChooseBestPD();

        tracker_.CheckAndSendQosConfig(pacs_discovery_.bd_addr,
                                 &int_strm_trackers_[pacs_discovery_.bd_addr]);
      } else {
        // now send the ASCS codec config
        std::vector<AseCodecConfigOp> ase_ops;
        for (auto it = reconf_streams->begin();
                             it != reconf_streams->end(); it++) {
          if(it->reconf_type == StreamReconfigType::CODEC_CONFIG) {
            StreamContext *context = contexts->FindOrAddByType(it->stream_type);
            for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
              UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
              if (stream) {
                tracker_.PrepareCodecConfigPayload(&ase_ops, stream);
              }
            }
          }
        }

        if(!ase_ops.empty()) {
          LOG(WARNING) << __func__ << ": Going For ASCS CodecConfig op";
          ascs_client->CodecConfig(ASCS_CLIENT_ID, pacs_discovery_.bd_addr,
                                   ase_ops);
        }

        // update the states to connecting or other internal states
        for (auto it = reconf_streams->begin();
                             it != reconf_streams->end(); it++) {
          if(it->reconf_type == StreamReconfigType::CODEC_CONFIG) {
            StreamContext *context = contexts->FindOrAddByType(it->stream_type);
            for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
              UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
              if (stream) {
                stream->ase_pending_cmd = AscsPendingCmd::CODEC_CONFIG_ISSUED;
                stream->overall_state = StreamTracker::kStateReconfiguring;
              }
            }
          } else {
            StreamContext *context = contexts->FindOrAddByType(it->stream_type);
            for (auto id = context->stream_ids.begin();
                      id != context->stream_ids.end(); id++) {
              UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
              if (stream) {
                stream->overall_state = StreamTracker::kStateReconfiguring;
              }
            }
          }
        }
      }
    } break;

    case ASCS_ASE_STATE_EVT: {
      LOG(WARNING) << __func__ << ": ASCS_ASE_STATE_EVT in StateReconfiguring";
      AscsState *ascs =  ((AscsState *) p_data);
      std::vector<StreamReconfig> *reconf_streams = tracker_.GetReconfStreams();
      StreamContexts *contexts = strm_mgr_->GetStreamContexts(ascs->bd_addr);
      UcastAudioStreams *audio_strms = strm_mgr_->GetAudioStreams(ascs->bd_addr);
      uint8_t ase_id = ascs->ase_params.ase_id;

      LOG(WARNING) << __func__ << ": reconfig streams size: "
                               << reconf_streams->size();
      LOG(WARNING) << __func__ << ": Ase id: "
                               << loghex(ase_id);

      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id) == nullptr) {
        LOG(WARNING) << __func__ << ": Internal stream tracker is null, create.";
        for (auto it = reconf_streams->begin();
               it != reconf_streams->end(); it++) {
          StreamContext *context = contexts->FindOrAddByType(it->stream_type);
          for (auto id = context->stream_ids.begin();
                       id != context->stream_ids.end(); id++) {
            UcastAudioStream *stream = audio_strms->FindByAseId(id->ase_id);
            if (!stream) {
              LOG(ERROR) << __func__ << ": stream is null";
              continue;
            }
            LOG(WARNING) << __func__ << ": Creating Internal stream tracker";
            int_strm_trackers_[ascs->bd_addr].
            FindOrAddBytrackerType(it->stream_type,
                                   id->ase_id,
                                   stream->cig_id,
                                   stream->cis_id,
                                   stream->codec_config,
                                   stream->qos_config);
            stream->cig_state = CigState::INVALID;
            stream->cis_state = CisState::INVALID;
            stream->audio_context = it->stream_type.audio_context;
          }
        }
      }

      tracker_.HandleAseStateEvent(p_data, StreamControlType::Reconfig,
                                   &int_strm_trackers_);
    } break;

    case ASCS_ASE_OP_FAILED_EVT: {
      tracker_.HandleAseOpFailedEvent(p_data);
    } break;

    case PACS_CONNECTION_STATE_EVT: {
      tracker_.HandlePacsConnectionEvent(p_data);
    } break;

    case ASCS_CONNECTION_STATE_EVT: {
      tracker_.HandleAscsConnectionEvent(p_data);
    } break;

    case BAP_TIME_OUT_EVT: {
      tracker_.OnTimeout(p_data);
    } break;

    case ASE_INT_TRACKER_CHECK_REQ_EVT: {
      AscsState *ascs =  reinterpret_cast<AscsState *> (p_data);
      uint8_t ase_id = ascs->ase_params.ase_id;
      if ((int_strm_trackers_)[ascs->bd_addr].FindByAseId(ase_id)) {
        LOG(WARNING) << __func__ << ": ASE tracking tracker exist. ";
        return true;
      } else {
        LOG(WARNING) << __func__ << ": ASE tracking tracker not exist. ";
        return false;
      }
    } break;

    default:
      LOG(WARNING) << __func__ << ": Un-handled event: "
                               << tracker_.GetEventName(event);
      break;
  }
  return true;
}

}  // namespace ucast
}  // namespace bap
}  // namespace bluetooth
