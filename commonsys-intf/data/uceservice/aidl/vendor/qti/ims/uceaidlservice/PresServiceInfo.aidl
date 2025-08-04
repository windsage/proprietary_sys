/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.MediaInfoType;

@VintfStability
parcelable PresServiceInfo {
    /**
     * media capability for a RCS feature
     */
    MediaInfoType mediaCap;
    /**
     * Service ID for Rcs Feature.
     * Format as per GSMA RCS 5.3 documentation
     */
    String serviceId;
    /**
     * Service description for Rcs Feature.
     * Format as per GSMA RCS 5.3 documentation
     */
    String serviceDesc;
    /**
     * Service version for Rcs Feature.
     * Format as per GSMA RCS 5.3 documentation
     */
    String serviceVer;
}
