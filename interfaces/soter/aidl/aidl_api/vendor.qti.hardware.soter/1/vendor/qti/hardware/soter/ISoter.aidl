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

package vendor.qti.hardware.soter;
@VintfStability
interface ISoter {
  vendor.qti.hardware.soter.SoterErrorCode exportAskPublicKey(in int uid, out vendor.qti.hardware.soter.SoterBufferReturn buf);
  vendor.qti.hardware.soter.SoterErrorCode exportAttkPublicKey(out vendor.qti.hardware.soter.SoterBufferReturn buf);
  vendor.qti.hardware.soter.SoterErrorCode exportAuthKeyPublicKey(in int uid, in String name, out vendor.qti.hardware.soter.SoterBufferReturn buf);
  vendor.qti.hardware.soter.SoterErrorCode finishSign(in long session, out vendor.qti.hardware.soter.SoterBufferReturn buf);
  vendor.qti.hardware.soter.SoterErrorCode generateAskKeyPair(in int uid);
  vendor.qti.hardware.soter.SoterErrorCode generateAttkKeyPair(in byte copyNum);
  vendor.qti.hardware.soter.SoterErrorCode generateAuthKeyPair(in int uid, in String name);
  vendor.qti.hardware.soter.SoterErrorCode getDeviceId(out vendor.qti.hardware.soter.SoterBufferReturn buf);
  vendor.qti.hardware.soter.SoterErrorCode hasAskAlready(in int uid);
  vendor.qti.hardware.soter.SoterErrorCode hasAuthKey(in int uid, in String name);
  vendor.qti.hardware.soter.SoterInitReturn initSign(in int uid, in String name, in String challenge);
  vendor.qti.hardware.soter.SoterErrorCode removeAllUidKey(in int uid);
  vendor.qti.hardware.soter.SoterErrorCode removeAuthKey(in int uid, in String name);
  vendor.qti.hardware.soter.SoterErrorCode verifyAttkKeyPair();
}
