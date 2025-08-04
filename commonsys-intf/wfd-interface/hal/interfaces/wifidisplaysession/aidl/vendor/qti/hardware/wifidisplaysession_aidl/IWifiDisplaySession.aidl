/* ==============================================================================
 * IWifiDisplaySession.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.IWifiDisplaySessionCb;
import vendor.qti.hardware.wifidisplaysession_aidl.WFDRuntimeCommands;
import vendor.qti.hardware.wifidisplaysession_aidl.DeviceInfo;

@VintfStability
interface IWifiDisplaySession {
    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow long to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void createSession(in IWifiDisplaySessionCb cb, in long clientData, in DeviceInfo local,
        in DeviceInfo peer, out int[] status, out long[] instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int destroysession(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int disableUIBCSession(in long instanceId, in int sessId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int enableUIBCSession(in long instanceId, in int sessId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int executeRuntimeCmd(in long instanceId, in WFDRuntimeCommands cmd);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getCommonResolutionBitmap(in long instanceId, out int[] status, out long[] bitmap);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getConfigItems(in long instanceId, out int[] status, out String[] cfgItems);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getNegotiatedResolutionBitmap(in long instanceId, out int[] status, out long[] bitmap);

    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    // FIXME: AIDL does not allow int to be an out parameter.
    // Move it to return, or add it to a Parcelable.
    void getSessionResolution(in long instanceId, out int[] status, out int[] width, out int[] height);

    // Adding return type to method instead of out param int status since there is only one return value.
    int negotiateRtpTransportType(in long instanceId, in int TransportType, in int BufferLenMs,
        in int portNum);

    // Adding return type to method instead of out param int status since there is only one return value.
    int pause(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int play(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int queryTCPSupport(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int sendAvFormatChange(in long instanceId, in int codec, in int profile, in int level,
        in int formatType, in long value, in int[] resParams);

    // Adding return type to method instead of out param int status since there is only one return value.
    int sendIDRRequest(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setAVMode(in long instanceId, in int mode);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setBitrateValue(in long instanceId, in int value);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setDecoderLatencyValue(in long instanceId, in int latency);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setRtpTransportType(in long instanceId, in int transportType);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setSessionResolution(in long instanceId, in int formatType, in long value,
        in int[] resParams);

    // Adding return type to method instead of out param int status since there is only one return value.
    int setUIBCSession(in long instanceId, in int sessId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int standby(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int start(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int startUIBCDataPath(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int stop(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int stopUIBCDataPath(in long instanceId);

    // Adding return type to method instead of out param int status since there is only one return value.
    int tcpPlaybackControlCmd(in long instanceId, in int cmdType, in int cmdVal);

    // Adding return type to method instead of out param int status since there is only one return value.
    int teardown(in long instanceId, in boolean isRTSP);
}
