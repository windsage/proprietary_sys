/**
*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*
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

package vendor.qti.hardware.spu;
@VintfStability
interface ISPUManager {
  int checkHealth(out vendor.qti.hardware.spu.ISPUManager.data_st data);
  int clearEventNotifier();
  int getImageType();
  vendor.qti.hardware.spu.ISPComClient getSPComClient(in String name);
  int getSPComMaxChannelNameLength();
  int getSPComMaxMessageSize();
  vendor.qti.hardware.spu.ISPComServer getSPComServer(in String name);
  boolean isAppLoaded(in String name);
  int loadApp(in String name, in android.hardware.common.Ashmem data, in int size);
  int resetSpu();
  int setEventNotifier(in vendor.qti.hardware.spu.ISPUNotifier notifier);
  int sysDataRead(in int id, in int arg1, in int arg2, out vendor.qti.hardware.spu.ISPUManager.data_st data);
  int sysParamRead(in int id, in int arg1, in int arg2, out vendor.qti.hardware.spu.ISPUManager.data_st value);
  int sysParamWrite(in int id, in int arg1, in int arg2);
  boolean waitForLinkUp(in int timeoutMs);
  int waitForSpuReady(in int timeoutSec);
  parcelable data_st {
    byte[] data;
    int size;
  }
}
