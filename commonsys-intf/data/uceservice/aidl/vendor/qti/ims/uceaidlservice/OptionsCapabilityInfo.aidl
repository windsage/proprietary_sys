/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.CapabilityInfo;

@VintfStability
parcelable OptionsCapabilityInfo {
    /**
     * the SDP(Session Description Protocol) packet.
     */
    String sdp;
    /**
     * RCS features supported by remote party
     */
    CapabilityInfo capInfo;
}
