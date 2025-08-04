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

#include "bta_gatt_api.h"
#include "bta_pacs_client_api.h"
#include "gattc_ops_queue.h"
#include <map>
#include <base/bind.h>
#include <base/callback.h>
#include <base/logging.h>
#include "stack/btm/btm_int.h"
#include "device/include/controller.h"
#include "osi/include/properties.h"

#include <vector>
#include "btif/include/btif_bap_config.h"
#include "osi/include/log.h"
#include "btif_util.h"
#include <hardware/bt_bap_uclient.h>
#include "btif_bap_codec_utils.h"
#include "bta_dm_adv_audio.h"

namespace bluetooth {
namespace bap {
namespace pacs {

//using bluetooth::bap::pacs::PacsClientCallbacks;
using base::Closure;
using bluetooth::bap::GattOpsQueue;

Uuid PACS_UUID               = Uuid::FromString("1850");
Uuid PACS_SINK_PAC_UUID      = Uuid::FromString("2BC9");
Uuid PACS_SINK_LOC_UUID      = Uuid::FromString("2BCA");
Uuid PACS_SRC_PAC_UUID       = Uuid::FromString("2BCB");
Uuid PACS_SRC_LOC_UUID       = Uuid::FromString("2BCC");
Uuid PACS_AVA_AUDIO_UUID     = Uuid::FromString("2BCD");
Uuid PACS_SUP_AUDIO_UUID     = Uuid::FromString("2BCE");

class PacsClientImpl;
PacsClientImpl* instance;

typedef uint8_t codec_type_t[5];

constexpr uint8_t SINK_PAC        = 0x01;
constexpr uint8_t SRC_PAC         = 0x02;
constexpr uint8_t SINK_LOC        = 0x04;
constexpr uint8_t SRC_LOC         = 0x08;
constexpr uint8_t AVAIL_CONTEXTS  = 0x10;
constexpr uint8_t SUPP_CONTEXTS   = 0x20;

#ifdef MIN_ATT_MTU
constexpr uint16_t ATT_MIN_MTU    = 100;
#endif

constexpr uint8_t LTV_TYPE_SUP_FREQS              = 0x01;
constexpr uint8_t LTV_TYPE_SUP_FRAME_DUR          = 0x02;
constexpr uint8_t LTV_TYPE_CHNL_COUNTS            = 0x03;
constexpr uint8_t LTV_TYPE_OCTS_PER_FRAME         = 0x04;
constexpr uint8_t LTV_TYPE_MAX_SUP_FRAMES_PER_SDU = 0x05;

constexpr uint8_t LTV_TYPE_PREF_AUD_CONTEXT       = 0x01;
constexpr uint8_t LTV_TYPE_VS_META_DATA           = 0xFF;
constexpr uint16_t QTI_ID                         = 0x000A;

constexpr uint8_t LTV_TYPE_VS_META_DATA_LC3Q      = 0x10;
constexpr uint8_t LTV_TYPE_VS_META_DATA_AAR3      = 0x11;
constexpr uint8_t LTV_TYPE_VS_META_DATA_AAR4      = 0x11;

// LTV types for AAR3 (common for V1 and V2)
constexpr uint8_t LTV_TYPE_SUP_FREQS_AAR3         = 0x01;
constexpr uint8_t LTV_TYPE_CHNL_COUNTS_AAR3       = 0x03;
constexpr uint8_t LTV_TYPE_OCTS_PER_FRAME_AAR3    = 0x04;

// LTV types for AAR4
constexpr uint8_t LTV_TYPE_SUP_FREQS_AAR4         = 0x01;
constexpr uint8_t LTV_TYPE_SUP_FRAME_DUR_AAR4     = 0x02;
constexpr uint8_t LTV_TYPE_CHNL_COUNTS_AAR4       = 0x03;
constexpr uint8_t LTV_TYPE_OCTS_PER_FRAME_AAR4    = 0x04;

//constexpr uint16_t SAMPLE_RATE_NONE        = 0x0;
constexpr uint16_t SAMPLE_RATE_8K          = 0x1 << 0;
//constexpr uint16_t SAMPLE_RATE_11K         = 0x1 << 1;
constexpr uint16_t SAMPLE_RATE_16K         = 0x1 << 2;
//constexpr uint16_t SAMPLE_RATE_22K         = 0x1 << 3;
constexpr uint16_t SAMPLE_RATE_24K         = 0x1 << 4;
constexpr uint16_t SAMPLE_RATE_32K         = 0x1 << 5;
constexpr uint16_t SAMPLE_RATE_441K        = 0x1 << 6;
constexpr uint16_t SAMPLE_RATE_48K         = 0x1 << 7;
constexpr uint16_t SAMPLE_RATE_882K        = 0x1 << 8;
constexpr uint16_t SAMPLE_RATE_96K         = 0x1 << 9;
constexpr uint16_t SAMPLE_RATE_176K        = 0x1 << 10;
constexpr uint16_t SAMPLE_RATE_192K        = 0x1 << 11;
//constexpr uint16_t SAMPLE_RATE_384K        = 0x1 << 12;

constexpr uint8_t CODEC_ID_LC3     = 0x06;
constexpr uint8_t CODEC_ID_VS      = 0xFF;
constexpr uint8_t DISCOVER_SUCCESS = 0x00;
constexpr uint8_t DISCOVER_FAIL    = 0xFF;

constexpr uint16_t VS_CODEC_ID_AAR3        = 0x0001;
// constexpr uint16_t VS_CODEC_ID_AAR3        = 0x00AD;  //have to change?
constexpr uint16_t VS_CODEC_ID_AAR4        = 0x01AD;

std::map<uint16_t, CodecSampleRate> freq_map = {
  {SAMPLE_RATE_8K,    CodecSampleRate::CODEC_SAMPLE_RATE_8000  },
  {SAMPLE_RATE_16K,   CodecSampleRate::CODEC_SAMPLE_RATE_16000 },
  {SAMPLE_RATE_24K,   CodecSampleRate::CODEC_SAMPLE_RATE_24000 },
  {SAMPLE_RATE_32K,   CodecSampleRate::CODEC_SAMPLE_RATE_32000 },
  {SAMPLE_RATE_441K,  CodecSampleRate::CODEC_SAMPLE_RATE_44100 },
  {SAMPLE_RATE_48K,   CodecSampleRate::CODEC_SAMPLE_RATE_48000 },
  {SAMPLE_RATE_882K,  CodecSampleRate::CODEC_SAMPLE_RATE_88200 },
  {SAMPLE_RATE_96K,   CodecSampleRate::CODEC_SAMPLE_RATE_96000 },
  {SAMPLE_RATE_176K,  CodecSampleRate::CODEC_SAMPLE_RATE_176400},
  {SAMPLE_RATE_192K,  CodecSampleRate::CODEC_SAMPLE_RATE_192000}
};

// ltv type to length
std::map<uint8_t, uint8_t> ltv_info_LC3Q = {
  {LTV_TYPE_SUP_FREQS,              0x03},
  {LTV_TYPE_SUP_FRAME_DUR,          0x02},
  {LTV_TYPE_CHNL_COUNTS,            0x02},
  {LTV_TYPE_OCTS_PER_FRAME,         0x05},
  {LTV_TYPE_MAX_SUP_FRAMES_PER_SDU, 0x02},
  {LTV_TYPE_PREF_AUD_CONTEXT,       0x03}
};

// ltv type to length
std::map<uint8_t, uint8_t> ltv_info_AAR3 = {
  {LTV_TYPE_SUP_FREQS_AAR3,         0x03},
  {LTV_TYPE_CHNL_COUNTS_AAR3,       0x02},
  {LTV_TYPE_OCTS_PER_FRAME_AAR3,    0x05},
};

// ltv type to length
std::map<uint8_t, uint8_t> ltv_info_AAR4 = {
  {LTV_TYPE_SUP_FREQS_AAR4,         0x03},
  {LTV_TYPE_CHNL_COUNTS_AAR4,       0x02},
  {LTV_TYPE_OCTS_PER_FRAME_AAR4,    0x05},
  {LTV_TYPE_SUP_FRAME_DUR_AAR4,     0x02},
};

// map of maps for combined ltv_info
std::map<CodecIndex, std::map<uint8_t, uint8_t>> ltv_info;

enum class ProfleOP {
  CONNECT,
  DISCONNECT
};

struct ProfileOperation {
  uint16_t client_id;
  ProfleOP type;
};

enum class DevState {
  IDLE = 0,
  CONNECTING,
  CONNECTED,
  DISCONNECTING
};

struct SinkPacsData {
  uint16_t sink_pac_handle;
  uint16_t sink_pac_ccc_handle;
  std::vector<CodecConfig> sink_pac_records;
  bool read_sink_pac_record;
};

struct SrcPacsData {
  uint16_t src_pac_handle;
  uint16_t src_pac_ccc_handle;
  std::vector<CodecConfig> src_pac_records;
  bool read_src_pac_record;
};

void pacs_gattc_callback(tBTA_GATTC_EVT event, tBTA_GATTC* p_data);
void encryption_callback(const RawAddress*, tGATT_TRANSPORT, void*,
                         tBTM_STATUS);

struct PacsDevice {
  RawAddress address;
  /* This is true only during first connection to profile, until we store the
   * device */
  bool first_connection;
  bool service_changed_rcvd;

  /* we are making active attempt to connect to this device, 'direct connect'.
   * This is true only during initial phase of first connection. */
  bool connecting_actively;

  uint16_t conn_id;
  std::vector<SinkPacsData>sink_info;
  std::vector<SrcPacsData>src_info;
  uint16_t sink_loc_handle;
  uint16_t sink_loc_ccc_handle;
  uint16_t src_loc_handle;
  uint16_t src_loc_ccc_handle;
  uint16_t avail_contexts_handle;
  uint16_t avail_contexts_ccc_handle;
  uint16_t supp_contexts_handle;
  uint16_t supp_contexts_ccc_handle;
  uint16_t srv_changed_ccc_handle;
  uint8_t chars_read;
  uint8_t chars_to_be_read;
  std::vector<CodecConfig> consolidated_sink_pac_records;
  std::vector<CodecConfig> consolidated_src_pac_records;
  uint32_t sink_locations;
  uint32_t src_locations;
  uint32_t available_contexts;
  uint32_t supported_contexts;
  bool discovery_completed;
  bool notifications_enabled;
  DevState state;
  bool is_congested;
  std::vector<ProfileOperation> profile_queue;
  std::vector<uint16_t> connected_client_list; //list client requested for connection
  PacsDevice(const RawAddress& address) : address(address) {}
  PacsDevice() {
    first_connection = false;
    service_changed_rcvd = false;
    conn_id = 0;
    sink_loc_handle = 0;
    sink_loc_ccc_handle = 0;
    src_loc_handle = 0;
    src_loc_ccc_handle = 0;
    avail_contexts_handle = 0;
    avail_contexts_ccc_handle = 0;
    supp_contexts_handle = 0;
    supp_contexts_ccc_handle = 0;
    srv_changed_ccc_handle = 0;
    chars_read = 0;
    sink_locations = 0;
    src_locations = 0;
    available_contexts = 0;
    supported_contexts = 0;
    discovery_completed = false;
    notifications_enabled = false;
    state = static_cast<DevState>(0);
    is_congested = false;
  }
};

class PacsDevices {
 public:
  void Add(PacsDevice device) {
    if (FindByAddress(device.address) != nullptr) return;

    devices.push_back(device);
  }

  void Remove(const RawAddress& address) {
    for (auto it = devices.begin(); it != devices.end();) {
      if (it->address != address) {
        ++it;
        continue;
      }

      it = devices.erase(it);
      return;
    }
  }

  PacsDevice* FindByAddress(const RawAddress& address) {
    auto iter = std::find_if(devices.begin(), devices.end(),
                             [&address](const PacsDevice& device) {
                               return device.address == address;
                             });

    return (iter == devices.end()) ? nullptr : &(*iter);
  }

  PacsDevice* FindByConnId(uint16_t conn_id) {
    auto iter = std::find_if(devices.begin(), devices.end(),
                             [&conn_id](const PacsDevice& device) {
                               return device.conn_id == conn_id;
                             });

    return (iter == devices.end()) ? nullptr : &(*iter);
  }

  size_t size() { return (devices.size()); }

  std::vector<PacsDevice> devices;
};

class PacsClientImpl : public PacsClient {
 public:
  ~PacsClientImpl() override = default;

