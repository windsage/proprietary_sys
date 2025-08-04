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

package vendor.qti.ims.imscmaidlservice;
@VintfStability
interface IImsCmService {
  vendor.qti.ims.imscmaidlservice.StatusCode InitializeService(in String iccId, in vendor.qti.ims.imscmaidlservice.IImsCmServiceListener cmListener, in int userData, out vendor.qti.ims.imscmaidlservice.ServiceListenerToken listenerId);
  vendor.qti.ims.imscmaidlservice.StatusCode addListener(in long connectionManager, in vendor.qti.ims.imscmaidlservice.IImsCmServiceListener cmListener, out vendor.qti.ims.imscmaidlservice.ServiceListenerToken listenerId);
  vendor.qti.ims.imscmaidlservice.StatusCode closeConnection(in long connectionManager, in long connectionHandle);
  vendor.qti.ims.imscmaidlservice.StatusCode closeService(in long connectionManager);
  void createConnection(in long connectionManager, in vendor.qti.ims.imscmaidlservice.IImsCMConnectionListener cmConnListener, in String uriStr, out vendor.qti.ims.imscmaidlservice.ConnectionInfo connInfo);
  vendor.qti.ims.imscmaidlservice.StatusCode getConfiguration(in long connectionManager, in vendor.qti.ims.imscmaidlservice.ConfigType configType, in int userdata);
  vendor.qti.ims.imscmaidlservice.StatusCode methodResponse(in long connectionManager, in vendor.qti.ims.imscmaidlservice.MethodResponseData data, in int userdata);
  vendor.qti.ims.imscmaidlservice.StatusCode removeListener(in long connectionManager, in long listenerId);
  vendor.qti.ims.imscmaidlservice.StatusCode triggerACSRequest(in long connectionManager, in vendor.qti.ims.imscmaidlservice.AutoconfigTriggerReason autoConfigReasonType, in int userdata);
  vendor.qti.ims.imscmaidlservice.StatusCode triggerDeRegistration(in long connectionManager, in int userdata);
  vendor.qti.ims.imscmaidlservice.StatusCode triggerRegistration(in long connectionManager, in int userdata);
}
