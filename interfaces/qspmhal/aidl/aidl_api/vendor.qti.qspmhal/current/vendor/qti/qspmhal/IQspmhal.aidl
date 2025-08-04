/*
  * Copyright (c) 2023 Qualcomm Technologies, Inc.
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

package vendor.qti.qspmhal;
@VintfStability
interface IQspmhal {
  vendor.qti.qspmhal.ProfData getGpuProf(in long pid);
  vendor.qti.qspmhal.ProfData getCpuProf(in String pkg_name);
  void setAppInfoH(in long pid, in String pkg_name, in String pkg_ver);
  void setAppProfile(in vendor.qti.qspmhal.ParcelableMemory prof_buf, in long size, in String prof_name, in vendor.qti.qspmhal.Command cmd);
}