  PacsClientImpl() : gatt_client_id(BTA_GATTS_INVALID_IF) {
    BTA_GATTC_AppRegister(
      pacs_gattc_callback,
      base::Bind(
        [](uint8_t client_id, uint8_t status) {
          if (status != GATT_SUCCESS) {
            LOG(ERROR) << ": Can't start PACS profile - no gatt "
                          "clients left, status=" << loghex(status);
          }

          if (instance) {
            LOG(WARNING) << ": PACS gatt_client_id: "
                         << loghex(client_id);
            if (status == GATT_SUCCESS)
              instance->gatt_client_id = client_id;

            for (const auto& it : instance->callbacks) {
              LOG(WARNING) << ": reg callback, pacs_client_id: "
                           << loghex(it.first);
              PacsClientCallbacks *callback = it.second;
              callback->OnInitialized(status, it.first);
            }
          }
        }),
      true);
  };

  bool Register(PacsClientCallbacks *callback) {
    LOG(WARNING) << __func__ << ": " << callback;
    // looks for client is already registered
    bool is_client_registered = false;
    for (auto it : callbacks) {
      PacsClientCallbacks *pac_callback = it.second;
      if (callback == pac_callback) {
        is_client_registered = true;
        break;
      }
    }

    LOG(WARNING) << __func__ << ": is_client_registered: "
                             << logbool(is_client_registered)
                             << ", gatt_client_id: "
                             << loghex(gatt_client_id);
    if (is_client_registered) return false;

    callbacks.insert(std::make_pair(++pacs_client_id, callback));
    LOG(WARNING) << __func__ << ": pacs_client_id: "
                 << loghex(pacs_client_id);
    if (gatt_client_id != BTA_GATTS_INVALID_IF) {
      callback->OnInitialized(0, pacs_client_id);
    }
    return true;
  }

  bool Deregister (uint16_t client_id) {
  LOG(WARNING) << __func__ << ": client_id: " << client_id;
    bool status = false;
    auto it = callbacks.find(client_id);
    if (it != callbacks.end()) {
      callbacks.erase(it);
      if(callbacks.empty()) {
       // deregister with GATT
       LOG(WARNING) << __func__ << ": Gatt de-register from pacs";
       BTA_GATTC_AppDeregister(gatt_client_id);
       gatt_client_id = BTA_GATTS_INVALID_IF;
      }
      status = true;
    }
    return status;
  }

  uint8_t GetClientCount () {
    LOG(WARNING) << __func__ << ": ClientCount: " << callbacks.size();
    return callbacks.size();
  }

  void Connect(uint16_t client_id, const RawAddress& address,
                         bool is_direct) override {
    LOG(WARNING) << __func__ << ": address: " << address
                             << ", is_direct: " << logbool(is_direct)
                             << ", client_id: " << client_id;
    PacsDevice *dev = pacsDevices.FindByAddress(address);
    ProfileOperation op;
    op.client_id = client_id;
    op.type = ProfleOP::CONNECT;

    if (dev == nullptr) {
      PacsDevice pac_dev(address);
      pacsDevices.Add(pac_dev);
      dev = pacsDevices.FindByAddress(address);
    }
    if (dev == nullptr) {
      LOG(ERROR) << __func__ << ": dev is null";
      return;
    }

    LOG(WARNING) << __func__ << ": state: " << static_cast<int>(dev->state);

    switch(dev->state) {
      case DevState::IDLE: {
        BTA_GATTC_Open(gatt_client_id, address, is_direct,
                                           BT_TRANSPORT_LE, false);
        dev->state = DevState::CONNECTING;
        dev->profile_queue.push_back(op);
      } break;

      case DevState::CONNECTING: {
        auto iter = std::find_if(dev->profile_queue.begin(),
                                 dev->profile_queue.end(),
                             [&client_id]( ProfileOperation entry) {
                               return ((entry.type == ProfleOP::CONNECT) &&
                                      (entry.client_id == client_id));
                             });
        LOG(INFO) << __func__ << ": profile_queue size: "
                              << dev->profile_queue.size();
        if (iter != dev->profile_queue.end()) {
          LOG(INFO) << __func__ << ": Op had existed in profile_queue.";
        } else {
          LOG(INFO) << __func__ << ": Push back op.";
          dev->profile_queue.push_back(op);
        }
      } break;

      case DevState::CONNECTED: {
        // add it to the client id list if not already done
        auto iter = std::find_if(dev->connected_client_list.begin(),
                                 dev->connected_client_list.end(),
                             [&client_id](uint16_t id) {
                               return id == client_id;
                             });

        if (iter == dev->connected_client_list.end())
          dev->connected_client_list.push_back(client_id);

        auto it = callbacks.find(client_id);
        if (it != callbacks.end()) {
          PacsClientCallbacks *callback = it->second;
          LOG(WARNING) << __func__ << ": calling OnConnectionState";
          callback->OnConnectionState(address,
                                      ConnectionState::CONNECTED);
        }
      } break;

      case DevState::DISCONNECTING: {
        dev->profile_queue.push_back(op);
      } break;
    }
  }

  void Disconnect(uint16_t client_id, const RawAddress& address) override {
    LOG(WARNING) << __func__ << ": address: " << address
                             << ", client_id: " << client_id;
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(INFO) << __func__ <<": Device not connected to profile: " << address;
      return;
    }

    ProfileOperation op;
    op.client_id = client_id;
    op.type = ProfleOP::DISCONNECT;

    LOG(WARNING) << __func__ << ": address: " << address
                             << ", state: " << static_cast<int>(dev->state);

    switch(dev->state) {
      case DevState::CONNECTING: {
        auto iter = std::find_if(dev->profile_queue.begin(),
                                 dev->profile_queue.end(),
                             [&client_id]( ProfileOperation entry) {
                               return ((entry.type == ProfleOP::CONNECT) &&
                                      (entry.client_id == client_id));
                             });

        LOG(INFO) << __func__ << ": profile_queue size: "
                              << dev->profile_queue.size();

        // If it is the last client requested for disconnect
        if (iter != dev->profile_queue.end() &&
           dev->profile_queue.size() == 1) {
          LOG(INFO) << __func__ << ": conn_id: " << dev->conn_id;
          if (dev->conn_id) {
            // Removes all registrations for connection.
            GattOpsQueue::Clean(dev->conn_id);
            BTA_GATTC_Close(dev->conn_id);
            dev->state = DevState::DISCONNECTING;
            dev->profile_queue.push_back(op);
          } else {
            // clear the connection queue
            dev->profile_queue.clear();
            dev->state = DevState::IDLE;
            BTA_GATTC_CancelOpen(gatt_client_id, address, true);
          }
        } else {
          // remove the connection entry from the list
          // as the same client has requested for disconnection
          if (iter != dev->profile_queue.end()) {
            LOG(WARNING) << __func__ << ": Valid profile op, erase it.";
            dev->profile_queue.erase(iter);
          } else {
            LOG(WARNING) << __func__ << ": Invalid profile op.";
          }
        }
      } break;

      case DevState::CONNECTED: {
        auto iter = std::find_if(dev->connected_client_list.begin(),
                                 dev->connected_client_list.end(),
                             [&client_id]( uint16_t stored_client_id) {
                               return stored_client_id == client_id;
                             });

        LOG(INFO) << __func__ << ": connected_client_list size: "
                              << dev->connected_client_list.size();
        // if it is the last client requested for disconnection
        if (iter != dev->connected_client_list.end() &&
           dev->connected_client_list.size() == 1) {
          LOG(INFO) << __func__ << ": conn_id: " << dev->conn_id;
          if (dev->conn_id) {
            // Removes all registrations for connection.
            GattOpsQueue::Clean(dev->conn_id);
            BTA_GATTC_Close(dev->conn_id);
            BTA_GATTC_CancelOpen(gatt_client_id, address, false);
            dev->profile_queue.push_back(op);
            dev->state = DevState::DISCONNECTING;
          }
        } else {
          // remove the client from connected_client_list
          auto it = callbacks.find(client_id);
          if (it != callbacks.end()) {
            PacsClientCallbacks *callback = it->second;
            LOG(WARNING) << __func__ << ": calling OnConnectionState Disc";
            callback->OnConnectionState(address,
                                        ConnectionState::DISCONNECTED);
          }

          if (iter != dev->connected_client_list.end()) {
            LOG(WARNING) << __func__ << ": Valid connected client, erase it.";
            dev->connected_client_list.erase(iter);
          } else {
            LOG(WARNING) << __func__ << ": Invalid connected client.";
          }
        }
      } break;

      case DevState::DISCONNECTING: {
        dev->profile_queue.push_back(op);
      } break;

      default:
        break;
    }
  }

  void StartDiscovery(uint16_t client_id, const RawAddress& address) override {
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(INFO) << __func__ << ": Device not connected to profile: " << address;
      return;
    }

    LOG(WARNING) << __func__ << ": address: " << address
                             << ", state: " << static_cast<int>(dev->state);
    switch(dev->state) {
      case DevState::CONNECTED: {
        auto iter = std::find_if(dev->connected_client_list.begin(),
                                 dev->connected_client_list.end(),
                             [&client_id]( uint16_t stored_client_id) {
                             LOG(WARNING) << __func__
                                 << ": client_id: " << client_id
                                 << ", stored_client_id:" << stored_client_id;
                               return stored_client_id == client_id;
                             });
        // check if the client present in the connected client list
        if (iter == dev->connected_client_list.end()) {
           break;
        }

        LOG(WARNING) << __func__
                 << ": discovery_completed: " << dev->discovery_completed
                 << ", notifications_enabled: " << dev->notifications_enabled;

        // check if the discovery is already finished
        // send back the same results to the other client
        if (dev->discovery_completed && dev->notifications_enabled) {
          auto iter = callbacks.find(client_id);
          if (iter != callbacks.end()) {
            LOG(WARNING) << __func__ << ": OnSearchComplete";
            PacsClientCallbacks *callback = iter->second;
            callback->OnSearchComplete(DISCOVER_SUCCESS,
                                       dev->address,
                                       dev->consolidated_sink_pac_records,
                                       dev->consolidated_src_pac_records,
                                       dev->sink_locations,
                                       dev->src_locations,
                                       dev->available_contexts,
                                       dev->supported_contexts);
          }
          break;
        }

        // reset it
        dev->chars_read = 0x00;
        dev->chars_to_be_read = 0x00;
        dev->sink_info.clear();
        dev->src_info.clear();
        dev->consolidated_sink_pac_records.clear();
        dev->consolidated_src_pac_records.clear();
        //TODO
        //btif_bap_remove_record()

        // queue the request to GATT queue module
        GattOpsQueue::ServiceSearch(client_id, dev->conn_id, &PACS_UUID);
      } break;

      default:
        break;
    }
  }

  void GetAudioAvailability(uint16_t client_id, const RawAddress& address) {
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(INFO) << __func__ << ": Device not connected to profile: " << address;
      return;
    }
    LOG(WARNING) << __func__ << ": address: " << address
                             << ", state: " << static_cast<int>(dev->state);

    switch(dev->state) {
      case DevState::CONNECTED: {
        auto iter = std::find_if(dev->connected_client_list.begin(),
                                 dev->connected_client_list.end(),
                             [&client_id]( uint16_t stored_client_id) {
                               return stored_client_id == client_id;
                             });
        // check if the client present in the connected client list
        if (iter == dev->connected_client_list.end()) {
           break;
        }

        // check if the discovery is already finished
        // send back the same results to the other client
        if (dev->discovery_completed && dev->notifications_enabled) {
          auto iter = callbacks.find(client_id);
          if (iter != callbacks.end()) {
            PacsClientCallbacks *callback = iter->second;
            callback->OnAudioContextAvailable(dev->address,
                                              dev->available_contexts);
          }
          break;
        }

        // queue the request to GATT queue module
        GattOpsQueue::ReadCharacteristic(
                 client_id, dev->conn_id, dev->avail_contexts_handle,
                 PacsClientImpl::OnReadAvailableAudioStatic, nullptr);
      } break;
      default:
        LOG(WARNING) << __func__ << ": default";
        break;
    }
  }

  void OnGattConnected(tGATT_STATUS status, uint16_t conn_id,
                       tGATT_IF client_if, RawAddress address,
                       tBTA_TRANSPORT transport, uint16_t mtu) {

    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      /* When device is quickly disabled and enabled in settings, this case
       * might happen */
      LOG(WARNING) << __func__
                   <<": Closing connection to non pacs device, address: "
                   << address;
      return;
    }

