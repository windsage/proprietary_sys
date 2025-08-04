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
#include "bta_pacs_client_api.h"
#include "bta_ascs_client_api.h"
#include <hardware/bt_pacs_client.h>
#include <base/bind.h>
#include <base/callback.h>
#include <base/logging.h>
#include "bta_closure_api.h"
#include "bt_trace.h"

namespace bluetooth {
namespace bap {
namespace ucast {

using base::Bind;
using base::Unretained;
using base::Closure;
using bluetooth::Uuid;

using bluetooth::bap::pacs::PacsClient;
using bluetooth::bap::pacs::ConnectionState;
using bluetooth::bap::pacs::CodecConfig;
using bluetooth::bap::pacs::PacsClientCallbacks;

using bluetooth::bap::ascs::AscsClient;
using bluetooth::bap::ascs::GattState;
using bluetooth::bap::ascs::AscsClientCallbacks;
using bluetooth::bap::ascs::AseOpId;
using bluetooth::bap::ascs::AseOpStatus;
using bluetooth::bap::ascs::AseParams;

using bluetooth::bap::ucast::UstreamManagers;
using bluetooth::bap::ucast::UstreamManager;

using bluetooth::bap::ucast::BapEventData;
using bluetooth::bap::ucast::BapEvent;
using bluetooth::bap::ucast::BapConnect;
using bluetooth::bap::ucast::BapDisconnect;
using bluetooth::bap::ucast::BapStart;
using bluetooth::bap::ucast::BapStop;
using bluetooth::bap::ucast::BapReconfig;
using bluetooth::bap::ucast::PacsConnectionState;
using bluetooth::bap::ucast::PacsDiscovery;
using bluetooth::bap::ucast::PacsAvailableContexts;

using bluetooth::bap::ucast::CisGroupState;
using bluetooth::bap::ucast::CisStreamState;
using bluetooth::bap::cis::CigState;
using bluetooth::bap::cis::CisState;
using bluetooth::bap::cis::CisInterface;
using bluetooth::bap::ucast::CISEstablishedEvtParamas;

using bluetooth::bap::alarm::BapAlarm;
using bluetooth::bap::alarm::BapAlarmCallbacks;

class UcastClientImpl;
UcastClientImpl* instance = nullptr;

class CisInterfaceCallbacksImpl : public CisInterfaceCallbacks {
  public:
    ~CisInterfaceCallbacksImpl() = default;
        /** Callback for connection state change */
    void OnCigState(uint8_t cig_id, CigState state) {
      do_in_bta_thread(FROM_HERE, Bind(&CisInterfaceCallbacks::OnCigState,
                                       Unretained(UcastClient::Get()), cig_id,
                                       state));

    }

    void OnCisState(uint8_t cig_id, uint8_t cis_id, uint8_t direction,
                    CISEstablishedEvtParamas cis_param, CisState state) {
      if(instance) {
        do_in_bta_thread(FROM_HERE, Bind(&CisInterfaceCallbacks::OnCisState,
                                       Unretained(UcastClient::Get()), cig_id,
                                       cis_id, direction, cis_param, state));
      }
    }
};

class PacsClientCallbacksImpl : public PacsClientCallbacks {
  public:
    ~PacsClientCallbacksImpl() = default;
    void OnInitialized(int status, int client_id) override {
      LOG(WARNING) << __func__ << ": status =" << loghex(status);
      do_in_bta_thread(FROM_HERE, Bind(&PacsClientCallbacks::OnInitialized,
                                       Unretained(UcastClient::Get()), status,
                                       client_id));
    }

    void OnConnectionState(const RawAddress& address,
                       bluetooth::bap::pacs::ConnectionState state) override {
      LOG(WARNING) << __func__ << ": address=" << address;
      do_in_bta_thread(FROM_HERE, Bind(&PacsClientCallbacks::OnConnectionState,
                                       Unretained(UcastClient::Get()),
                                       address, state));
    }

