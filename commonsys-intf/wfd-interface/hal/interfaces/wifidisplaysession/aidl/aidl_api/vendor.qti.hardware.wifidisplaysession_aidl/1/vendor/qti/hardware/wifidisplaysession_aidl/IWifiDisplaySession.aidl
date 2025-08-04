/* ==============================================================================
 * IWifiDisplaySession.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.qti.hardware.wifidisplaysession_aidl;
@VintfStability
interface IWifiDisplaySession {
  void createSession(in vendor.qti.hardware.wifidisplaysession_aidl.IWifiDisplaySessionCb cb, in long clientData, in vendor.qti.hardware.wifidisplaysession_aidl.DeviceInfo local, in vendor.qti.hardware.wifidisplaysession_aidl.DeviceInfo peer, out int[] status, out long[] instanceId);
  int destroysession(in long instanceId);
  int disableUIBCSession(in long instanceId, in int sessId);
  int enableUIBCSession(in long instanceId, in int sessId);
  int executeRuntimeCmd(in long instanceId, in vendor.qti.hardware.wifidisplaysession_aidl.WFDRuntimeCommands cmd);
  void getCommonResolutionBitmap(in long instanceId, out int[] status, out long[] bitmap);
  void getConfigItems(in long instanceId, out int[] status, out String[] cfgItems);
  void getNegotiatedResolutionBitmap(in long instanceId, out int[] status, out long[] bitmap);
  void getSessionResolution(in long instanceId, out int[] status, out int[] width, out int[] height);
  int negotiateRtpTransportType(in long instanceId, in int TransportType, in int BufferLenMs, in int portNum);
  int pause(in long instanceId);
  int play(in long instanceId);
  int queryTCPSupport(in long instanceId);
  int sendAvFormatChange(in long instanceId, in int codec, in int profile, in int level, in int formatType, in long value, in int[] resParams);
  int sendIDRRequest(in long instanceId);
  int setAVMode(in long instanceId, in int mode);
  int setBitrateValue(in long instanceId, in int value);
  int setDecoderLatencyValue(in long instanceId, in int latency);
  int setRtpTransportType(in long instanceId, in int transportType);
  int setSessionResolution(in long instanceId, in int formatType, in long value, in int[] resParams);
  int setUIBCSession(in long instanceId, in int sessId);
  int standby(in long instanceId);
  int start(in long instanceId);
  int startUIBCDataPath(in long instanceId);
  int stop(in long instanceId);
  int stopUIBCDataPath(in long instanceId);
  int tcpPlaybackControlCmd(in long instanceId, in int cmdType, in int cmdVal);
  int teardown(in long instanceId, in boolean isRTSP);
}
