/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.PresResInstanceInfo;

@VintfStability
parcelable PresResInfo {
    /**
     * SIP or TEL URI of the contact
     * the type of URI is dependent on the network.
     */
    String resUri;
    /**
     * network preferred contact name
     *  this is usually the phone number
     */
    String displayName;
    PresResInstanceInfo instanceInfo;
}