    void OnAudioContextAvailable(const RawAddress& address,
                          uint32_t available_contexts) override {
      do_in_bta_thread(FROM_HERE,
                       Bind(&PacsClientCallbacks::OnAudioContextAvailable,
                            Unretained(UcastClient::Get()),
                            address, available_contexts));
    }

    void OnSearchComplete(int status, const RawAddress& address,
                          std::vector<CodecConfig> sink_pac_records,
                          std::vector<CodecConfig> src_pac_records,
                          uint32_t sink_locations,
                          uint32_t src_locations,
                          uint32_t available_contexts,
                          uint32_t supported_contexts) override {
      do_in_bta_thread(FROM_HERE, Bind(&PacsClientCallbacks::OnSearchComplete,
                                       Unretained(UcastClient::Get()),
                                       status, address,
                                       sink_pac_records,
                                       src_pac_records,
                                       sink_locations,
                                       src_locations,
                                       available_contexts,
                                       supported_contexts));
    }
};

class AscsClientCallbacksImpl : public AscsClientCallbacks {
  public:
    ~AscsClientCallbacksImpl() = default;
    void OnAscsInitialized(int status, int client_id) override {
      do_in_bta_thread(FROM_HERE, Bind(&AscsClientCallbacks::OnAscsInitialized,
                                       Unretained(UcastClient::Get()), status,
                                       client_id));
    }

    void OnConnectionState(const RawAddress& address,
                       bluetooth::bap::ascs::GattState state) override {
      DVLOG(2) << __func__ << " address: " << address;
      do_in_bta_thread(FROM_HERE, Bind(&AscsClientCallbacks::OnConnectionState,
                                       Unretained(UcastClient::Get()),
                                       address, state));
    }

    void OnAseOpFailed(const RawAddress& address,
                             AseOpId ase_op_id,
                             std::vector<AseOpStatus> status) {
      do_in_bta_thread(FROM_HERE,
                       Bind(&AscsClientCallbacks::OnAseOpFailed,
                            Unretained(UcastClient::Get()),
                            address, ase_op_id, status));

    }

    void OnAseState(const RawAddress& address,
                          AseParams ase) override {
      do_in_bta_thread(FROM_HERE,
                       Bind(&AscsClientCallbacks::OnAseState,
                            Unretained(UcastClient::Get()),
                            address, ase));
    }

    void OnSearchComplete(int status, const RawAddress& address,
                          std::vector<AseParams> sink_ase_list,
                          std::vector<AseParams> src_ase_list) override {
      do_in_bta_thread(FROM_HERE, Bind(&AscsClientCallbacks::OnSearchComplete,
                                       Unretained(UcastClient::Get()),
                                       status, address, sink_ase_list,
                                       src_ase_list));
    }
};

class BapAlarmCallbacksImpl : public BapAlarmCallbacks {
  public:
    ~BapAlarmCallbacksImpl() = default;
    /** Callback for timer timeout */
    void OnTimeout(void* data) {
      do_in_bta_thread(FROM_HERE, Bind(&BapAlarmCallbacks::OnTimeout,
                                       Unretained(UcastClient::Get()), data));
    }
};

class UcastClientImpl : public UcastClient {
 public:
  ~UcastClientImpl() override = default;

  // APIs exposed for upper layers
  void Connect(std::vector<RawAddress> address, bool is_direct,
               std::vector<StreamConnect> streams) override {

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    for (auto it = address.begin(); it != address.end(); it++) {
      strm_mgrs.FindorAddByAddress(*it,
                                   pacs_client, pacs_client_id,
                                   ascs_client, cis_intf,
                                   ucl_callbacks, bap_alarm,
                                   &stream_trackers);
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address[0],
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    BapConnect data = { .bd_addr = address, .is_direct = is_direct,
                        .streams = streams};
    mgr->ProcessEvent(BAP_CONNECT_REQ_EVT, &data, &strm_mgrs);
  }

  void Disconnect(const RawAddress& address,
                  std::vector<StreamType> streams) override {

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);

    // hand over the request to stream manager
    BapDisconnect data = { .bd_addr = address,
                          .streams = streams};
    mgr->ProcessEvent(BAP_DISCONNECT_REQ_EVT, &data, &strm_mgrs);
  }

