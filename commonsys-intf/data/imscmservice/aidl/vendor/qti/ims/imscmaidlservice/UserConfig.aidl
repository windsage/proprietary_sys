/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;

/*
 * User Config Data type
 */
@VintfStability
parcelable UserConfig {
    KeyValuePairStringType[] data;
}
