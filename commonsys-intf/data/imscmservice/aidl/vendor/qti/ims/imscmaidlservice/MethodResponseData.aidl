/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;

/*
 * Method Response data
 */
@VintfStability
parcelable MethodResponseData {
    KeyValuePairStringType[] data;
}
