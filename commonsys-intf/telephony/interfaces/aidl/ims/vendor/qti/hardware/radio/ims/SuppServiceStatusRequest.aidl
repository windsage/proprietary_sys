/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CbNumListInfo;
import vendor.qti.hardware.radio.ims.FacilityType;
import vendor.qti.hardware.radio.ims.SuppSvcOperationType;


/**
 * Data structure is used to store information related to supplemetary
 * service status request.
 * Telephony/lower layers will process SuppServiceStatusRequest based on
 * individual default parameters.
 */
@VintfStability
parcelable SuppServiceStatusRequest {
    SuppSvcOperationType operationType = SuppSvcOperationType.INVALID;
    FacilityType facilityType = FacilityType.INVALID;
    CbNumListInfo cbNumListInfo;
    String password;
    boolean expectMore;
}
