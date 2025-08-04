/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ServiceClassStatus;

/**
 * Data structure to store call waiting related information.
 * Lower layers will process CallWaitingInfo when CallWaitingInfo.serviceStatus
 * is not ServiceClassStatus.INVALID and CallWaitingInfo.serviceClass is not
 * INT32_MAX.
 */
@VintfStability
parcelable CallWaitingInfo {
    ServiceClassStatus serviceStatus = ServiceClassStatus.INVALID;
    /*
    * Values are as defined in spec 7.11 (+CCFC) and 7.4 (CLCK)
    */
    int serviceClass;
}
