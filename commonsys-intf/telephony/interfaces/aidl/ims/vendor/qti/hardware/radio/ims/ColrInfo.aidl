/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.ServiceClassProvisionStatus;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

/**
 * Lower layers will process ColrInfo if IpPresentation is not IpPresentation#INVALID.
 */
@VintfStability
parcelable ColrInfo {
    ServiceClassStatus status = ServiceClassStatus.INVALID;
    ServiceClassProvisionStatus provisionStatus = ServiceClassProvisionStatus.INVALID;
    IpPresentation presentation = IpPresentation.INVALID;
    SipErrorInfo errorDetails;
}
