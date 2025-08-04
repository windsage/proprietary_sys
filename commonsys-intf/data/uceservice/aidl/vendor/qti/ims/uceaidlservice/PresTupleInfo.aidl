/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

@VintfStability
parcelable PresTupleInfo {
    /**
     * RCS feature tag provided by network.
     * Format of featureTag described in
     * GSMA RCS 5.3 documentation
     */
    String featureTag;
    /**
     * SIP or TEL URI of the contact
     * the type of URI is dependent on the network.
     */
    String contactUri;
    /**
     * Wallclock time of format type NTP (Network Time Protocol)
     */
    String timestamp;
    /**
     * version of featureTag used
     */
    String ftVersion;
}