  void Start(std::vector<RawAddress> address,
             std::vector<StreamType> streams,
             bool is_mono_mic, bool mode) override {
    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }
    for (auto it = address.begin(); it != address.end();) {
      strm_mgrs.FindorAddByAddress(*it,
                                   pacs_client, pacs_client_id,
                                   ascs_client, cis_intf,
                                   ucl_callbacks, bap_alarm,
                                   &stream_trackers);
       ConnectionState pacs_state = strm_mgrs.GetPacsState(*it);
       GattState ascs_state = strm_mgrs.GetAscsState(*it);
       LOG(WARNING) << __func__ << ": iterator address: " << *it
                    << ", pacs_state: " << static_cast<int>(pacs_state)
                    << ", ascs_state: " << static_cast<int>(ascs_state);
       if (pacs_state == ConnectionState::DISCONNECTED ||
           ascs_state == GattState::DISCONNECTED) {
         LOG(WARNING) << __func__ << ": pacs or ascs is disconnected.";
         it = address.erase(it);
       } else {
         it++;
       }
    }
    if(!address.size()) {
      LOG(WARNING) << __func__  << " Not eligible device for stream start";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address[0],
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    BapStart data = { .bd_addr = address,
                      .streams = streams,
                      .is_mono_mic = is_mono_mic,
                      .mode = mode};
    mgr->ProcessEvent(BAP_START_REQ_EVT, &data, &strm_mgrs);
  }

  void Stop(const RawAddress& address,
            std::vector<StreamType> streams, bool mode) override {
    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }
    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);

    // hand over the request to stream manager
    BapStop data = { .bd_addr = address,
                     .streams = streams,
                     .mode = mode};
    mgr->ProcessEvent(BAP_STOP_REQ_EVT, &data, &strm_mgrs);

  }

  void Reconfigure(const RawAddress& address,
                   std::vector<StreamReconfig> streams) override  {

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);

    // hand over the request to stream manager
    BapReconfig data = { .bd_addr = address,
                         .streams = streams};
    mgr->ProcessEvent(BAP_RECONFIG_REQ_EVT, &data, &strm_mgrs);
  }

  void UpdateStream(const RawAddress& address,
                   std::vector<StreamUpdate> update_streams) override  {

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);

