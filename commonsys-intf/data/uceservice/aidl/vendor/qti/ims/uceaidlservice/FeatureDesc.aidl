/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

/**
 * RCS (Rich Client Suite) Feature description,
 * specifies a specific feature and version, as specified in GSMA RCS 7.0.
 */
@VintfStability
parcelable FeatureDesc {
    /**
     * RCS feature tag provided by network.
     * Format of featureTag described in GSMA RCS 5.3 documentation
     */
    String featureTag;
    /**
     * RCS Feature version string.
     * Format of version desribed in GSMA RCC.07 v11.0
     * Example: +g.gsma.rcs.botversion="#=1,#=2"
     * Version string may be null for features not supporting version in tag
     */
    String version;
}
