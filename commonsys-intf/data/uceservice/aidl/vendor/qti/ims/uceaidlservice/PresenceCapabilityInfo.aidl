/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.CapabilityInfo;

@VintfStability
parcelable PresenceCapabilityInfo {
    /**
     * SIP or TEL URI of the contact
     * the type of URI is dependent on the network.
     */
    String contactUri;
    /*
     * RCS capabilites of the contact
     */
    CapabilityInfo capInfo;
}