    // hand over the request to stream manager
    BapStreamUpdate data = { .bd_addr = address,
                            .update_streams = update_streams};
    mgr->ProcessEvent(BAP_STREAM_UPDATE_REQ_EVT, &data, &strm_mgrs);
  }

  // To be called from device specific stream manager
  bool ReportStreamState(const RawAddress& address) {
    //TODO to check
    return true;

  }

  // PACS client related callbacks
  // to be forwarded to device specific stream manager
  void OnInitialized(int status, int client_id) override {
    LOG(WARNING) << __func__ << ": actual client_id = " << loghex(client_id);
    pacs_client_id = client_id;
  }

  void OnConnectionState(const RawAddress& address,
                         ConnectionState state) override {
    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    PacsConnectionState data = { .bd_addr = address,
                                 .state = state
                               };
    mgr->ProcessEvent(PACS_CONNECTION_STATE_EVT, &data, &strm_mgrs);
  }

  void OnAudioContextAvailable(const RawAddress& address,
                        uint32_t available_contexts) override {
    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    PacsAvailableContexts data = {
                           .bd_addr = address,
                           .available_contexts = available_contexts,
                         };
    mgr->ProcessEvent(PACS_AUDIO_CONTEXT_RES_EVT, &data, &strm_mgrs);
  }

  void OnSearchComplete(int status, const RawAddress& address,
                        std::vector<CodecConfig> sink_pac_records,
                        std::vector<CodecConfig> src_pac_records,
                        uint32_t sink_locations,
                        uint32_t src_locations,
                        uint32_t available_contexts,
                        uint32_t supported_contexts) override {
    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    PacsDiscovery data = {
                           .status = status,
                           .bd_addr = address,
                           .sink_pac_records = sink_pac_records,
                           .src_pac_records = src_pac_records,
                           .sink_locations = sink_locations,
                           .src_locations = src_locations,
                           .available_contexts = available_contexts,
                           .supported_contexts = supported_contexts
                         };
    mgr->ProcessEvent(PACS_DISCOVERY_RES_EVT, &data, &strm_mgrs);
  }

  // ASCS client related callbacks
  // to be forwarded to device specific stream manager
  void OnAscsInitialized(int status, int client_id) override {

  }

  void OnConnectionState(const RawAddress& address,
                     bluetooth::bap::ascs::GattState state) override {
    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    AscsConnectionState data = { .bd_addr = address,
                                 .state = state
                               };
    mgr->ProcessEvent(ASCS_CONNECTION_STATE_EVT, &data, &strm_mgrs);
  }

  void OnAseOpFailed(const RawAddress& address,
                     AseOpId ase_op_id,
                     std::vector<AseOpStatus> status) {

    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    AscsOpFailed data = {
                           .bd_addr = address,
                           .ase_op_id = ase_op_id,
                           .ase_list = status
                        };
    mgr->ProcessEvent(ASCS_ASE_OP_FAILED_EVT, &data, &strm_mgrs);
  }

  void OnAseState(const RawAddress& address,
                        AseParams ase_params) override {
    LOG(WARNING) << __func__ << ": address=" << address;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    AscsState data = {
                           .bd_addr = address,
                           .ase_params = ase_params
                     };
    mgr->ProcessEvent(ASCS_ASE_STATE_EVT, &data, &strm_mgrs);
  }

  void OnSearchComplete(int status, const RawAddress& address,
                                std::vector<AseParams> sink_ase_list,
                                std::vector<AseParams> src_ase_list) override {

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(address,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    AscsDiscovery data = {
                           .status = status,
                           .bd_addr = address,
                           .sink_ases_list = sink_ase_list,
                           .src_ases_list = src_ase_list
                         };
    mgr->ProcessEvent(ASCS_DISCOVERY_RES_EVT, &data, &strm_mgrs);
  }

  // cis callbacks
  void OnCigState(uint8_t cig_id, CigState state) override {
    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    std::vector<UstreamManager *> *mgrs_list =  strm_mgrs.GetAllManagers();
    // hand over the request to stream manager

    for (auto it = mgrs_list->begin(); it != mgrs_list->end(); it++) {
      CisGroupState data = {
                        .bd_addr = (*it)->GetAddress(),
                        .cig_id = cig_id,
                        .state = state
                      };
      (*it)->ProcessEvent(CIS_GROUP_STATE_EVT, &data, &strm_mgrs);
    }
  }

  void OnCisState(uint8_t cig_id, uint8_t cis_id, uint8_t direction,
                  CISEstablishedEvtParamas cis_param, CisState state) override {
    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    std::vector<UstreamManager *> *mgrs_list =  strm_mgrs.GetAllManagers();
    // hand over the request to stream manager
    for (auto it = mgrs_list->begin(); it != mgrs_list->end(); it++) {
      CisStreamState data = {
                             .bd_addr = (*it)->GetAddress(),
                             .cig_id = cig_id,
                             .cis_id = cis_id,
                             .direction = direction,
                             .state = state,
                             .param = cis_param
                           };
      (*it)->ProcessEvent(CIS_STATE_EVT, &data, &strm_mgrs);
    }
  }

  void OnTimeout(void* data) override {
    LOG(ERROR) << __func__;

    LOG(WARNING) << __func__ << ": is_uclient_cleanup_completed: "
                             << is_uclient_cleanup_completed;
    if (is_uclient_cleanup_completed) {
      LOG(WARNING) << __func__ << ": ucast cleaned up, return.";
      return;
    }

    BapTimeout* data_ = (BapTimeout *)data;
    UstreamManager *mgr = strm_mgrs.FindorAddByAddress(data_->bd_addr,
                                    pacs_client, pacs_client_id,
                                    ascs_client, cis_intf,
                                    ucl_callbacks, bap_alarm,
                                    &stream_trackers);
    // hand over the request to stream manager
    mgr->ProcessEvent(BAP_TIME_OUT_EVT, data, &strm_mgrs);
  }

  void CleanupStreamContext() {
    std::vector<UstreamManager *> *mgrs_list =  strm_mgrs.GetAllManagers();
    for (auto it = mgrs_list->begin(); it != mgrs_list->end(); it++) {
      LOG(WARNING) << __func__
                   <<": Call CleanupStreamContext()";
      (*it)->CleanupStreamContext();
    }
  }

  bool Init(UcastClientCallbacks *callback) {
    // register callbacks with CIS, ASCS client, PACS client
    LOG(WARNING) << __func__
                 <<": Init pacs, ascs clients, cis intf and bap_alarm.";
    pacs_callbacks = new PacsClientCallbacksImpl;
    PacsClient::Initialize(pacs_callbacks);
    pacs_client = PacsClient::Get();

    ascs_callbacks = new AscsClientCallbacksImpl;
    AscsClient::Init(ascs_callbacks);
    ascs_client = AscsClient::Get();

    cis_callbacks = new CisInterfaceCallbacksImpl;
    CisInterface::Initialize(cis_callbacks);
    cis_intf = CisInterface::Get();

    bap_alarm_cb = new BapAlarmCallbacksImpl;
    BapAlarm::Initialize(bap_alarm_cb);
    bap_alarm = BapAlarm::Get();

    pacs_client_id = 0;
    is_uclient_cleanup_completed = false;
    ClearAllBapTimeoutDevices();
    if(ucl_callbacks != nullptr) {
      // flag an error
      return false;
    } else {
      ucl_callbacks = callback;
      return true;
    }
  }

  bool CleanUp() {
    if(ucl_callbacks != nullptr) {
      ucl_callbacks = nullptr;
      //call clean ups for each clients(ascs, pacs, cis and bap_alarm)
      LOG(ERROR) << __func__
                 <<": Cleaning up pacs, ascs clients, cis intf and bap_alarm.";
      LOG(ERROR) << __func__ << ": pacs_client_id: "<< pacs_client_id;
      pacs_client->CleanUp(pacs_client_id);
      ascs_client->CleanUp(0x01);
      cis_intf->CleanUp();
      bap_alarm->CleanUp();
      pacs_client = nullptr;
      ascs_client = nullptr;
      cis_intf = nullptr;
      bap_alarm = nullptr;
      CleanupStreamContext();
      strm_mgrs.CleanUp();
      ClearAllBapTimeoutDevices();
      stream_trackers.CleanUpStrmTrackers();
      is_uclient_cleanup_completed = true;
      // remove all stream managers and other clean ups
      return true;
    } else {
      return false;
    }
  }

 private:
  UcastClientCallbacks* ucl_callbacks;
  UstreamManagers strm_mgrs;
  StreamTrackers stream_trackers;
  PacsClient *pacs_client;
  AscsClient *ascs_client;
  PacsClientCallbacks *pacs_callbacks;
  AscsClientCallbacks *ascs_callbacks;
  CisInterface *cis_intf;
  CisInterfaceCallbacks *cis_callbacks;
  uint16_t pacs_client_id;
  BapAlarm* bap_alarm;
  BapAlarmCallbacks* bap_alarm_cb;
  bool is_uclient_cleanup_completed;
};

void UcastClient::Initialize(UcastClientCallbacks* callbacks) {
  if (!instance) {
    LOG(ERROR) << __func__ << ": Instantiate UcastClient.";
    instance = new UcastClientImpl();
    instance->Init(callbacks);
  } else {
    LOG(ERROR) << __func__ << ": UcastClient instance already exist.";
    instance->Init(callbacks);
    LOG(ERROR) << __func__ << ": 2nd client registration ignored";
  }
}

void UcastClient::CleanUp() {
  if(instance && instance->CleanUp()) {
    //Don't remove instance
    LOG(WARNING) << __func__ << ": nothing to do.";
  }
}

UcastClient* UcastClient::Get() {
  CHECK(instance);
  return instance;
}

}  // namespace ucast
}  // namespace bap
}  // namespace bluetooth