    LOG(WARNING) << __func__ << " address: " << address
                             << ", state: " << static_cast<int>(dev->state)
                             << ", status: " << loghex(status)
                             << ", dev->conn_id: " << loghex(dev->conn_id);

    if (dev->state == DevState::CONNECTING) {
      if (status != GATT_SUCCESS) {
        LOG(ERROR) << __func__ <<  ": Failed to connect to PACS device";
        for (auto it = dev->profile_queue.begin();
                             it != dev->profile_queue.end();) {
          if(it->type == ProfleOP::CONNECT) {
            // get the callback and update the upper layers
            auto iter = callbacks.find(it->client_id);
            if (iter != callbacks.end()) {
              PacsClientCallbacks *callback = iter->second;
              callback->OnConnectionState(address,
                                          ConnectionState::DISCONNECTED);
            }
            dev->profile_queue.erase(it);
          } else {
            it++;
          }
        }
        dev->state = DevState::IDLE;
        pacsDevices.Remove(address);
        return;
      }
    } else if (dev->state == DevState::DISCONNECTING) {
      // TODO will this happens ?
      // it could have called the cancel open to expect the
      // open cancelled event
      if (status != GATT_SUCCESS) {
        LOG(ERROR)  << __func__ << ": Failed to connect to PACS device";
        for (auto it = dev->profile_queue.begin();
                             it != dev->profile_queue.end();) {
          if(it->type == ProfleOP::DISCONNECT) {
            // get the callback and update the upper layers
            auto iter = callbacks.find(it->client_id);
            if (iter != callbacks.end()) {
              PacsClientCallbacks *callback = iter->second;
              callback->OnConnectionState(address,
                                          ConnectionState::DISCONNECTED);
            }
            dev->profile_queue.erase(it);
          } else {
            it++;
          }
        }
        dev->state = DevState::IDLE;
        pacsDevices.Remove(address);
        return;
      } else {
        // gatt connected successfully
        // if the disconnect entry is found we need to initiate the
        // gatt disconnect. may be a race condition just after sending
        // cancel open gatt connected event received
        for (auto it = dev->profile_queue.begin();
                             it != dev->profile_queue.end();) {
          if(it->type == ProfleOP::DISCONNECT) {
            // get the callback and update the upper layers
            auto iter = callbacks.find(it->client_id);
            if (iter != callbacks.end()) {
              // Removes all registrations for connection.
              GattOpsQueue::Clean(dev->conn_id);
              BTA_GATTC_Close(dev->conn_id);
              BTA_GATTC_CancelOpen(gatt_client_id, address, false);
              break;
            }
          } else {
            it++;
          }
        }
        return;
      }
    } else {
      // return unconditinally
      return;
    }

    // success scenario code
    dev->conn_id = conn_id;

    tACL_CONN* p_acl = btm_bda_to_acl(address, BT_TRANSPORT_LE);
    if (p_acl != nullptr &&
        controller_get_interface()->supports_ble_2m_phy() &&
        HCI_LE_2M_PHY_SUPPORTED(p_acl->peer_le_features)) {
      LOG(INFO) << __func__
                << ": set preferred PHY to 2M for device: " << address;
      BTM_BleSetPhy(address, PHY_LE_2M, PHY_LE_2M, 0);
    }

    /* verify bond */
    uint8_t sec_flag = 0;
    BTM_GetSecurityFlagsByTransport(address, &sec_flag, BT_TRANSPORT_LE);

    if (sec_flag & BTM_SEC_FLAG_ENCRYPTED) {
      /* if link has been encrypted */
      LOG(WARNING) << __func__
                   << ": link has been encrypted for device: " << address;
      OnEncryptionComplete(address, true);
      return;
    }

#ifdef MIN_ATT_MTU
    if(mtu < ATT_MIN_MTU) {
      BTA_GATTC_ConfigureMTU(conn_id, ATT_MIN_MTU);
    }
#endif

    if (sec_flag & BTM_SEC_FLAG_LKEY_KNOWN) {
      /* if bonded and link not encrypted */
      sec_flag = BTM_BLE_SEC_ENCRYPT;
      LOG(WARNING) << __func__
                   << ": trying to encrypt now for device: " << address;
      BTM_SetEncryption(address, BTA_TRANSPORT_LE, encryption_callback,
                        nullptr, sec_flag);
      return;
    }

