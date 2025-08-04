/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */// FIXME: license file, or use the -l option to generate the files with the header.
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

package vendor.qti.hardware.factory;
@VintfStability
interface IFactory {
  boolean chargerEnable(in boolean enable);
  vendor.qti.hardware.factory.FactoryResult delegate(in String cmd, in String value);
  vendor.qti.hardware.factory.IResultType dirListFileNameToFile(in String path, in String name);
  boolean enterShipMode();
  vendor.qti.hardware.factory.IResultType eraseAllFiles(in String path);
  vendor.qti.hardware.factory.FactoryResult getSmbStatus();
  vendor.qti.hardware.factory.ReadFileResult readFile(in String path, in vendor.qti.hardware.factory.ReadFileReq req);
  vendor.qti.hardware.factory.FactoryResult runApp(in String name, in String params, in boolean isStart);
  boolean wifiEnable(in boolean enable);
  vendor.qti.hardware.factory.IResultType writeFile(in String path, in vendor.qti.hardware.factory.WriteFileReq req);
}
