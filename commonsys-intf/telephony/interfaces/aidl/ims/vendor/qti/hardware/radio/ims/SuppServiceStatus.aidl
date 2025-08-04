/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CbNumListInfo;
import vendor.qti.hardware.radio.ims.FacilityType;
import vendor.qti.hardware.radio.ims.ServiceClassProvisionStatus;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

@VintfStability
parcelable SuppServiceStatus {
    ServiceClassStatus status = ServiceClassStatus.INVALID;
    ServiceClassProvisionStatus provisionStatus = ServiceClassProvisionStatus.INVALID;
    /*
     * values are of type enum FacilityType
     */
    FacilityType facilityType = FacilityType.INVALID;
    /*
     * Deprecated. Please use errorDetails field
     */
    String failureCause;
    /*
     * used by FACILITY_BS_MT for query operation
     */
    CbNumListInfo[] cbNumListInfo;
    boolean hasErrorDetails;
    SipErrorInfo errorDetails;
    boolean isPasswordRequired = true;
}

