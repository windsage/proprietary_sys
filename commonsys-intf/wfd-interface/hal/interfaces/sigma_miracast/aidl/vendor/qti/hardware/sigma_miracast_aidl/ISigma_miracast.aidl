/*==============================================================================
 *  @file ISigma_miracast.aidl
 *
 *  @par DESCRIPTION:
 *       AIDL HAL API Defination
 *
 *
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ==============================================================================*/

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.sigma_miracast_aidl;

@VintfStability
interface ISigma_miracast {
    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow String to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void connect_go_start_wfd(in String cmd_string, in String peer_ip, in int rtsp_port,
        in int dev_type, in String session_id, out int[] status, out String[] rtsp_session_id);

    // Adding return type to method instead of out param int status since there is only one return value.
    int dev_exec_action(in String cmd);

    // Adding return type to method instead of out param int status since there is only one return value.
    int dev_send_frame(in String cmd);

    // Adding return type to method instead of out param int status since there is only one return value.
    int sta_generate_event(in String cmd_string);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow String to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void sta_preset_testparameters(in String cmd, out int[] status, out String[] respBuf);

    // Adding return type to method instead of out param int status since there is only one return value.
    int sta_reset_default(in String cmd);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow String to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void start_wfd_connection(in String cmd_string, in String peer_ip, in int rtsp_port,
        in int dev_type, in String session_id, out int[] status, out String[] rtsp_session_id);
}
