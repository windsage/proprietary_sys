/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.KeyValuePairBufferType;
import vendor.qti.ims.imscmaidlservice.KeyValuePairStringType;

/*
 * outgoing Message Data Type
 */
@VintfStability
parcelable OutgoingMessage {
    KeyValuePairStringType[] data;
    KeyValuePairBufferType[] bufferData;
}
