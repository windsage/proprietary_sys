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
 * Copyright 2017 The Android Open Source Project
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

#include "gatts_ops_queue.h"

#include <list>
#include <unordered_map>
#include <unordered_set>

namespace bluetooth {
namespace bap {

using gatts_operation = GattsOpsQueue::gatts_operation;
using bluetooth::Uuid;

constexpr uint8_t GATT_NOTIFY = 1;
constexpr uint8_t GATT_SEND_RESPONSE = 2;

std::unordered_map<uint16_t, std::list<gatts_operation>>
                                       GattsOpsQueue::gatts_op_queue;
std::unordered_set<uint16_t> GattsOpsQueue::gatts_op_queue_executing;
std::unordered_map<uint16_t, bool> GattsOpsQueue::congestion_queue;

void GattsOpsQueue::mark_as_not_executing(uint16_t conn_id) {
  APPL_TRACE_DEBUG("%s: conn_id=0x%x", __func__, conn_id);
  gatts_op_queue_executing.erase(conn_id);
}

void GattsOpsQueue::gatts_execute_next_op(uint16_t conn_id) {
  APPL_TRACE_DEBUG("%s: conn_id=0x%x", __func__, conn_id);

  if (gatts_op_queue.empty()) {
    APPL_TRACE_DEBUG("%s: op queue is empty", __func__);
    return;
  }

  auto ptr = congestion_queue.find(conn_id);

  if (ptr != congestion_queue.end()) {
    bool is_congested = ptr->second;
    APPL_TRACE_DEBUG("%s: congestion queue exist, conn_id: %d, is_congested: %d",
                                               __func__, conn_id, is_congested);
    if(is_congested) {
      APPL_TRACE_DEBUG("%s: lower layer is congested", __func__);
      return;
    }
  }

  auto map_ptr = gatts_op_queue.find(conn_id);

  if (map_ptr == gatts_op_queue.end()) {
    APPL_TRACE_DEBUG("%s: Queue is null", __func__);
    return;
  }

  if (map_ptr->second.empty()) {
    APPL_TRACE_DEBUG("%s: queue is empty for conn_id: %d", __func__,
                     conn_id);
    return;
  }

  if (gatts_op_queue_executing.count(conn_id)) {
    APPL_TRACE_DEBUG("%s: can't enqueue next op, already executing", __func__);
    return;
  }

  std::list<gatts_operation>& gatts_ops = map_ptr->second;
  gatts_operation& op = gatts_ops.front();
  APPL_TRACE_DEBUG("%s: op.type=%d, attr_id=%d",
                              __func__, op.type, op.attr_id);

  if(op.type == GATT_NOTIFY) {
    if(GATTS_CheckStatusForApp(conn_id,op.need_confirm) == GATT_SUCCESS) {
      APPL_TRACE_DEBUG("%s: conn_id: %d, attr handle: %d",
                                    __func__, conn_id, op.attr_id);
      BTA_GATTS_HandleValueIndication(conn_id, op.attr_id, op.value, op.need_confirm);
      gatts_op_queue_executing.insert(conn_id);
    }
  }

  while(!gatts_ops.empty() && gatts_ops.front().type == GATT_SEND_RESPONSE) {
    gatts_operation& op = gatts_ops.front();
    APPL_TRACE_DEBUG("%s: conn_id: %d, trans id: %d",
                                    __func__, conn_id, op.trans_id);
    BTA_GATTS_SendRsp(conn_id, op.trans_id, op.status, op.rsp_value);
    osi_free(op.rsp_value);
    gatts_ops.pop_front();
  }
}

void GattsOpsQueue::Clean(uint16_t conn_id) {
  APPL_TRACE_DEBUG("%s: conn_id=0x%x", __func__, conn_id);
  gatts_op_queue.erase(conn_id);
  gatts_op_queue_executing.erase(conn_id);
  congestion_queue[conn_id] = false;
}

void GattsOpsQueue::SendNotification(uint16_t conn_id,
                                     uint16_t handle,
                                     std::vector<uint8_t> value,
                                     bool need_confirm) {
  APPL_TRACE_DEBUG("%s: conn_id: %d, attr handle: %d",
                                    __func__, conn_id, handle);
  gatts_op_queue[conn_id].push_back({.type = GATT_NOTIFY,
                                     .attr_id = handle,
                                     .value = value,
                                     .need_confirm = need_confirm});
  gatts_execute_next_op(conn_id);
}

void GattsOpsQueue::SendResponse(uint16_t conn_id,
                                 uint32_t trans_id,
                                 uint8_t status,
                                 tGATTS_RSP* rsp_value) {
  APPL_TRACE_DEBUG("%s: conn_id: %d, trans id: %d",
                                    __func__, conn_id, trans_id);
  tGATTS_RSP* p_rsp = (tGATTS_RSP*)osi_calloc(sizeof(tGATTS_RSP));
  if (p_rsp != NULL) {
    memcpy(p_rsp, rsp_value, sizeof(tGATTS_RSP));
    gatts_op_queue[conn_id].push_back({.type = GATT_SEND_RESPONSE,
                                      .trans_id = trans_id,
                                      .status = status,
                                      .rsp_value = p_rsp});
    gatts_execute_next_op(conn_id);
  } else {
    APPL_TRACE_DEBUG("%s: mem alloc failed for gatt response", __func__);
  }
}

void GattsOpsQueue::NotificationCallback(uint16_t conn_id,
                                         uint8_t status) {
  APPL_TRACE_DEBUG("%s: conn_id: %d, status: %d",
                                    __func__, conn_id, status);
  auto map_ptr = gatts_op_queue.find(conn_id);
  if (map_ptr == gatts_op_queue.end() || map_ptr->second.empty()) {
    APPL_TRACE_DEBUG("%s: no more operations queued for conn_id %d",
                                            __func__, conn_id);
    return;
  }
  APPL_TRACE_DEBUG("%s:dequeue entry & mark not-executing", __func__);
  std::list<gatts_operation>& gatts_ops = map_ptr->second;
  gatts_operation op = gatts_ops.front();
  gatts_ops.pop_front();
  mark_as_not_executing(conn_id);
  gatts_execute_next_op(conn_id);
}

void GattsOpsQueue::CongestionCallback(uint16_t conn_id, bool congested) {
  APPL_TRACE_DEBUG("%s: conn_id: %d, congested: %d",
                                         __func__, conn_id, congested);

  congestion_queue[conn_id] = congested;
  if (!congested) {
    gatts_execute_next_op(conn_id);
  }
}

}
} // namespace ends
