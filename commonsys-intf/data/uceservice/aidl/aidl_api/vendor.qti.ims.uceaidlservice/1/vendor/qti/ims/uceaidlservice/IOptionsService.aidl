/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
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

package vendor.qti.ims.uceaidlservice;
@VintfStability
interface IOptionsService {
  vendor.qti.ims.uceaidlservice.UceStatus addListener(in long serviceHandle, in vendor.qti.ims.uceaidlservice.IOptionsListener listener, in long clientHandle);
  vendor.qti.ims.uceaidlservice.UceStatus getCapabilityInfo(in long serviceHandle, in long userData);
  vendor.qti.ims.uceaidlservice.UceStatus getContactCapability(in long serviceHandle, in String remoteUri, in long userData);
  vendor.qti.ims.uceaidlservice.UceStatus getContactListCapability(in long serviceHandle, in String[] remoteUriList, in long userData);
  vendor.qti.ims.uceaidlservice.UceStatus removeListener(in long serviceHandle, in long clientHandle);
  vendor.qti.ims.uceaidlservice.UceStatus responseIncomingOptions(in long serviceHandle, in int tId, in char sipResonseCode, in String reasonPhrase, in String reasonHeader, in vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo capInfo, in byte isContactinBlackList);
  vendor.qti.ims.uceaidlservice.UceStatus setCapabilityInfo(in long serviceHandle, in vendor.qti.ims.uceaidlservice.CapabilityInfo capinfo, in long userData);
}