    /* otherwise let it go through */
    LOG(WARNING)  << __func__
                  << ": encryption failed for device: " << address;
    OnEncryptionComplete(address, false);
  }

  void OnGattDisconnected(tGATT_STATUS status, uint16_t conn_id,
                          tGATT_IF client_if, RawAddress remote_bda,
                          tBTA_GATT_REASON reason) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(ERROR)  << __func__
                  << ": Skipping unknown device disconnect, conn_id="
                  << loghex(conn_id);
      return;
    }

    LOG(WARNING) << __func__ << " remote_bda: " << remote_bda
                             << ", state: " << static_cast<int>(dev->state)
                             << ", status: " << loghex(status)
                             << ", dev->conn_id: " << loghex(dev->conn_id);

    switch(dev->state) {
      case DevState::CONNECTING: {
        // sudden disconnection
        for (auto it = dev->profile_queue.begin();
                             it != dev->profile_queue.end();) {
          if (it->type == ProfleOP::CONNECT) {
            // get the callback and update the upper layers
            auto iter = callbacks.find(it->client_id);
            if (iter != callbacks.end()) {
              PacsClientCallbacks *callback = iter->second;
              callback->OnConnectionState(remote_bda,
                                          ConnectionState::DISCONNECTED);
            }
            it = dev->profile_queue.erase(it);
          } else {
            it++;
          }
        }
      } break;
      case DevState::CONNECTED: {
        // sudden disconnection
        for (auto it = dev->connected_client_list.begin();
                             it != dev->connected_client_list.end();) {
          // get the callback and update the upper layers
          auto iter = callbacks.find(*it);
          if (iter != callbacks.end()) {
            PacsClientCallbacks *callback = iter->second;
            callback->OnConnectionState(remote_bda,
                                        ConnectionState::DISCONNECTED);
          }
          it = dev->connected_client_list.erase(it);
        }
      } break;
      case DevState::DISCONNECTING: {
        for (auto it = dev->profile_queue.begin();
                             it != dev->profile_queue.end();) {
          if (it->type == ProfleOP::DISCONNECT) {
            // get the callback and update the upper layers
            auto iter = callbacks.find(it->client_id);
            if (iter != callbacks.end()) {
              PacsClientCallbacks *callback = iter->second;
              callback->OnConnectionState(remote_bda,
                                          ConnectionState::DISCONNECTED);
            }
            it = dev->profile_queue.erase(it);
          } else {
            it++;
          }
        }

        for (auto it = dev->connected_client_list.begin();
                             it != dev->connected_client_list.end();) {
          // get the callback and update the upper layers
          it = dev->connected_client_list.erase(it);
        }
        // check if the connection queue is not empty
        // if not initiate the Gatt connection
      } break;
      default:
        break;
    }

    if (dev->conn_id) {
      DeregisterAllPacsNotifcations(gatt_client_id, dev);
      // Removes all registrations for connection.
      GattOpsQueue::Clean(dev->conn_id);
      BTA_GATTC_Close(dev->conn_id);
      BTA_GATTC_CancelOpen(gatt_client_id, remote_bda, false);
      dev->conn_id = 0;
    }

    dev->state = DevState::IDLE;
    pacsDevices.Remove(remote_bda);
  }

  void DeregisterAllPacsNotifcations(uint16_t conn_id,
                                         PacsDevice* dev) {

    LOG(WARNING) << __func__
                 << ": conn_id: " << loghex(conn_id);

    for (auto it = dev->sink_info.begin();
                             it != dev->sink_info.end(); it ++) {
      if (it->sink_pac_handle != 0xFFFF) {
        LOG(WARNING) << __func__
                     << ": Deregister sink pac notif for handle: "
                     << loghex(it->sink_pac_handle);
        DeregisterPacsNotification(conn_id, dev,
                                  it->sink_pac_ccc_handle,
                                  it->sink_pac_handle);
      }
    }

    for (auto it = dev->src_info.begin();
                             it != dev->src_info.end(); it ++) {
      if (it->src_pac_handle != 0xFFFF) {
        LOG(WARNING) << __func__
                     << ": Deregister src pac notif for handle: "
                     << loghex(it->src_pac_handle);
        DeregisterPacsNotification(conn_id, dev,
                                  it->src_pac_ccc_handle,
                                  it->src_pac_handle);
      }
    }

    if (dev->sink_loc_handle != 0xFFFF) {
      LOG(WARNING) << __func__
                     << ": Deregister sink loc for handle: "
                     << loghex(dev->sink_loc_handle);
      DeregisterPacsNotification(conn_id, dev,
                                dev->sink_loc_ccc_handle,
                                dev->sink_loc_handle);
    }

    if (dev->src_loc_handle != 0xFFFF) {
      LOG(WARNING) << __func__
                     << ": Deregister src loc for handle: "
                     << loghex(dev->src_loc_handle);
      DeregisterPacsNotification(conn_id, dev,
                                dev->src_loc_ccc_handle,
                                dev->src_loc_handle);
    }

    if (dev->supp_contexts_handle != 0xFFFF) {
      LOG(WARNING) << __func__
                     << ": Deregister supp contexts for handle: "
                     << loghex(dev->supp_contexts_handle);
      DeregisterPacsNotification(conn_id, dev,
                                dev->supp_contexts_ccc_handle,
                                dev->supp_contexts_handle);
    }

    if (dev->avail_contexts_handle != 0xFFFF) {
      LOG(WARNING) << __func__
                     << ": Deregister avail contexts for handle: "
                     << loghex(dev->avail_contexts_handle);
      DeregisterPacsNotification(conn_id, dev,
                                dev->avail_contexts_ccc_handle,
                                dev->avail_contexts_handle);
    }
  }

  void OnConnectionUpdateComplete(uint16_t conn_id, tBTA_GATTC* p_data) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(ERROR) << __func__
                 << ": Skipping unknown device, conn_id="
                 << loghex(conn_id);
      return;
    }
  }

  void OnEncryptionComplete(const RawAddress& address, bool success) {
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(ERROR) << __func__ << ": Skipping unknown device:" << address;
      return;
    }

    if(dev->state != DevState::CONNECTING) {
      LOG(ERROR) << __func__ << ": received in wrong state: " << address;
      return;
    }

    LOG(WARNING) << __func__ << ": address=" << address
                             << ", Status : " << loghex(success);
    // encryption failed
    if (!success) {
      for (auto it = dev->profile_queue.begin();
                           it != dev->profile_queue.end();) {
        if (it->type == ProfleOP::CONNECT) {
          // get the callback and update the upper layers
          auto iter = callbacks.find(it->client_id);
          if (iter != callbacks.end()) {
            PacsClientCallbacks *callback = iter->second;
            callback->OnConnectionState(address,
                                        ConnectionState::DISCONNECTED);
          }
          // change the type to disconnect
          it->type = ProfleOP::DISCONNECT;
        } else {
          it++;
        }
      }
      dev->state = DevState::DISCONNECTING;
      // Removes all registrations for connection.
      BTA_GATTC_Close(dev->conn_id);
      BTA_GATTC_CancelOpen(gatt_client_id, address, false);
    } else {
      for (auto it = dev->profile_queue.begin();
                           it != dev->profile_queue.end();) {
        if (it->type == ProfleOP::CONNECT) {
          // get the callback and update the upper layers
          auto iter = callbacks.find(it->client_id);
          if (iter != callbacks.end()) {
            dev->connected_client_list.push_back(it->client_id);
            PacsClientCallbacks *callback = iter->second;
            LOG(WARNING) << __func__ << ": calling OnConnectionState";
            callback->OnConnectionState(address, ConnectionState::CONNECTED);
          }
          dev->profile_queue.erase(it);
        } else {
          it++;
        }
      }
      dev->state = DevState::CONNECTED;
    }
  }

  void OnServiceChangeEvent(const RawAddress& address) {
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(ERROR) << __func__ << ": Skipping unknown device: " << address;
      return;
    }
    LOG(INFO) << __func__ << ": address: " << address;
    dev->first_connection = true;
    dev->service_changed_rcvd = true;
    GattOpsQueue::Clean(dev->conn_id);
  }

  void OnServiceDiscDoneEvent(const RawAddress& address) {
    PacsDevice* dev = pacsDevices.FindByAddress(address);
    if (!dev) {
      LOG(ERROR) << __func__ << ": Skipping unknown device: " << address;
      return;
    }

    LOG(WARNING) << __func__ << " service_changed_rcvd: "
                << dev->service_changed_rcvd;
    if (dev->service_changed_rcvd) {
      // queue the request to GATT queue module with dummu client id
      GattOpsQueue::ServiceSearch(0XFF, dev->conn_id, &PACS_UUID);
    }
  }

  void RegisterForNotification(uint16_t client_id, uint16_t conn_id,
                               PacsDevice* dev, uint16_t ccc_handle,
                               uint16_t handle) {
    if(handle && ccc_handle) {
      /* Register and enable Notification */
      tGATT_STATUS register_status;
      register_status = BTA_GATTC_RegisterForNotifications(
          conn_id, dev->address, handle);
      if (register_status != GATT_SUCCESS) {
        LOG(ERROR) << __func__
                   << ": BTA_GATTC_RegisterForNotifications failed, status="
                   << loghex(register_status);
      }
      std::vector<uint8_t> value(2);
      uint8_t* ptr = value.data();
      UINT16_TO_STREAM(ptr, GATT_CHAR_CLIENT_CONFIG_NOTIFICATION);
      GattOpsQueue::WriteDescriptor(
          client_id, conn_id, ccc_handle,
          std::move(value), GATT_WRITE, nullptr, nullptr);
    }
  }

  void DeregisterPacsNotification(uint16_t conn_id, PacsDevice* dev,
                                 uint16_t ccc_handle, uint16_t handle) {
    LOG(ERROR) << __func__
                   << ": handle:" << loghex(handle)
                   << ", ccc_handle: " << loghex(ccc_handle);

    if(handle && ccc_handle) {
      /* Register and enable Notification */
      tGATT_STATUS register_status;
      register_status = BTA_GATTC_DeregisterForNotifications(
                                      conn_id, dev->address, handle);
      if (register_status != GATT_SUCCESS) {
        LOG(ERROR) << __func__
                   << ": BTA_GATTC_DeregisterForNotifications failed, status="
                   << loghex(register_status);
      } else {
        LOG(ERROR) << __func__
                   << ": BTA_GATTC_DeregisterForNotifications success, status="
                   << loghex(register_status);
      }
    }
  }

  void OnServiceSearchComplete(uint16_t conn_id, tGATT_STATUS status) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(ERROR) << __func__ << ": Skipping unknown device, conn_id = "
               << loghex(conn_id);
      return;
    }

    LOG(WARNING) << __func__ << ": conn_id = " << loghex(conn_id);

    uint16_t client_id = GattOpsQueue::ServiceSearchComplete(conn_id,
                                          status);
    LOG(WARNING) << __func__ << ": client_id = " << loghex(client_id);
    auto iter = callbacks.find(client_id);
    if (status != GATT_SUCCESS) {
      /* close connection and report service discovery complete with error */
      LOG(ERROR) << __func__ << ": Service discovery failed";
      if (iter != callbacks.end()) {
        PacsClientCallbacks *callback = iter->second;
        callback->OnSearchComplete(DISCOVER_FAIL,
                                   dev->address,
                                   dev->consolidated_sink_pac_records,
                                   dev->consolidated_src_pac_records,
                                   dev->sink_locations,
                                   dev->src_locations,
                                   dev->available_contexts,
                                   dev->supported_contexts);
      }
      return;
    }

    const std::vector<gatt::Service>* services = BTA_GATTC_GetServices(conn_id);

    const gatt::Service* service = nullptr;
    if (services) {
      for (const gatt::Service& tmp : *services) {
        if (tmp.uuid == Uuid::From16Bit(UUID_SERVCLASS_GATT_SERVER)) {
          LOG(INFO) << __func__ << ": Found UUID_CLASS_GATT_SERVER, handle="
                    << loghex(tmp.handle);
          const gatt::Service* service_changed_service = &tmp;
          find_server_changed_ccc_handle(conn_id, service_changed_service);
        } else if (tmp.uuid == PACS_UUID) {
          LOG(INFO) << __func__ << ": Found PACS service, handle="
                   << loghex(tmp.handle);
          service = &tmp;
        }
      }
    } else {
      LOG(ERROR) << __func__
                 << ": no services found for conn_id: " << loghex(conn_id);
      return;
    }

    if (!service) {
      LOG(ERROR) << __func__ << ": No PACS service found";
      if (iter != callbacks.end()) {
        PacsClientCallbacks *callback = iter->second;
        callback->OnSearchComplete(DISCOVER_FAIL,
                                   dev->address,
                                   dev->consolidated_sink_pac_records,
                                   dev->consolidated_src_pac_records,
                                   dev->sink_locations,
                                   dev->src_locations,
                                   dev->available_contexts,
                                   dev->supported_contexts);
      }
      return;
    }

    for (const gatt::Characteristic& charac : service->characteristics) {
      LOG(INFO) << __func__ << ": uuid: " << charac.uuid;
      if (charac.uuid == PACS_SINK_PAC_UUID) {
        LOG(INFO) << __func__ << ": sink pac uuid found. ";

        SinkPacsData info;
        memset(&info, 0, sizeof(info));
        info.sink_pac_handle = charac.value_handle;
        info.sink_pac_ccc_handle = find_ccc_handle(conn_id, charac.value_handle);
        dev->sink_info.push_back(info);
        dev->chars_to_be_read |= SINK_PAC;
        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

        if (info.sink_pac_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  info.sink_pac_ccc_handle,
                                  info.sink_pac_handle);
        }

      } else if (charac.uuid == PACS_SINK_LOC_UUID) {
        LOG(INFO) << __func__ << ": sink loc uuid found. ";
        dev->sink_loc_handle = charac.value_handle;
        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

        dev->sink_loc_ccc_handle =
            find_ccc_handle(conn_id, charac.value_handle);

        dev->chars_to_be_read |= SINK_LOC;
        if (dev->sink_loc_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  dev->sink_loc_ccc_handle,
                                  dev->sink_loc_handle);
        }

      } else if (charac.uuid == PACS_SRC_PAC_UUID) {
        LOG(INFO) << __func__ << ": src pac uuid found. ";

        SrcPacsData info;
        memset(&info, 0, sizeof(info));
        info.src_pac_handle = charac.value_handle;
        info.src_pac_ccc_handle = find_ccc_handle(conn_id, charac.value_handle);
        dev->src_info.push_back(info);
        dev->chars_to_be_read |= SRC_PAC;
        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

        if (info.src_pac_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  info.src_pac_ccc_handle,
                                  info.src_pac_handle);
        }

      } else if (charac.uuid == PACS_SRC_LOC_UUID) {
        LOG(INFO) << __func__ << ": src loc uuid found. ";
        dev->src_loc_handle = charac.value_handle;

        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

         dev->src_loc_ccc_handle =
             find_ccc_handle(conn_id, charac.value_handle);
        dev->chars_to_be_read |= SRC_LOC;
        if (dev->src_loc_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  dev->src_loc_ccc_handle,
                                  dev->src_loc_handle);
        }

      } else if (charac.uuid == PACS_AVA_AUDIO_UUID) {
        LOG(INFO) << __func__ << ": avaliable audio uuid found. ";
        dev->avail_contexts_handle = charac.value_handle;

        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

         dev->avail_contexts_ccc_handle =
             find_ccc_handle(conn_id, charac.value_handle);
        dev->chars_to_be_read |= AVAIL_CONTEXTS;
        if (dev->avail_contexts_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  dev->avail_contexts_ccc_handle,
                                  dev->avail_contexts_handle);
        }

      } else if (charac.uuid == PACS_SUP_AUDIO_UUID) {
        LOG(INFO) << __func__ << ": supported audio uuid found. ";
        dev->supp_contexts_handle = charac.value_handle;

        GattOpsQueue::ReadCharacteristic(
                 client_id, conn_id, charac.value_handle,
                 PacsClientImpl::OnReadOnlyPropertiesReadStatic, nullptr);

        dev->supp_contexts_ccc_handle =
            find_ccc_handle(conn_id, charac.value_handle);
        dev->chars_to_be_read |= SUPP_CONTEXTS;
        if (dev->supp_contexts_ccc_handle) {
          RegisterForNotification(client_id, conn_id, dev,
                                  dev->supp_contexts_ccc_handle,
                                  dev->supp_contexts_handle);
        }
      } else {
         LOG(WARNING) << "Unknown characteristic found:" << charac.uuid;
      }
    }

    dev->notifications_enabled = true;

    LOG(INFO) << __func__
              << ": service_changed_rcvd: " << dev->service_changed_rcvd;
    if (dev->service_changed_rcvd) {
      dev->service_changed_rcvd = false;
    }
  }

  void OnNotificationEvent(uint16_t conn_id, uint16_t handle, uint16_t len,
                           uint8_t* value) {

    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(INFO) << __func__
                << ": Skipping unknown device, conn_id=" << loghex(conn_id);
      return;
    }

    LOG(WARNING) << __func__ << ": conn_id: " << loghex(conn_id);

    if(dev->avail_contexts_handle == handle) {
      uint8_t* p = value;
      STREAM_TO_UINT32(dev->available_contexts, p);
    }
  }

  void OnCongestionEvent(uint16_t conn_id, bool congested) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(INFO) << __func__
                << ": Skipping unknown device, conn_id=" << loghex(conn_id);
      return;
    }

    LOG(WARNING) << __func__ << ": conn_id=" << loghex(conn_id)
                             << ", congested: " << congested;
    dev->is_congested = congested;
    GattOpsQueue::CongestionCallback(conn_id, congested);
  }

  void OnReadAvailableAudio(uint16_t client_id,
                                uint16_t conn_id, tGATT_STATUS status,
                                uint16_t handle, uint16_t len, uint8_t* value,
                                void* data) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    if (!dev) {
      LOG(ERROR) << __func__ << ": unknown conn_id: " << loghex(conn_id);
      return;
    }

    LOG(WARNING) << __func__ << ": conn_id: " << loghex(conn_id);

    if(dev->avail_contexts_handle == handle) {
      uint8_t* p = value;
      STREAM_TO_UINT32(dev->available_contexts, p);
      // check if all pacs characteristics are read
      // send out the callback as service discovery completed
      // get the callback and update the upper layers
      auto iter = callbacks.find(client_id);
      if (iter != callbacks.end()) {
        PacsClientCallbacks *callback = iter->second;
        callback->OnAudioContextAvailable(dev->address,
                                          dev->available_contexts);
      }
    }
  }

  bool IsRecordReadable(uint16_t total_len, uint16_t processed_len,
                        uint16_t req_len) {
    LOG(WARNING) << __func__ << ": processed_len: " << loghex(processed_len)
                             << ", req_len: " << loghex(req_len);
    if((total_len > processed_len) &&
       ((total_len - processed_len) >= req_len)) {
      return true;
    } else {
      return false;
    }
  }

  bool IsLtvValid(uint8_t ltv_type, uint16_t ltv_len, CodecIndex codec) {
    bool valid = true;
    for (auto it : ltv_info[codec]) {
      LOG(INFO) << __func__ << ": ltv_type: " << loghex(ltv_type)
                            << ": it.first : " << loghex(it.first);
      LOG(INFO) << __func__ << ": ltv_len: " << ltv_len
                            << ": it.second : " << loghex(it.second);
      if(ltv_type == it.first &&
         ltv_len != it.second) {
        LOG(INFO) << __func__ << ": ltv length not matched ";
        valid = false;
        break;
      }
      LOG(INFO) << __func__ << ": ltv length matched ";
    }
    return valid;
  }

  void parse_LC3Q(uint8_t  **p, std::vector<CodecConfig>& pac_records,
                  CodecIndex codec_type, uint16_t context_type,
                  uint16_t total_len, uint16_t& processed_len,
                  bool& stop_reading, uint8_t& num_pac_recs, PacsDevice *dev,
                  uint16_t handle, SinkPacsData* sinkinfo, SrcPacsData* srcinfo) {

    uint8_t codec_cap_len;
    std::vector<uint8_t> codec_caps;
    uint8_t meta_data_len;
    std::vector<uint8_t> meta_data;

    // codec_cap_len is of 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid codec id, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(codec_cap_len, *p);
    processed_len ++;

    LOG(WARNING) << __func__
                 << ": codec_cap_len: " << loghex(codec_cap_len)
                 << ", processed_len: " <<  loghex(processed_len);

    if (!codec_cap_len) {
      LOG(ERROR) << __func__
                 << ": Invalid codec cap len";
      return;
    }

    if (!IsRecordReadable(total_len, processed_len, codec_cap_len)) {
      LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
      return;
    }

    codec_caps.resize(codec_cap_len);
    STREAM_TO_ARRAY(codec_caps.data(), *p, codec_cap_len);
    uint8_t len = codec_cap_len;
    uint8_t *pp = codec_caps.data();

    // Now look for supported freq LTV entry
    while (len) {
      LOG(WARNING) << __func__ << ": len: " << loghex(len);

      if (!IsRecordReadable(total_len, processed_len, 1)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        break;
      }
      uint8_t ltv_len = *pp++;
      len--;
      processed_len++;

      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
      if (!ltv_len ||
          !IsRecordReadable(total_len, processed_len, ltv_len)) {
        LOG(ERROR) << __func__ << ": Not valid ltv length";
        stop_reading = true;
        break;
      }

      processed_len += ltv_len;

      // get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if(!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_LC3)) {
        LOG(ERROR) << __func__ << ": No ltv type to length match";
        stop_reading = true;
        break;
      }

      if(ltv_type == LTV_TYPE_SUP_FREQS) {
        uint16_t supp_freqs;
        STREAM_TO_UINT16(supp_freqs, pp);
        LOG(WARNING) << __func__ << ": supp_freqs: " << supp_freqs;
        LOG(INFO) << __func__ << " ltv_type is LTV_TYPE_SUP_FREQS ";
        for (auto it : freq_map) {
          if(supp_freqs & it.first) {
            CodecConfig codec_config;
            codec_config.codec_type = codec_type;
            codec_config.sample_rate = it.second;
            pac_records.push_back(codec_config);
          }
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++; };
      }

      if (len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << "wrong len";
        len = 0;
      }
    }

    LOG(WARNING) << __func__ << ": stop_reading: " << stop_reading;
    if (stop_reading) return;

    // set the default chnl count to mono as it is optional
    for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
      it->channel_mode = CodecChannelMode::CODEC_CHANNEL_MODE_MONO;
    }

    // now check for other LTV values
    len = codec_cap_len;
    pp = codec_caps.data();
    while (len) {
      LOG(WARNING) << __func__
                   << ": checking other LTV values,len: " << loghex(len);
      uint8_t ltv_len = *pp++;
      len--;
      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);

      //get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if(ltv_type == LTV_TYPE_SUP_FRAME_DUR) {
        uint8_t supp_frames;
        STREAM_TO_UINT8(supp_frames, pp);
        LOG(WARNING) << __func__
                     << ": pac rec len: " << loghex(pac_records.size());
        for (auto it = pac_records.begin(); it != pac_records.end();
                                            ++it) {
          UpdateCapaSupFrameDurations(&(*it), supp_frames);
        }
      } else if (ltv_type == LTV_TYPE_CHNL_COUNTS) {
        uint8_t chnl_allocation;
        STREAM_TO_UINT8(chnl_allocation, pp);
        LOG(INFO) << __func__ << ": chnl_allocation = " << loghex(chnl_allocation);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          it->channel_mode =
                     static_cast<CodecChannelMode> (chnl_allocation);
        }
      } else if (ltv_type == LTV_TYPE_OCTS_PER_FRAME) {
        uint32_t octs_per_frame;
        STREAM_TO_UINT32(octs_per_frame, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          UpdateCapaSupOctsPerFrame(&(*it), octs_per_frame);
        }
      } else if (ltv_type == LTV_TYPE_MAX_SUP_FRAMES_PER_SDU) {
        uint32_t max_sup_frames_per_sdu;
        STREAM_TO_UINT32(max_sup_frames_per_sdu, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          UpdateCapaMaxSupLc3Frames(&(*it), max_sup_frames_per_sdu);
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++;};
      }

      if(len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << ": wrong len";
        len = 0;
      }
    }

    //Meta data length 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid meta data len, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(meta_data_len, *p);
    processed_len ++;
    LOG(WARNING) << __func__ << ": meta_data_len: " << loghex(meta_data_len)
                             << ": processed_len: " << loghex(processed_len);

    if (meta_data_len) {
      if (!IsRecordReadable(total_len, processed_len, meta_data_len)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        return;
      }

      meta_data.resize(meta_data_len);
      STREAM_TO_ARRAY(meta_data.data(), *p, meta_data_len);
      uint8_t len = meta_data_len;
      uint8_t *pp = meta_data.data();

      while (len) {
        LOG(WARNING) << __func__ << ": len: " << loghex(len);
        uint8_t ltv_len = *pp++;
        len--;
        processed_len++;

        LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
        if (!ltv_len ||
            !IsRecordReadable(total_len, processed_len, ltv_len)) {
          LOG(ERROR) << __func__ << ": Not valid ltv length";
          stop_reading = true;
          break;
        }

        processed_len += ltv_len;

        // get type and value
        uint8_t ltv_type = *pp++;

        LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);

        if (!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_LC3)) {
          LOG(ERROR) << __func__ << ": No ltv type to length match";
          stop_reading = true;
          break;
        }

        if (ltv_type == LTV_TYPE_PREF_AUD_CONTEXT) {
          STREAM_TO_UINT16(context_type, pp);
          LOG(WARNING) << __func__
                       << ": ltv_context_type: " << loghex(context_type);

          for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
             UpdateCapaPreferredContexts(&(*it), context_type);
          }
        } else if (ltv_type == LTV_TYPE_VS_META_DATA) {
          // init enc and dec version as 0x1 as at least LC3Q is guarenteed
          uint8_t encoder_version = 0x1, decoder_version = 0x1;
          uint16_t company_id;
          STREAM_TO_UINT16(company_id, pp);

          //total vs meta data length(meta length -4) in bytes.
          uint8_t total_vendor_ltv_len = meta_data_len - 4;
          LOG(WARNING) << __func__
                       << ": total_vendor_ltv_len: " << loghex(total_vendor_ltv_len);

          if (company_id == QTI_ID) {
            while (total_vendor_ltv_len) {
              uint8_t vs_meta_data_len = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_len: " << loghex(vs_meta_data_len);

              // get type and value
              uint8_t vs_meta_data_type = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_type: " << loghex(vs_meta_data_type);

              if (vs_meta_data_type == LTV_TYPE_VS_META_DATA_LC3Q) {
                uint8_t vs_meta_data_value[vs_meta_data_len - 1];
                STREAM_TO_ARRAY(&vs_meta_data_value, pp,
                                static_cast<int> (sizeof(vs_meta_data_value)));
                encoder_version = vs_meta_data_value[0];
                if (vs_meta_data_value[1] != 0)
                  decoder_version = vs_meta_data_value[1];
                LOG(WARNING) << __func__
                             << ": peer PAC encoder version: " << loghex(encoder_version)
                             << ": peer PAC decoder version: " << loghex(decoder_version);
                for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
                  UpdateCapaVendorMetaDataLc3QPref(&(*it), true);
                  UpdateCapaVendorMetaDataCodecEncoderVer(&(*it), encoder_version);
                  UpdateCapaVendorMetaDataCodecDecoderVer(&(*it), decoder_version);
                }
              } else {
                //TODO check for other ltvs
                uint8_t rem_len = vs_meta_data_len - 1;
                LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
                while (rem_len--) { pp++;};
              }

              /* 5bytes (VS length bypte + Meta datatype +
                         company ID(2 bytes) + Lc3q length) */
              if(total_vendor_ltv_len >= (vs_meta_data_len + 5)) {
                total_vendor_ltv_len -= (vs_meta_data_len + 5);
                len = total_vendor_ltv_len;
              } else {
                LOG(ERROR) << __func__ << ": wrong len.";
                total_vendor_ltv_len = 0;
              }
            }
          } else {
            //TODO check for other comany IDs
            uint8_t rem_len = total_vendor_ltv_len - 1;
            LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
            while (rem_len--) { pp++;};
          }
        } else {
          uint8_t rem_len = ltv_len - 1;
          LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
          while (rem_len--) { pp++;};
        }

        if (len >= ltv_len) {
          len -= ltv_len;
        } else {
          LOG(ERROR) << __func__ << ": wrong len";
          len = 0;
        }
      }
    }

    if (sinkinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        sinkinfo->sink_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SINK,
                            &record);
        LOG(INFO) << __func__ << " btif_bap_add_record() executed for sinkinfo ";
      }
    } else if (srcinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        srcinfo->src_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SRC,
                            &record);
        LOG(INFO) << __func__ << " btif_bap_add_record() executed for srcinfo ";
      }
    }
    num_pac_recs--;

    return;
  }

  void parse_AAR3(uint8_t  *p, std::vector<CodecConfig>& pac_records,
                  CodecIndex codec_type, uint16_t context_type,
                  uint16_t total_len, uint16_t processed_len,
                  bool& stop_reading, uint8_t& num_pac_recs, PacsDevice *dev,
                  uint16_t handle, SinkPacsData* sinkinfo, SrcPacsData* srcinfo) {

    uint8_t codec_cap_len;
    std::vector<uint8_t> codec_caps;
    uint8_t meta_data_len;
    std::vector<uint8_t> meta_data;

    //codec_cap_len is of 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid codec id, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(codec_cap_len, p);
    processed_len ++;

    LOG(WARNING) << __func__
                 << ": codec_cap_len: " << loghex(codec_cap_len)
                 << ": processed_len: " <<  loghex(processed_len);

    if (!codec_cap_len) {
      LOG(ERROR) << __func__
                 << ": Invalid codec cap len";
      return;
    }

    if (!IsRecordReadable(total_len, processed_len, codec_cap_len)) {
      LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
      return;
    }

    codec_caps.resize(codec_cap_len);
    STREAM_TO_ARRAY(codec_caps.data(), p, codec_cap_len);
    uint8_t len = codec_cap_len;
    uint8_t *pp = codec_caps.data();

    //Now look for supported freq LTV entry
    while (len) {
      LOG(WARNING) << __func__ << ": len: " << loghex(len);

      if (!IsRecordReadable(total_len, processed_len, 1)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        break;
      }
      uint8_t ltv_len = *pp++;
      len--;
      processed_len++;

      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
      if (!ltv_len ||
          !IsRecordReadable(total_len, processed_len, ltv_len)) {
        LOG(ERROR) << __func__ << ": Not valid ltv length";
        stop_reading = true;
        break;
      }

      processed_len += ltv_len;

      //get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if(!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE)) {
        LOG(ERROR) << __func__ << ": No ltv type to length match";
        stop_reading = true;
        break;
      }

      if (ltv_type == LTV_TYPE_SUP_FREQS_AAR3) {
        uint16_t supp_freqs;
        STREAM_TO_UINT16(supp_freqs, pp);
        LOG(WARNING) << __func__ << ": supp_freqs: " << supp_freqs;
        LOG(INFO) << __func__ << " ltv_type is LTV_TYPE_SUP_FREQS_AAR3";
        for (auto it : freq_map) {
          if(supp_freqs & it.first) {
            CodecConfig codec_config;
            codec_config.codec_type = codec_type;
            codec_config.sample_rate = it.second;
            pac_records.push_back(codec_config);
          }
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++; };
      }

      if (len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << "wrong len";
        len = 0;
      }
    }

    LOG(WARNING) << __func__ << ": stop_reading: " << stop_reading;
    if (stop_reading) return;

    //set the default chnl count to mono as it is optional
    for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
      it->channel_mode = CodecChannelMode::CODEC_CHANNEL_MODE_MONO;
    }

    //now check for other LTV values
    len = codec_cap_len;
    pp = codec_caps.data();
    while (len) {
      LOG(WARNING) << __func__
                   << ": checking other LTV values,len: " << loghex(len);
      uint8_t ltv_len = *pp++;
      len--;
      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);

      //get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if (ltv_type == LTV_TYPE_CHNL_COUNTS_AAR3) {
        uint8_t chnl_allocation;
        STREAM_TO_UINT8(chnl_allocation, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          it->channel_mode =
                     static_cast<CodecChannelMode> (chnl_allocation);
        }
      } else if (ltv_type == LTV_TYPE_OCTS_PER_FRAME_AAR3) {
        uint32_t octs_per_frame;
        STREAM_TO_UINT32(octs_per_frame, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          UpdateCapaSupOctsPerFrame(&(*it), octs_per_frame);
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++;};
      }

      if(len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << ": wrong len";
        len = 0;
      }
    }

    //Meta data length 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid meta data len, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(meta_data_len, p);
    processed_len ++;
    LOG(WARNING) << __func__ << ": meta_data_len: " << loghex(meta_data_len)
                             << ": processed_len: " << loghex(processed_len);

    if (meta_data_len) {
      if (!IsRecordReadable(total_len, processed_len, meta_data_len)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        return;
      }

      meta_data.resize(meta_data_len);
      STREAM_TO_ARRAY(meta_data.data(), p, meta_data_len);
      uint8_t len = meta_data_len;
      uint8_t *pp = meta_data.data();

      while (len) {
        LOG(WARNING) << __func__ << ": len: " << loghex(len);
        uint8_t ltv_len = *pp++;
        len--;
        processed_len++;

        LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
        if (!ltv_len ||
            !IsRecordReadable(total_len, processed_len, ltv_len)) {
          LOG(ERROR) << __func__ << ": Not valid ltv length";
          stop_reading = true;
          break;
        }

        processed_len += ltv_len;

        //get type and value
        uint8_t ltv_type = *pp++;

        LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);

        if (!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE)) {
          LOG(ERROR) << __func__ << ": No ltv type to length match";
          stop_reading = true;
          break;
        }

        if (ltv_type == LTV_TYPE_PREF_AUD_CONTEXT) {
          STREAM_TO_UINT16(context_type, pp);
          LOG(WARNING) << __func__
                       << ": ltv_context_type: " << loghex(context_type);

          for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
             UpdateCapaPreferredContexts(&(*it), context_type);
          }
        } else if (ltv_type == LTV_TYPE_VS_META_DATA) {
          uint16_t company_id;
          STREAM_TO_UINT16(company_id, pp);

          //total vs meta data length(meta length -4) in bytes.
          uint8_t total_vendor_ltv_len = meta_data_len - 4;
          LOG(WARNING) << __func__
                       << ": total_vendor_ltv_len: " << loghex(total_vendor_ltv_len);

          if (company_id == QTI_ID) {
            while (total_vendor_ltv_len) {
              uint8_t vs_meta_data_len = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_len: " << loghex(vs_meta_data_len);

              //get type and value
              uint8_t vs_meta_data_type = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_type: " << loghex(vs_meta_data_type);

              if (vs_meta_data_type == LTV_TYPE_VS_META_DATA_AAR3) {
                uint8_t vs_meta_data_value[vs_meta_data_len - 1];
                STREAM_TO_ARRAY(&vs_meta_data_value, pp,
                                static_cast<int> (sizeof(vs_meta_data_value)));

                for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
                  UpdateCapaVendorCodecVerAptx(&(*it), vs_meta_data_value[1]);
                  LOG(WARNING) << __func__ << " vs_meta_data_value[1]: "
                                           << loghex(vs_meta_data_value[1]);
                  UpdateCapaMinSupFrameDur(&(*it), vs_meta_data_value[2]);
                  UpdateCapaFeatureMap(&(*it), vs_meta_data_value[3]);
                }
              }  else {
                //TODO check for other ltvs
                uint8_t rem_len = vs_meta_data_len - 1;
                LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
                while (rem_len--) { pp++;};
              }

              /* 5bytes (VS length bypte + Meta datatype +
                         company ID(2 bytes) + Lc3q length) */
              if(total_vendor_ltv_len >= (vs_meta_data_len + 5)) {
                total_vendor_ltv_len -= (vs_meta_data_len + 5);
                len = total_vendor_ltv_len;
              } else {
                LOG(ERROR) << __func__ << ": wrong len.";
                total_vendor_ltv_len = 0;
              }
            }
          } else {
            //TODO check for other comany IDs
            uint8_t rem_len = total_vendor_ltv_len - 1;
            LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
            while (rem_len--) { pp++;};
          }
        } else {
          uint8_t rem_len = ltv_len - 1;
          LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
          while (rem_len--) { pp++;};
        }

        if (len >= ltv_len) {
          len -= ltv_len;
        } else {
          LOG(ERROR) << __func__ << ": wrong len";
          len = 0;
        }
      }
    }

    if (sinkinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        sinkinfo->sink_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SINK,
                            &record);
      }
    } else if (srcinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        srcinfo->src_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SRC,
                            &record);
      }
    }
    num_pac_recs--;

    return;
  }

  void parse_AAR4(uint8_t  *p, std::vector<CodecConfig>& pac_records,
                  CodecIndex codec_type, uint16_t context_type,
                  uint16_t total_len, uint16_t processed_len,
                  bool& stop_reading, uint8_t& num_pac_recs, PacsDevice *dev,
                  uint16_t handle, SinkPacsData* sinkinfo, SrcPacsData* srcinfo) {

    uint8_t codec_cap_len;
    std::vector<uint8_t> codec_caps;
    uint8_t meta_data_len;
    std::vector<uint8_t> meta_data;

    //codec_cap_len is of 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid codec id, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(codec_cap_len, p);
    processed_len ++;

    LOG(WARNING) << __func__
                 << ": codec_cap_len: " << loghex(codec_cap_len)
                 << ": processed_len: " <<  loghex(processed_len);

    if (!codec_cap_len) {
      LOG(ERROR) << __func__
                 << ": Invalid codec cap len";
      return;
    }

    if (!IsRecordReadable(total_len, processed_len, codec_cap_len)) {
      LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
      return;
    }

    codec_caps.resize(codec_cap_len);
    STREAM_TO_ARRAY(codec_caps.data(), p, codec_cap_len);
    uint8_t len = codec_cap_len;
    uint8_t *pp = codec_caps.data();

    //Now look for supported freq LTV entry
    while (len) {
      LOG(WARNING) << __func__ << ": len: " << loghex(len);

      if (!IsRecordReadable(total_len, processed_len, 1)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        break;
      }
      uint8_t ltv_len = *pp++;
      len--;
      processed_len++;

      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
      if (!ltv_len ||
          !IsRecordReadable(total_len, processed_len, ltv_len)) {
        LOG(ERROR) << __func__ << ": Not valid ltv length";
        stop_reading = true;
        break;
      }

      processed_len += ltv_len;

      //get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if(!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4)) {
        LOG(ERROR) << __func__ << ": No ltv type to length match";
        stop_reading = true;
        break;
      }

      if (ltv_type == LTV_TYPE_SUP_FREQS_AAR4) {
        uint16_t supp_freqs;
        STREAM_TO_UINT16(supp_freqs, pp);
        LOG(WARNING) << __func__ << ": supp_freqs: " << supp_freqs;
        LOG(INFO) << __func__ << " (vs_codec_id == VS_CODEC_ID_AAR4 && ltv_type == LTV_TYPE_SUP_FREQS_AAR4) is true ";
        for (auto it : freq_map) {
          if(supp_freqs & it.first) {
            CodecConfig codec_config;
            codec_config.codec_type = codec_type;
            codec_config.sample_rate = it.second;
            pac_records.push_back(codec_config);
          }
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++; };
      }

      if (len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << "wrong len";
        len = 0;
      }
    }

    LOG(WARNING) << __func__ << ": stop_reading: " << stop_reading;
    if (stop_reading) return;

    //now check for other LTV values
    len = codec_cap_len;
    pp = codec_caps.data();
    while (len) {
      LOG(WARNING) << __func__
                   << ": checking other LTV values,len: " << loghex(len);
      uint8_t ltv_len = *pp++;
      len--;
      LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);

      //get type and value
      uint8_t ltv_type = *pp++;
      LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);
      if (ltv_type == LTV_TYPE_SUP_FRAME_DUR_AAR4) {
        uint8_t supp_frames;
        STREAM_TO_UINT8(supp_frames, pp);
        LOG(WARNING) << __func__
                     << ": pac rec len: " << loghex(pac_records.size());
        for (auto it = pac_records.begin(); it != pac_records.end();
                                            ++it) {
          UpdateCapaSupFrameDurations(&(*it), supp_frames);
        }
      } else if (ltv_type == LTV_TYPE_CHNL_COUNTS_AAR4) {
        uint8_t chnl_allocation;
        STREAM_TO_UINT8(chnl_allocation, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          it->channel_mode =
                     static_cast<CodecChannelMode> (chnl_allocation);
        }
      } else if (ltv_type == LTV_TYPE_OCTS_PER_FRAME_AAR4) {
        uint32_t octs_per_frame;
        STREAM_TO_UINT32(octs_per_frame, pp);
        for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
          UpdateCapaSupOctsPerFrame(&(*it), octs_per_frame);
        }
      } else {
        uint8_t rem_len = ltv_len - 1;
        LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
        while (rem_len--) { pp++;};
      }

      if(len >= ltv_len) {
        len -= ltv_len;
      } else {
        LOG(ERROR) << __func__ << ": wrong len";
        len = 0;
      }
    }

    //Meta data length 1 byte
    if (!IsRecordReadable(total_len, processed_len, 1)) {
      LOG(ERROR) << __func__ << ": Not valid meta data len, Bad pacs record.";
      return;
    }

    STREAM_TO_UINT8(meta_data_len, p);
    processed_len ++;
    LOG(WARNING) << __func__ << ": meta_data_len: " << loghex(meta_data_len)
                             << ": processed_len: " << loghex(processed_len);

    if (meta_data_len) {
      if (!IsRecordReadable(total_len, processed_len, meta_data_len)) {
        LOG(ERROR) << __func__ << ": not enough data, Bad pacs record.";
        return;
      }

      meta_data.resize(meta_data_len);
      STREAM_TO_ARRAY(meta_data.data(), p, meta_data_len);
      uint8_t len = meta_data_len;
      uint8_t *pp = meta_data.data();

      while (len) {
        LOG(WARNING) << __func__ << ": len: " << loghex(len);
        uint8_t ltv_len = *pp++;
        len--;
        processed_len++;

        LOG(WARNING) << __func__ << ": ltv_len: " << loghex(ltv_len);
        if (!ltv_len ||
            !IsRecordReadable(total_len, processed_len, ltv_len)) {
          LOG(ERROR) << __func__ << ": Not valid ltv length";
          stop_reading = true;
          break;
        }

        processed_len += ltv_len;

        //get type and value
        uint8_t ltv_type = *pp++;

        LOG(WARNING) << __func__ << ": ltv_type: " << loghex(ltv_type);

        if (!IsLtvValid(ltv_type, ltv_len, CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4)) {
          LOG(ERROR) << __func__ << ": No ltv type to length match";
          stop_reading = true;
          break;
        }

        if (ltv_type == LTV_TYPE_PREF_AUD_CONTEXT) {
          STREAM_TO_UINT16(context_type, pp);
          LOG(WARNING) << __func__
                       << ": ltv_context_type: " << loghex(context_type);

          for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
             UpdateCapaPreferredContexts(&(*it), context_type);
          }
        } else if (ltv_type == LTV_TYPE_VS_META_DATA) {
          uint16_t company_id;
          STREAM_TO_UINT16(company_id, pp);

          //total vs meta data length(meta length -4) in bytes.
          uint8_t total_vendor_ltv_len = meta_data_len - 4;
          LOG(WARNING) << __func__
                       << ": total_vendor_ltv_len: " << loghex(total_vendor_ltv_len);

          if (company_id == QTI_ID) {
            while (total_vendor_ltv_len) {
              uint8_t vs_meta_data_len = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_len: " << loghex(vs_meta_data_len);

              //get type and value
              uint8_t vs_meta_data_type = *pp++;
              LOG(WARNING) << __func__
                           << ": vs_meta_data_type: " << loghex(vs_meta_data_type);

              if (vs_meta_data_type == LTV_TYPE_VS_META_DATA_AAR4) {
                uint8_t vs_meta_data_value[vs_meta_data_len - 1];
                STREAM_TO_ARRAY(&vs_meta_data_value, pp,
                                static_cast<int> (sizeof(vs_meta_data_value)));

                for (auto it = pac_records.begin(); it != pac_records.end(); ++it) {
                  UpdateCapaVendorMetaDataCodecEncoderVer(&(*it), vs_meta_data_value[0]);
                  UpdateCapaVendorMetaDataCodecDecoderVer(&(*it), vs_meta_data_value[1]);
                  UpdateCapaMinSupFrameDur(&(*it), vs_meta_data_value[2]);
                  UpdateCapaFeatureMap(&(*it), vs_meta_data_value[3]);
                }
              }  else {
                //TODO check for other ltvs
                uint8_t rem_len = vs_meta_data_len - 1;
                LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
                while (rem_len--) { pp++;};
              }

              /* 5bytes (VS length bypte + Meta datatype +
                         company ID(2 bytes) + Lc3q length) */
              if(total_vendor_ltv_len >= (vs_meta_data_len + 5)) {
                total_vendor_ltv_len -= (vs_meta_data_len + 5);
                len = total_vendor_ltv_len;
              } else {
                LOG(ERROR) << __func__ << ": wrong len.";
                total_vendor_ltv_len = 0;
              }
            }
          } else {
            //TODO check for other comany IDs
            uint8_t rem_len = total_vendor_ltv_len - 1;
            LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
            while (rem_len--) { pp++;};
          }
        } else {
          uint8_t rem_len = ltv_len - 1;
          LOG(WARNING) << __func__ << ": rem_len: " << loghex(rem_len);
          while (rem_len--) { pp++;};
        }

        if (len >= ltv_len) {
          len -= ltv_len;
        } else {
          LOG(ERROR) << __func__ << ": wrong len";
          len = 0;
        }
      }
    }

    if (sinkinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        sinkinfo->sink_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SINK,
                            &record);
      }
    } else if (srcinfo != nullptr) {
      // Now update all records to conf file
      while (!pac_records.empty()) {
        CodecConfig record = pac_records.back();
        srcinfo->src_pac_records.push_back(record);
        pac_records.pop_back();
        btif_bap_add_record(dev->address, REC_TYPE_CAPABILITY,
                            context_type, CodecDirection::CODEC_DIR_SRC,
                            &record);
      }
    }
    num_pac_recs--;

    return;
  }

  void ParsePacRecord (PacsDevice *dev, uint16_t handle, uint16_t total_len,
                       uint8_t *value, void* data) {
    std::vector<CodecConfig> pac_records;
    CodecIndex codec_type;
    uint8_t *p = value;
    codec_type_t codec_id;
    uint16_t vs_codec_id;
    bool stop_reading = false;
    std::vector<uint8_t> codec_caps;
    std::vector<uint8_t> meta_data;
    uint16_t processed_len = 0;
    uint8_t num_pac_recs = 0;
    uint16_t context_type;

    SinkPacsData* sinkinfo = FindSinkByHandle(dev, handle);
    SrcPacsData* srcinfo = FindSrcByHandle(dev, handle);

    ltv_info[CodecIndex::CODEC_INDEX_SOURCE_LC3] = ltv_info_LC3Q;
    ltv_info[CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE] = ltv_info_AAR3;

    // Number_of_PAC_records is 1 byte
    if (!total_len) {
      LOG(ERROR) << __func__  << ": zero len record ";
    } else {
      STREAM_TO_UINT8(num_pac_recs, p);
      processed_len++;
    }

    LOG(WARNING) << __func__ << ": num_pac_recs: " << loghex(num_pac_recs)
                             << ", total_len: " << loghex(total_len);
    while (total_len && !stop_reading && num_pac_recs) {
      // reset context type for before reading record
      context_type = ucast::CONTENT_TYPE_UNSPECIFIED;
      // read the complete record
      // codec_id is of 5 bytes.
      if (!IsRecordReadable(total_len, processed_len, sizeof(codec_id))) {
        LOG(ERROR) << __func__ << ": Not valid codec id, Bad pacs record.";
        break;
      }

      STREAM_TO_ARRAY(&codec_id, p, static_cast<int> (sizeof(codec_id)));

      processed_len += static_cast<int> (sizeof(codec_id));

      vs_codec_id = (codec_id[3] & 0xFFFF) | ((codec_id[4] & 0xFFFF) << 8);  //octet 3 and 4 has vs codec id

      switch (codec_id[0]) {
        case CODEC_ID_LC3: {
          LOG(INFO) << __func__ << ": LC3 codec and stop_reading = " << stop_reading;
          codec_type = CodecIndex::CODEC_INDEX_SOURCE_LC3;
          parse_LC3Q(&p, pac_records, codec_type, context_type, total_len,
                     processed_len, stop_reading, num_pac_recs, dev, handle, sinkinfo, srcinfo);
          //*p = *p + processed_len;
          break;
        }
        case CODEC_ID_VS: {
          LOG(INFO) << __func__ << ": Vendor codec ";
          switch (vs_codec_id) {
            case VS_CODEC_ID_AAR3: {
              LOG(INFO) << __func__ << "AAR3 codec";
              codec_type = CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_LE;
              parse_AAR3(p, pac_records, codec_type, context_type, total_len,
                         processed_len, stop_reading, num_pac_recs, dev, handle, sinkinfo, srcinfo);
              break;
            }
            case VS_CODEC_ID_AAR4: {
              LOG(INFO) << __func__ << "AAR4 codec";
              codec_type = CodecIndex::CODEC_INDEX_SOURCE_APTX_ADAPTIVE_R4;
              parse_AAR4(p, pac_records, codec_type, context_type, total_len,
                         processed_len, stop_reading, num_pac_recs, dev, handle, sinkinfo, srcinfo);
              break;
            }
          }
          break;
        }
        default:
          break;
      }
    }

    if (sinkinfo != nullptr) {
      sinkinfo->read_sink_pac_record = true;
      bool all_sink_pacs_read = false;
      for (auto it = dev->sink_info.begin();
                             it != dev->sink_info.end(); it ++) {
        if (it->read_sink_pac_record == true) {
          all_sink_pacs_read = true;
          continue;
        } else {
          all_sink_pacs_read = false;
          break;
        }
      }

      LOG(WARNING) << __func__
                   << ": all_sink_pacs_read: " << all_sink_pacs_read;
      if (all_sink_pacs_read)
        dev->chars_read |= SINK_PAC;

    } else if (srcinfo != nullptr) {
      srcinfo->read_src_pac_record = true;
      bool all_source_pacs_read = false;
      for (auto it = dev->src_info.begin();
                             it != dev->src_info.end(); it ++) {
        if (it->read_src_pac_record == true) {
          all_source_pacs_read = true;
          continue;
        } else {
          all_source_pacs_read = false;
          break;
        }
      }

      LOG(WARNING) << __func__
                   << ": all_source_pacs_read: " << all_source_pacs_read;
      if (all_source_pacs_read)
        dev->chars_read |= SRC_PAC;
    }
  }

  void OnReadOnlyPropertiesRead(uint16_t client_id, uint16_t conn_id,
                                tGATT_STATUS status, uint16_t handle,
                                uint16_t len, uint8_t *value, void* data) {
    PacsDevice* dev = pacsDevices.FindByConnId(conn_id);
    DeviceType sink_dev_type = DeviceType::NONE;
    DeviceType src_dev_type = DeviceType::NONE;
    uint16_t sink_chan_cnt = 0;
    uint16_t src_chan_cnt = 0;

    if (!dev) {
      LOG(ERROR) << __func__ << "unknown conn_id=" << loghex(conn_id);
      return;
    }
    if (status != GATT_SUCCESS) {
      LOG(ERROR) << __func__ << " failed with status: " << loghex(status);
      return;
    }
    SinkPacsData* sinkinfo = FindSinkByHandle(dev, handle);
    SrcPacsData* srcinfo = FindSrcByHandle(dev, handle);

    if (sinkinfo != nullptr || srcinfo != nullptr) {
      ParsePacRecord(dev, handle, len, value, data);

    } else if (dev->sink_loc_handle == handle) {
      uint8_t *p = value;
      STREAM_TO_UINT32(dev->sink_locations, p);
      dev->chars_read |= SINK_LOC;

#ifdef WRITE_SINK_LOC
      std::vector<uint8_t> vect_val;
      vect_val.insert(vect_val.end(), &dev->sink_locations,
                                      &dev->sink_locations + sizeof(uint32_t));
      GattOpsQueue::WriteCharacteristic(gatt_client_id, dev->conn_id,
                                      dev->sink_loc_handle, vect_val,
                                      GATT_WRITE, nullptr, nullptr);
#endif
      //BAP/UCL/STR/BV-543-C
      //BAP/UCL/STR/BV-546-C
      //For these 2 PTS TCs we need to make TWM as EB_with_no_CSIP
      //So need to enable below property.
      char pts_ac3_eb_no_csip_snk_loc[PROPERTY_VALUE_MAX] = "false";
      property_get("persist.vendor.btstack.pts_ac3_eb_no_csip_enable",
                                    pts_ac3_eb_no_csip_snk_loc, "false");
      LOG(WARNING) << __func__
                   << ": pts_ac3_eb_no_csip_snk_loc: "
                   << pts_ac3_eb_no_csip_snk_loc;
      if (!strncmp("true", pts_ac3_eb_no_csip_snk_loc, 4)) {
        dev->sink_locations = 0x01;
      }
      btif_bap_add_audio_loc(dev->address, CodecDirection::CODEC_DIR_SINK,
                             dev->sink_locations);
      LOG(WARNING) << __func__  << ": sink loc: " << loghex(dev->sink_locations);

    } else if(dev->src_loc_handle == handle) {
      uint8_t *p = value;
      STREAM_TO_UINT32(dev->src_locations, p);
      dev->chars_read |= SRC_LOC;

#ifdef WRITE_SRC_LOC
      std::vector<uint8_t> vect_val;
      vect_val.insert(vect_val.end(), &dev->src_locations,
                                      &dev->src_locations + sizeof(uint32_t));
      GattOpsQueue::WriteCharacteristic(gatt_client_id, dev->conn_id,
                                      dev->src_loc_handle, vect_val,
                                      GATT_WRITE, nullptr, nullptr);
#endif
      //BAP/UCL/STR/BV-543-C
      //BAP/UCL/STR/BV-546-C
      //For these 2 PTS TCs we need to make TWM as EB_with_no_CSIP
      //So need to enable below property.
      char pts_ac3_eb_no_csip_src_loc[PROPERTY_VALUE_MAX] = "false";
      property_get("persist.vendor.btstack.pts_ac3_eb_no_csip_enable",
                                    pts_ac3_eb_no_csip_src_loc, "false");
      LOG(WARNING) << __func__
                   << ": pts_ac3_eb_no_csip_src_loc: "
                   << pts_ac3_eb_no_csip_src_loc;
      if (!strncmp("true", pts_ac3_eb_no_csip_src_loc, 4)) {
        dev->src_locations = 0x01;
      }
      btif_bap_add_audio_loc(dev->address, CodecDirection::CODEC_DIR_SRC,
                             dev->src_locations);
      LOG(WARNING) << __func__  << ": src loc: " << loghex(dev->src_locations);

    } else if(dev->avail_contexts_handle == handle) {
      uint8_t* p = value;
      STREAM_TO_UINT32(dev->available_contexts, p);
      dev->chars_read |= AVAIL_CONTEXTS;

    } else if(dev->supp_contexts_handle == handle) {
      uint8_t* p = value;
      STREAM_TO_UINT32(dev->supported_contexts, p);
      dev->chars_read |= SUPP_CONTEXTS;
      btif_bap_add_supp_contexts(dev->address, dev->supported_contexts);
    }

    LOG(WARNING) << __func__ << ": chars_read: " << loghex(dev->chars_read);

    // check if all pacs characteristics are read
    // send out the callback as service discovery completed
    if (dev->chars_read == dev->chars_to_be_read) {

      UpdateConsolidatedsinkPacRecords(dev);
      UpdateConsolidatedsrcPacRecords(dev);

      // get the callback and update the upper layers
      auto iter = callbacks.find(client_id);
      if (iter != callbacks.end()) {
        dev->discovery_completed = true;
        PacsClientCallbacks *callback = iter->second;
        callback->OnSearchComplete(DISCOVER_SUCCESS,
                                   dev->address,
                                   dev->consolidated_sink_pac_records,
                                   dev->consolidated_src_pac_records,
                                   dev->sink_locations,
                                   dev->src_locations,
                                   dev->available_contexts,
                                   dev->supported_contexts);
      }

      //When both src and sink device types as EBs,
      // then consder it as CSIP and no need to update device type
      if (!btif_bap_is_device_supports_csip(dev->address)) {
        //update sink device type, as here we come to know
        //about the sink audio location.
        LOG(INFO) << __func__ <<": dev->address= " << dev->address;
        btif_bap_get_sink_channel_count(dev->address,
                                        &sink_chan_cnt,
                                        CodecDirection::CODEC_DIR_SINK);
        LOG(WARNING) << __func__
                     << ": Sink Channel count: " << loghex(sink_chan_cnt);

        //For TMAP and CAP all PTS TCs set this prop,
        //because PTS is giving channel cont as 0.
        char pts_tmap_cap_channel_count[PROPERTY_VALUE_MAX] = "false";
        property_get("persist.vendor.btstack.pts_tmap_cap_channel_count",
                                      pts_tmap_cap_channel_count, "false");
        LOG(WARNING) << __func__
                     << ": pts_tmap_cap_channel_count: "
                     << pts_tmap_cap_channel_count;

        if (!strncmp("true", pts_tmap_cap_channel_count, 4) &&
            sink_chan_cnt == 0) {
          sink_chan_cnt = 1;
        }

        if (sink_chan_cnt &
            static_cast<uint16_t>(CodecCapChnlCount::CHNL_COUNT_ONE)) {
          if ((dev->sink_locations == ucast::AUDIO_LOC_LEFT) ||
              (dev->sink_locations == ucast::AUDIO_LOC_RIGHT)) {
            sink_dev_type = DeviceType::EARBUD_NO_CSIP;
            btif_bap_update_sink_device_type(dev->address, sink_dev_type);
          }
        }

        //update source device type, as here we come to know
        //about the source audio location.
        btif_bap_get_src_channel_count(dev->address,
                                       &src_chan_cnt,
                                       CodecDirection::CODEC_DIR_SRC);
        LOG(WARNING) << __func__
                     << ": Src Channel count: " << loghex(src_chan_cnt);

        if (!strncmp("true", pts_tmap_cap_channel_count, 4) &&
            src_chan_cnt == 0) {
          src_chan_cnt = 1;
        }

        if (src_chan_cnt &
            static_cast<uint16_t>(CodecCapChnlCount::CHNL_COUNT_ONE)) {
          if ((dev->src_locations == ucast::AUDIO_LOC_LEFT) ||
              (dev->src_locations == ucast::AUDIO_LOC_RIGHT)) {
            src_dev_type = DeviceType::EARBUD_NO_CSIP;
            btif_bap_update_src_device_type(dev->address, src_dev_type);
          }
        }
        LOG(WARNING) << __func__
                     << ": If single channel support, then: "
                     << " Sink device type: "
                     << loghex(static_cast<uint16_t>(sink_dev_type))
                     << ", Src device type: "
                     << loghex(static_cast<uint16_t>(src_dev_type));
      }
    }
  }

  static void OnReadOnlyPropertiesReadStatic(uint16_t client_id,
                                             uint16_t conn_id,
                                             tGATT_STATUS status,
                                             uint16_t handle, uint16_t len,
                                             uint8_t* value, void* data) {
    if (instance)
      instance->OnReadOnlyPropertiesRead(client_id, conn_id, status, handle,
                                         len, value, data);
  }

  static void OnReadAvailableAudioStatic(uint16_t client_id,
                                             uint16_t conn_id,
                                             tGATT_STATUS status,
                                             uint16_t handle, uint16_t len,
                                             uint8_t* value, void* data) {
    if (instance)
      instance->OnReadAvailableAudio(client_id, conn_id, status, handle,
                                     len, value, data);
  }


 private:
  uint8_t gatt_client_id = BTA_GATTS_INVALID_IF;
  uint16_t pacs_client_id = 0;
  PacsDevices pacsDevices;
  // client id to callbacks mapping
  std::map<uint16_t, PacsClientCallbacks *> callbacks;

  void find_server_changed_ccc_handle(uint16_t conn_id,
                                      const gatt::Service* service) {
    PacsDevice* pacsDevice = pacsDevices.FindByConnId(conn_id);
    if (!pacsDevice) {
      LOG(ERROR) << __func__
               << ": Skipping unknown device, conn_id=" << loghex(conn_id);
      return;
    }
    for (const gatt::Characteristic& charac : service->characteristics) {
      if (charac.uuid == Uuid::From16Bit(GATT_UUID_GATT_SRV_CHGD)) {
        pacsDevice->srv_changed_ccc_handle =
            find_ccc_handle(conn_id, charac.value_handle);
        if (!pacsDevice->srv_changed_ccc_handle) {
          LOG(ERROR) << __func__
                     << ": cannot find service changed CCC descriptor";
          continue;
        }
        LOG(INFO) << __func__ << ": service_changed_ccc="
                  << loghex(pacsDevice->srv_changed_ccc_handle);
        break;
      }
    }
  }

  // Find the handle for the client characteristics configuration of a given
  // characteristics
  uint16_t find_ccc_handle(uint16_t conn_id, uint16_t char_handle) {
    const gatt::Characteristic* p_char =
        BTA_GATTC_GetCharacteristic(conn_id, char_handle);

    if (!p_char) {
      LOG(WARNING) << __func__ << ": No such characteristic: " << char_handle;
      return 0;
    }

    for (const gatt::Descriptor& desc : p_char->descriptors) {
      if (desc.uuid == Uuid::From16Bit(GATT_UUID_CHAR_CLIENT_CONFIG))
        return desc.handle;
    }

    return 0;
  }

  SinkPacsData *FindSinkByHandle(PacsDevice *dev, uint16_t handle) {
    LOG(INFO) << __func__ << ": handle:" << loghex(handle);
    auto iter = std::find_if(dev->sink_info.begin(),
                             dev->sink_info.end(),
                         [&handle](SinkPacsData data) {
                           return (data.sink_pac_handle == handle);
                         });

    return (iter == dev->sink_info.end()) ? nullptr : &(*iter);
  }

  SrcPacsData *FindSrcByHandle(PacsDevice *dev, uint16_t handle) {
    LOG(INFO) << __func__ << ": handle:" << loghex(handle);
    auto iter = std::find_if(dev->src_info.begin(),
                             dev->src_info.end(),
                         [&handle](SrcPacsData data) {
                           return (data.src_pac_handle == handle);
                         });

    return (iter == dev->src_info.end()) ? nullptr : &(*iter);
  }

  void UpdateConsolidatedsinkPacRecords(PacsDevice *dev) {
    LOG(INFO) << __func__;
    for (auto it = dev->sink_info.begin();
                             it != dev->sink_info.end(); it ++) {
      for (auto i = it->sink_pac_records.begin();
                    i != it->sink_pac_records.end(); i ++) {
        dev->consolidated_sink_pac_records.
             push_back(static_cast<CodecConfig>(*i));
      }
    }
  }

  void UpdateConsolidatedsrcPacRecords(PacsDevice *dev) {
    LOG(INFO) << __func__;
    for (auto it = dev->src_info.begin();
              it != dev->src_info.end(); it ++) {
      for (auto i = it->src_pac_records.begin();
                i != it->src_pac_records.end(); i ++) {
        dev->consolidated_src_pac_records.
             push_back(static_cast<CodecConfig>(*i));
      }
    }
  }
};

