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

#if AHIM_ENABLED
void btif_register_cb();
void btif_acm_handle_event(uint16_t event, char* p_param);
bool btif_acm_source_start_session(const RawAddress& peer_address);
bool btif_acm_source_end_session(const RawAddress& peer_address);
bool btif_acm_source_restart_session(const RawAddress& old_peer_address,
                                      const RawAddress& new_peer_address);
void btif_acm_source_command_ack(tA2DP_CTRL_CMD cmd, tA2DP_CTRL_ACK status);
void btif_acm_source_on_stopped(tA2DP_CTRL_ACK status);
void btif_acm_source_on_suspended(tA2DP_CTRL_ACK status);
void btif_acm_source_direction_on_suspended(tA2DP_CTRL_ACK status, uint8_t direction);
bool btif_acm_on_started(tA2DP_CTRL_ACK status);
bt_status_t btif_acm_source_setup_codec();
bool btif_acm_update_sink_latency_change(uint16_t sink_latency);
void btif_acm_send_vs_cmd(const RawAddress& bd_addr, uint16_t content_type);

#endif
