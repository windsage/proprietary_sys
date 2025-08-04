/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
package vendor.qti.ims.imscmaidlservice;

/*
 * Configuration Data Type
 */
@VintfStability
@Backing(type="int")
enum ConfigType {
    USER_CONFIG,
    DEVICE_CONFIG,
    AUTO_CONFIG,
}
