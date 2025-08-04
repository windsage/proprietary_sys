/* ==============================================================================
 * Uibc_event_parms.aidl
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
parcelable Uibc_event_parms {
  vendor.qti.hardware.wifidisplaysession_aidl.Uibc_touch_event_parms touch_event;
  vendor.qti.hardware.wifidisplaysession_aidl.Uibc_key_event_parms key_event;
  vendor.qti.hardware.wifidisplaysession_aidl.Uibc_zoom_event_parms zoom_event;
  vendor.qti.hardware.wifidisplaysession_aidl.Uibc_scroll_event_parms scroll_event;
  vendor.qti.hardware.wifidisplaysession_aidl.Uibc_rotate_event_parms rotate_event;
}