const char* get_gatt_event_name(uint32_t event) {
  switch (event) {
    CASE_RETURN_STR(BTA_GATTC_DEREG_EVT)
    CASE_RETURN_STR(BTA_GATTC_OPEN_EVT)
    CASE_RETURN_STR(BTA_GATTC_CLOSE_EVT)
    CASE_RETURN_STR(BTA_GATTC_SEARCH_CMPL_EVT)
    CASE_RETURN_STR(BTA_GATTC_NOTIF_EVT)
    CASE_RETURN_STR(BTA_GATTC_ENC_CMPL_CB_EVT)
    CASE_RETURN_STR(BTA_GATTC_CONN_UPDATE_EVT)
    CASE_RETURN_STR(BTA_GATTC_SRVC_CHG_EVT)
    CASE_RETURN_STR(BTA_GATTC_SRVC_DISC_DONE_EVT)
    CASE_RETURN_STR(BTA_GATTC_CONGEST_EVT)
    CASE_RETURN_STR(BTA_GATTC_CFG_MTU_EVT)
    default:
      return "Unknown Event";
  }
}

void pacs_gattc_callback(tBTA_GATTC_EVT event, tBTA_GATTC* p_data) {
  if (p_data == nullptr || !instance) {
    LOG(ERROR) << __func__ << ": p_data is null or no instance, return";
    //Add new code to clear the remaining op operation of gatt ops queue when receiving close
    if (p_data && event == BTA_GATTC_CLOSE_EVT) {
      LOG(ERROR) << __func__ << ": p_data is not null, need to delete gatt op queue, conn_id : " << (int)p_data->close.conn_id;
      GattOpsQueue::Clean(p_data->close.conn_id);
    }
    return;
  }
  LOG(INFO) << __func__ << ": Event : " << get_gatt_event_name(event);

  switch (event) {
    case BTA_GATTC_DEREG_EVT:
      break;
    case BTA_GATTC_CFG_MTU_EVT:
      break;
    case BTA_GATTC_OPEN_EVT: {
      tBTA_GATTC_OPEN& o = p_data->open;
      instance->OnGattConnected(o.status, o.conn_id, o.client_if, o.remote_bda,
                                o.transport, o.mtu);
      break;
    }

    case BTA_GATTC_CLOSE_EVT: {
      tBTA_GATTC_CLOSE& c = p_data->close;
      instance->OnGattDisconnected(c.status, c.conn_id, c.client_if,
                                   c.remote_bda, c.reason);
    } break;

    case BTA_GATTC_SEARCH_CMPL_EVT:
      instance->OnServiceSearchComplete(p_data->search_cmpl.conn_id,
                                        p_data->search_cmpl.status);
      break;

    case BTA_GATTC_NOTIF_EVT:
      if (!p_data->notify.is_notify || p_data->notify.len > GATT_MAX_ATTR_LEN) {
        LOG(ERROR) << __func__ << ": rejected BTA_GATTC_NOTIF_EVT. is_notify="
                   << p_data->notify.is_notify
                   << ", len=" << p_data->notify.len;
        break;
      }
      instance->OnNotificationEvent(p_data->notify.conn_id,
                                    p_data->notify.handle, p_data->notify.len,
                                    p_data->notify.value);
      break;

    case BTA_GATTC_ENC_CMPL_CB_EVT:
      instance->OnEncryptionComplete(p_data->enc_cmpl.remote_bda, true);
      break;

    case BTA_GATTC_CONN_UPDATE_EVT:
      instance->OnConnectionUpdateComplete(p_data->conn_update.conn_id,
                                           p_data);
      break;

    case BTA_GATTC_SRVC_CHG_EVT:
      instance->OnServiceChangeEvent(p_data->remote_bda);
      break;

    case BTA_GATTC_SRVC_DISC_DONE_EVT:
      instance->OnServiceDiscDoneEvent(p_data->remote_bda);
      break;
    case BTA_GATTC_CONGEST_EVT:
      instance->OnCongestionEvent(p_data->congest.conn_id,
                                  p_data->congest.congested);
      break;
    default:
      break;
  }
}

void encryption_callback(const RawAddress* address, tGATT_TRANSPORT, void*,
                         tBTM_STATUS status) {
  if (instance) {
    instance->OnEncryptionComplete(*address,
                                   status == BTM_SUCCESS ? true : false);
  }
}

void PacsClient::Initialize(PacsClientCallbacks* callbacks) {
  LOG(INFO) << __func__;
  if (instance) {
    LOG(INFO) << __func__ << ": pacsClient instance already exist.";
    instance->Register(callbacks);
  } else {
    LOG(INFO) << __func__ << ": instantiate pacs_client.";
    instance = new PacsClientImpl();
    instance->Register(callbacks);
  }
}

void PacsClient::CleanUp(uint16_t client_id) {
  LOG(INFO) << __func__ << ": client_id : " << loghex(client_id);
  if(instance && instance->GetClientCount()) {
    instance->Deregister(client_id);
    if(!instance->GetClientCount()) {
      LOG(INFO) << __func__ << ": delete pacsClient instance.";
      delete instance;
      instance = nullptr;
    }
  }
}

PacsClient* PacsClient::Get() {
  CHECK(instance);
  return instance;
}

}  // namespace pacs
}  // namespace bap
}  // namespace bluetooth
