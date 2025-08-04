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

package vendor.qti.hardware.fingerprint;
@VintfStability
interface IQfpExtendedSession {
  String getConfigValue(in String name);
  vendor.qti.hardware.fingerprint.Status setConfigValue(in String name, in String value);
  vendor.qti.hardware.fingerprint.EnrollRecord getEnrollRecord(in String enrolleeId);
  byte[] processRequest(in byte[] request);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal asyncProcessRequest(in byte[] request);
  vendor.qti.hardware.fingerprint.Status registerDebugDataCallback(in vendor.qti.hardware.fingerprint.IQfpExtendedSessionCallback cb);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal captureImage(in int mode, in int forceLUT);
  byte[] openFramework();
  vendor.qti.hardware.fingerprint.Status closeFramework();
  byte[] handlePVCmd(in byte[] request);
  vendor.qti.hardware.fingerprint.Status toggleIRQ(in int irq, in int enable);
  byte[] calibTest(in byte[] request);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal captureAirImage(in int version, in int flags);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal captureHRM(in int version);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal tieUntie(in int version, in String enrolleeId, in vendor.qti.hardware.fingerprint.AuthToken token, in int type, in int tieId, in int numFingers, in int[] fingerIds);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal retrieveTiedFingerList(in int version, in String enrolleeId);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal updateQfsSetting(in int version, in vendor.qti.hardware.fingerprint.AuthToken token, in int settingId, in int valueLen, in byte[] value);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal retrieveQfsSetting(in int version, in int settingId, in long challengeId);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal startForceSense(in int version, in int mode);
  vendor.qti.hardware.fingerprint.Status notifyForceSense(in int version, in int cmd, in int subcmd, in byte[] data);
  vendor.qti.hardware.fingerprint.Status setTouchInfo(in int version, in int numEvents, in int eventLength, in byte[] events);
  vendor.qti.hardware.fingerprint.IQfpExtendedCancellationSignal startStylus(in int version);
  vendor.qti.hardware.fingerprint.Status notifyStylus(in int version, in int cmd, in byte[] data);
}
