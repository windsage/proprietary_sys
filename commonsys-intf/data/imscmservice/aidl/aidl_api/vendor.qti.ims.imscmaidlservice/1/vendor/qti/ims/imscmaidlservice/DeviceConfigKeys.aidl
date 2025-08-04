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
@Backing(type="int") @VintfStability
enum DeviceConfigKeys {
  UEBehindNAT = 2001,
  IpSecEnabled = 2002,
  CompactFormEnabled = 2003,
  KeepAliveEnableStatus = 2004,
  GruuEnabled = 2005,
  StrSipOutBoundProxyName = 2006,
  SipOutBoundProxyPort = 2007,
  PCSCFClientPort = 2008,
  PCSCFServerPort = 2009,
  ArrAuthChallenge = 2010,
  ArrNC = 2011,
  ServiceRoute = 2012,
  SecurityVerify = 2013,
  PCSCFOldSAClientPort = 2014,
  TCPThresholdValue = 2015,
  PANI = 2016,
  PATH = 2017,
  UriUserPart = 2018,
  PLANI = 2019,
  PPA = 2020,
  PIDENTIFIER = 2021,
}
