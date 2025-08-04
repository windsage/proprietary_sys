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

package vendor.qti.hardware.hexlp;
@Backing(type="int") @VintfStability
enum HexlpPortFormat {
  HEXLP_PORT_FORMAT_INVALID = 0,
  HEXLP_PORT_FORMAT_RGB565,
  HEXLP_PORT_FORMAT_ARGB8565,
  HEXLP_PORT_FORMAT_RGBA5658,
  HEXLP_PORT_FORMAT_RGB888,
  HEXLP_PORT_FORMAT_ARGB8888,
  HEXLP_PORT_FORMAT_RGBA8888,
  HEXLP_PORT_FORMAT_XRGB8888,
  HEXLP_PORT_FORMAT_RGBX8888,
  HEXLP_PORT_FORMAT_RGB555,
  HEXLP_PORT_FORMAT_ARGB1555,
  HEXLP_PORT_FORMAT_RGBA5551,
  HEXLP_PORT_FORMAT_XRGB1555,
  HEXLP_PORT_FORMAT_RGBX5551,
  HEXLP_PORT_FORMAT_ARGB2101010,
  HEXLP_PORT_FORMAT_XRGB2101010,
  HEXLP_PORT_FORMAT_RGBA1010102,
  HEXLP_PORT_FORMAT_RGBX1010102,
  HEXLP_PORT_FORMAT_HALFFLOATFP16,
  HEXLP_PORT_FORMAT_BGR565,
  HEXLP_PORT_FORMAT_ABGR8565,
  HEXLP_PORT_FORMAT_BGRA5658,
  HEXLP_PORT_FORMAT_BGR888,
  HEXLP_PORT_FORMAT_ABGR8888,
  HEXLP_PORT_FORMAT_BGRA8888,
  HEXLP_PORT_FORMAT_BGRX8888,
  HEXLP_PORT_FORMAT_XBGR8888,
  HEXLP_PORT_FORMAT_BGR555,
  HEXLP_PORT_FORMAT_ABGR1555,
  HEXLP_PORT_FORMAT_BGRA5551,
  HEXLP_PORT_FORMAT_XBGR1555,
  HEXLP_PORT_FORMAT_BGRX5551,
  HEXLP_PORT_FORMAT_ABGR2101010,
  HEXLP_PORT_FORMAT_XBGR2101010,
  HEXLP_PORT_FORMAT_BGRA1010102,
  HEXLP_PORT_FORMAT_BGRX1010102,
  HEXLP_PORT_FORMAT_UBWC_RGBA8888,
  HEXLP_PORT_FORMAT_UBWC_RGBA1010102,
  HEXLP_PORT_FORMAT_UBWC_RGBA16161616F,
  HEXLP_PORT_FORMAT_UBWC_RGB565,
  HEXLP_PORT_FORMAT_UBWC_RGB8_PLANAR,
  HEXLP_PORT_FORMAT_UBWC_RGB10_PLANAR,
  HEXLP_PORT_FORMAT_YU12,
  HEXLP_PORT_FORMAT_YV12,
  HEXLP_PORT_FORMAT_NV12,
  HEXLP_PORT_FORMAT_NV21,
  HEXLP_PORT_FORMAT_PD8,
  HEXLP_PORT_FORMAT_P010,
  HEXLP_PORT_FORMAT_TP10,
  HEXLP_PORT_FORMAT_PD10,
  HEXLP_PORT_FORMAT_P016,
  HEXLP_PORT_FORMAT_TILED_P016,
  HEXLP_PORT_FORMAT_UBWC_NV12,
  HEXLP_PORT_FORMAT_UBWC_NV12_INTERLACE,
  HEXLP_PORT_FORMAT_UBWC_TP10,
  HEXLP_PORT_FORMAT_UBWC_NV124R,
  HEXLP_PORT_FORMAT_UBWC_P010,
  HEXLP_PORT_FORMAT_UBWC_P016,
  HEXLP_PORT_FORMAT_UBWC_TBAYER10_2R,
  HEXLP_PORT_FORMAT_CUSTOM,
  HEXLP_PORT_FORMAT_MAX,
}
