/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;

/*
 * Device Config Data Type
 */
@VintfStability
parcelable DeviceConfig {
    KeyValuePairStringType[] data;
}
