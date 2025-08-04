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

/******************************************************************************
 *
 *  Copyright (C) 2014 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#define LOG_TAG "bt_btif_bap_config"

#include "btif_bap_config.h"
#include <base/strings/string_split.h>

#include <base/logging.h>
#include <ctype.h>
#include <openssl/rand.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <string>

#include <mutex>

#include "bt_types.h"
#include "btcore/include/module.h"
#include "btif_api.h"
#include "btif_common.h"
#include "btif_util.h"
#include "osi/include/alarm.h"
#include "osi/include/allocator.h"
#include "osi/include/compat.h"
#include "osi/include/config.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"
#include "osi/include/properties.h"
#include "btif_bap_codec_utils.h"

#define BT_CONFIG_SOURCE_TAG_NUM 1010001

#define INFO_SECTION "Info"
#define FILE_TIMESTAMP "TimeCreated"
#define FILE_SOURCE "FileSource"
#define TIME_STRING_LENGTH sizeof("YYYY-MM-DD HH:MM:SS")
#define INDEX_FREE      (0x00)
#define INDEX_OCCUPIED  (0x01)
#define MAX_INDEX       (255)
#define MAX_INDEX_LEN   (0x04)
#define MAX_SECTION_LEN  (255)

using bluetooth::bap::pacs::CodecSampleRate;

static const char* TIME_STRING_FORMAT = "%Y-%m-%d %H:%M:%S";

// TODO(armansito): Find a better way than searching by a hardcoded path.
#if defined(OS_GENERIC)
static const char* CONFIG_FILE_PATH = "bap_config.conf";
static const char* CONFIG_BACKUP_PATH = "bap_config.bak";
#else   // !defined(OS_GENERIC)
static const char* CONFIG_FILE_PATH = "/data/misc/bluedroid/bap_config.conf";
static const char* CONFIG_BACKUP_PATH = "/data/misc/bluedroid/bap_config.bak";
#endif  // defined(OS_GENERIC)
static const period_ms_t CONFIG_SETTLE_PERIOD_MS = 3000;

static void timer_config_save_cb(void* data);
static void btif_bap_config_write(uint16_t event, char* p_param);
static bool is_factory_reset(void);
static void delete_config_files(void);
static void btif_bap_config_remove_restricted(config_t& config);
static std::unique_ptr<config_t> btif_bap_config_open(const char* filename);

static enum ConfigSource {
  NOT_LOADED,
  ORIGINAL,
  BACKUP,
  NEW_FILE,
  RESET
} btif_bap_config_source = NOT_LOADED;

//static int btif_bap_config_devices_loaded = -1;
static char btif_bap_config_time_created[TIME_STRING_LENGTH];

static std::unique_ptr<config_t> config;
static std::recursive_mutex config_lock;  // protects operations on |config|.
static alarm_t* config_timer;

#define BAP_DIRECTION_KEY              "Direction"
#define BAP_CODEC_TYPE_KEY             "CodecType"

#define BAP_RECORD_TYPE_KEY            "RecordType"
#define BAP_RECORD_TYPE_CAPA            "Capability"
#define BAP_RECORD_TYPE_CONF           "Configuration"

#define BAP_SAMP_FREQS_KEY             "SamplingRate"
#define BAP_CONTEXT_TYPE_KEY           "ContextType"

#define BAP_SUPP_FRM_DURATIONS_KEY     "SupFrameDurations"
#define BAP_SUP_MIN_OCTS_PER_FRAME_KEY "SupMinOctsPerFrame"
#define BAP_SUP_MAX_OCTS_PER_FRAME_KEY "SupMaxOctsPerFrame"
#define BAP_MAX_SUP_CODEC_FRAMES_PER_SDU "SupMaxFramesPerSDU"
#define BAP_LC3Q_SUP_KEY               "LC3QSupport"
#define BAP_LC3Q_VER_KEY               "LC3QVersion"

#define BAP_AAR3_CODEC_VER             "AAR3CodecVersionNumber"
#define BAP_AAR3_MIN_SUPP_FRAME_DUR    "AAR3MinSuppFrameDur"
#define BAP_AAR3_FEATURE_MAP           "AAR3FeatureMap"

#define BAP_AAR4_VS_METADATA           "AAR4VSMetadata"
#define BAP_AAR4_CODEC_ENCODER_VER_NUM "AAR4CodecEncoderVersionNumber"
#define BAP_AAR4_CODEC_DECODER_VER_NUM "AAR4CodecDecoderVersionNumber"
#define BAP_AAR4_MIN_SUPP_FRAME_DUR    "AAR4MinSuppFrameDur"
#define BAP_AAR4_FEATURE_MAP           "AAR4FeatureMap"

#define BAP_CONF_FRAME_DUR_KEY         "ConfiguredFrameDur"
#define BAP_CONF_OCTS_PER_FRAME_KEY    "ConfiguredOctsPerFrame"
#define BAP_LC3_FRAMES_PER_SDU_KEY     "Lc3FramesPerSDU"
#define BAP_CHNL_ALLOCATION_KEY        "ChannelAllocation"

#define BAP_SRC_LOCATIONS_KEY          "SrcLocation"
#define BAP_SINK_LOCATIONS_KEY         "SinkLocation"
#define BAP_SUP_AUDIO_CONTEXTS_KEY     "SupAudioContexts"
#define BAP_NUM_SINK_KEY               "NumSinkASEs"
#define BAP_NUM_SRC_KEY                "NumSrcASEs"
#define BAP_SINK_CHANNEL_COUNT_KEY     "SinkChannelCount"
#define BAP_SRC_CHANNEL_COUNT_KEY      "SrcChannelCount"
#define BAP_SINK_DEVICE_TYPE_KEY       "SinkDeviceType"
#define BAP_SRC_DEVICE_TYPE_KEY        "SrcDeviceType"

// Module lifecycle functions
static future_t* init(void) {
  std::unique_lock<std::recursive_mutex> lock(config_lock);

  if (is_factory_reset()) delete_config_files();

  std::string file_source;
  std::string empty = "";
  config = btif_bap_config_open(CONFIG_FILE_PATH);
  btif_bap_config_source = ORIGINAL;
  if (!config) {
    LOG_WARN(LOG_TAG, "%s unable to load config file: %s; using backup.",
             __func__, CONFIG_FILE_PATH);
    config = btif_bap_config_open(CONFIG_BACKUP_PATH);
    btif_bap_config_source = BACKUP;
    file_source = "Backup";
  }

  if (!config) {
    LOG_ERROR(LOG_TAG,
              "%s unable to transcode legacy file; creating empty config.",
              __func__);
    config = config_new_empty();
    btif_bap_config_source = NEW_FILE;
    file_source = "Empty";
  }

  if (!config) {
    LOG_ERROR(LOG_TAG, "%s unable to allocate a config object.", __func__);
    goto error;
  }

  if (!file_source.empty())
    config_set_string(config.get(), INFO_SECTION, FILE_SOURCE, file_source.c_str());

  // Cleanup temporary pairings if we have left guest mode
  if (!is_restricted_mode()) btif_bap_config_remove_restricted(*config);

  // Read or set config file creation timestamp
  const std::string* time_str;

  time_str = config_get_string(*config, INFO_SECTION, FILE_TIMESTAMP, &empty);
  if (time_str != NULL) {
    strlcpy(btif_bap_config_time_created, time_str->c_str(), TIME_STRING_LENGTH);
  } else {
    time_t current_time = time(NULL);
    struct tm* time_created = localtime(&current_time);
    if (time_created) {
      if (strftime(btif_bap_config_time_created, TIME_STRING_LENGTH,
                   TIME_STRING_FORMAT, time_created)) {
        config_set_string(config.get(), INFO_SECTION, FILE_TIMESTAMP,
                          btif_bap_config_time_created);
      }
    }
  }
  // TODO(sharvil): use a non-wake alarm for this once we have
  // API support for it. There's no need to wake the system to
  // write back to disk.
  config_timer = alarm_new("btif_bap.config");
  if (!config_timer) {
    LOG_ERROR(LOG_TAG, "%s unable to create alarm.", __func__);
    goto error;
  }

  LOG_EVENT_INT(BT_CONFIG_SOURCE_TAG_NUM, btif_bap_config_source);

  return future_new_immediate(FUTURE_SUCCESS);

error:
  alarm_free(config_timer);
  if (config != NULL)
    config.reset();
  config_timer = NULL;
  btif_bap_config_source = NOT_LOADED;
  return future_new_immediate(FUTURE_FAIL);
}

static std::unique_ptr<config_t> btif_bap_config_open(const char* filename) {
  std::unique_ptr<config_t> config = config_new(filename);
  if (!config) return NULL;

  return config;
}

static void btif_bap_config_save(void) {
  CHECK(config != NULL);
  CHECK(config_timer != NULL);

  if (config_timer == NULL) {
    LOG(WARNING) << __func__ << "config_timer is null";
    return;
  }
  alarm_set(config_timer, CONFIG_SETTLE_PERIOD_MS, timer_config_save_cb, NULL);
}

static void btif_bap_config_flush(void) {
  CHECK(config != NULL);
  CHECK(config_timer != NULL);

  alarm_cancel(config_timer);
  btif_bap_config_write(0, NULL);
}

bool btif_bap_config_clear(void) {
  CHECK(config != NULL);
  CHECK(config_timer != NULL);

  alarm_cancel(config_timer);

  std::unique_lock<std::recursive_mutex> lock(config_lock);
  if (config != NULL)
    config.reset();

  config = config_new_empty();
  if (config == NULL) return false;

  bool ret = config_save(*config, CONFIG_FILE_PATH);
  btif_bap_config_source = RESET;
  return ret;
}

static future_t* shut_down(void) {
  btif_bap_config_flush();
  return future_new_immediate(FUTURE_SUCCESS);
}

static future_t* clean_up(void) {
  btif_bap_config_flush();

  alarm_free(config_timer);
  config_timer = NULL;

  std::unique_lock<std::recursive_mutex> lock(config_lock);
  config.reset();
  return future_new_immediate(FUTURE_SUCCESS);
}

EXPORT_SYMBOL module_t btif_bap_config_module = {.name = BTIF_BAP_CONFIG_MODULE,
                                             .init = init,
                                             .start_up = NULL,
                                             .shut_down = shut_down,
                                             .clean_up = clean_up};

static void timer_config_save_cb(UNUSED_ATTR void* data) {
  // Moving file I/O to btif context instead of timer callback because
  // it usually takes a lot of time to be completed, introducing
  // delays during A2DP playback causing blips or choppiness.
  btif_transfer_context(btif_bap_config_write, 0, NULL, 0, NULL);
}

static void btif_bap_config_write(UNUSED_ATTR uint16_t event,
                              UNUSED_ATTR char* p_param) {
  CHECK(config != NULL);
  CHECK(config_timer != NULL);

  std::unique_lock<std::recursive_mutex> lock(config_lock);
  rename(CONFIG_FILE_PATH, CONFIG_BACKUP_PATH);
  if (config == NULL) {
    LOG(WARNING) << __func__ << "config is null";
    return;
  }
  std::unique_ptr<config_t> config_paired = config_new_clone(*config);

  if (config_paired != NULL) {
    //btif_bap_config_remove_unpaired(config_paired);
    config_save(*config_paired, CONFIG_FILE_PATH);
    config_paired.reset();
  }
}

static void btif_bap_config_remove_restricted(config_t& config) {
  for (auto snode = config.sections.begin(); snode != config.sections.end();) {
    std::string& section = snode->name;
    if (config_has_key(config, section, "Restricted")){
     // BTIF_TRACE_DEBUG("%s: Removing restricted device %s",
     //                                    __func__, section);
	// erase return next iterator, no need to move the node here
    snode = config.sections.erase(snode);
    continue;
    }
    snode++;
  }
}

static bool is_factory_reset(void) {
  char factory_reset[PROPERTY_VALUE_MAX] = {0};
  osi_property_get("persist.bluetooth.factoryreset", factory_reset, "false");
  return strncmp(factory_reset, "true", 4) == 0;
}

static void delete_config_files(void) {
  remove(CONFIG_FILE_PATH);
  remove(CONFIG_BACKUP_PATH);
  osi_property_set("persist.bluetooth.factoryreset", "false");
}

static bool btif_bap_get_section_index(const std::string &section,
                                       uint16_t *index) {
  char *temp = nullptr;
  if (section.length() != 20) return false;

  std::vector<std::string> byte_tokens =
              base::SplitString(section, ":", base::TRIM_WHITESPACE,
              base::SPLIT_WANT_ALL);

  LOG(WARNING) << __func__ << ": LC# codec ";
  if (byte_tokens.size() != 7) return false;

  // get the last nibble
  const auto& token = byte_tokens[6];

  if (token.length() != 2) return false;

  *index = strtol(token.c_str(), &temp, 16);

  if (*temp != '\0') return false;

  return true;
}

static bool btif_bap_get_free_section_id(const RawAddress& bd_addr,
                                         char *section) {
  uint16_t i = 0;
  uint8_t index_status[MAX_INDEX] = {0};
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();

  // reserve the first index for sink, src, locations
  index_status[0] = INDEX_OCCUPIED;
  for (auto snode = config->sections.begin(); snode != config->sections.end(); snode++) {
    std::string& section = snode->name;
    uint16_t index;

    // first check the address
    if(!strcasestr(section.c_str(), bdstr)) {
      continue;
    }

    if(btif_bap_get_section_index(section, &index)) {
      index_status[index] = INDEX_OCCUPIED;
    }
  }

  // find the unused index
  for(i = 0; i < MAX_INDEX; i++) {
    if(index_status[i] == INDEX_FREE) break;
  }

  if(i != MAX_INDEX) {
    char index_str[MAX_INDEX_LEN];
    // form the section entry ( bd address plus index)
    snprintf(index_str, sizeof(index_str), ":%02x", i);
    strlcpy(section, bdstr, MAX_SECTION_LEN);
    strlcat(section, index_str, MAX_SECTION_LEN);
    return true;
  } else {
    return false;
  }
}

static bool btif_bap_find_sections(const RawAddress& bd_addr,
                            btif_bap_record_type_t rec_type,
                            uint16_t context_type,
                            CodecDirection direction,
                            CodecConfig *record,
                            std::vector<char *> *sections) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  for (auto snode = config->sections.begin(); snode != config->sections.end(); snode++) {
    std::string& section = snode->name;
    // first check the address
    if(!strcasestr(section.c_str(), bdstr)) {
      continue;
    }

    // next check the record type
    const std::string* value_str = config_get_string(*config, section,
                                          BAP_RECORD_TYPE_KEY, NULL);
    if(value_str == nullptr ||
      ((rec_type == REC_TYPE_CAPABILITY &&
       strcasecmp(value_str->c_str(), BAP_RECORD_TYPE_CAPA)) ||
       (rec_type == REC_TYPE_CONFIGURATION &&
       strcasecmp(value_str->c_str(), BAP_RECORD_TYPE_CONF)))) {
      continue;
    }

    // next check the record type
    uint16_t context = config_get_uint16(*config, section,
                                         BAP_CONTEXT_TYPE_KEY, 0XFFFF);
    LOG(WARNING) << __func__ << ": context " << context
                             << ": context_type " << context_type;
    if(context != context_type) {
      continue;
    }

    // next check the direction
    value_str = config_get_string(*config, section,
                                          BAP_DIRECTION_KEY, NULL);
    if(value_str == nullptr ||
      ((direction == CodecDirection::CODEC_DIR_SRC &&
        strcasecmp(value_str->c_str(), "SRC")) ||
       (direction == CodecDirection::CODEC_DIR_SINK &&
        strcasecmp(value_str->c_str(), "SINK")))) {
      continue;
    }

    if(record == nullptr) {
      sections->push_back((char*) section.c_str());
    } else {
      // next check codec type
      value_str = config_get_string(*config, section,
                                    BAP_CODEC_TYPE_KEY, NULL);

      if(value_str == nullptr ||
        (record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_LC3 &&
         strcasecmp(value_str->c_str(), "LC3")) ||
        (record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE &&
         strcasecmp(value_str->c_str(), "AAR3")) ||
        (record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4 &&
         strcasecmp(value_str->c_str(), "AAR4"))) {
        continue;
      }

      // next check the freqency
      uint16_t value = config_get_uint16(*config, section,
                                         BAP_SAMP_FREQS_KEY, 0);
      if(value == static_cast<uint16_t> (record->sample_rate)) {
        sections->push_back((char*) section.c_str());
      }
    }
  }

  if(sections->size()) {
    return true;
  } else {
    return false;
  }
}

static bool btif_bap_update_LC3_codec_info(char *section, CodecConfig *record,
                                           btif_bap_record_type_t rec_type) {
  if(section == nullptr || record == nullptr) {
    return false;
  }

  if(rec_type == REC_TYPE_CAPABILITY) {

    config_set_string(config.get(), section, BAP_RECORD_TYPE_KEY,
                      BAP_RECORD_TYPE_CAPA);

    if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_LC3) {
      config_set_string(config.get(), section, BAP_CODEC_TYPE_KEY, "LC3");
    }
    // update freqs
    config_set_uint16(config.get(), section, BAP_SAMP_FREQS_KEY ,
                     static_cast<uint16_t>(record->sample_rate));

    // update chnl count
    config_set_uint16(config.get(), section, BAP_CHNL_ALLOCATION_KEY,
                   static_cast<uint16_t> (record->channel_mode));
    LOG(INFO) << __func__ << ": record->channel_mode = "
                       << loghex(static_cast<uint16_t> (record->channel_mode));

    // update supp frames
    config_set_uint16(config.get(), section, BAP_SUPP_FRM_DURATIONS_KEY ,
        static_cast<uint16_t> (GetCapaSupFrameDurations(record)));

    // update chnl supp min octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MIN_OCTS_PER_FRAME_KEY ,
        static_cast<uint16_t> (GetCapaSupOctsPerFrame(record) &
                               0xFFFF));

    // update chnl supp max octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MAX_OCTS_PER_FRAME_KEY,
        static_cast<uint16_t> ((GetCapaSupOctsPerFrame(record) &
                                0xFFFF0000) >> 16));

    // update max supp codec frames per sdu
    config_set_uint16(config.get(), section, BAP_MAX_SUP_CODEC_FRAMES_PER_SDU,
        static_cast<uint16_t> (GetCapaMaxSupLc3Frames(record)));

    // update LC3Q support
    if (GetCapaVendorMetaDataLc3QPref(record)) {
      config_set_string(config.get(), section, BAP_LC3Q_SUP_KEY, "true");
    } else {
      config_set_string(config.get(), section, BAP_LC3Q_SUP_KEY, "false");
    }

    // update LC3Q Version
    config_set_uint16(config.get(), section, BAP_LC3Q_VER_KEY,
        static_cast<uint16_t> (GetCapaVendorMetaDataCodecEncoderVer(record)));
  } else {

  }

  if (is_restricted_mode()) {
    LOG(WARNING) << __func__ << ": records will be removed if unrestricted";
    config_set_uint16(config.get(), section, "Restricted", 1);
  }

  return true;
}

static bool btif_bap_update_AAR3_codec_info(char *section, CodecConfig *record,
                                           btif_bap_record_type_t rec_type) {
  if(section == nullptr || record == nullptr) {
    return false;
  }

  if(rec_type == REC_TYPE_CAPABILITY) {

    config_set_string(config.get(), section, BAP_RECORD_TYPE_KEY,
                      BAP_RECORD_TYPE_CAPA);

    if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE) {
      config_set_string(config.get(), section, BAP_CODEC_TYPE_KEY, "AAR3");
    }
    // update freqs
    config_set_uint16(config.get(), section, BAP_SAMP_FREQS_KEY ,
                     static_cast<uint16_t>(record->sample_rate));

    // update chnl count
    config_set_uint16(config.get(), section, BAP_CHNL_ALLOCATION_KEY,
                   static_cast<uint16_t> (record->channel_mode));

    //CHECK BELOW MIN MAX
    // update chnl supp min octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MIN_OCTS_PER_FRAME_KEY ,
        static_cast<uint16_t> (GetCapaSupOctsPerFrame(record) &
                               0xFFFF));

    // update chnl supp max octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MAX_OCTS_PER_FRAME_KEY,
        static_cast<uint16_t> ((GetCapaSupOctsPerFrame(record) &
                                0xFFFF0000) >> 16));

    //update vendor specific metadata for AAR3
    config_set_uint16(config.get(), section, BAP_AAR3_CODEC_VER,
        static_cast<uint16_t> (GetCapaVendorMetaDataCodecVerAptx(record)));

    config_set_uint16(config.get(), section, BAP_AAR3_MIN_SUPP_FRAME_DUR,
        static_cast<uint16_t> (GetCapaMinSupFrameDur(record)));

    config_set_uint16(config.get(), section, BAP_AAR3_FEATURE_MAP,
        static_cast<uint16_t> (GetCapaFeatureMap(record)));

  } else {
    // [TBD] CONFIGURATIONS TBD
  }

  if (is_restricted_mode()) {
    LOG(WARNING) << __func__ << ": records will be removed if unrestricted";
    config_set_uint16(config.get(), section, "Restricted", 1);
  }

  return true;
}

static bool btif_bap_update_AAR4_codec_info(char *section, CodecConfig *record,
                                           btif_bap_record_type_t rec_type) {
  if(section == nullptr || record == nullptr) {
    return false;
  }

  if(rec_type == REC_TYPE_CAPABILITY) {

    config_set_string(config.get(), section, BAP_RECORD_TYPE_KEY,
                      BAP_RECORD_TYPE_CAPA);

    if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4) {
      config_set_string(config.get(), section, BAP_CODEC_TYPE_KEY, "AAR4");
    }
    // update freqs
    config_set_uint16(config.get(), section, BAP_SAMP_FREQS_KEY ,
                     static_cast<uint16_t>(record->sample_rate));

    // update chnl count
    config_set_uint16(config.get(), section, BAP_CHNL_ALLOCATION_KEY,
                   static_cast<uint16_t> (record->channel_mode));

    //CHECK BELOW MIN MAX
    // update chnl supp min octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MIN_OCTS_PER_FRAME_KEY ,
        static_cast<uint16_t> (GetCapaSupOctsPerFrame(record) &
                               0xFFFF));

    // update chnl supp max octs per frame
    config_set_uint16(config.get(), section, BAP_SUP_MAX_OCTS_PER_FRAME_KEY,
        static_cast<uint16_t> ((GetCapaSupOctsPerFrame(record) &
                                0xFFFF0000) >> 16));

    //update vendor specific metadata for AAR4
    // std::vector<uint8_t> vs_meta_data_AAR4 = GetVendorSpecificMetaDataAAR4(record);
    //all 4 bytes separately
    config_set_uint16(config.get(), section, BAP_AAR4_CODEC_ENCODER_VER_NUM,
        static_cast<uint16_t> (GetCapaVendorMetaDataCodecEncoderVer(record)));

    config_set_uint16(config.get(), section, BAP_AAR4_CODEC_DECODER_VER_NUM,
        static_cast<uint16_t> (GetCapaVendorMetaDataCodecDecoderVer(record)));

    config_set_uint16(config.get(), section, BAP_AAR4_MIN_SUPP_FRAME_DUR,
        static_cast<uint16_t> (GetCapaMinSupFrameDur(record)));

    config_set_uint16(config.get(), section, BAP_AAR4_FEATURE_MAP,
        static_cast<uint16_t> (GetCapaFeatureMap(record)));

  } else {
    // [TBD] CONFIGURATIONS TBD
  }

  if (is_restricted_mode()) {
    LOG(WARNING) << __func__ << ": records will be removed if unrestricted";
    config_set_uint16(config.get(), section, "Restricted", 1);
  }

  return true;
}

bool btif_bap_add_record(const RawAddress& bd_addr,
                         btif_bap_record_type_t rec_type,
                         uint16_t context_type,
                         CodecDirection direction,
                         CodecConfig *record) {
  // first check if same record already exists
  std::unique_lock<std::recursive_mutex> lock(config_lock);
  std::vector<char *> sections;
  LOG(INFO) << __func__;
  if(btif_bap_find_sections(bd_addr, rec_type, context_type,
                            direction, record, &sections)) {
    for (auto it = sections.begin();
                         it != sections.end(); it++) {
    if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4)
      btif_bap_update_AAR4_codec_info((*it), record , rec_type);
    else if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE)
      btif_bap_update_AAR3_codec_info((*it), record , rec_type);
    else
      btif_bap_update_LC3_codec_info((*it), record , rec_type);
    }
  } else {
    LOG(WARNING) << __func__ << ": section not found";
    char section[MAX_SECTION_LEN];
    btif_bap_get_free_section_id(bd_addr, section);

    config_set_uint16(config.get(), section, BAP_CONTEXT_TYPE_KEY, context_type);

    if(direction == CodecDirection::CODEC_DIR_SRC) {
      config_set_string(config.get(), section, BAP_DIRECTION_KEY, "SRC");
    } else {
      config_set_string(config.get(), section, BAP_DIRECTION_KEY, "SINK");
    }
    if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4)
      btif_bap_update_AAR4_codec_info(section, record , rec_type);
    else if(record->codec_type == CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE)
      btif_bap_update_AAR3_codec_info(section, record , rec_type);
    else
      btif_bap_update_LC3_codec_info(section, record , rec_type);
  }
  btif_bap_config_save();
  return true;
}

bool btif_bap_remove_record(const RawAddress& bd_addr,
                            btif_bap_record_type_t rec_type,
                            uint16_t context_type,
                            CodecDirection direction,
                            CodecConfig *record) {
  // first check if same record exists
  // if exists remove the record by complete section
  std::unique_lock<std::recursive_mutex> lock(config_lock);
  bool record_removed = false;
  std::vector<char *> sections;

  if(btif_bap_find_sections(bd_addr, rec_type, context_type,
                            direction, record, &sections)) {
    for (auto snode = config->sections.begin();
                  snode != config->sections.end();) {
        // erase returns next iterator
	snode = config->sections.erase(snode);
    }
    record_removed = true;
    btif_bap_config_flush();
  }
  return record_removed;
}

bool btif_bap_remove_record_by_context(const RawAddress& bd_addr,
                                       btif_bap_record_type_t rec_type,
                                       uint16_t context_type,
                                       CodecDirection direction) {
  // first check if same record exists
  // if exists remove the record by complete section
  std::unique_lock<std::recursive_mutex> lock(config_lock);
  bool record_removed = false;
  std::vector<char *> sections;

  if(btif_bap_find_sections(bd_addr, rec_type, context_type,
                            direction, nullptr, &sections)) {
    for (auto it = config->sections.begin();
                         it != config->sections.end();) {
      // erase returns next iterator
      it = config->sections.erase(it);
    }
    record_removed = true;
    btif_bap_config_flush();
  }
  return record_removed;
}

bool btif_bap_remove_all_records(const RawAddress& bd_addr) {
  // loop through the file if any record is found delete it
  std::unique_lock<std::recursive_mutex> lock(config_lock);
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  bool record_removed = false;

  for (auto snode = config->sections.begin(); snode != config->sections.end();) {
    std::string& section = snode->name;
    //BTIF_TRACE_DEBUG("%s: section:  %s", __func__, section);
    if(strcasestr(section.c_str(), bdstr)) {
      BTIF_TRACE_DEBUG("%s: section and bdaddress are matched.",
                                              __func__);
      record_removed = true;
      //erase returns next iterator
      snode  = config->sections.erase(snode);
      continue;
    }
    snode++;
  }

  btif_bap_config_flush();
  return record_removed;
}

bool btif_bap_get_records(const RawAddress& bd_addr,
                          btif_bap_record_type_t rec_type,
                          uint16_t context_type,
                          CodecDirection direction,
                          std::vector<CodecConfig> *pac_records) {
  std::unique_lock<std::recursive_mutex> lock(config_lock);
  std::vector<char *> sections;

  if(btif_bap_find_sections(bd_addr, rec_type, context_type,
                            direction, nullptr, &sections)) {
    for (auto it = sections.begin();
                         it != sections.end(); it++) {
      CodecConfig record;
      memset(&record, 0, sizeof(record));

      if(config_has_key(*config, (*it), BAP_SAMP_FREQS_KEY)) {
        record.sample_rate = static_cast<CodecSampleRate>
                                (config_get_uint16(*config, (*it),
                                              BAP_SAMP_FREQS_KEY,
                                              0x00));
      }

      if (config_has_key(*config, (*it), BAP_LC3Q_SUP_KEY)) {
        bool lc3q_sup = false;
        std::string empty = "";
        const std::string * is_lc3q_sup = config_get_string(*config, (*it),
                                                BAP_LC3Q_SUP_KEY,
                                                &empty);
        if(!strcmp(is_lc3q_sup->c_str(), "true")) {
           lc3q_sup = true;
        }
        LOG(WARNING) << __func__ << ": lc3q_sup: " << lc3q_sup;
        if (lc3q_sup) {
          UpdateCapaVendorMetaDataLc3QPref(&record, lc3q_sup);
        }
      }

      if(config_has_key(*config, (*it), BAP_LC3Q_VER_KEY)) {
        uint16_t lc3q_ver = config_get_uint16(*config, (*it),
                                                BAP_LC3Q_VER_KEY,
                                                0x00);
        LOG(WARNING) << __func__ << ": lc3q_ver: " << lc3q_ver;
        // UpdateCapaVendorMetaDataLc3QVer(&record, lc3q_ver);
        UpdateCapaVendorMetaDataCodecEncoderVer(&record, lc3q_ver);
      }

      record.codec_type = CodecIndex::CODEC_INDEX_SOURCE_LC3;
      if(rec_type == REC_TYPE_CAPABILITY) {

        if(config_has_key(*config, (*it), BAP_SUPP_FRM_DURATIONS_KEY)) {
          uint16_t supp_frames = config_get_uint16(*config, (*it),
                                                BAP_SUPP_FRM_DURATIONS_KEY,
                                                0x00);
          UpdateCapaSupFrameDurations(&record, supp_frames);
        }

        // update chnl supp octs per frame
        if(config_has_key(*config, (*it), BAP_SUP_MIN_OCTS_PER_FRAME_KEY) &&
           config_has_key(*config, (*it), BAP_SUP_MAX_OCTS_PER_FRAME_KEY)) {
          uint16_t sup_min_octs = config_get_uint16(*config, (*it),
                                           BAP_SUP_MIN_OCTS_PER_FRAME_KEY,
                                           0x00);
          uint16_t sup_max_octs =  config_get_uint16(*config, (*it),
                                           BAP_SUP_MAX_OCTS_PER_FRAME_KEY,
                                           0x00);
          UpdateCapaSupOctsPerFrame(&record, sup_min_octs | sup_max_octs << 16);
        }

        // update max supp codec frames per sdu
        if(config_has_key(*config, (*it), BAP_MAX_SUP_CODEC_FRAMES_PER_SDU)) {
          uint16_t max_sup_codec_frames_per_sdu = config_get_uint16(*config, (*it),
                                           BAP_MAX_SUP_CODEC_FRAMES_PER_SDU,
                                           0x00);
          UpdateCapaMaxSupLc3Frames(&record, max_sup_codec_frames_per_sdu);
        }

        // update preferred context type.
        if(config_has_key(*config, (*it), BAP_CONTEXT_TYPE_KEY)) {
          uint16_t context_type = config_get_uint16(*config, (*it),
                                           BAP_CONTEXT_TYPE_KEY,
                                           0x00);
          UpdateCapaPreferredContexts(&record, context_type);
        }
      } else {

        if(config_has_key(*config, (*it), BAP_CONF_FRAME_DUR_KEY)) {
          uint16_t conf_frames = config_get_uint16(*config, (*it),
                                                BAP_CONF_FRAME_DUR_KEY,
                                                0x00);
          UpdateFrameDuration(&record, conf_frames);
        }

        if(config_has_key(*config, (*it), BAP_CONF_OCTS_PER_FRAME_KEY)) {
          uint16_t conf_octs_per_frame = config_get_uint16(*config, (*it),
                                                BAP_CONF_OCTS_PER_FRAME_KEY,
                                                0x00);
          UpdateOctsPerFrame(&record, conf_octs_per_frame);
        }

        if(config_has_key(*config, (*it), BAP_LC3_FRAMES_PER_SDU_KEY)) {
          uint16_t lc3_frms_per_sdu = config_get_uint16(*config, (*it),
                                                BAP_LC3_FRAMES_PER_SDU_KEY,
                                                0x00);
          UpdateLc3BlocksPerSdu(&record, lc3_frms_per_sdu);
        }
      }
      pac_records->push_back(record);
    }
  }

  if(pac_records->size()) {
    return true;
  } else {
    return false;
  }
}

bool btif_bap_add_audio_loc(const RawAddress& bd_addr,
                             CodecDirection direction, uint32_t audio_loc) {
  // first check if same already exists
  // if exists update the same entry
  // audio location will always be stored @ 0th index
  // form the section entry ( bd address plus index)
  LOG(INFO) << __func__ << ": audio_loc = " << audio_loc
                        << ", direction: " << loghex(static_cast<uint8_t> (direction));
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  if(direction == CodecDirection::CODEC_DIR_SRC) {
    config_set_uint16(config.get(), section, BAP_SRC_LOCATIONS_KEY, audio_loc);
  } else {
    config_set_uint16(config.get(), section, BAP_SINK_LOCATIONS_KEY, audio_loc);
  }
  btif_bap_config_save();
  return true;
}

bool btif_bap_add_num_src_sink(const RawAddress& bd_addr,
                             uint16_t num_sink, uint16_t num_src) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  config_set_uint16(config.get(), section, BAP_NUM_SINK_KEY, num_sink);
  config_set_uint16(config.get(), section, BAP_NUM_SRC_KEY, num_src);
  btif_bap_config_save();
  return true;
}

bool btif_bap_add_channel_count(const RawAddress& bd_addr,
                             uint16_t channel_count, CodecDirection dir) {
  LOG(INFO) << __func__ << ": channel_count = " << loghex(channel_count)
                        << ", direction: " << loghex(static_cast<uint8_t> (dir));
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  if (dir == CodecDirection::CODEC_DIR_SINK) {
      config_set_uint16(config.get(), section, BAP_SINK_CHANNEL_COUNT_KEY, channel_count);
  }
  if (dir == CodecDirection::CODEC_DIR_SRC) {
      config_set_uint16(config.get(), section, BAP_SRC_CHANNEL_COUNT_KEY, channel_count);
  }
  btif_bap_config_save();
  return true;
}

bool btif_bap_rem_audio_loc(const RawAddress& bd_addr,
                             CodecDirection direction) {
  // first check if same record already exists
  // if exists remove the record by complete section
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  if (direction == CodecDirection::CODEC_DIR_SRC) {
    config_remove_key(config.get(), section, "BAP_SRC_LOCATIONS_KEY");
  } else {
    config_remove_key(config.get(), section, "BAP_SINK_LOCATIONS_KEY");
  }
  btif_bap_config_flush();
  return true;
}

bool btif_bap_add_supp_contexts(const RawAddress& bd_addr,
                                  uint32_t supp_contexts) {
  // first check if same already exists
  // if exists update the same entry
  // supp contexts will always be stored @ 0th index
  // form the section entry ( bd address plus index)
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  LOG(WARNING) << __func__ << " supp_contexts "  << supp_contexts;
  config_set_uint64(config.get(), section,
                    BAP_SUP_AUDIO_CONTEXTS_KEY, supp_contexts);
  btif_bap_config_save();
  return true;
}

bool btif_bap_get_params(const RawAddress& bd_addr,
                                 uint16_t *sink_aud_loc,
                                 uint16_t *src_aud_loc,
                                 uint16_t *sink_num,
                                 uint16_t *src_num,
                                 uint16_t *sink_chan_cnt,
                                 uint16_t *src_chan_cnt) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  if(sink_aud_loc) {
    *sink_aud_loc = config_get_uint16(*config, section,
                                     BAP_SINK_LOCATIONS_KEY, 0);
  }
  if(src_aud_loc) {
    *src_aud_loc = config_get_uint16(*config, section,
                                     BAP_SRC_LOCATIONS_KEY, 0);
  }
  if(sink_num) {
    *sink_num = config_get_uint16(*config, section,
                                     BAP_NUM_SINK_KEY, 0);
  }
  if(src_num) {
    *src_num = config_get_uint16(*config, section,
                                     BAP_NUM_SRC_KEY, 0);
  }
  if(sink_chan_cnt) {
    *sink_chan_cnt = config_get_uint16(*config, section,
                                     BAP_SINK_CHANNEL_COUNT_KEY, 0);
  }
  if(src_chan_cnt) {
    *src_chan_cnt = config_get_uint16(*config, section,
                                     BAP_SRC_CHANNEL_COUNT_KEY, 0);
  }
  return true;
}

bool btif_bap_get_device_type(const RawAddress& bd_addr,
                               DeviceType *device_type, CodecDirection dir) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));

  char pts_tmap_cap_device_type[PROPERTY_VALUE_MAX] = "false";
  property_get("persist.vendor.btstack.pts_device_type", pts_tmap_cap_device_type, "false");
  LOG(WARNING) << __func__
               << ": pts_tmap_cap_device_type: "
               << pts_tmap_cap_device_type;

  if (dir == CodecDirection::CODEC_DIR_SINK) {
    if (device_type) {
      if (!strncmp("true", pts_tmap_cap_device_type, 4)) {
        *device_type = DeviceType::HEADSET_SPLIT_STEREO;
      } else {
        *device_type = static_cast<DeviceType>(config_get_uint16(*config, section,
                                     BAP_SINK_DEVICE_TYPE_KEY, 0));
      }
    }
  } else {
    if (device_type) {
      if (!strncmp("true", pts_tmap_cap_device_type, 4)) {
        *device_type = DeviceType::NONE;
      } else {
       *device_type = static_cast<DeviceType>(config_get_uint16(*config, section,
                                     BAP_SRC_DEVICE_TYPE_KEY, 0));
      }
    }
  }
  return true;
}

bool btif_bap_is_device_supports_csip(const RawAddress& bd_addr) {
  DeviceType sink_dev_type = DeviceType::NONE;
  DeviceType src_dev_type = DeviceType::NONE;

  btif_bap_get_device_type(bd_addr, &src_dev_type,
                           CodecDirection::CODEC_DIR_SRC);
  btif_bap_get_device_type(bd_addr, &sink_dev_type,
                           CodecDirection::CODEC_DIR_SINK);

  BTIF_TRACE_DEBUG("%s: src_dev_type: %d, sink_dev_type: %d",
                   __func__, static_cast<DeviceType>(src_dev_type),
                   static_cast<DeviceType>(sink_dev_type));

  //When both src and sink device types as EBs,
  //then consder it as CSIP device
  if ((src_dev_type == DeviceType::EARBUD &&
       sink_dev_type == DeviceType::EARBUD)) {
     BTIF_TRACE_DEBUG("%s: bd_addr: %s is a CSIP device",
                            __func__, bd_addr.ToString().c_str());
     return true;
  }
  return false;
}

bool btif_bap_get_sink_channel_count(const RawAddress& bd_addr,
                                     uint16_t *sink_chan_cnt,
                                     CodecDirection dir) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  LOG(INFO) << __func__ << ": *sink_chan_cnt = " << loghex(*sink_chan_cnt)
                        << ", direction: " << loghex(static_cast<uint8_t> (dir));
  if (dir == CodecDirection::CODEC_DIR_SINK) {
    if (sink_chan_cnt) {
      *sink_chan_cnt = config_get_uint16(*config, section,
                                     BAP_SINK_CHANNEL_COUNT_KEY, 0);
    }
  }
  return true;
}

bool btif_bap_get_src_channel_count(const RawAddress& bd_addr,
                                     uint16_t *src_chan_cnt,
                                     CodecDirection dir) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  if (dir == CodecDirection::CODEC_DIR_SRC) {
    if (src_chan_cnt) {
      *src_chan_cnt = config_get_uint16(*config, section,
                                     BAP_SRC_CHANNEL_COUNT_KEY, 0);
    }
  }
  return true;
}

bool btif_bap_add_device_type(const RawAddress& bd_addr,
                              DeviceType sink_device_type,
                              DeviceType src_device_type) {
  // first check if same already exists
  // if exists update the same entry
  // audio location will always be stored @ 0th index
  // form the section entry ( bd address plus index)
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  config_set_uint16(config.get(), section, BAP_SINK_DEVICE_TYPE_KEY,
    static_cast<uint16_t>(sink_device_type));
  config_set_uint16(config.get(), section, BAP_SRC_DEVICE_TYPE_KEY,
    static_cast<uint16_t>(src_device_type));
  btif_bap_config_save();
  return true;
}

bool btif_bap_update_sink_device_type(const RawAddress& bd_addr,
                                   DeviceType sink_device_type) {
  // first check if same already exists
  // if exists update the same entry
  // audio location will always be stored @ 0th index
  // form the section entry ( bd address plus index)
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  config_set_uint16(config.get(), section, BAP_SINK_DEVICE_TYPE_KEY,
    static_cast<uint16_t>(sink_device_type));
  btif_bap_config_save();
  return true;
}

bool btif_bap_update_src_device_type(const RawAddress& bd_addr,
                                  DeviceType src_device_type) {
  // first check if same already exists
  // if exists update the same entry
  // audio location will always be stored @ 0th index
  // form the section entry ( bd address plus index)
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  config_set_uint16(config.get(), section, BAP_SRC_DEVICE_TYPE_KEY,
    static_cast<uint16_t>(src_device_type));
  btif_bap_config_save();
  return true;
}

bool btif_bap_get_supp_contexts(const RawAddress& bd_addr,
                                 uint32_t *supp_contexts) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  *supp_contexts = config_get_uint64(*config, section,
                                     BAP_SUP_AUDIO_CONTEXTS_KEY, 0);
  return true;
}

bool btif_bap_rem_supp_contexts(const RawAddress& bd_addr) {
  std::string addrstr = bd_addr.ToString();
  const char* bdstr = addrstr.c_str();
  char section[MAX_SECTION_LEN];
  char index[MAX_INDEX_LEN];
  snprintf(index, sizeof(index), ":%02x", 0);
  strlcpy(section, bdstr, sizeof(section));
  strlcat(section, index, sizeof(section));
  config_remove_key(config.get(), section, BAP_SUP_AUDIO_CONTEXTS_KEY);
  btif_bap_config_flush();
  return true;
}
