/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
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

package vendor.qti.snapdragonServices.qape;
@VintfStability
interface IQape {
  int setPkg(in String pkgName);
  int boostCpu(in int boostVal);
  int boostGpu(in int boostVal);
  int hintLowLatency(in int tid);
  int hintHighCpuutil(in int tid);
  int hintLowCpuutil(in int tid);
  int hintThreadPipeline(in int[] tids);
  int releaseThreadHints(in int category, in int tid);
  int getGpuHeadroom(in int pastDuration, in boolean average);
  int getScenarioSupport(in String scenarioId);
  int startScenario(in String scenarioId);
  int stopScenario(in String scenarioId);
  int registerThermalZoneCallback(in vendor.qti.snapdragonServices.qape.IThermalZoneCallback callback);
  int unregisterThermalZoneCallback(in vendor.qti.snapdragonServices.qape.IThermalZoneCallback callback);
  int queryThermalHeadroomStatus();
  int getPipelineThreadMaxNumber();
  int setPipelineNumber(in int number);
  int resetPipelineNumber();
  int setDesiredContentRate(in vendor.qti.snapdragonServices.qape.SupportedFrameRate contentRate);
  int getCpuHeadroom(in vendor.qti.snapdragonServices.qape.CpuHeadroomType type, in int cpuCoreClusterNo, in int duration);
  int appGetData(in int gameID, in int cmdType, inout double[] value);
  int appSet(in int gameID, in int cmdType);
  int appSetConfig(in int gameID, in int cmdType, in String[] key, in double[] value);
  int appSetData(in int gameID, in int cmdType, in double[] value);
  int oemRegisterCallback(in vendor.qti.snapdragonServices.qape.IQapeCallback callback, in int clientId);
  int oemUnregisterCallback(in vendor.qti.snapdragonServices.qape.IQapeCallback callback, in int clientId);
  int oemSetData(in int gameID, in int cmdType, in double[] value);
}
