/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
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

/* MCP Interface */
#define LOG_TAG "bt_btif_mcp"

#include "bt_target.h"
#include "bta_closure_api.h"
#include "bta_mcp_api.h"
#include "btif_common.h"
#include "btif_storage.h"

#include <base/bind.h>
#include <base/callback.h>
#include <hardware/bluetooth.h>
#include <hardware/bt_mcp.h>

using base::Bind;
using base::Unretained;
using base::Owned;
using bluetooth::Uuid;
using std::vector;
using base::Bind;
using base::Unretained;

using bluetooth::mcp_server::McpServerCallbacks;
using bluetooth::mcp_server::McpServerInterface;

namespace {
class McpServerInterfaceImpl;
std::unique_ptr<McpServerInterface> McpServerInstance;

class McpServerInterfaceImpl
  : public McpServerInterface, public McpServerCallbacks {
  ~McpServerInterfaceImpl() = default;

  void Init(McpServerCallbacks* callback, Uuid bt_uuid) override {
    LOG(INFO) << __func__ ;
    this->callbacks = callback;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::Initialize, this, bt_uuid));
  }

  void MediaState(uint8_t state) override {
    LOG(INFO) << __func__ << ": state " << state;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::MediaState, Unretained(McpServer::Get()), state));
  }

  void MediaPlayerName(uint8_t* name) override {
    LOG(INFO) << __func__ << ": name" << name;
    uint8_t* mediaPlayerName = (uint8_t*)osi_malloc(sizeof(uint8_t)*strlen((char*)name)+1);
    if (mediaPlayerName != NULL) {
      memcpy(mediaPlayerName, name, strlen((char*)name)+1);
      do_in_bta_thread(FROM_HERE,
            Bind(&McpServer::MediaPlayerName, Unretained(McpServer::Get()), mediaPlayerName));
    }
  }

  void MediaControlPointOpcodeSupported(uint32_t feature) override {
    LOG(INFO) << __func__ << ": feature" << feature;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::MediaControlPointOpcodeSupported, Unretained(McpServer::Get()), feature));
  }

  void MediaControlPoint(uint8_t value, uint8_t status) override {
    LOG(INFO) << __func__ << ": value" << value;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::MediaControlPoint, Unretained(McpServer::Get()), value, status));
  }

  void TrackChanged() override {
    LOG(INFO) << __func__;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::TrackChanged, Unretained(McpServer::Get())));
  }

  void TrackTitle(uint8_t* title) override {
    LOG(INFO) << __func__ << ": title" << title;
    uint8_t* trackTitle = (uint8_t*)osi_malloc(sizeof(uint8_t)*strlen((char*)title)+1);
    if (trackTitle != NULL) {
      memcpy(trackTitle, title, strlen((char*)title)+1);
      do_in_bta_thread(FROM_HERE,
            Bind(&McpServer::TrackTitle, Unretained(McpServer::Get()), trackTitle));
    }
  }

  void TrackPosition(int32_t position) override {
    LOG(INFO) << __func__ << ": position" << position;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::TrackPosition, Unretained(McpServer::Get()), position));
  }

  void TrackDuration(int32_t duration) override {
    LOG(INFO) << __func__ << ": duration" << duration;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::TrackDuration, Unretained(McpServer::Get()), duration));
  }

  void ContentControlId(uint8_t ccid) override {
    LOG(INFO) << __func__ << ": ccid" << ccid;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::ContentControlId, Unretained(McpServer::Get()), ccid));
  }

  void PlayingOrderSupported(uint16_t order) override {
    LOG(INFO) << __func__ << ": order" << order;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::PlayingOrderSupported, Unretained(McpServer::Get()), order));
  }

  void PlayingOrder(uint8_t value) override {
    LOG(INFO) << __func__ << ": value" << value;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::PlayingOrder, Unretained(McpServer::Get()), value));
  }

  void SetActiveDevice(const RawAddress& address, int set_id, int profile, int is_csip) override {
    LOG(INFO) << __func__ << ": set_id" << set_id << ": device" << address
                                                  << ": is_csip " << is_csip;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::SetActiveDevice, Unretained(McpServer::Get()), address, set_id,
                                                                          profile, is_csip));
  }

  void DisconnectMcp(const RawAddress& address) override {
    LOG(INFO) << __func__ << ": device"<< address;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::DisconnectMcp, Unretained(McpServer::Get()), address));
  }

  void BondStateChange(const RawAddress& address, int state) override {
    LOG(INFO) << __func__ << ": device"<< address << " state : " << state;
    do_in_bta_thread(FROM_HERE,
          Bind(&McpServer::BondStateChange, Unretained(McpServer::Get()), address, state));
  }

  void Cleanup(void) override {
    LOG(INFO) << __func__;
    do_in_bta_thread(FROM_HERE, Bind(&McpServer::CleanUp));
  }

  void OnConnectionStateChange(int status,
                         const RawAddress& address) override {
    LOG(INFO) << __func__ << ": device=" << address << " state=" << (int)status;
    do_in_jni_thread(FROM_HERE, Bind(&McpServerCallbacks::OnConnectionStateChange,
            Unretained(callbacks), status, address));
  }

  void MediaControlPointChangeReq(uint8_t state,
                         const RawAddress& address) override {
    LOG(INFO) << __func__ << ": device=" << address << " state=" << (int)state;
    do_in_jni_thread(FROM_HERE, Bind(&McpServerCallbacks::MediaControlPointChangeReq,
            Unretained(callbacks), state, address));
  }

  void TrackPositionChangeReq(int32_t position) override {
    LOG(INFO) << __func__ << " position=" << (int)position;
    do_in_jni_thread(FROM_HERE, Bind(&McpServerCallbacks::TrackPositionChangeReq,
            Unretained(callbacks), position));
  }

  void PlayingOrderChangeReq(uint32_t order) override {
    LOG(INFO) << __func__ << ": order=" << order;
    do_in_jni_thread(FROM_HERE, Bind(&McpServerCallbacks::PlayingOrderChangeReq,
            Unretained(callbacks), order));
  }

  void McpServerInitializedCallback(uint8_t state) override{
    LOG(INFO) << __func__ << ": state= " << state;
    do_in_jni_thread(FROM_HERE, Bind(&McpServerCallbacks::McpServerInitializedCallback,
               Unretained(callbacks), state));
  }
  private:
    McpServerCallbacks* callbacks;
  };
}//namespace

const McpServerInterface* btif_mcp_server_get_interface(void) {
   LOG(INFO) << __func__;
   if (!McpServerInstance)
     McpServerInstance.reset(new McpServerInterfaceImpl());
   return McpServerInstance.get();
}
